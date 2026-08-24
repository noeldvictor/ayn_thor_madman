# A tidying commit renamed every key it touched, and the crash was the lucky outcome

**Goal: two XenDroid commits looked like one story — a tidying change that broke
key lookups. Read them.**

**Device-free: two commit diffs. No device used.**

## The story

**Commit one, `5de5ff9cd`: *"Put settings in the TOML section their cvar is read
from."*** A correctness tidy-up. It moved **90 lines of `default_config.toml`**
and 16 lines of schema.

**Commit two, `fe05bc4be`: *"Fix the profile screens crashing on a moved settings
key."*** Immediately after.

**The mechanism, in their own comment:**

> **"A cvar's toml section can move, **which changes its schema key**; fall back
> rather than hard-cast."**

`XConfig|user_language` became `Console|user_language`. The screens did:

```kotlin
(SettingsSchema.byKey["XConfig|user_language"] as Setting.ListChoice).default.toInt()
```

**A hard cast on a map lookup that now returns null.**

## Lesson 1: if the key embeds the section, reorganising sections RENAMES keys

**`CLAUDE.md` says every setting needs a stable key, because a setting without
one cannot be overridden.** It does not say what threatens stability.

> **A composite key of `section|name` makes the section part of the identity.
> Tidying the sections is then a breaking rename**, and nothing about the tidy-up
> looks like a rename.

**And a crash was the LUCKY outcome.** Per-game overrides are stored **by key**.
A silent version of this bug is worse:

> **The override file holds `XConfig|user_language`, the schema now says
> `Console|user_language`, the resolver misses, and the user's saved setting
> silently reverts to the default.** Nothing throws. **That is the
> `DID_IT_APPLY` symptom, produced by a commit whose subject line is a
> tidy-up.**

**Our shell is already better here by construction, and it was not deliberate.**
`SettingSpec` carries `key` and `group` as **separate fields**, so moving a
setting between groups need not touch its key. **The invariant was implicit;
it is now written into the contract.**

## Lesson 2: a top-level `val` fails before anything can catch it

**The sharpest detail, and it is theirs:**

> **"Top-level vals, so a hard cast on a key whose toml section moved would throw
> **during class init, before anything can catch it**."**

**A hard cast in a top-level property initialiser fails at class
initialisation** — not at first use, and outside any `try`. The screen does not
degrade; it cannot be constructed.

**The fix moves the lookup into a function and uses `as?`**, so the failure
becomes a null at the point of use where a fallback can apply.

## Lesson 3: name the fallback, do not just supply one

```kotlin
?: 1     // en
?: 103   // United States
```

**A bare `?: 1` is a magic number that will be wrong later and unexplainable
now.**

## And the fix landed with a test

The same commit added **13 lines to `SettingsSchemaTest`** —
`keys_referenced_by_code_resolve_to_the_right_type`, whose comment is:

> *"Keys the app looks up by string with a hard cast, so **a section move that
> changes the key must not go unnoticed**."*

**The bug came first and the test came after, pinned to the exact keys the code
casts.** That is this project's *"a propagation lands with a test, or it does not
land"* rule applied to a bug fix — **and the test is cheap precisely because it
asserts about a schema rather than about behaviour.**

## What this does NOT establish

- **It does not show a rename ever silently orphaned an override in XenDroid.**
  Their failure was the crash. **The silent variant is my inference from where
  overrides are keyed**, and it was not verified in their code.
- **Our shell has no persisted overrides yet**, so it cannot have the bug today.
  The invariant is recorded before the storage exists, which is the cheap end.

## Limits

- **Two commit diffs.** Nothing built or run, no device.
- **The TOML move was not read in full** — 90 changed lines, and only the two
  profile keys were traced.
- **No claim about the other forks.** Whether any of them composes keys from a
  section was not checked.

## Sources

- XenDroid `5de5ff9cd`, `fe05bc4be`
- `app/shell/.../Backend.kt`, `SettingSpec`
