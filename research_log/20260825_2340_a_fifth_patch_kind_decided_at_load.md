# A fifth patch kind, and its constraint is stronger than "needs a restart"

**Goal: XenDroid fixed an Ace Combat 6 audio deadlock with a new
`game_quirks.cc`. This repo's patch taxonomy has four kinds. Check whether this
is a fifth.**

**Device-free: one commit diff. No device used.**

## It is a fifth, and it fits none of the four

| Existing kind | Touches |
| --- | --- |
| content patch | the guest filesystem |
| code patch | guest instructions |
| file mod | game assets |
| host config fix | an env var, a launch arg, an INI |
| **game quirk** | **the BACKEND'S OWN SEMANTICS** |

**XenDroid's quirk switches its emulated kernel's auto-reset event to strict NT
hand-off semantics, for one title.** No guest bytes, no files, no host
configuration — **the emulator itself behaves differently.**

## The bug underneath is a real synchronisation subtlety

> *"NT auto-reset `SetEvent` semantics (hand-off mode): a `Set` releases one
> **ALREADY-WAITING** thread via a per-waiter FIFO, so **the setter can never
> reclaim its own signal**; with no waiter it latches one idempotent token."*

**A naive auto-reset event — signal a condition variable, first waiter wins —
lets the SETTER race back around and consume its own signal**, starving the
thread the signal was for. **On Windows that cannot happen.** The guest was
written against semantics the host implementation did not reproduce.

**And the fix carried its own hazard**: *"Signal first and drop its lock
(**ABBA/self-deadlock otherwise**), then wait through this object's own `Wait()`
so type-specific machinery is kept."* **A deadlock fix with a lock-ordering trap
inside it.**

## The constraint is the part that changes our design

> **"flipped per title at launch, BEFORE GUEST THREADS EXIST."**

**A quirk cannot be toggled mid-game at all**, because the semantics of objects
that threads are already waiting on would change underneath them.

> **`liveChangeable = false` means "needs a restart". A quirk means "must be
> DECIDED AT LOAD."** Those are different, and `SettingSpec` expresses only the
> first.

**`SettingScope` has `PER_GAME`, `PROMOTED` and `GLOBAL_ONLY`** — all about
*where a value is stored*. **Nothing expresses *when it may be decided*.**

**And a quirk is backend-declared by construction**: only the backend knows its
kernel has an auto-reset event, or a DSP, or a vector unit with a flush mode.
**So this is a per-backend extension, not a shared list** — the ownership test
from earlier today, arriving in the patch taxonomy.

## What I did not do

**Not implemented.** Adding a fourth `SettingScope` value or a `decidedAtLoad`
flag is a contract change, and **the right shape is not obvious**: a quirk may
not belong in the settings system at all, being closer to a launch parameter than
to a preference a person browses.

**Recorded so whoever designs the per-game screen meets it**, rather than
discovering that one class of fix cannot be represented.

## Limits

- **One commit diff.** Nothing built or run, no device.
- **The NT hand-off description is XenDroid's comment**, not verified against
  Windows documentation here.
- **Whether other backends need quirks of this kind is unknown.** The category is
  proposed from one instance; **a second would make it a pattern.**
- **No claim that the four-kind taxonomy was wrong** — it was incomplete, and it
  was built from forks that had not shipped this.

## Sources

- XenDroid, `[Kernel] Add per-title auto-reset event hand-off to fix Ace Combat 6
  audio deadlock`, `game_quirks.{cc,h}`
- `CLAUDE.md`, the patch-kind table
