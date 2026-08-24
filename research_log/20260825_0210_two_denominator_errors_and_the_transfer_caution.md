# Two denominator errors in one document, and the caution that belongs beside the propagation list

**Goal: continue `spin.md`, this time mapping its self-corrections FIRST — the
rule written an hour ago after missing one.**

**Five correction markers found before reading. The one at line 549 produced
three rules, and one of them is about the activity this whole project is built
on.**

## The incident

`passive_lock` turned out to be a **backoff-tuning problem rather than
contention**, and the obvious next move was the same fix on the far bigger
target: `GETLLAR`, at **82.5% of all emulator spin**.

**The evidence looked strong.** Instrumenting GETLLAR wait **episodes** against
busy-waits executed gave **0.834 spins per episode** — apparently a shorter wait
than `passive_lock`'s 1.24, and it seemed to explain the WFE result too.

**A graduated ladder went in and measured an exact no-op: 300.0 ticks per call,
unchanged to the decimal.**

> **The busy-wait is only reached once a switch decided at
> `getllar_spin_count == 4` is set. Every call therefore sees a count of at least
> 4, so a ladder keyed on that count can never select its short tiers.** The
> change **could not have done anything.**

## Rule 1: a no-op is a result, and a cheap one

> *"'300.0 ticks per call, unchanged' is unambiguous in a way that a small
> improvement would not have been — **had the ladder produced a 5% shift, it
> would have been tempting to keep it and never notice the denominator was
> wrong.**"*

**An exact no-op is MORE informative than a small win**, because a small win
invites you to keep it and stop investigating. **This repo's verdict vocabulary
has `FLAT` and treats it as a disappointment.** It is not: **a `FLAT` that is
exact is evidence the change never reached the code**, which is a different and
more useful statement than "the change did not help".

## Rule 2: instrument the thing you CHANGED, not only the thing you hoped to improve

**The tick count caught it.** A measurement of the outcome — frame time, cores
busy, power — would have shown nothing and been read as "the lever is small".
**A measurement of the mechanism showed the mechanism never ran.**

**That is `DID_IT_APPLY.md`'s rule reaching the experiment rather than the
setting**, and it composes with the positive-control rule: **prove the instrument
can return non-zero, AND prove the change can reach the code.**

## Rule 3, and it belongs beside the propagation list

> **"A fix that transfers from one site to another is a HYPOTHESIS, not a
> conclusion."**

`passive_lock` and `GETLLAR` looked like the same shape at the level of *"a
busy-wait whose measured wait seems shorter than its backoff"*. **They are not
the same shape: one spins immediately, the other spins only after a separate gate
has already decided the wait is durable.**

> **"The gate was visible in the source the whole time."**

**`CLAUDE.md`'s propagation list is fourteen rows of exactly this move** — take
this from that fork, give it to these. **This is the caution that belongs beside
it**, and the failure mode is precise: **shape similarity established at the
wrong level of description.** Two things can both be "a busy-wait with an
oversized backoff" and differ in whether the code is reachable at all.

**This repo already has the same lesson from another direction** — *separate
host-side from guest-side before claiming a technique transfers*, learned when
half an rpcs3 list died because Espresso has no VMX. **That rule is about the
GUEST. This one is about the SITE.**

## And the denominator error is the second in this document

**`0.834 spins per episode` conflated two populations.** Episodes count every
wait that *starts*; **only the minority surviving four iterations ever reach the
spin.** *"Spins per episode is not spins per SPINNING wait."*

**The other one, found an hour ago:** *"93% of spin is GETLLAR"* said nothing
about **how much of the total was spin**, and the sweep measured −2.9%.

> **Two ratios, one document, both wrong in the same way: a denominator that
> includes cases the change cannot reach.**

**The general form:** when a rate is used to justify a change, **check that the
denominator counts only cases the change can affect.** In one it was waits that
never reach the spin; in the other, CPU that was never spinning.

## Limits

- **Not reproduced.** rpcsx's instrumentation, device and title.
- **The reachability argument was read from the document, not from the source.**
  The gate at `getllar_spin_count == 4` was not verified in `SPUThread.cpp`.
- **Roughly 1,200 lines of `spin.md` remain unread**, including the present-path
  spin and the GPU fence poll.

## Sources

- rpcsx `docs/arm64/spin.md:533-580`
- `research_log/20260825_0030_park_versus_spin_and_a_desktop_default.md`
