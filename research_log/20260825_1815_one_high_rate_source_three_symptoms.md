# One high-rate source, three symptoms, and two writers for one D-pad

**Goal: finish XenDroid's input cluster. Five commits over two days, and this
project is gamepad-first by decision.**

**Device-free: five commit diffs. No device used.**

## The axis flood has a THIRD symptom, and it is the worst

**Already recorded**: analog hardware never reports exactly zero, so an
equality-based de-duplication never matched — **every sample crossed into
native**, and **each took the global critical region.**

**The third symptom, from the keystroke queue:**

> *"**Transitions only:** analog axes re-send every motion sample, **which would
> keep `GetKeystroke` from ever reaching EMPTY.**"*

**The guest drains keystrokes by polling until the queue reports EMPTY.** A queue
fed by a continuous source **never reports EMPTY**, so the guest polls forever.

> **One root cause — a control emitting continuously at rest — with three
> unrelated symptoms: JNI traffic, global-lock contention, and a guest protocol
> that cannot terminate.**

**The third is the one that would have been debugged in the wrong place.** It
presents as a guest hang in a keystroke loop, and the cause is an analog stick.

**The rule this repo now carries** — *quantise the input, or the check is
decoration* — **was written from the first two symptoms.** It is worth knowing
that the same defect reaches a completely different subsystem.

## The keystroke queue protocol, worth taking whole

> *"**One keystroke per call, lowest index first, clearing only its bit**; the
> guest drains the rest by polling until EMPTY."*

**The bug it replaced discarded all but the last.** A guest keystroke API is a
**queue**, and returning only the most recent loses characters — **text entry
silently drops input.**

**Three properties in one sentence**: one per call, deterministic order, and
**clearing only the bit that was delivered.** A shared input layer serving seven
guests needs exactly this shape, and it is not obvious from the API name.

## Two writers for one D-pad

> *"Hat D-pad state, **edge-detected**: the hat only releases what IT pressed, so
> **it can't clobber a D-pad held via real `KEYCODE_DPAD_*` key events.**"*

**A gamepad reports its D-pad BOTH as a hat axis and as key events.** A naive hat
handler that sets the D-pad on movement and clears it on centre **cancels a press
that arrived through the key path.**

> **Two sources writing one piece of state, with no owner.** That is the same
> class as the two defaults found in our own contract this morning — the settings
> screen reading `SettingSpec.default` while the resolver read `defaults()`.
> **Third instance of the second-writer shape in two days.**

**The remedy generalises: release only what you pressed.** Each writer tracks its
own contribution and withdraws only that, so the two compose instead of racing.

## Two smaller ones that are the same rule twice

**An early return.** *"No early return: sticks/triggers must still process while
a hat is held."* **A control-flow shortcut in one input branch silently disabled
every other input** for as long as the D-pad was held.

**And float equality against hardware, again.** *"Hat axes -> D-pad;
**thresholds, not `==±1f`** (some pads are inexact)."*

> **That is the THIRD and FOURTH instance of comparing a hardware float for exact
> equality** in this fleet — after the resting-stick deadzone and ARMSX2's
> animated-texture hash. **A pad that reports 0.999 is not at the edge, and a
> stick at rest is not at zero.**

## What this means for the shell

**`app/shell/` has no input layer**, so all five are requirements recorded before
the code exists — the cheap end. **When it is written:**

- **Quantise before de-duplicating.** A deadzone is a correctness requirement,
  not only a feel one.
- **Enqueue on transitions only**, never on every sample.
- **Deliver one queued event per poll**, lowest index first, clearing only that
  bit.
- **Two sources for one logical control release only what they pressed.**
- **Thresholds, never equality, on any hardware-derived float.**
- **No early return in an input branch.**

## Limits

- **Five commit diffs, one fork, comments read rather than full implementations.**
  Nothing built or run, no device.
- **The `GetKeystroke` protocol is the Xbox 360's.** Whether other guests drain
  by polling to EMPTY was not checked, and the shape may not generalise.
- **No claim about the other forks' input paths**, which were not examined.
- **The "third symptom" framing is mine.** XenDroid fixed three things; it does
  not itself say they share a root cause, though its own comment names the axis
  re-send as the reason for the transitions-only change.

## Sources

- XenDroid, five input commits dated 2026-07-24 and 2026-07-25
- `research_log/20260825_1235_a_resting_controller_that_never_stopped_talking.md`
