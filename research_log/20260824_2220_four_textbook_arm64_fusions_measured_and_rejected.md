# Four textbook ARM64 fusions, measured and rejected — and one regressed on every core including the X3

**Goal: mine the unread `Do not` invariants in azahar's `AGENTS.md`.**

**The cluster at lines 1402-1425 is a rejection ledger for ARM64 instruction
fusion, with exact numbers. It is the strongest evidence in this fleet against
the lane this project keeps being drawn to.**

**No device. Reading one fork's recorded measurements.**

## The four

**Every one is a "fewer instructions must be faster" peephole. All four were
built, measured and reverted.**

| Fusion | Replaces | Measured |
| --- | --- | --- |
| shifted operand folded into **`BIC`** | split shift + `BIC` (A32 `AndNot32`) | **A510 0.9857x and 0.9926x** |
| **`SABA`/`UABA`** | `SABD`/`UABD` + `ADD` (`VABA`) | **A510 0.6595x-0.6890x** |
| **`MADD`/`MSUB`** | `MUL` + `ADD`/`SUB` (A32 `MLA`/`MLS`) | **regressed A510 dependent, BOTH A715 patterns, and A710 and X3 independent "badly"** |
| **`SMULL`/`SMSUBL`** | lane extract + multiply/subtract (`SMUSD`) | **A510 2.599973 -> 2.702699 ns/op = 0.961991x**; `SMUSDX` a 0.999915x tie |

**These are dynarmic lowerings, and dynarmic is vendored by azahar, Vita3K and
eden** — three backends in the packed binary.

## 1. `MLA` to `MADD` is the sharpest result, because it breaks the usual story

**`MUL` + `ADD` folded into `MADD` is the textbook ARM64 fusion.** One
instruction, one destination, an operation the architecture provides directly.

**It regressed the dependent A510 path, both measured A715 patterns, and the
independent A710 and X3 patterns "badly".**

> **This repo's all-core gate is framed as "the big cores like a wide
> instruction, the A510 does not". This case says the big cores did not like it
> either.**

**So the gate is not an A510 tax.** It is a gate, and the A510 is merely where it
fails most often.

## 2. The A510 is now the discriminator in SEVEN measured results

**Method: this table is assembled from findings ALREADY RECORDED in this repo's
own logs, plus the four above. It is not a new search of the forks**, so it is a
lower bound — a fork that measured an A510 regression and did not write it into a
document this repo has read is not represented.

**This repo had three.** With these four it has seven, from three forks, on this
SoC:

| Result | Source |
| --- | --- |
| `TBX2` about 2x `TBL2` | xenia |
| `BCAX` 2.02x on A510 against 0.94x on X3 | rpcsx |
| packed-float24 `TBL` at **0.52x-0.54x on A510** | azahar |
| **`SABA`/`UABA` at 0.66x-0.69x on A510** | **azahar** |
| **`BIC` fold at 0.986x-0.993x on A510** | **azahar** |
| **`SMULL`/`SMSUBL` at 0.962x on A510** | **azahar** |
| **`MADD` regressed on A510, A715, A710 and X3** | **azahar** |

**A wide or fused instruction is a bad default on this part.** That is no longer
a hypothesis from three data points.

## 3. Dependency shape is the deciding variable, and it is a THIRD axis

**Every rejection above says the same thing: the independent shape can win and
the dependent shape regresses.** The gate azahar asks for each time is
**"dependency-aware"**.

> *"Although the unary shifted-input and **independent** shapes can improve..."*
> *"Although **independent** and big-core **dependency** patterns can win..."*
> *"attractive **independent** A510 results but regressed the **dependent** A510
> path..."*

**This repo has the axis and had not connected it to fusion.** `CLAUDE.md`
records whatcookie's model: *"the two axes worth optimising are arithmetic-port
pressure and dependency depth."* **A fusion shortens the instruction stream and
LENGTHENS the dependency chain**, because the fused instruction cannot begin
until both inputs are ready, where the split pair could overlap.

> **Fusion trades instruction count for dependency depth. On a machine with
> spare issue width, that is a bad trade** — which is exactly what rpcsx measured
> from the other direction, where `BCAX` won 2x on a serial chain and lost
> slightly on four independent chains.

**So a fusion proposal needs three things stated, not one: the core class, the
dependency shape, and the operand width.**

## 4. The acceptance rule, and it names instruction count explicitly

> *"**Instruction count and the manuals' logical timing rows are candidate
> guidance, not sufficient acceptance evidence.**"*
>
> *"The manuals' slower A510 `SABA`/`UABA` timing is a **warning, not a
> substitute** for the retained all-core benchmark evidence."*

**This repo has both halves and had not seen them stated together.** The manual
prior is recorded as **thirteen refuted predictions**; the instruction-count rule
is recorded as *"use inflation to choose which subsystem to attack, do not use it
to judge a peephole."*

**azahar reached both independently, and adds the case where the manual was
RIGHT and still insufficient** — its timing rows warned that A510 `SABA` is slow,
the measurement agreed, **and the fork still records that the manual was not what
settled it.**

## 5. A confound this project's measurement rules do not name: code alignment

The `SMUSD` result is stated **"after 64-byte loop alignment"**.

> **A 3.8% effect cannot be measured in a loop whose alignment is uncontrolled**,
> because moving a hot loop across a fetch-block boundary moves results by more
> than that.

**`MEASUREMENT.md` covers thermal drift, scene variance, cold caches, persisted
config and workload saturation. It does not mention code alignment**, and every
result in this cluster is in the few-percent band where it dominates.

## What this means for this project

**It is direct evidence against the CPU lane, in the fleet's own numbers.** This
repo already records the fleet as **thirteen for thirteen** on refuted
manual-derived predictions and notes that *"every win was a bug, not an
optimisation"*. **Four more refutations, with exact ns/op figures, all of the
"obvious better instruction" kind.**

**And it is a reason the shared layer should carry the ledger, not just the
code.** These four rejections live in one fork's `AGENTS.md`. **eden and Vita3K
vendor the same dynarmic and would each have to re-derive them** — which is the
unit-of-work argument this project makes everywhere else, applied to negative
results.

> **A rejected optimisation is worth recording exactly as much as an accepted
> one, and it is cheaper to lose.**

## Limits

- **Every number is azahar's, on azahar's benchmarks, on its Thor.** Nothing
  reproduced here and no device used.
- **The benchmark shapes are not described beyond "exact four-chain",
  "accumulator-chain" and "representative ASR/LSL forms".** The figures cannot be
  re-derived from the text.
- **These are rejections of GLOBAL folds.** Each entry explicitly leaves the door
  open to a narrower dependency-aware gate. **They do not say the fusion is
  always wrong** — they say it is wrong as an unconditional lowering.
- **`SMUSDX` was a tie, not a regression**, and is recorded as such.
- **No claim about dynarmic's current upstream state**; this is azahar's fork's
  recorded position.

## Sources

- azahar `AGENTS.md:1402-1425`
- `research_log/20260824_2350_the_all_core_gate.md`
- `shared_layer/TRANSLATION.md`
