# The WFE stampede, the syscall rule for parks, and 12% of drift from temperature alone

**Goal: finish `spin.md` with its last section, a review of ARMSX3 0.7 and of
whatcookie's *"what didn't make the cut"* — the author whose 8 Gen 2 numbers
`CLAUDE.md` already cites.**

**Six findings. Two change measurement rules here, and one is a structural
argument against a mechanism this repo currently records approvingly.**

## 1. Thermal drift is worth 12% on identical work

> *"**Thermal drift corrupts benchmarking**, and the author hit it across seasons.
> **This project hit the same thing inside one session: 12% for identical work as
> the device went from 30 C to 68 C.**"*

**`MEASUREMENT.md` says temperature proves a run happened, and to run fifteen
minutes or more when heat matters. It does not say what drift is WORTH.**

> **12% is larger than almost every effect queued in `DEVICE_QUEUE.md`.** An A/B
> whose two arms start 38 degrees apart is measuring temperature.

**This is why the preflight notes elsewhere in the fleet matter and looked
fussy**: ARMSX2's *"equal 40 C starts"*, xenia's *"ON was handicapped by a 4 C
hotter start, so the true effect is likely slightly larger"*, rpcsx's
*"both arms from a 34.7 C preflight"*. **Each is guarding a 12%-scale confound.**

## 2. The WFE park wakes EVERY waiting core — a stampede

**Arm, quoted directly by whatcookie:** the periodic event stream *"wakes up all
processors waiting in WFE at the same time which would **amplify contention**."*

> **One thread parking is a weak case for that. Six threads parking on the same
> event stream is exactly the case Arm warns about.**

**`CLAUDE.md` records the `SEVL`/`WFE` park as tier 1 — the best of three spin
tiers, reached independently by ARMSX2 and dynarmic — without this caveat.**
**It is still the best mechanism; the caveat is about HOW MANY threads use it.**

**And it matters here specifically**: every guest in this fleet is multi-core.
PS2 has EE, IOP and two VUs; Wii U has three; Switch has four; the 360 has six
hardware threads. **A shared spin policy that parks every guest thread on one
event stream is the amplification case, not the win case.**

**Two numbers agree with the ~100 µs period**, from different measurements: the
event stream fires about every **100 µs** on Linux and Android against about
**1 µs** on Apple; an armed `WFE` here parked for **72,024 ns**; and the wake
floor recorded earlier is **95.06 µs**. **A `WFE` park is a coarse ~100 µs
instrument on this device**, and `FEAT_WFxT` being absent means it cannot carry
its own timeout.

## 3. A park that costs a syscall loses

> *"It takes no exclusive monitor and **no syscall**, which is the interesting
> part: **every park measured here that traded a spin for a *syscall* lost.**"*

**That is a clean decision rule and this repo has nothing like it.** The tier
table says which primitive; this says which *class* of primitive is affordable.
**`SEVL`/`WFE` is in-band. A futex is not.**

## 4. The double `WFE`, and why

```c
__asm__ volatile("sevl \n wfe \n wfe" ::: "memory");
```

> **`SEVL` sets the local event so the FIRST `WFE` consumes it and the SECOND
> genuinely parks**, rather than returning at once on a stale event.

**`CLAUDE.md` records ARMSX2's `SEVL`/`WFE` + `LDAXR` form** — which parks on an
*address*. **This is the address-free variant**, for a thread with nothing to
watch. **Two shapes, two uses**, and the second is what the fleet's bare
`yield()` sites want.

## 5. A pre-spin that another fork learned the hard way

The call site spins **eight times** before parking, and the comment records why:
**`ouroboros420/rpcsx` parked bare there and had to revert it when the wake
latency cost frame-time smoothness.**

> **The pre-spin is not a tuning detail. It is a reverted experiment, encoded.**

**And it is the same shape as the break-even rule found earlier today**: parking
pays only when the expected remaining wait exceeds the wake floor, so a bare park
on a short wait is a regression. **Two forks, two routes, one conclusion.**

## 6. Two smaller ones worth keeping

- **SVE's `TBL` is "not naturally vector length agnostic"**, and the
  length-agnostic forms arrived in **SVE2.1**, on hardware implementing only
  128-bit vectors. **A further reason SVE stays unported, independent of this
  chip not exposing it** — this repo's argument was only ever "the SoC does not
  expose it" and "128-bit is NEON width anyway".
- **32 vector registers are rated the ARM64 advantage for SPU work, above AVX2.**
  Held next to rpcsx's own 10.1% spill traffic: **the spilling is the cost of 128
  SPU registers, not a shortage.** **That is `TRANSLATION.md`'s thesis in a
  second guest** — the Xenon's 128 VMX onto 32 NEON, and the SPU's 128 onto 32.

## And two forks this fleet has never looked at

ARMSX3 0.7 credits **`ouroboros420/rpcsx`** and **`rfandango/rpcsx`**. Between
them: a **persistent SPU object cache**, **GPU turbo with power and thermal
handling**, **RAM and VRAM budgeting**, a **Turnip ZCULL deadlock fix**, and
**ARM64 SPU checksum handling.**

**Three of those are subjects this project has open**: a persistent code cache is
the `PERSIST` operation; RAM and VRAM budgeting is the single memory-budget
owner; **a Turnip ZCULL deadlock fix is a defect for extraction candidate 9's
table.** **None is PS3-specific in shape**, and **rpcsx is GPL-2.0-only, so the
ideas cross and the code does not.**

## Limits

- **Every number is rpcsx's or whatcookie's.** Nothing reproduced, no device.
- **The 12% figure has no scene, title or metric attached** in the source line —
  only "identical work" across a 30 C to 68 C range. **Treat it as an order of
  magnitude, not a coefficient.**
- **The two credited forks were not opened.** Their existence is read from a
  commit message.
- **The stampede argument is Arm's own wording via whatcookie**, not a
  measurement anyone here has taken.

## Sources

- rpcsx `docs/arm64/spin.md:1413-1480`
- whatcookie, *"rpcs3 on arm64: what didn't make the cut"*
- `research_log/20260825_0030_park_versus_spin_and_a_desktop_default.md`
