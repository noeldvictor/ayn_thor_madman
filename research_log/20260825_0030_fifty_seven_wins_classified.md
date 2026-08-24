# All 57 `WIN` entries classified: a WIN is a decisive result, and two are speed levers on working code

**Goal: test this repo's own central claim — "the wins have come from code that
was broken, not code that was slow" — against the complete `WIN` list rather
than the sample it was built from.**

**The claim survives, sharpens, and picks up two corrections about how to read
the ledger at all.**

## The shape of the 57

| Category | Entries | What they are |
| --- | --- | --- |
| **`rearch`** | **26** | milestones in one build-out, whose **premise was later refuted** |
| `cpu` | 9 | the only category with per-lever speed numbers |
| `thor` | 8 | on-device bring-up, crashes and confound quantification |
| `measurement` | 6 | frame anatomy and route unblocking |
| `gpu` | 6 | HLE colour path, capability confirmation |
| `fill` | 2 | overdraw isolation, and one **lossy** shipped stack |

> **A `WIN` verdict in this ledger means "decisive result", not "got faster".**

**Entry 23 is a WIN for building a Gears gameplay route.** **The Gears CPU
profile is a WIN whose content is a negative** — it concludes that "generalise
the guest busy-wait fastpath", recorded in `CLAUDE.md` as the highest-value CPU
work available, **does not apply to that title at all**, because the load is
distributed with no single dominant function.

**`shared_layer/MEASUREMENT.md` quotes "177 entries: 75 `OPEN`, 57 `WIN`, 32
`DEAD`" with no such caveat.** Read as speedups, 57 is wrong by an order of
magnitude. **Corrected there.**

## The nine `cpu` entries, read in full

| Entry | Result | Broken, or slow? |
| --- | --- | --- |
| `rlwinm` fastpaths off by stale device config | **+2.88%** | **broken** — persisted config |
| object cache never enabled on GUI launches | ~60 s black screen removed, **zero cold compiles** | **broken** — wrong launch path |
| `vmaxfp`/`vminfp` `FixupVmxMaxMinNan` deletion | correctness, −6 uOPs | **broken** — an **x86 `MAXPS` transliteration** violating the PPC rule |
| `VECTOR_DOT_PRODUCT` constant operand | correctness | **broken** — latent, default path |
| `VECTOR_ROTATE_LEFT` + `VECTOR_DENORMFLUSH` | correctness | **broken** — two more, same shape |
| XMA non-forward read offset | **13,534 warnings to 0** | **broken** — a livelock |
| Gears gameplay CPU profile | a negative | measurement |
| LLVM residency + writeback | "#1 LLVM perf lever" | **later `CONFOUNDED`** — see below |
| **`a64_stackpoint_prolog_fastpath`** | **+2.04%, 11/11 intervals** | **the one counter-example** |

**Six of nine are bugs.** One is a measurement. One is superseded.

## The counter-example, read closely, is the same finding one layer over

`a64_stackpoint_prolog_fastpath` is a real, reasoned, measured optimisation of
working code: 18 emitted instructions to 14, **+2.04% guest throughput, every one
of 11 intervals favouring the arm, on a handicapped 4 C hotter start.**

**But read what it changed:**

- **`MOV`+`CMP`** → **`cmp` against an encoded immediate**
- **`MOV`+`UMULL`+`ADD`** → **a shifted-register `ADD`** for a 16-byte struct index
- **re-loading a depth already live in `w9`** → reusing it

> **Every one of those is x86-shaped code generation.** x86 folds `[base +
> index*16]` into an addressing mode, so an explicit multiply costs nothing
> there; ARM64 has the shifted-register form and this code was not using it.

**So the counter-example is not "reasoned micro-optimisation beat the prior".**
It is **the x86-detour thesis paying out on the CPU side**, which is
`CLAUDE.md`'s own guiding idea. **The correct statement of the pattern is
therefore wider than the one recorded:**

> **The wins have come from code that was WRONG FOR THIS MACHINE** — either
> broken outright, or shaped for a machine this is not.

**That is a strictly better claim**, because it covers the counter-example
instead of being embarrassed by it, and it is the same rule as the x86-correction
lens rpcsx calls its most productive heuristic.

**And the honest limit: it is one entry.** A single +2.04% from tightening
addressing does not prove a lane. `a64_backend.cc` carries 111 tuning flags and
**most are off by default.**

## Correction 1: a broken lever does not only cost speed, it produces false research

**The kExtern entry is a third instance of "the lever never fired"** — alongside
the stale config and the wrong launch path — and it has a consequence the other
two did not.

`X64Emitter::Call` never checked `kExtern` for planted externs, so a direct
guest `bl` resolved straight to the compiled guest body and **silently bypassed
the handler**. Every desktop HLE diagnostic intercept returned `count=0`.

> **"So ALL desktop diag intercepts were FALSE NEGATIVES."** The fix
> **corrected an earlier research finding** — a "field = composite,
> `BeginTiling` count=0" conclusion that was an artefact of the broken
> intercept, not a property of the game.

**A lever that silently does not fire turns every measurement taken through it
into a false negative.** That is a fourth mechanism for "a setting that exists
is not a setting that applies", and it is the most expensive of the four,
because the cost is **wrong conclusions rather than lost frames.**

**Consequence for this project's instruments:** `hle_coverage.py`,
`capability_probe.py` and `bug_class_sweep.py` all report **absence**. **An
instrument that can return zero must be proved able to return non-zero.**
`bug_class_sweep.py` has that property by construction — every class ships with
a case that already paid — **and it was not stated as a requirement.**

## Correction 2: the ledger cannot always order its own entries

**Register residency holds two verdicts at once:** `cpu_backend_llvm_residency_
writeback` is a **`WIN`** dated `backfill`, and `llvm_residency_ladder_thor` is
**`CONFOUNDED`** dated **2026-07-24**.

**This repo's rule is "a newest failure outranks an older success".** It cannot
be applied here, because **`backfill` is not a date.** A reader who queries
`residency` sees a `WIN` and a `CONFOUNDED` with no way to order them, and the
`WIN` prints first.

> **An undated entry defeats recency ordering.** Adopting the ledger means
> adopting that gap.

**This is the superseded-conclusion trap seen from inside the instrument** rather
than from a dated document, and it is why `fleet_docs_index.py --after` exists.
**The equivalent query does not exist for the ledger.**

## Limits

- **Titles and the `cpu` bodies were read; the 26 `rearch` bodies were not.**
  The category claim rests on their titles and on the rearch document already
  read, which records that the premise was refuted.
- **Nothing reproduced.** All of it is xenia's measurement on xenia's workloads.
- **`WIN` counts are xenia's alone.** No other fork in the fleet has a ledger.
- **The classification is a judgement.** "Broken" against "slow" is not a field
  in the schema, and somebody else might sort two of the nine differently.

## Sources

- xenia `tools/exp_ledger.py wins`, all 57 entries
- xenia `docs/research/native-render-path-rearch.md`
- `research_log/20260824_0855_every_win_was_a_bug.md`
- `research_log/20260824_1910_residency_on_device_is_confounded.md`
