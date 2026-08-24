# The present path spins on a 2017 desktop-driver workaround, and a failure that erases its own evidence

**Goal: read `spin.md`'s present-path section — the last part of that document
touching a subsystem this project plans to own.**

## A fourth instance of one shape

**The fork states the pattern itself, better than this repo has:**

> *"This is the same shape as everything else in this document, arrived at from a
> different direction: **a wait implemented as a spin because a timeout was tuned
> for different hardware.** `GETLLAR` spins because a percentage defaults to 100.
> `passive_lock` spun ten times too long because a backoff was sized for a 3 GHz
> timer. This spins because **a workaround for a 2017 AMD desktop driver was left
> unconditional.**"*

**Two things stack in `VKPresent.cpp`:**

- **Android already gets a timeout 100,000 times shorter than desktop** — `1 us`
  against `100 ms` — so it reaches the `VK_TIMEOUT` case far more readily.
- **That case then sets the timeout to ZERO and continues**, justified by a
  comment naming *"AMD Crimson 17.7.2"*.

**On Android the acquire goes through a `BufferQueue`**, so a zero timeout turns
*"wait for a buffer"* into *"ask for a buffer as fast as the CPU allows"* —
**37,000 iterations per second, burning a core in the flip path.**

> **A driver workaround with no driver condition is a timing constant tuned for
> hardware you are not running on.** The `#ifdef ANDROID` narrowed the *timeout*
> and left the *spin* unconditional.

## The diagnostic finding, which is the more transferable half

**24,459 log messages in 655 ms.**

> *"It also flooded the log buffer hard enough to **evict everything before it**,
> so **the failure mode destroyed its own diagnostic context** — the reason the
> earlier freeze could not be traced further back."*

**And it repeated:** re-reading the buffer later, *"the oldest surviving line was
again the storm's first message, having evicted the 20:28 fault entirely."*

**This repo has xenia's rule — capture before force-stopping or clearing.** This
is a different mechanism: **the failure does the clearing.** No operator error is
involved, and the evidence for the *cause* is gone by the time anyone looks.

> **A log-storming failure erases what preceded it.** Raise the buffer, or filter
> at the source, **before** reproducing one.

**And it is why the surrounding investigation stalled**, which is the cost made
concrete rather than asserted.

## The fleet check: it does not propagate

**Searched all seven packed-binary forks** for `vkAcquireNextImageKHR` and its
wrappers, then read the timeout each passes.

| Fork | timeout passed |
| --- | --- |
| **xenia, Cemu, azahar, Vita3K** | **`UINT64_MAX`** — block until an image is available |
| ARMSX2, melonDS, eden | **not resolved** by this search; their call sites did not match the three-line context grep |

**Four of seven demonstrably block, which is the correct and power-friendly
behaviour, and none of the seven was found spinning.** **rpcsx's is inherited
from RPCS3 and rpcsx is out of the packed binary**, so **this defect does not
arrive in the shared present path from any fork that will be in it.**

**Recorded as a negative with its limit**: three forks were not resolved, so this
is "four confirmed correct, three unchecked", not "nobody spins".

## What the shared present path must inherit instead

`CLAUDE.md` lists *"one frame pacing and present path"* among the flows that
justify the packed binary, and `app/shell/FramePacing.kt` already carries a
`PRESENT_WAIT_TIMEOUT_NS` of 40 ms taken from Cemu.

**Two rules from this incident belong beside it:**

1. **Acquire blocks. It does not poll.** A zero timeout on a `BufferQueue` is a
   busy-wait against the compositor.
2. **A driver workaround carries the driver it is for.** ARMSX2's driver-profile
   database — extraction candidate 9 — is exactly the mechanism for this: a
   defect named, a workaround named, matched on **driverID and version**. *"AMD
   Crimson 17.7.2"* in a comment, applied unconditionally on an Adreno, is what
   that table exists to prevent.

## And a discipline note worth taking

> *"**Deliberately not changed yet.** The obvious fix is to keep a small nonzero
> timeout on Android... But this is the middle of an unresolved crash
> investigation, two reservation-path changes were just reverted, and **adding an
> untested change to the present path while the cause of a guest fault is unknown
> would make the next result unattributable.** Recorded now, changed after the
> crash question is settled."*

**That is the one-variable rule applied across TIME rather than within a run** —
and it is the same instinct as Vita3K's *"a planned attempt is a blocker"*, from
the other side: **do not open a second front while the first is unresolved.**

## Limits

- **Not reproduced.** rpcsx's logs, device and session.
- **Three forks' acquire timeouts were not resolved**, so the fleet claim is
  partial and says so.
- **The `BufferQueue` explanation is the fork's**, not verified here.
- **`spin.md` still has roughly 1,100 lines unread**, including the GPU fence
  poll and a section on a port that was thought missing and was not.

## Sources

- rpcsx `docs/arm64/spin.md:927-995`, `VKPresent.cpp`
- `app/shell/FramePacing.kt`, `shared_layer/OWNED.md` candidate 9
