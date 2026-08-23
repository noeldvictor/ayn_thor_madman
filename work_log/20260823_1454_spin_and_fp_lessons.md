# Eight audits, and the tests that keep them

**Goal: close standing open questions in [`CLAUDE.md`](../CLAUDE.md) using code
analysis alone.**

No device. No fork modified. The investigation is in five research logs; this
records what changed in the repo.

## The findings, in one line each

- **The spin-wait lead had the right check and the wrong fix.** `yield` is a
  measured no-op on this SoC, and replacing it with `ISB` alone is a measured
  regression in three forks. See
  [`research_log/20260823_1454_spin_wait_audit.md`](../research_log/20260823_1454_spin_wait_audit.md).
- **All three CPU leads are already implemented in xenia**, with primary-source
  citations, mostly off by default with the trade stated. See
  [`research_log/20260823_1520_cpu_leads_already_done.md`](../research_log/20260823_1520_cpu_leads_already_done.md).

## Code written

**Two rules that were only ever prose are now executable.** Both are shared-layer
logic with no backend behind them yet, which is the point: the rule outlives
whoever read the research log.

| File | Rule it holds | Tests |
| --- | --- | --- |
| `SpinBudget.kt` | A spin budget is **time**, and the step cost is **measured**, never assumed | 10 |
| `BlockCacheKey.kt` | **Whatever a block bakes in must appear in its key** | 7 |

### `SpinBudget`

Extracted from ARMSX2's `MeasurePauseTime`. It calibrates from samples — **taking
the minimum, not the mean**, because a calibration run competes with everything
else on the device and a high sample is contamination.

**The test that matters is the counter-example.** One test proves a time budget
holds the total backoff constant across a 32x change in step cost; the next
proves a fixed iteration count does not, and grows the wait by 32x. **That
second test is the regression three forks measured**, written down so nobody
re-derives it.

### `BlockCacheKey`

Extracted from ARMSX2's mVU sentinel. **It exists because of a hazard, not
because a cache key is hard.** Baking the guest FP environment into generated
code is the right way to keep `FPCR` out of the hot path, and it silently makes
that code wrong the moment the guest changes rounding mode.

The PS2 has three independent FP environments, so the tests cover a change to
any one of several, and to their order.

## Documents changed

- **`CLAUDE.md`** — the `yield` section rewritten with the measurements and the
  refusal; "Two CPU leads worth chasing" now opens with the audit; three rows
  added to the "Read before you claim" table.
- **`shared_layer/PROPAGATION.md`** — item 10 split into a fact and a refusal;
  items 21 to 27 added.
- **`DEVICE_QUEUE.md`** — entry 11, narrowed to the two parts that still need
  hardware, both with a predicted `FLAT`.

## Corrections to CLAUDE.md, from reading

1. **dynarmic is vendored by three forks, not two** — azahar, Vita3K and
   **eden**. Vita3K's is a submodule, which is why a file count missed it.
2. **Three forks have an rpcs3 ARM64 cross-pollination document, not two.**
   Vita3K has one.
3. **The FP lead had the wrong root cause.** Not faithfulness for its own sake:
   **the Xenon has two FP mode registers and ARM64 has one.**

## Corrections I made to myself while working

Recorded because both would have shipped as false findings.

- **"xenia's `SpinLoopHint` has zero call sites."** It has one. My grep passed
  `'*.cpp' '*.h'` and the caller is a `.cc`.
- **"Vita3K does not vendor dynarmic."** It does, as a submodule.
  `git ls-files` returns nothing for submodule contents.

**Both were the same mistake: trusting a search's shape instead of its subject.**

## Measured

- **87 tests pass**, up from 70.
- `assembleDebug` succeeds.
- **Every fork shows 0 modified tracked files.** azahar's 14 are the user's own
  pre-existing C++ work, unchanged by this session.

## Not done

- **Nothing here is a performance claim.** Every number is read from rpcsx's
  results file or from a comment in ARMSX2, Cemu or xenia.
- **eden's `yield` is not shown to cost anything.** It is provably a `nop` on two
  hot paths; how often they contend is unmeasured, and eden does not build here.
- **Not committed**, per the hold on emulator adaptation.

---

# Addendum — three more audits, same session

**Same constraint: reading only, no device, no fork modified.**

## 3. The symbol collision census — the packed binary's central risk, measured

**`llvm-nm` over six forks' built `arm64-v8a` libraries.**

**25,526 symbols are exported by two or more forks. Zero are emulator code.**
Every collision is a vendored dependency.

**This supports the packed binary and reorders the work.** Seven cores in one
binary is not the risk; seven sets of dependencies is.

**Three dependencies nobody had recorded:** **OpenSSL** (azahar, Cemu, Vita3K —
the largest single collider), **Teakra** (azahar, melonDS — the only genuine
shared *emulation* dependency in the fleet), **rcheevos** (ARMSX2, melonDS).

**And one hard blocker:** ARMSX2 and Vita3K both statically link SDL, so both
export `JNI_OnLoad` and the `Java_org_libsdl_app_*` families. **`JNI_OnLoad`
cannot exist twice and cannot be renamed.**

**The visibility row is now measured, and it reorders the forks.** Exports span
43x — xenia 2,285 to Vita3K 98,550 — and the earlier header-scan percentages in
`CLAUDE.md` ranked them wrongly. Withdrawn.

Log: [`research_log/20260823_1508_symbol_collision_census.md`](../research_log/20260823_1508_symbol_collision_census.md).

## 4. Cheat formats — a question CLAUDE.md marked as design-deciding

**3DS: yes, by construction.** `dmnt_cheat_vm.h` marks three opcodes "not
implemented by Gateway's VM", and **Gateway is the 3DS system** — dmnt was built
as a superset.

**PS2: mostly, with three gaps.** No byte swap (dmnt has **zero** endianness
handling; six of pnach's nine data types need it). No scheduling (pnach's
`place` field). And **the region selector is guest knowledge** — `EE`/`IOP`
against `MainNso`/`Heap`/`Alias`/`Aslr`.

**That third gap is the same finding as texture classes and filter lists, for
the third time.** The instruction set is shared; the region namespace is
declared by the backend.

**A sixth format found on the way, and it is not a format:** NTR `.plg` plugins
are compiled ARM code, not codes.

Log: [`research_log/20260823_1532_cheat_format_mapping.md`](../research_log/20260823_1532_cheat_format_mapping.md).

## 5. azahar's applets — CLAUDE.md said "survey before designing it"

**It is already the contract.** Abstract interface in
`src/core/frontend/applets/`, guest HLE behind it, Qt and Android in front.
**GPL-2.0-or-later, so the code is usable and not only the design.**

**Five things the three-call sketch missed:** three-phase validation with a
twelve-value error enum; a guest-callback round trip so the guest can reject
input; caller-supplied button labels; a default implementation always registered
so a partial shell still boots; and the extension policy written into the header
in the same words this project chose independently.

Log: [`research_log/20260823_1548_azahar_applets_read.md`](../research_log/20260823_1548_azahar_applets_read.md).

## 6. Dependency versions — Open Decision 2, answered in part

**Version strings read from the same six binaries, plus ABI namespaces recovered
free from the collision census data.**

**The question had the wrong shape.** It is not "pick one version of each".

**A dependency is safe when it versions its own ABI, not when it is C++.**
**fmt** puts its version in an inline namespace, so xenia's `v6` and three
forks' `v12` **cannot collide** — which is why fmt contributed ~142 colliding
symbols against OpenSSL's ~6,400. **libc++ is already unified** at `std::__ndk1`
in all six, because the NDK supplies it.

**Every plain-C dependency readable in two forks disagreed:** OpenSSL 3.5.0 vs
3.6.2, SDL 3.5.0 vs 3.2.28, zlib 1.3.1 vs 1.3.2. **imgui joins them** at 1.92.6
vs 1.91.3 — C++, but with no versioned namespace, so it mangles identically.

**SDL is the sharpest case**: the two forks exporting a duplicate `JNI_OnLoad`
are also on different SDL versions.

Log: [`research_log/20260823_1600_vendored_versions_from_binaries.md`](../research_log/20260823_1600_vendored_versions_from_binaries.md).

## 7. The remaining four cheat formats

**AR DS, VitaCheat `.psv`, rpcs3 YAML and `.ncl` read. All six formats now
surveyed.**

**One opcode dominates every real corpus.** `be32` is **5,467 of 6,497** rpcs3
patch entries (84%); `0` — a 32-bit write — is **60,091 of 91,372** `.ncl` lines
(66%) across 2,501 files. **The VM's complexity is entirely in the rare tail**,
which is the number behind the tiered engine.

**A fourth gap in dmnt, and it is emulator knowledge rather than guest
knowledge.** VitaCheat's `$A000`/`$A100`/`$A200` are **code writes with JIT cache
invalidation**. dmnt has no such opcode because Atmosphere runs on real hardware.
**A cheat engine that only writes data does nothing on a recompiled guest.**

**Endianness confirmed a third time** — rpcs3's `be32`/`be16`/`bef32`, and both
PS2 and PS3 are big-endian. **`bef32` is not a fifth width**: a float write is
bit-identical to a 32-bit write once encoded, so the front end converts it.

**Provenance is already solved.** `.ncl` carries an author per cheat; rpcs3 YAML
carries `Author`, `Notes`, `Patch Version`, keyed by `PPU-<hash>` — **183
distinct hashes across 173 files, which is `DumpId` in production.**

Log: [`research_log/20260823_1620_cheat_formats_all_six.md`](../research_log/20260823_1620_cheat_formats_all_six.md).

### Code written

`BlockCacheKey` gained `guestLength`, `overlaps`, `invalidatedBy` and a
`WriteTarget` enum, **because the cheat survey found the rule**: a cheat that
writes guest code invalidates compiled blocks covering that range. **The same
hazard as the FP-environment one, from the other direction.** 9 new tests,
including both off-by-one edges — an off-by-one here throws away a warm cache on
every cheat tick.

## 8. eden's dependencies, without building eden

**"eden is unmeasured because it does not build" was too pessimistic.** Its
`.cache/cpm/` is a **resolved dependency manifest from a real configure run** —
28 packages with versions, free.

**OpenSSL is the most fragmented dependency in the fleet: three distinct versions
across three forks** — Cemu **3.5.0**, eden **3.6.0**, Vita3K **3.6.2** — with
azahar carrying a fourth copy unread. **It is also the largest collider. It goes
first.**

**This fills a blank in the standard row.** Oboe was pinned with "version not yet
chosen"; **eden runs Oboe 1.10.0 on Android arm64 today.**

**boost and glslang close the other named lead**, and the answer is better than a
version number: **neither versions its ABI** — plain `boost::archive`,
`glslang::`, `TIntermNode`. **For those the version is moot; any two copies
collide.** fmt is confirmed safe: eden's 12.1.0 is the same `v12` ABI three forks
export, and xenia's `v6` cannot collide with it.

**Two of my hypotheses were wrong and checking caught both.** eden's SDL is
**SDL2, forced off on Android**, and no file under `src/android/` references it —
so **eden does not add to the `JNI_OnLoad` problem**, which I had drafted as an
escalation. And `ENABLE_CUBEB` is **OFF** on Android, so eden uses Oboe. **A
dependency declared at the root is not a dependency in the Android build.**

Log: [`research_log/20260823_1600_vendored_versions_from_binaries.md`](../research_log/20260823_1600_vendored_versions_from_binaries.md).

## Session totals

- **Eight audits**, all from reading.
- **Six `CLAUDE.md` open questions closed or narrowed**, two of them explicitly
  marked design-deciding, plus **one blank in the standard row filled**.
- **Nine corrections** to `CLAUDE.md`, including two claims withdrawn.
- **96 tests pass.** No fork modified. No device used.

**The recurring shape:** five of the eight audits found the fleet had already
done the work and not recorded it. **The instrument was wrong, not the search** —
capability-shaped searches miss a `DEFINE_bool` with a paragraph of reasoning, a
comment carrying a measurement, a header that is already a contract, or a CPM
cache that is a dependency manifest.

**And the second recurring shape: four hypotheses were wrong and checking caught
all four.** xenia's `SpinLoopHint` call site, Vita3K's dynarmic submodule,
imgui's missing ABI namespace, and eden's SDL. **Three of the four would have
shipped as false findings in a document nobody would re-check.**

---

# Addendum — the supervision layer, from AVO

**A skill and a check, built after reading NVIDIA's AVO write-up.**

## What actually transferred

**AVO has four parts. This repo already had three.**

| AVO part | Here |
| --- | --- |
| Main agent loop | already |
| Persistent memory | already: `research_log/`, `work_log/`, `capability_inventory.md`, `exp_ledger.py`, `DEVICE_QUEUE.md`, `PROPAGATION.md` |
| Domain tools | already: the builds, `llvm-nm`, `fleet_lint.py` |
| **Supervision layer** | **missing** |

**Only the fourth is worth taking**, and it happens to target this repo's
best-documented failure mode: the "Read before you claim" table, which records
that **every** absolute negative this repo has written was wrong.

**AVO's numbers do not transfer** — 183 levels, 6,624 environment actions, 10.5%
over FlashAttention-4 are a different benchmark on different hardware. **The
skill says so explicitly**, so nobody quotes them later as if they were ours.

## Built

- **`tools/supervise.py`** — six checks, reading **added lines only**. Scanning
  whole documents reported the correction tables as the disease.
- **`.claude/skills/supervise/SKILL.md`** — the procedure, plus the half a regex
  cannot see: re-reading a file already read, three empty searches, scope
  widening after a dead end, re-opening a settled decision.

## It found a real error on its first run

A research log written earlier today said **"No other fork does this. ARMSX2,
Cemu and melonDS spill to the stack"** — asserted from reading **xenia alone**.

**Searched again, with different words.** First for
`spill.*(vector|vreg|VPR|fmov|umov)` and the reverse, then for any `spill` at
all. The second search found the three register allocators — ARMSX2
`x86emitter`, Cemu `IMLRegisterAllocator`, melonDS `ARMJIT_Compiler` — and
**none targets the vector file. The claim holds.**

**It did not hold when it was written**, which is the point. Three earlier claims
in that exact shape were wrong.

## And it produced a false positive on its first run

It failed `DEVICE_QUEUE.md` entry 3 for having no prediction. **Entry 3 states
its predictions in a table column headed `Prediction`**, and the pattern demanded
bold markers.

**The document was right and the tool was wrong.** Same lesson as the ABI lint
and Cemu: **when a tool and a document disagree, read the actual line.** The
check was fixed and the reason recorded beside it.

**Two more were tuned rather than obeyed.** The negatives check first reported
**51** hits against **6** real ones, because it scanned whole files; and
`dead-levers` fired on every markdown change, which is noise. **A check nobody
trusts is worse than no check.**

## State

`python tools/supervise.py --strict` exits **0** on this working tree. Three
`WARN`s remain and all three are legitimate prompts rather than errors.

---

# Addendum — the negative audit, eleven of fifteen checked

**`tools/supervise.py` found 15 unqualified absolute negatives still in
`CLAUDE.md`, written before the rule that a negative needs a second search.**
The repo's own table says every such claim it has made has been wrong. Eleven
are now checked.

## Six wrong or stale

| Claim | Reality |
| --- | --- |
| only ARMSX2 has frame generation | **xenia has one**, by extrapolation — no held frame, where ARMSX2's interpolation costs one |
| storage aggregation has no prior art | **GameThor has 2,136 lines**, including `moveGame` between internal and external storage |
| haptics nobody else ships | **seven of eight forks have them**; xenia is the outlier |
| **no fork plans render passes** | **xenia plans them, and patches the pass begin retroactively** |
| nobody resolves MSAA on-chip | **xenia does**, with the multisample store elided |
| melonDS is the only verified build recipe | **four forks now build** on the standard row |

## Five hold, three with corrected evidence

| Claim | Note |
| --- | --- |
| nobody uses input attachments | every hit in every fork is a zero-initialiser |
| Vita3K tracks transient attachments alone | two searches, different words |
| almost no emulator gates a build on performance | Vita3K's manual matrix is closest |
| Vita3K has a content path resolver | **holds**, but eden solves the adjacent SAF/removable-volume half |
| xenia alone uses the device's vector features | **holds**, but its table conflates three things |

## The render pass finding is the consequential one

**`THOR_RENDER.md` commitment 2 rests on a false premise.** `CLAUDE.md` argued a
shared render graph "adds planning nobody has". **xenia has it**, and a graph
that ignored it would replace analysis with a lookup.

**What xenia does that nothing else does:** an on-chip MSAA resolve with
`STORE_OP_DONT_CARE` on the multisample colour; `LOAD_OP_DONT_CARE` **proven by
replaying the first draw's vertex positions on the CPU**, per-attachment, falling
back to loading on any uncertainty; a depth path that knows **`STORE_OP_NONE`
preserves EDRAM where `DONT_CARE` would undefine it**; and — the part to take —
**it patches the already-recorded `vkCmdBeginRenderPass`** after accumulating
coverage, **because load and store ops do not affect render pass
compatibility.**

**That removes the hardest constraint on pass planning**: that a renderer cannot
usually know at `BeginRenderPass` what the pass will contain. **It is BSD.**

## Two lessons about the instrument, not the fleet

**Filename counts are the wrong tool twice over.** Matching
`bench|perf.*test|regression` gives ARMSX2 32 files and azahar 39; **after
excluding vendored trees, one and zero.** And frame generation returned **zero**
files for xenia because its implementation lives inside `presenter.*`.

**eden is the worst case for the guest/host trap in the fleet, because its guest
ISA is the host ISA.** Every ARM64 mnemonic appears there as guest decoding, so
`EOR3` in dynarmic means it *decodes* the instruction, not that it emits it.
**Cemu's two hits are comments in `cpu_features.h`** — detection, not use.

## The supervisor needed three fixes while doing this

All three were **false positives on quoted claims** — a list item, a table row,
a heading — each *discussing* a claim rather than asserting one. Generalised to
one rule: a quoted claim inside a list item, table row or heading is exempt.
**The self-test still catches a planted bare assertion.**

**Four claims remain unchecked**, all low value.
