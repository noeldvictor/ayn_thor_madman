# Second extraction: the hotkey layer

**Goal: extract the input feature that matters on a device with physical
buttons.**

No device. No fork modified.

## Why hotkeys and not the overlay

**A virtual gamepad is near dead weight on the Thor.** Hotkeys are the opposite:
they are driven by the real buttons, and **"one hotkey set works on every system,
always" is a stated requirement**, not a convenience. It is the named RetroArch
failure this project exists to fix — each core does its own thing.

**Owned outright, unlike the pipeline cache.** There is no native half to write,
because the app owns the hotkey layer by requirement: **a backend never defines a
hotkey and is only told what happened.**

## What was taken from ARMSX2

**The action set and the capture interaction.** ARMSX2 split hotkeys out of its
Pad tab so they are easy to find — **this project's own usability complaint,
fixed inside the fleet before this repo existed.**

**And four learned lessons, each now a test.**

### 1. Binding capture may not use a modal dialog

**A shipped bug with a version number.** ARMSX2's comment:

> instead of a focus-stealing `AlertDialog` whose separate window **swallowed
> controller keys before Compose's `onPreviewKeyEvent` could see them** (the
> **2.6.0 "can't remap buttons" bug**)

**Capture belongs in the screen that already has focus.** `HotkeyPolicy` states
it as a constant so the rule is checkable, not folklore.

**A second capture rule falls out of it:** while capture is armed, **the next
press is a binding, not an action** — otherwise binding "menu" opens the menu and
the binding never lands.

### 2. A hotkey has a kind

**Fast forward ships twice**, as a hold and as a toggle, and ARMSX2's pressure
modifier is explicitly *"handled as a HOLD ... not as a one-shot action like the
others"*.

**A single "on press" model cannot express either.** `HotkeyKind` is
`ONE_SHOT`, `HOLD`, `TOGGLE`.

### 3. An action can be unavailable, and must say why

**Slow motion is disabled under hardcore achievements** because slowdown is a
banned advantage, and ARMSX2 **shows a notice rather than ignoring the press.**

**Silence is the bug.** A hotkey that does nothing and says nothing is
indistinguishable from a broken binding — the same failure shape as the PINE
setting that moved and did nothing. **`HotkeyDenial` has six distinct values and
none of them is silence.**

### 4. Screenshot is bindable on purpose

ARMSX2's reason: a spare button, **instead of the Android system gesture, which
interrupts play.**

## What was built

**`app/shell` `Hotkeys.kt` with 15 tests. 125 tests pass in total.**

- **17 shared actions**, **3 binding kinds**, **6 distinct denials**
- `resolve()` — capture wins over everything, then binding, then availability
- `availability()` — no game running, hardcore ban, not applicable to this title
- `conflicts()` — **reported, not resolved.** A hold and a one-shot can
  legitimately share a button; the person decides
- **Bindings key on the enum name**, so the action set may be reordered — the
  opposite of the upscale-algorithm enum, which is persisted as an integer and
  is append-only forever

## State

**`OWNED.md` records it as owned outright, no fork converted.** The only
remaining work is fork conversion, which the standing rule reserves.

## Not done

- **No UI.** The binding screen is not built; the rule that it must not use a
  modal dialog is recorded, not implemented.
- **No persistence.** Bindings key on the enum name; where they are stored is
  the settings resolver's job and is not wired.
- **The default binding set is not chosen.** ARMSX2's defaults are per-key
  preferences, and a Thor default map should be picked deliberately.
- **`REWIND` is in the action set, and the shell has no rewind behind it.** The
  action exists because the requirement lists it; **whether any backend in the
  fleet supports rewind was not checked.**
