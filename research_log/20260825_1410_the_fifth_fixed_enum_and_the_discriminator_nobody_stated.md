# The fifth fixed enum, and the discriminator the first four never stated

**Goal: an hour ago I found `AcceptedInput` was the FOURTH fixed enum in this
project derived from one guest's taxonomy — and I found it by accident. Look for
the fifth deliberately.**

**Device-free: an audit of our own two contracts. No device used.**

## Method

**Every `enum class` in `shared_layer/thor_backend.h` and
`app/shell/.../Backend.kt`, listed and read.** Seven in the header, six in the
Kotlin. **Thirteen enums, all opened.**

## The fifth, and it is worse than the fourth

```cpp
enum class ValidationError : uint8_t {
  kNone, kButtonOutOfRange, kMaxDigitsExceeded,
  kAtSignNotAllowed, kPercentNotAllowed, kBackslashNotAllowed,
  kProfanityNotAllowed, ...
```

> **`kAtSignNotAllowed`. `kPercentNotAllowed`. `kBackslashNotAllowed`.**
> **One console's software-keyboard rules, character by character, in the
> fleet-wide contract.**

**A Switch, Wii U or PS3 keyboard forbids a different set**, and the enum cannot
express *"`#` is not allowed here"* at all.

**It is worse than `AcceptedInput`**, whose five cases are at least abstract —
not-empty, not-blank. **These name literal characters.** The header even
described itself as *"superset of azahar's ValidationError"*, which is the
mistake stated as a feature.

## The discriminator, which the first four instances never produced

**Four previous rejections of this shape** — texture classes, filter lists, cheat
memory regions, `AcceptedInput` — **each argued case by case. None gave a test.**
The audit produces one, because the audit found enums that are FINE:

> **A fixed enum is correct when the HOST owns the concept, and wrong when the
> GUEST does.**

| Enum | Owner | Verdict |
| --- | --- | --- |
| `UploadResult` — `kDeclinedBudget`, `kDeclinedRateLimit`, `kDeclinedClassDisabled` | **ours**: our budget, our limiter, our class list | **correct** |
| `SettingScope`, `SettingSource`, `SettingType`, `LifecycleOp`, `Counter` | host concepts | **correct** |
| `AppletKind` | the shell must know which UI to draw | **correct** |
| `AcceptedInput` | guest-derived, **but shell-actionable** | **keep, plus raw guest flags** |
| **`ValidationError`** | **the guest, character by character** | **wrong** |

**That table is the useful output, more than the fix.** It says which enums to
leave alone, which this project has never said — every previous instance produced
a rejection and no boundary.

## The fix, and it is the shape already used twice

**Keep only what the shell can act on WITHOUT guest knowledge** — empty, blank,
max length, fixed length, max digits, button range. **All six are things the
shell already knows because it drew the dialog.**

**Everything else becomes `kGuestRejected`, carrying a `GuestRejection { code,
message }`**: the code opaque to the shell, the message what it displays.

> **This is exactly the resolution `CLAUDE.md` already took for BUTTON LABELS.**
> azahar's defaults are `"Ok"`, `"Cancel"` and **`"I Forgot"`** — a 3DS
> parental-controls button no generic host would predict — so **the guest
> supplies the strings and the host supplies the styling.**
>
> **A rejection message is the same kind of text.** The repo had the principle
> and had not applied it one field over.

**And it matches the cheat design**: a shared instruction set with a
backend-declared region namespace. **Three subsystems, one shape.**

## What this does not fix

- **`AcceptedInput` is still a fixed guest-derived enum**, kept deliberately with
  `guest_flags` beside it. **The audit did not resolve it, only bounded it.**
- **`AppletKind`'s three values may be too few.** Vita3K, eden and Cemu all have
  applets this project has not surveyed; three kinds is a guess that happens to
  be host-side, so the discriminator does not flag it. **Being on the right side
  of the test does not make it complete.**

## Limits

- **A design change to a header nothing links against.** It compiles under NDK
  29 clang++ at C++20 for `aarch64-linux-android33`, and that is all it proves.
- **The claim that other consoles forbid different characters is reasoning from
  the existence of different keyboards**, not from reading their APIs. **No
  guest's keyboard specification was opened.**
- **Thirteen enums audited, in two files.** Other contracts in this repo — the
  cheat spec, the patch spec, `GameData.kt` — were not audited for the same
  shape.

## Sources

- `shared_layer/thor_backend.h`
- `research_log/20260825_1320_a_blocking_applet_needs_a_canceller.md`
- `CLAUDE.md`, azahar applets: button labels come from the guest
