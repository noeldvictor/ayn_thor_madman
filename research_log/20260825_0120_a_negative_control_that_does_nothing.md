# A negative control that does nothing looks exactly like one that works

**Goal: apply the rule written twenty minutes ago — before quoting a section of a
long document, search that document for its own later corrections — to
everything else quoted from a long document today.**

**Four documents checked. Neither of the two hits invalidates what was written,
and both add something that was missing.**

## The check

| Document | Self-correction markers | Affects what I quoted? |
| --- | --- | --- |
| rpcsx `spin.md` | — | **yes, and it was found and corrected** |
| rpcsx `instruments.md` | 5 | **no** — the retracted number is not one I used |
| rpcsx `memory-model.md` | 6 | **no** — the superseded paragraph is at line 72, my section starts at 98 |
| rpcsx `thermal.md` | 0 | — |
| Cemu, azahar `AGENTS.md` | 0 | — |

## 1. The retraction I did not quote, whose LESSON this repo needs

I quoted **0.48 cores busy at 0.628 W, idle, emulator not running** — which
survives. **The withdrawn number is a ~1.78 W attribution of power to the
emulator**, and it was withdrawn for two independent reasons:

> *"It compared a 'running' against a 'stopped' arm, and the stop was
> `am force-stop net.rpcsx.thortest`. **That package does not exist**: the APK is
> named for its build variant, `thortest`, while the `applicationId` is
> **`net.rpcsx.easy`**. The force-stop **silently did nothing**, so both arms
> were the same configuration and the difference between them was whatever the
> device happened to be doing."*

**The second reason is one this repo already has** — an instantaneous wattage
with a 1.66 W noise floor around a claimed 1.78 W effect.

**The first is new and it is the sharper of the two:**

> **"A negative control that does nothing looks exactly like a negative control
> that works."** `am force-stop` on a package that is not installed **exits zero
> and prints nothing.**

**This project's own rule is "prove the instrument can return non-zero before
believing a zero".** This is the same disease **in the control arm**: the
instrument was fine and **the thing being switched off was never switched off.**

**And its remedy is concrete:** *"Check that the thing you turned off actually
went off — here, `pidof` against the real package name."*

**Directly actionable here.** `DEVICE_QUEUE.md` requires a fresh process per arm
and `CLAUDE.md` requires force-stopping your own package when finished. **Neither
says to verify the stop.** A wrong package name, a build-variant suffix, a flavour
suffix — `com.armsx2` against `com.armsx2.github` — and both arms are the same
configuration.

## 2. A superseded paragraph that qualifies today's `+lse` work

`memory-model.md:68` originally called finer-grained per-reservation locking
using LSE *"the highest-value ARM work left here"*. **Struck through, with the
correction beside it:**

> *"Locking is **already** per-reservation; `vm::reservation_lock` takes the
> 128-byte line's own word and each entry has its own cache line. **LSE was never
> the missing piece, because the cost is WHICH LINE an atomic touches rather than
> HOW IT IS SPELLED.**"*

**That is a reason for the FLAT prediction in `DEVICE_QUEUE.md` entry 25, where
there was previously only a prior.** The `+lse` question is about how an atomic
is *spelled* — `ldaddal` against a call to an outline helper. **For a contended
atomic the dominant cost is the cache line it touches**, which `+lse` does not
change.

**It does not make the entry pointless.** The outline dispatch is a real
call-plus-load-plus-branch on every atomic, contended or not, and **the uncontended
case is where it would show.** But it **sharpens the prediction**: if `+lse` moves
anything, it will be in atomic-dense uncontended code, **not** in the contended
guest-synchronisation paths that look most tempting.

**And the same correction records what the cost actually was:** one
`bit_test_set` on `g_range_lock_bits[1]`, **paid by `passive_lock`'s READERS
rather than by the writer**, measured at **17.5% of all emulator spin** — *"most
of which was then removed by fixing that site's backoff instead of redesigning
anything."*

> **A backoff constant beat a lock redesign.** Third instance today of a timing
> constant outperforming a structural change.

## What changed here

- `DEVICE_QUEUE.md` gate: **verify the stop**, with `pidof` against the real
  `applicationId`, not the APK or variant name.
- `MEASUREMENT.md`: the negative-control rule, and the which-line-not-how-spelled
  qualification.
- `DEVICE_QUEUE.md` entry 25's prediction gains its reason.

## Limits

- **Both findings are rpcsx's**, on its own device runs. Nothing reproduced.
- **The `applicationId` mismatch is that fork's**; whether any fork here has the
  same package-versus-variant gap **was not checked.**
- **The 17.5%-of-spin figure is from a wait profiler** whose limits this repo has
  already recorded: **it can only report sites it was told to instrument.**

## Sources

- rpcsx `docs/arm64/instruments.md:121-145`
- rpcsx `docs/arm64/memory-model.md:66-80`
