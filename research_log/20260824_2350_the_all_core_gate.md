# The all-core gate: azahar's answer to "one code layout cannot suit all four cores"

**Goal: mine azahar's 54 `Do not` invariants, named twice as an unmined
rejection ledger.**

**They are almost all hand-tuned AArch64 lowerings, each naming a rejected
alternative with a measurement — and three of them settle an open question in
`CLAUDE.md`.**

## The question this repo left open

`CLAUDE.md` records that the four Thor cores give **directly conflicting layout
advice** — the A715 wants branches concentrated where the X3 and A710 want them
spread, and the A510 wants at most one conditional branch per 16 bytes. Its
conclusion:

> *"**Tune the recompiler for the X3 and place hot code there.** The alternative
> is per-cluster codegen variants, which multiplies the code cache and the
> testing."*

**That answer depends on being able to place hot code**, and this repo's own
affinity survey found **two forks set no host affinity at all** and melonDS tunes
its compiler for the X3 while never asking for it.

## azahar's answer: neither. Require a candidate to win on EVERY core

**Its concept, named in its own words:** *"shorter dirty batches keep that same
route **because copy setup did not clear the all-core gate**."*

> **A lowering is accepted only if it wins on all four cores. One that loses on
> the A510 is rejected, however well it does on the X3.**

**That needs no affinity control, no per-cluster variants, and no code-cache
multiplication.** It costs candidate lowerings instead — which is the cheaper
currency.

## And the A510 rejects things the big cores like

**Two measured rejections, both from the same section:**

| Candidate | Result |
| --- | --- |
| **packed-float24 `TBL`** for the boolean-uniform path | *"despite **exact random equality**, it ran at about **0.52x–0.54x on A510**"* |
| **grouped-float24** batching | *"its **small-batch A510 timing was unstable or regressive**"* |

> **`TBL` at half speed on the little cores.** A lowering can be **correct**,
> **faster on the big cores**, and **half speed on the A510.**

**This repo already holds two adjacent measurements and did not have the third.**
xenia found `TBX2` costs about **2x** `TBL2` on this SoC and its fix measured
null; rpcsx measured BCAX at **2.02x on A510** for throughput and **0.94x on the
X3**. **Three forks, three table-lookup or wide-instruction results, and the
A510 is the discriminator in all three.**

**And the second row is the subtler one.** The A510's problem is not only that it
is slower — **its small-batch timing is *unstable*.** A candidate that measures
well on one small-batch run there may not reproduce, **so the gate has to be
about reproducibility and not only about the mean.**

## The labelling rule, applied three times in three consecutive entries

- *"Treat the **1.03x–1.83x** measured gain as **boolean-uniform-path work, not
  whole-game FPS or watts**."*
- *"Treat the **1.15x–14.84x** measurements as **uploader-kernel ratios, not
  whole-game FPS or watts**."*

> **Even a 14.84x is labelled a kernel ratio.** That is the discipline this repo
> calls `migration-credit`, applied to a microbenchmark rather than to a
> structural change — **and applied every single time, not just when the number
> is small enough to be embarrassing.**

## What to take

1. **Adopt the all-core gate as the codegen acceptance rule**, and replace *"tune
   for the X3 and place hot code there"* with it. **The placement answer needs
   affinity this fleet largely does not have; the gate needs none.**
2. **Add A510 instability to the gate**, not just A510 slowness. A candidate must
   be **reproducible** there, not merely acceptable once.
3. **Label every microbenchmark ratio as a kernel ratio at the point of
   measurement**, not at the point of quotation.

## Limits

- **azahar's numbers are its own**, on 3DS shader-uniform paths, and none was
  reproduced here.
- **"All-core gate" is read from one clause**; the mechanism behind it — how the
  gate is evaluated and where — was **not** read.
- **Roughly 40 of the 54 invariants remain unread.** They are dense AArch64
  lowering detail and most concern the 3DS shader JIT and dynarmic specifically.
- **The affinity claim about this fleet is from an earlier survey in this repo**,
  not re-verified today.

## Sources

- azahar `AGENTS.md:588-616`
- `CLAUDE.md`, "One code layout cannot suit all four cores"
- `research_log/20260823_0030_thread_affinity.md`,
  `research_log/20260825_1200_instruments_cumulative_counters_and_the_observer.md`
