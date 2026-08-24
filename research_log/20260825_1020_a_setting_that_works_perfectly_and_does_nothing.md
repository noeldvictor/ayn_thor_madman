# A setting that works perfectly and does nothing, because the guest ignores it

**Goal: two XenDroid commits looked like a `DID_IT_APPLY` instance when I listed
their subjects. Check.**

**Device-free: two commit diffs. No device used.**

## The story, in two commits

**Before.** The app exposed `internal_display_resolution`, described as:

> *"Resolution the console reports to games; supported titles render at it, **not
> guaranteed for all**"*

**A person reads that and reasonably concludes it raises the resolution.**

**After.** The description was rewritten:

> *"**Not a scaler:** it only tells the game what the TV is. **Most titles render
> at a fixed size and ignore it, so the picture usually will not change.** For
> higher detail use **Resolution scale** under GPU"*

**And the control that actually works — `draw_resolution_scale_x` / `_y` — was
not exposed at all until the second commit added it**, along with 13 lines in
`xboxkrnl_video.cc`.

> **So the app offered a control that mostly does nothing, and withheld the one
> that works.**

## Why this is a mechanism the other fourteen do not cover

**Nothing here is broken.** The host writes the value, passes it to the guest,
and the guest receives it. **Every layer behaves correctly.**

> **The guest is simply free to disregard it — and most titles do.**

**The fourteen mechanisms already catalogued are all failures in the HOST
stack**: a value that never reaches the code, a gate that is off, a dispatcher
that never fires, a coupled setting, a substituted value. **This one reaches the
guest intact and the guest declines.**

**That makes it undetectable by every technique this project uses.** A persisted-
config audit finds it set. A probe at the call site sees it applied. A rejection
counter counts zero rejections. **The only evidence is the picture not
changing.**

## The mitigations are editorial, and all three are worth taking

1. **Say what the setting is NOT.** *"Not a scaler."* Three words that prevent
   the whole misunderstanding.
2. **Name the setting that DOES work, in the description of the one that does
   not.** *"For higher detail use Resolution scale under GPU."* **When a control
   is commonly mistaken for another, point at the other one.**
3. **Expose the control that works.** The first two are worthless without it.

**And a fourth, from the same diff, which answers a bug found yesterday:**

> *"**Whole steps only, and it cannot go below 1x.**"*

**The constraint is stated in the copy rather than left to the widget to clamp
silently** — which is exactly the coercion defect that produced this project's
schema validator, answered in prose instead of in a guard. **Both are needed:
the guard stops the wrong value, the copy stops the wrong expectation.**

## Where this bites this project specifically

**[Foundation](../CLAUDE.md) point 4 says a person should not have to study to
play a game, and names RetroArch as the anti-pattern.**

> **A settings screen full of options qualified as "supported titles" honour this
> IS that anti-pattern.** Every such option costs the person a trial, and the
> trial is inconclusive because a null result is indistinguishable from an
> unsupported title.

**This project will have many of them**, because every backend passes settings to
a guest that may ignore them. **The contract should let a backend say so.**
`SettingSpec` has `liveChangeable` and `scope`; **it has nothing for "the guest
may ignore this"**, and a screen cannot warn about what it cannot see.

**Recorded as a contract gap, not implemented.** The phase says prefer a
reversible decision, and the right shape is unclear: it could be a flag, or it
could be per-title knowledge the backend only has once a game is loaded.

## Limits

- **Two commit diffs and their descriptions.** Nothing built, nothing run, no
  device.
- **"Most titles ignore it" is XenDroid's claim**, not a measurement, and it is
  about the Xbox 360 specifically. **How the equivalent behaves on the other
  seven guests is unknown and was not checked.**
- **The 13 lines in `xboxkrnl_video.cc` were not read**, so what the working
  control does at the kernel boundary is unexamined.
- **No claim that our own shell has an instance** — it has no backend behind it
  yet, so it cannot.

## Sources

- XenDroid `4351ecd3b`, `37ec6058a`
- `shared_layer/DID_IT_APPLY.md`, mechanism 15
