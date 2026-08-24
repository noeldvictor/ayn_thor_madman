# Pause existing is not pause working: three defects in one 18-line commit

**Goal: `AGENT_LOOP.md` rests entirely on pause being reliable, and its evidence
for pause is a file count. XenDroid has a commit titled "stop pausing from
hanging when the guest is behind on audio". Read it.**

**Device-free: one commit diff. No device used.**

## Three defects, one subsystem, 18 lines

### 1. A pause request invisible while the guest is busy

> *"A **wait-any reports the LOWEST signalled handle** and the client semaphores
> sit **below** `shutdown_event_`, so **a pause request is invisible while audio
> flows.** Test the flag rather than infer it from the handle."*

**A wait-any returns the lowest-indexed signalled handle.** Put a frequently
signalled handle below your control handle and **the control handle is never
reported while the busy one keeps firing.**

**This is the one that matters most here.** The agent loop pauses **during**
guest activity — that is the whole mechanism. **A defect that hides a pause
request while the guest is busy fails precisely when the loop needs it**, and it
fails by continuing rather than by erroring, so nothing reports a problem.

> **Never infer a control state from which handle a wait-any returned. Test the
> flag.**

### 2. Stop while paused deadlocks

> *"A worker parked on `resume_event_` **never sees `worker_running_` go
> false.**"*

The fix is three lines: `if (paused_) { Resume(); }` before joining.

**The loop pauses constantly**, so **every stop, backend switch and error path
happens while paused.** This repo already records **xenia's save-state deadlock**
as a blocker on the whole measurement plan — same family, different subsystem.

### 3. The pause flag was not atomic

`bool paused_` became `std::atomic<bool> paused_`, with the reason in a comment:
*"The worker's exit from a pause depends on observing this."*

**A plain flag written by a control thread and read by a worker may never be
observed**, because nothing forces the read.

## What this changes in `AGENT_LOOP.md`

**Its control-surface table reads `pause / resume | all seven forks — ARMSX2 35
files, eden 34, xenia 24, azahar 14, melonDS 11, Vita3K 4, Cemu 2`.**

> **That is a PRESENCE census counted from files that mention pause. It cannot
> see any of these three defects.**

**This is the repo's own rule turned on its own document**: *"a capability
recorded from a listing is a hypothesis."* **Seven rows of file counts are seven
hypotheses**, and the document that depends on them most is the one that treated
them as settled.

**A conformance statement now sits beside the table:** a backend's pause works
when **a pause requested during heavy guest activity takes effect**, **a stop
issued while paused completes**, and **the flag is atomic.**

## What is NOT claimed

- **No fork was audited for these three.** The defects are XenDroid's, in its
  audio system, found by reading one commit. **Whether any packed-binary backend
  has them is unknown.**
- **The file counts are not wrong**, they are answering a different question.
- **This does not make the agent loop unworkable** — XenDroid's fix is 18 lines
  and each defect has an obvious remedy. **It makes "pause exists" insufficient
  as evidence.**

## Limits

- **One commit, one fork, one subsystem.** Nothing built, nothing run.
- **The audio system is not the emulator's main pause path**, so the same three
  defects need not exist in the CPU or GPU pause. **They are a shape to check
  for, not a finding about any backend.**
- **The three conformance conditions are mine**, derived from the three defects,
  and no backend has been checked against them.

## Sources

- XenDroid, `[APU] Stop pausing from hanging when the guest is behind on audio`
- `shared_layer/AGENT_LOOP.md`
