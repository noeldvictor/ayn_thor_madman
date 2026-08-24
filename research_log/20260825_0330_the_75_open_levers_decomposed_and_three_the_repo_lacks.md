# The 75 OPEN levers decomposed, and three x86-detour instances this repo did not have

**Goal: `CLAUDE.md` calls xenia's 75 `OPEN` ledger entries "analysed levers
awaiting a run, a resource this repo had never named". Name them.**

**Device-free: one ledger query. No device used.**

## First, the figure needs decomposing before it is treated as a queue

**By category:** `rearch` **23**, `cpu` **18**, `gpu` **16**, `measurement` **4**,
the rest uncategorised.

**By date, and this is the part that matters:**

| Date | `OPEN` entries |
| --- | --- |
| **2026-07-06** | **19** |
| **2026-08-06** | **18** |
| **2026-07-05** | **13** |
| 2026-07-24 | 11 |
| everything else | 14 |

**The 2026-07-05 and 2026-07-06 pair is 32 of 75 — 43%** — and reading them shows
what they are: **the working notes of a blocked effort**, not levers awaiting
device time. Samples: *"Baseline ALSO hangs = the render-then-freeze is the
intermittent JIT hang"*, *"Adreno crash is NOT MSAA/resolve — even samples1
minimal native render crashes the driver"*, *"the 6-in-a-row hang is DEVICE
BOOT-STALL from my over-firing... Must REST the device."*

**They are `OPEN` because nothing resolved them, not because somebody analysed a
lever and queued it.** And `CLAUDE.md` already records that **26 of the 57 `WIN`
entries are `rearch` milestones of a build-out whose premise was later refuted** —
**the same lane.**

> **"75 analysed levers awaiting a run" overstates it. The genuinely actionable
> queue is the 2026-08-06 burst**, which is recent, categorised, and written as
> levers rather than as obstacles.

**This is the `backfill` lesson again**: a ledger's aggregate count is not a
measure of available work until the entries are read.

## Three levers from that burst, all the same disease, none recorded here

**Every one is the x86 detour**, and each names it explicitly.

### 1. `a64_v128_const_pool` — up to ten instructions where ARM64 needs one

> *"ARM64 cannot encode a 128-bit immediate, so `LoadV128Const`'s fallback builds
> one with `MOVZ` + up to 3 `MOVK` per half then `FMOV`/`INS` — **up to TEN
> instructions, eight on the ARITHMETIC ports and serially dependent.** x86
> encodes 64-bit immediates inline so **the x64 backend never faced this; the ARM
> port inherited the gap instead of ARM64's answer.** A PC-relative `LDR
> Qd,literal` is ONE instruction on the LOAD ports, the abundant resource on this
> SoC."*

**And xenia names the source:** *"This is Whatcookie's headline novel claim (a
materialised constant load can beat computing it) **applied where it actually
bites**."*

**`CLAUDE.md` carries that hardware argument** — the A715 and A710 have **three
128-bit load ports against two arithmetic ports**, so a load can beat computing —
**and does not record that xenia has implemented it.**

**State: implemented, default OFF, with a per-function literal pool, deduplicated,
emitted after the tail code, 16-byte aligned, inside the function's own code
allocation so PC-relative distances survive code-cache relocation.** Device smoke
on verified Turnip: 4 handled faults, 0 hard crashes, normal throughput.

**Its own honesty is the model:** *"NO PERF CLAIM — cross-build with no
same-session A/B, and drift here is ~2.8%. NOT CORRECTNESS-PROVEN EITHER: the two
screenshots are different moments of an attract replay, so this is a
no-gross-corruption check, not pixel-exact."*

> **"A wrong constant is SILENT data corruption rather than a crash, so the gate
> before default-on is the qemu-a64 differential (device-free)."**

**A device-free gate for a device-free failure mode.** That is the differential
testing this project says it lacks, specified for one lever.

**And it carries its own next step:** the `LOAD_VECTOR_SHL`/`SHR` sites in
`a64_seq_vector.cc` **hand-build permute tables with the same mov/fmov/mov/ins
pattern** and should use the pool directly.

**This one also connects to a constraint recorded here today.** The pool lives
**inside the function's own code allocation** precisely so PC-relative distances
survive relocation — the same reasoning as ARMSX2's constant-VA arena, reached
independently for a literal pool rather than a branch.

### 2. `arm64_offset_memory_address_fastpath` — default-off, on the hottest PPC class

> *"PPC `lwz r3,disp(r4)` and siblings are among the most common compiled
> instructions; **x86 folds base+index+disp into ONE addressing mode so the x64
> backend never faced the displacement**, but AArch64 has no base+index+immediate
> form."*

**What ships is the naive form:** `mov w0,w_r4` / `add w0,w0,#disp` / `ldr` — a
pure register copy, then the add. **The folded form exists** —
`add w0,w_r4,#disp` / `ldr` — and picks the right displacement encoding
(`imm12`, `imm12 LSL 12`, or materialised) **instead of always materialising.**

> **One instruction per offset access, on one of the hottest instruction classes
> in PowerPC, and it is compiled default-off.**

**Risk is low and stated:** it folds only 4 KB-granularity mappings and falls back
where large-page compensation is needed — *"default-off pending validation rather
than because it is known-bad."*

**And it repeats a warning this repo already carries:** *"the device config audit
found several validated levers silently off, **so check the persisted config
too**."* **`CLAUDE.md` records three `rlwinm` fastpaths found in exactly that
state, worth +2.88%. This is a fourth.**

### 3. A latent one in the same entry

The inline MMIO range check over `[0x7FC00000,0x7FFFFFFF]` **uses two full-width
`mov`+`cmp` pairs where ARM64 needs `lsr w0,w17,#22` + `cmp w0,#0x1FF`.**
**Latent only**, because `emit_inline_mmio_checks` is *also* default-off.

## And a correctness bug worth more than the three levers

**`Burnout crash: LLVM violates +reserve-x20`.**

The fault instruction decodes to **`LDP x22, x20, [x20, #72]`** — it **uses x20 as
the base AND writes x20 via `Rt2`**. x20 is the reserved guest-context register,
and **LLVM is handed `+reserve-x20,+reserve-x21` precisely so it never touches
it.**

**The elimination is clean and device-free.** xenia's a64 backend references x20
in **exactly one shape** — `stp`/`ldp(x19, x20, ptr(sp, 0x00))` in the
host-to-guest thunk, **always with SP as base** — so it never emits an `ldp` using
x20 as a base. **The faulting code cannot have come from a64.**

**The part that generalises is the mitigation gap:**

> *"`cpu_llvm_no_runtime_compiles=true` stops LLVM COMPILING during gameplay, but
> **this is bad code LLVM produced during the LOAD WINDOW and executed later — a
> real gap in the protection.**"*

> **A guard on WHEN code is produced does not protect against code already
> produced.** The mitigation was correct for the failure it was designed for and
> silently wrong for this one.

**And the proposed structural guard is this repo's own rule pointed at a JIT:**

> *"scan LLVM-emitted code for any instruction writing x20/x21 **before
> publishing it**, and reject to the a64 fallback if found."*

**Verify from the emitted artefact — at run time, on generated code, before it
executes.** This project applies that rule to compiler flags and to binaries. **It
had not considered applying it to a JIT's output as a publishing gate.**

**One discipline note from the same entry**, worth taking: a second flag
(`cpu_llvm_target_features_native`) had been flipped default-on the same day and
**was reverted to default-off specifically so it would not confuse the bisect** —
*"changing LLVM instruction selection while an LLVM codegen bug is under
investigation."* **Stop changing the thing you are bisecting.**

## Limits

- **Everything here is xenia's**, from its ledger, on its device and titles.
  Nothing reproduced, no device used.
- **The date decomposition is a proxy.** Entries were classified by reading a
  sample of the 2026-07-05/06 pair and the whole 2026-08-06 burst, **not all 75.**
  The 2026-07-24 group of 11 was not read.
- **None of the three levers has a performance number.** Two are default-off and
  unmeasured; the const pool explicitly refuses a perf claim.
- **The `reserve-x20` diagnosis is one fault instruction plus an elimination
  argument.** xenia itself lists a one-flag bisect as step 1, so it is not
  closed.
- **No claim that these transfer to other backends.** The const-pool and
  offset-fold shapes are general ARM64 problems; whether any other fork has them
  was not checked.

## Sources

- xenia `tools/exp_ledger.py list`, `check` for `v128_const_pool`,
  `offset_memory_address_fastpath`, `reserve-x20`
- `research_log/20260824_2145_a_branch_means_something_else_on_arm64.md`
- `shared_layer/REJECTED.md`
