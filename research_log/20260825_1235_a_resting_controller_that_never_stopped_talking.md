# A resting controller that never stopped talking, and a global lock in the input path

**Goal: keep reading XenDroid. Its *"keep controller input off the global
critical region"* commit is about a gamepad-first device, which is what this
project is.**

**Device-free: one commit diff. No device used.**

## Two bugs, and the second causes the first to matter

### 1. The de-duplication never de-duplicated

```kotlin
// Analog hardware never reports exactly 0; without this the drift
// crosses into native on every sample.
private const val AXIS_DEADZONE = 0.08f
// Snap to exactly zero so emitAxis' equality check can match.
val v = if (abs(raw) < AXIS_DEADZONE) 0f else raw
```

**`emitAxis` forwards a value only when it CHANGED.** Analog hardware never
reports exactly `0.0`, so a resting stick emits a slightly different value every
sample, **so the equality check never matched and every sample crossed into
native.**

> **A controller lying still on a table was generating continuous JNI traffic.**

**The de-duplication did not fail loudly. It silently stopped de-duplicating.**

### 2. And each of those samples took the GLOBAL lock

```cpp
-  auto global_lock = global_critical_region_.Acquire();
+  std::lock_guard<std::mutex> key_lock(key_status_mutex_);
```

with the reason written down:

> *"**Deliberately not the global critical region: analog axes push a sample per
> motion event** and nothing here needs its semantics."*

**A process-wide lock, taken per motion event.** Combined with bug 1, **a resting
controller was serialising the emulator's global critical region continuously.**

## The general rule, and this repo already had the other half

**`CLAUDE.md` records ARMSX2's requirement for a hash-stability heuristic**,
because *"animated textures re-hash every frame and will otherwise generate
unbounded work."*

> **That is the same bug.** A skip-if-unchanged test is defeated by any input
> that never exactly repeats.

| Instance | The check | What defeats it |
| --- | --- | --- |
| **ARMSX2 texture cache** | hash equality, to skip re-upscaling | **an animated texture's hash** |
| **XenDroid input** | value equality, to skip a JNI crossing | **analog drift at rest** |

**Both were recorded as one-off gotchas in their own forks. They are one rule:
quantise the input, or the check is decoration.** Other members of the family
are easy to name once it is stated — a timestamp, an accumulating float, a
frame-varying seed.

**The rule is now beside the texture entry in `CLAUDE.md`**, because that is
where somebody will next meet it.

## Why the lock half matters HERE specifically

**This project is gamepad-first by decision** — *"every screen must be drivable
without touching the glass"* — **so analog axes are continuous and unavoidable,
not an edge case.**

**And the shared-layer design multiplies the exposure.** `CLAUDE.md` specifies
**one scheduler, one memory budget owner, one device layer**. **A shared owner is
a shared lock**, and a path that fires per motion event reaching one of them
would serialise seven backends rather than one.

> **Choose lock scope by the CALL RATE of the path, not by which lock is
> nearest.** And XenDroid's comment is the standard to meet: it says which lock
> was rejected **and why the narrower one is sufficient.**

## What was NOT done

- **Our shell has no analog input path.** Searched
  `app/src/main/java/com/aynthor/shell/` for `axis`, `Axis`, `deadzone` and
  `analog`: nothing. `Hotkeys.kt` models actions, not axes. **So there is no code
  here to fix — only a requirement recorded before the code exists.**
- **The deadzone value 0.08 is XenDroid's** and is a feel decision as well as a
  correctness one. **Do not copy the number without saying it came from another
  device's tuning.**

## Limits

- **One commit, one fork.** Nothing built or run, no device.
- **The JNI cost is not measured** — neither by them in this commit nor here.
  **The argument is structural: a per-sample crossing plus a global lock at rest
  is wrong regardless of its size.**
- **No claim about the other forks' input paths.** They were not examined.

## Sources

- XenDroid, `[Android] Keep controller input off the global critical region`
- `CLAUDE.md`, the ARMSX2 texture-upscaling requirements
