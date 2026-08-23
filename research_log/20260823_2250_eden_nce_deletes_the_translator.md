# eden deleted the recompiler, does Rosetta's AOT patch, and throws it away every launch

**Goal: find the largest unexamined waste at the intersection of unification and
the Rosetta model.**

**Found. eden runs Switch guest code natively on this ARM64 host — no
translation at all — using whole-text-segment ahead-of-time patching keyed by the
guest build ID. Then it discards the result and redoes it on the next launch.**

**The type for the cache that would fix this exists in the header and is used
nowhere.**

## What NCE is

`src/core/arm/nce/`, alongside `src/core/arm/dynarmic/`. Two execution backends,
and the second one is not a recompiler:

| File | Size |
| --- | --- |
| `visitor_base.h` | 88,838 bytes |
| `patcher.cpp` | 27,089 |
| `interpreter_visitor.cpp` | 23,134 |
| `arm_nce.cpp` | 15,526 |
| `arm_nce.s` | 9,591 |

**`GetArchitecture()` returns `Architecture::AArch64` and the guest runs on the
host's own pipeline.** Only what cannot run at EL0 is intercepted — that is what
`arm_nce.s` and the interpreter visitor are for.

**This is the DELETE operation applied to pipeline 1, code translation**, which
`shared_layer/PATTERNS.md` calls the deepest reach into a core and
`CLAUDE.md` orders **last** because of that depth. **eden did it already**, and
it is the only instance in the fleet.

**The reason it is possible is the reason `CLAUDE.md` already gives for eden
being the hardest fork to reason about: its guest ISA is the host ISA.** That
line was recorded as a difficulty. **It is also the opportunity, and nobody wrote
that down.**

## The mechanism is Rosetta's, and it is the third instance in this fleet

`src/core/arm/nce/patcher.h`:

```cpp
using ModuleID = std::array<u8, 32>;  // NSO build ID
struct PatchCacheKey {
    ModuleID module_id;
    uintptr_t offset;
};
enum class PatchMode : u32 { None, PreText, PostData, Split };
bool PatchText(std::span<const u8> program_image, const Kernel::CodeSet::Segment& code);
bool RelocateAndCopy(Common::ProcessAddress load_base, ..., EntryTrampolines* out_trampolines);
```

Every part of Rosetta's shape is there:

- **Whole text segment, ahead of time**, not block-at-a-time on demand.
- **A patch section placed around the module** — before `.text`, after `.data`,
  or split.
- **Relocation and trampolines**, which is the hard part of moving emitted code.
- **A content-addressed key**, the 32-byte NSO build ID.

**That build ID is the same key `CLAUDE.md` already records for binding a patch
to the right game build.** One identifier, two subsystems, recorded once.

## The waste: it re-does the work on every launch

`PatchText` and `RelocateAndCopy` are called from the **loader** —
`src/core/loader/nso.cpp` and `nro.cpp` — so they run **at every module load, on
every launch**.

**`PatchCacheKey` is declared, given a `std::hash` specialisation, and used
nowhere.** Verified with `git grep 'PatchCacheKey'` across the whole fork: four
hits, all inside the declaration itself.

**That is dead scaffolding for a cache nobody built.**

## The two halves are in two forks and neither knows

| Half | Fork | State |
| --- | --- | --- |
| **AOT whole-text patching, relocation, content-addressed key** | **eden**, NCE | built; **not persisted** |
| **Persisting relocatable emitted code, with a validity key** | **ARMSX2**, VU program cache | built and tested; **VU only** |

**ARMSX2 solved exactly the problem eden's unused key was declared for**: a
constant-VA arena plus a placement-relative fixup table make emitted output
relocatable, a 64-byte options sentinel plus an ABI-version handshake decide
whether a cached artifact is still valid, and payloads are content-addressed and
written tmp-then-rename. See
[`20260823_2205_translate_once_ship_it.md`](20260823_2205_translate_once_ship_it.md).

**Neither fork cites the other. Neither is recorded in this repo.** This is the
PROPAGATE operation with both endpoints already built.

## The open question worth more than this finding

**Four of the fleet's guests are ARM**: Switch is ARM64, Vita is ARMv7, 3DS is
ARMv6, DS is ARMv4 and ARMv5. **eden proves the same-ISA case works.**

**Searched Vita3K, azahar and melonDS for `nce`, `native.*execut`, `patcher`
and `run.*nativ` in their CPU directories and found nothing**, and no design
note in this repo mentions it. **Treat that as unread rather than settled** —
one search, one wording.

**It turns on one fact that is not established here: whether this device executes
AArch32 at EL0.** The Snapdragon 8 Gen 2's cores differ on this, and a build file
is not evidence — GameThor and melonDS both build `armeabi-v7a`, but building an
ABI is a choice, not proof the device runs it.

**`getprop ro.product.cpu.abilist` settles it in one line.** The device is in use
by somebody else, so it goes to `DEVICE_QUEUE.md`, and it is the cheapest entry
in that file.

**If the answer is no, the question closes for Vita, 3DS and DS**, and NCE stays
a Switch-only technique. **If yes, it reopens the deepest pipeline for three more
backends**, and that is a larger prize than anything else found today.

**Do not assume yes.** ARMv9 cores dropped AArch32 in stages, and this is exactly
the shape of claim this repo has been wrong about repeatedly.

## Limits, stated

- **Nothing here is measured.** No NCE-versus-dynarmic comparison, no patch time,
  no launch time. **The claim is structural: the work is repeated, and the type
  for not repeating it exists unused.**
- **Whether NCE is the default on Android was not established.** A search of the
  Android settings for `use_nce` and `NCE` returned only unrelated matches, so
  this is **unread, not absent.**
- **eden is Tier 2** and holds no custom work, so this is a technique to take, not
  a fork to change. **The standing rule forbids modifying it regardless.**
- **NCE's correctness cost is not assessed here.** Running guest code natively
  means the guest shares the host's real registers and memory protection, and
  what that costs in isolation and debuggability was not read.

## Sources

- eden `src/core/arm/nce/patcher.h`, `patcher.cpp`, `arm_nce.h`, `arm_nce.s`,
  `src/core/loader/nso.cpp`, `src/core/loader/nro.cpp`
- ARMSX2 `pcsx2/arm64/microVU_ProgCache-arm64.h`
- `shared_layer/PATTERNS.md` for the pipeline ordering
