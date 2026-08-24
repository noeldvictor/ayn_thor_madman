# REJECTED.md — measured rejections across the fleet

**Somebody in this fleet has already built your optimisation, measured it on this
device, and reverted it. Read this before proposing one.**

**This is an INDEX, not a copy.** Every row names the fork that measured it and
the document that states it in full. **Nothing here was measured by this repo.**

**It complements xenia's `tools/exp_ledger.py`, which is queryable and
xenia-only.** This file is fleet-wide, narrative and diffable. **Query both.**

---

## How to read a row

**A rejection is of a SPECIFIC IMPLEMENTATION, not of an idea.** Almost every
entry below leaves the door open to a materially different implementation, or to
a matched title showing a material win, and the forks say so explicitly. **Treat
a row as a bar to clear, not a wall.**

**And a rejection is worth as much as an acceptance.** `CLAUDE.md`: *"Recording a
rejection matters as much as recording an extraction, because both stop the
question being re-argued."*

---

## The recurring shapes

**Five patterns account for nearly every row.**

| # | Shape | Where it is clearest |
| --- | --- | --- |
| **1** | **The target improved and the complete path did not.** The change worked at its stated metric and lost anyway | azahar's Vulkan rejections: `Map`/`Commit` cycles **-18.64%**, `SetupVertexArray()` **+2.41%** |
| **2** | **It merely MOVED cost.** Work left the profiled function and arrived in a caller nobody watched | `UnscheduleEvent` **0.33% -> 0.08%**, two callers rose, whole-app cycles **+0.532%** |
| **3** | **The ceiling was below the noise floor.** The site was too small to win in, whatever the implementation | `ShaderDiskCache::GetPipeline()` **under 1% of process work, trace variance larger than the whole function** |
| **4** | **The pattern does not occur.** The transform was correct and had nothing to transform | xenia `EOR3`/`BCAX`: **0 of 1 fusable candidates**; rpcsx's seven x86 tricks: **~0.04% of emitted instructions** |
| **5** | **Fewer instructions, deeper dependency chain.** Instruction count is not the metric | azahar's four fusions; xenia's prolog **18 -> 13 instructions and slower** |

**A sixth is not a performance shape at all: a change can pass an enormous test
suite and be wrong.** azahar's `ready_queue.remove()` deletion passed **439,504
of 439,505 assertions** and crashed a title **within one second**.

---

## CPU and code generation

| Rejected | Fork | Measured | Stated in |
| --- | --- | --- | --- |
| **`MADD`/`MSUB` for `MLA`/`MLS`** | azahar | regressed **A510 dependent, both A715 patterns, A710 and X3 independent** | `research_log/20260824_2220_*` |
| **`SABA`/`UABA` for `SABD`+`ADD`** | azahar | **A510 0.6595x - 0.6890x** | same |
| **`SMULL`/`SMSUBL` for `SMUSD`** | azahar | A510 **0.961991x**; `SMUSDX` a **0.999915x** tie | same |
| **shifted operand folded into `BIC`** | azahar | A510 **0.9857x**, **0.9926x** | same |
| **packed-float24 `TBL`** | azahar | **0.52x - 0.54x on A510**, despite exact random equality | `research_log/20260824_2350_*` |
| **grouped-float24 batching** | azahar | **A510 small-batch timing unstable or regressive** | same |
| **`LD2`/`LD4`/`ST4` structured loads** | azahar | A510 Q-form 32-bit **`ST4` throughput `1/50`** (manual figure) | `research_log/20260825_0200_*` |
| **`EOR3`/`BCAX` fusion for VMX bitwise chains** | xenia | **`DEAD` — 0 of 1 V128 XORs are fusable chains** | `CLAUDE.md` |
| **`TBL2` + `ORR` for `TBX2`** | xenia | **0.555 against 0.555 ns** | `CLAUDE.md` |
| **seven x86 SIMD tricks** — `PSADBW`, `SUMB`, `VPDPBUSD`, `VDBPSADBW`, `GF2P8AFFINEQB`, `GBB`, `FCGT` | rpcsx | four already optimal, two have no ARM equivalent, one unreachable — **~210 of 509,424 emitted instructions** | `research_log/20260824_1015_*` |
| **spilling GPRs to the vector register file** | xenia; rpcsx | xenia: implementable, **off by default** — the 360 guest already squeezes 128 vector registers into 28. rpcsx: **not expressible**, LLVM's allocator decides placement | `CLAUDE.md` |
| **`ISB` in place of `yield`** | rpcsx; xenia; Cemu | rpcsx **+23% regression**; xenia **`CONFOUNDED`**; **`ISB` costs 11.42 ns against `yield` 0.36** | `research_log/20260823_1454_*` |
| **time-based spin backoff** | Cemu | **worse on the Thor** — `LatteCP_readU32Deprc` 5.13% -> 7.12% of total CPU. *"Leave the count alone"* | same |
| **a 64-bit nonempty-priority mask** using `RBIT`/`CLZ` | azahar | task-clock **+1.051%**, cycles **+1.156%**, **instructions +1.836%** | `research_log/20260825_0015_*` |
| **guest FP single-mode / `FPCR` switching** | xenia | implemented, **off by default, census first** | `CLAUDE.md` |

> **The one large ACCEPTANCE in this area belongs here too, so nobody reads the
> pattern as "never touch codegen".** azahar's **first-class
> `RSHRN`/`SQRSHRN`/`SQRSHRUN`/`UQRSHRN` IR operation measured 3.5x - 14.8x** —
> because it removed a **portable polyfill DAG**, not because it fused two
> instructions. See `research_log/20260825_0200_*`.

---

## Graphics

| Rejected | Fork | Measured | Stated in |
| --- | --- | --- | --- |
| **one `StreamBuffer` reservation** for vertex + fixed attributes | azahar | `Map`/`Commit` **-18.64% / -15.27%**, **`SetupVertexArray()` +2.41%** | `research_log/20260824_2305_*` |
| **one-entry texture-descriptor cache** | azahar | driver leaves **-38.53% / -15.74%**, **total sampled work +0.70%**, `SyncTextureUnits()` **+3.79%** | same |
| **`VK_EXT_extended_dynamic_state3` blending** | **azahar AND xenia, independently** | azahar: `tu_CmdBindPipeline` **unchanged at noise scale**, own `BindPipeline` **+9.17% relative**. xenia: **pipeline binds stayed at EXACTLY 208** — *"blend is not a variant driver"* | same |
| **bypassing the pipeline map lookup** | azahar | **crashed Turnip in `tu_cmd_render<chip7>`, null dereference** | same |
| **consecutive disk-bookkeeping pipeline cache** | azahar | only **51.28% of 300,000 queries** repeated the prior static state | same |
| **bindless resources** | xenia | **regressed 129 ms -> 161 ms**; descriptor binds unchanged at ~1074 because **they are per-draw constants, not textures** | `CLAUDE.md` |
| **native GMEM render targets** | xenia | `DEAD` | `CLAUDE.md` |
| **forced GMEM over autotune** | xenia | **never beat autotune** — parity from 16 draws up, **+157% worse at one draw** | `CLAUDE.md` |
| **`LOAD_OP_CLEAR` conversion** | rpcsx | correct, applied to 100% of clears, **12.39% against 12.65% GPU busy** at an identical clock | `CLAUDE.md` |
| **foliage forced early-Z** | xenia | **decisive math ceiling** — even a perfect reject floors at ~95 ms | `CLAUDE.md` |
| **forced-Sysmem Turnip R8** | azahar | **+21.86% GPU time** against generic R8 | `CLAUDE.md` |
| **transient and `LAZILY_ALLOCATED` attachments, and subpass merging, AS PERFORMANCE** | xenia | the frame measured **ALU-bound, not bandwidth-bound** — blend free, 2x bytes/pixel **+8-10%**, flat across a 36x working set | `research_log/20260824_2000_*` |

---

## Audio, power and build

| Rejected | Fork | Measured | Stated in |
| --- | --- | --- | --- |
| **a 4,096-frame Cubeb buffer** | azahar | **271.84 ms** and **989 underruns in ~1 minute**, against 118-131 ms and zero — it crossed the **4,000-frame power-saving threshold** | `research_log/20260824_2340_*` |
| **SoundTouch `quickseek`** | azahar | trades audio quality for speed, **rejected on quality**; the retained batch path is *"a recurring-hotspot reduction, not an FPS or battery-watt win"* | same |
| **an atomic fast path around `System::signal_mutex`** | azahar | **0.05% of whole-app cycles** — a ceiling that does not buy a change to reset, save, load and shutdown timing | `research_log/20260825_0015_*` |
| **Gradle `--configuration-cache`** | azahar | rejected **after native and APK tasks succeed**, because the build script runs `git` during configuration. **Four forks have the same blocker** | `research_log/20260825_0110_*` |
| **A510 pinning to satisfy a 72 C guard** | rpcsx | **the guard compared a junction maximum against a package-shaped limit** — a load detector, not a thermal bound | `research_log/20260824_1050_*` |

---

## What to do before proposing an optimisation

1. **Search this file, and run `exp_ledger.py check` for the lever.**
2. **Compute the CEILING** — the site's share of process work — **and compare it
   to the workload's noise floor.** If the ceiling is smaller, stop.
3. **Count APPLICABILITY.** How many times does the pattern actually occur?
   xenia replaced a three-opcode build with one cvar and one run.
4. **Name the complete path you will measure**, not the helper — **and name the
   callers**, or displaced cost reads as a win.
5. **State the core class and the dependency shape** for anything touching
   codegen. **The A510 is the discriminator in all eight measured results.**

---

## Limits

- **Nothing here was measured by this repo.** Every figure belongs to the fork
  named, on that fork's workload, on its device.
- **This is not exhaustive.** It indexes what has been READ — mostly four forks'
  `AGENTS.md` files and xenia's ledger. **xenia alone has 553 research documents
  and 75 `OPEN` ledger entries** that are not represented here.
- **Several rows quote manuals rather than measurements**, the `ST4` `1/50`
  figure especially. This repo's record is **thirteen refuted manual-derived
  predictions**, so a manual row is a warning, not a result.
- **A rejection can go stale.** A driver update, a different title, or a changed
  surrounding design can move any of these. **The forks' own wording anticipates
  that. Keep it.**
