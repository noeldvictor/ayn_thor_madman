# Thread and cluster affinity: four forks do it, two do not, and three hits were guest code

**Goal: close the "thread and cluster affinity not surveyed in any fork" item.**

Session 2026-08-23 00:30. This matters more here than on most devices: the Thor
is 1+4+3, the guides for those cores **contradict each other**, and xenia already
measured guest threads pinned to the A510s while the Cortex-X3 sat idle.

**Result: the naive search was wrong in two different ways at once, and the
corrected answer is that two forks set no host affinity at all.**

---

## The naive search, and why it lied

Searching for `sched_setaffinity`, `CPU_SET`, `cpu_set_t` returned seven forks.
**Three of those hits are guest code and one is a vendored sample app.**

| Hit | What it actually is |
| --- | --- |
| Cemu `Cafe/OS/libs/coreinit/coreinit_Thread.cpp` | **the Wii U guest OS thread API.** Guest-side |
| azahar `core/hle/kernel/svc.cpp` | **a 3DS guest kernel syscall.** Guest-side |
| rpcsx `kernel/orbis/src/sys/sys_cpuset.cpp` | **guest kernel emulation.** Guest-side |
| melonDS `app/src/main/cpp/oboe/apps/OboeTester/...` | **inside vendored Oboe's test app.** Not melonDS code |

**This is the rule `CLAUDE.md` already states, hit live:** *separate host-side
from guest-side before claiming a technique transfers.* An emulator implements
the guest's affinity API **as a feature**, so a search for affinity finds the
emulated console's scheduler, not the host's.

**And it is the fifth time today a search needed correcting.** The pattern is
now: filename misses, category name misses, and **guest code masquerades as
host code**.

## The corrected picture

| Fork | Host-side affinity | Host-side priority |
| --- | --- | --- |
| **xenia** | `base/threading_posix.cc`, `a64_backend.cc` | `apu/audio_system.cc`, `base/threading_posix.cc` |
| **ARMSX2** | `common/Linux/LnxThreads.cpp` | `common/Linux/LnxThreads.cpp` |
| **eden** | `common/thread.cpp` | `common/thread.cpp` |
| **rpcsx** | `rpcs3/util/Thread.cpp` | `sys_p1003_1b.cpp`, `sys_resource.cpp` |
| **melonDS** | **none** | `MelonInstance.cpp` (priority only) |
| **Vita3K** | **none** | not found |
| Cemu, azahar | **none found host-side** | not found |

**xenia is the only fork that sets affinity from inside its JIT backend**
(`a64_backend.cc`), which is consistent with it being the fork that measured the
problem.

---

## The finding: two forks leave placement entirely to the kernel

**melonDS sets thread priority but never affinity. Vita3K sets neither.**

On this device that is not neutral. xenia's own research recorded guest threads
running on the **2.0 GHz A510 cores while the 3.2 GHz Cortex-X3 sat idle**. A
fork that never expresses a preference gets whatever the Android scheduler
decides, and the scheduler does not know which thread is the emulation thread.

**melonDS is the case worth naming**, because it is the one fork that already
tuned its compiler for the X3 — `-mtune=cortex-x3`, with the reasoning written
in its `CMakeLists.txt`. **It schedules its code generation for the prime core
and then does not ask for the prime core.**

That is not a contradiction anyone chose; it is two decisions made in different
files by different people. **It is exactly the kind of gap a fleet-wide view
exists to find.**

---

## It resolves the apparent contradiction in `CLAUDE.md`

The document records two findings that look opposed:

- **xenia**: guest threads were pinned to the little cores while the X3 idled.
- **rpcsx**: keeps the **full** core mask deliberately, because restricting the
  process drags Java, audio and compiler threads onto the emulation cores.

**Both are host-side, both are true, and they are about different scopes.**
xenia's is about **per-thread** placement; rpcsx's is about the **process** mask.
The policy already written in `THOR_TARGET.md` — place the hot guest thread,
do not restrict the whole process — is the correct reading of both, and this
survey confirms the two forks are not in conflict.

---

## What the shared scheduler must own

- **Per-thread placement**, expressed as a role rather than a core number:
  emulation, render submit, audio, JIT compile, present.
- **The process mask left alone.**
- **Never two vector-heavy threads on a paired A510**, which share a VPU and an
  L2.
- **A backend does not set its own affinity.** It declares its threads and their
  roles; the shared layer places them. This belongs in the contract and is not
  there yet.

## Not measured, and not read

- **No placement was measured on the device.** Every claim above is about what
  the source calls, not about where a thread ran.
- **What each fork's affinity call actually asks for** was not read. Knowing
  `sched_setaffinity` is called does not say which cores it names, and the
  answer decides whether the fork is helping or hurting on this SoC.
- Cemu and azahar returned nothing host-side. Given the record, **treat that as
  unread rather than absent.**
