# Do pnach and 3DS AR codes map onto dmnt bytecode

**Goal: answer the question [`CLAUDE.md`](../CLAUDE.md) flags as "still
unverified, and it decides the design".**

No device. Reading only: eden's `dmnt_cheat_vm.h`, azahar's `gateway_cheat.cpp`,
ARMSX2's `Patch.h` and `Patch.cpp`.

## Answer

**3DS: yes, and by construction. PS2: mostly, with three named gaps, and the
third is structural.**

**A shared cheat VM is viable.** It needs two additions to dmnt's instruction
set and one thing removed from it.

## The 3DS half is already settled, in eden's own source

`dmnt_cheat_vm.h` marks three opcodes with a comment:

> // These are not implemented by Gateway's VM.

**Gateway is the 3DS cheat system.** dmnt was built as a superset of it, so the
question of whether 3DS codes fit is answered by the file that would have to
run them.

Checked against azahar's implementation, which documents every Gateway opcode
in comments:

| Gateway | dmnt |
| --- | --- |
| `0`/`1`/`2` — store word/half/byte at addr+offset | `StoreStatic` |
| `3`–`6` — conditional word `>`, `<`, `==`, `!=` | `BeginConditionalBlock` |
| `7`–`A` — conditional half **with mask** | `BeginConditionalBlock` |
| `B` — offset = word[addr+offset] | `LoadRegisterMemory` |
| `DA`/`DB` — register load, masked | `LoadRegisterMemory` |
| `DC` — offset += value | `PerformArithmeticStatic` |
| `DD` — **keypad** conditional | `BeginKeypressConditionalBlock` |
| `E` — write a block of data | loop + `StoreRegisterToAddress` |

**Only `E` is not one-to-one.** It is expressible with `ControlLoop`, so it
compiles rather than fails.

**dmnt has more than Gateway**, not less: register arithmetic, register-to-
address stores, save and restore registers, and pause/resume/debug-log.

## The PS2 half: three gaps

pnach is not a bytecode. It is a line format:
`patch=<place>,<cpu>,<address>,<type>,<data>`, with an `extended` type carrying
a small opcode set of its own — `0x3040` increment, `0x3050` decrement, `0x4000`
fill, `0x5000` copy, `0x6000` pointer chain with N pointers.

**Most of it maps.** Byte, short, word and double writes are `StoreStatic` at
dmnt's four widths. Increment and decrement are `PerformArithmeticStatic`. The
pointer chain is `LoadRegisterMemory` in a `ControlLoop`.

### Gap 1: big-endian data types, and dmnt has no byte swap

pnach carries `beshort`, `beword` and `bedouble` alongside their little-endian
forms — **six of its nine data types are about width and endianness.**

**`dmnt_cheat_vm.cpp` contains no endianness handling whatsoever.** Searched for
`endian`, `bswap` and `byteswap`: **zero hits.** The Switch is little-endian and
the VM never needed the concept.

**A shared VM needs a byte-swap, either as an opcode or as a width modifier.**
This is a real addition, and a small one.

### Gap 2: `place` is scheduling, and dmnt has no concept of it

pnach's first field says **when** a patch runs:

```
PPT_ONCE_ON_LOAD = 0
PPT_CONTINUOUSLY = 1
PPT_COMBINED_0_1 = 2
PPT_ON_LOAD_OR_WHEN_ENABLED = 3
```

**dmnt has no scheduling at all.** Atmosphere runs the whole cheat program every
frame, so "apply once at load" cannot be expressed as bytecode — it is a
property *of the program*, not an instruction *in* it.

**This belongs outside the VM**, as metadata on the compiled cheat, next to its
name and enabled flag. **Do not add an opcode for it.** A "run once" opcode
would make the program's behaviour depend on execution history, which is exactly
what makes a VM hard to reason about.

**And ARMSX2 warns that its own semantics are muddier than the names suggest**,
in a comment above the enum: `0` patches may or may not land before the first
vsync depending on circumstances. **Any shared design should define the
lifecycle it wants rather than inherit that.**

### Gap 3: the memory region selector is guest knowledge, and this one is structural

pnach's second field selects **`EE` or `IOP`** — two processors with two address
spaces. dmnt's equivalent is `MemoryAccessType`: `MainNso`, `Heap`, `Alias`,
`Aslr`.

**Neither list means anything to the other.** `MainNso` is a Switch executable
concept. `IOP` is a PS2 coprocessor. The Wii U, Vita and 360 would each add
their own.

**This is the third time this repo has hit the same shape.** Texture classes are
backend-declared because ARMSX2 has two and melonDS three. Upscale filters are
backend-declared because ARMSX2's `Anime4K` is a neural net and melonDS's is a
nine-texel kernel. **The cheat VM's region selector is the same finding again:**

> **The instruction set is shared. The region namespace is declared by the
> backend.**

A compiled cheat carries an opaque region id; the backend resolves it to an
address space. **dmnt's four-value enum must not become the shared type**, for
the same reason `GSTextureUpscaleAlgorithm` did not.

## Consequences for the design

1. **Take dmnt as the instruction set.** It already covers the 3DS by
   construction and most of the PS2.
2. **Add a byte swap.** Six of pnach's nine data types need it.
3. **Do not add scheduling.** Carry `place` as metadata on the cheat, and define
   the lifecycle rather than inheriting ARMSX2's.
4. **Replace `MemoryAccessType` with a backend-declared region list.** This is
   the change that makes the VM shareable at all.
5. **Per-format front ends compile to the bytecode**, which was already the
   plan. This confirms two of the six formats compile; four remain unchecked.

## A fifth format, found on the way

azahar's `docs/thor-cheat-gaps.md` — a device scan of 113 3DS games from
2026-05-09 — records that some titles have cheats **only as NTR `.plg`
plugins**.

**Those are compiled ARM code, not codes.** They do not compile to any VM
bytecode; they are programs that run on the guest. **That is a sixth category
this repo's format list does not have**, and it is the one case where "write a
front end" is not the answer.

That document is also a working example of the cheat-coverage problem the
library badge implies: it names the sources it checked — CTRPF-AR-CHEAT-CODES,
Sharkive, citra-games-wiki, a GameBrew index — and records **29 of 113 games
with no cheat available at all**.

## Limits

- **Four of the six formats are unchecked**: Cemu graphic packs, `.ncl`, and the
  two remaining. This read covers pnach, Gateway/AR and dmnt.
- **No claim that the compilers are easy.** Establishing that an instruction
  set is sufficient is not the same as writing five front ends.
- **Nothing was built or run.** This is a read of three files.
