# The verification debt after a second day of borrowing

**Goal: I updated this file once mid-session and then quoted another dozen
figures from XenDroid, ARMSX2 and Cemu. Bring it current.**

**No device. One file.**

## Six borrowed figures added

**All from the second half of the session, none measured here:**

| Figure | Whose | The qualification that matters |
| --- | --- | --- |
| **~187 Hz click train at a 5.3 ms block** | XenDroid | **arithmetic in a comment**, not a measurement |
| **a 32,728 Hz, 1,962-frame AudioTrack** | azahar | **1,962 is probably a SUBSTITUTED value** — my suspicion, unverified |
| **eighteen quirk bits; `XgKickHack` breaks Tri-ace** | ARMSX2 | **read, not tested** — no title was run either way |
| **~21M calls/sec = 85% of all guest calls** | xenia | its profile, its title |
| **Cemu reduces memory for ~15 Lego titles** | Cemu | **why** those titles need it is not in the code |
| **Turnip push delivery broken on Adreno 6xx** | XenDroid | no reproduction, no titles, **and a GPU this device is not** |

## Three argued-only rows added, two of them mine

- **"A quirk taxonomy shipped as data would work"** — **no fork does it**, and the
  argument is by analogy with cheats and GameThor's fixes. **Analogy is not
  evidence.**
- **"xenia's `--linux-arm64` cross build succeeds"** — **the toolchain is tested
  and the build is not.**
- **"the PPC harness runs under qemu-user with threads"** — **a ten-line static
  program is not xenia.**

**The last two are the honest residue of a thread I spent an hour narrowing.**
Each step was real and the conclusion is still unproven, **and the debt file is
where that distinction survives after the excitement of the narrowing.**

## What the update says about the day

**Every figure quoted into `CLAUDE.md` today came from another fork.** That is
the correct outcome for a device-free session and it is also a risk: **six months
from now the provenance lives here or nowhere.**

> **The rule this file states — "delete the row when it reaches `measured`" —
> has still deleted nothing.** Two sessions, no device, so nothing could.
> **A debt list that only grows stops being read**, and this one is growing.

**That is not an argument for pruning it.** It is an argument that **the next
useful session is a measuring one**, and the file now says which measurements
would retire the most rows.

## Limits

- **Not exhaustive.** It holds figures quoted in decisions, not every number
  read.
- **Nothing was verified.** The update records provenance only.
- **The 1,962-frame suspicion is mine and is marked as mine** — azahar does not
  claim it.

## Files

- `VERIFICATION_DEBT.md`
