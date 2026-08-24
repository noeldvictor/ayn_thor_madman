# Moving the people who never chose

**Goal: `CLAUDE.md` records "a persisted value overrides a compiled default
forever" as a hazard with a measured cost, and XenDroid has the cure. `app/shell`
had neither. Build it.**

**No device. One new file, nine tests.**

## Why this one was worth building rather than recording

**The hazard is not hypothetical and its cost is measured.** xenia found **three
validated `rlwinm` fastpaths sitting false on a device whose code said true**,
forced them on for **+2.88% with 11 of 11 intervals favouring on**, and recorded
that *"every device number taken this session was on a handicapped baseline."*

> **Shipping a better default reaches only NEW installs unless something moves
> the people already holding the old one.**

**And this project will hit it harder than xenia did**, because it plans per-game
overrides on every setting — **a sparse override store is exactly where stale
defaults accumulate.**

## The mechanism, and the half that makes it safe

**XenDroid's spelling**: `UPDATE_from_string(turnip_debug, 2026, 7, 24, 12, "")`
— *at this stamp the default changed, and the OLD default was this value.*

**`DefaultChange(key, at, previousDefault)` plus a watermark.** The safe half:

> **A stored value that still equals the OLD default means the person never
> chose. Anything else means they did, and their choice is left alone.**

**Two ordering decisions, both pinned by tests:**

- **Changes apply oldest first**, whatever order they were declared in, because
  **a key whose default changed twice must walk both steps** or a user on the
  original value is stranded by the second change.
- **The watermark advances past changes that moved nothing**, so a later run does
  not re-examine them.

## The limit, pinned as a test rather than hidden

**The mechanism cannot distinguish a chosen value that HAPPENS to equal an old
default from a genuinely stale one.** A person who deliberately selected `v2`,
when `v2` was also the previous default, is moved to `v3`.

**`the_mechanism_cannot_tell_a_chosen_value_from_a_matching_old_default` asserts
that behaviour**, so it is a known property rather than a surprise. **The real
fix is to record WHETHER a value was chosen, not only what it is** — which is
ARMSX2's per-game "sticky once overridden" rule. **That rule turns out to be
already built for the per-game layer; see the correction below.** The limit
therefore applies to the GLOBAL layer only, where no record of choice exists.

## CORRECTED WITHIN THE HOUR, by reading the code next to it

**I wrote that the sticky-once-overridden rule was "recorded here and not yet
built". It IS built.** `SettingResolver.writeOverride` maintains a `pinned` set —
existing per-game keys plus whatever the caller just changed — and writes exactly
those into the per-game map.

> **So presence in the per-game map is PROOF the person chose, and that is a
> STRONGER signal than value equality.**

**Which makes the function as first written defective.** Run over a per-game map,
it would move a deliberate choice that happened to equal an old default — **which
is precisely the bug ARMSX2's pinning rule exists to prevent**, and which this
repo records as a shipped bug where a per-game cheat setting was silently lost.

**Fixed by making the layer explicit in the signature** — `migrateGlobal`, with
the reason in the doc comment — **and by adding `chosenKeys`, which is skipped
unconditionally.** The global layer has no record of choice, which is exactly why
it needs this and per-game does not.

**One behaviour worth stating: the watermark still advances for a skipped key**,
because the change WAS considered. A test asserts it.

## Result

**`DefaultMigrationTest`, 10 cases. Suite 246, 0 failures**, up from 236.

## What this is not

- **It is not the melonDS schema migration.** That one migrates a SHAPE — fields
  appearing, moving, changing type. **This migrates a VALUE.** A settings system
  needs both and `app/shell` still lacks the first.
- **Nothing calls it.** There is no persisted config yet, so it is exercised only
  by tests. **Third piece added this way today**; the structural version needs a
  config load path that does not exist.
- **The stamp is a sortable string compared as a string.** Simpler than a date
  type and correct as long as the format is fixed-width; **a mixed set of
  formats would sort wrongly and nothing checks that.**

## Files

- `app/shell/.../DefaultMigration.kt` — new
- `app/shell/.../DefaultMigrationTest.kt` — 9 cases
