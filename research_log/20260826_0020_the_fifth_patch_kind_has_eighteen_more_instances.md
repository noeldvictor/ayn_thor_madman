# The fifth patch kind has eighteen more instances, and one of them fixes one game while breaking another

**Goal: I proposed "game quirk" as a fifth patch kind from ONE instance and wrote
that "a second would make it a pattern". Look for a second.**

**Device-free: a fleet sweep and one header read. No device used.**

## Confirmed, and not marginally

**Swept five forks for `gamefix|game_?quirk|GameDatabase|per-title hack`, vendored
trees excluded:**

| Fork | Files |
| --- | --- |
| **ARMSX2** | **67** |
| Cemu, azahar, Vita3K, melonDS | **0** |

**`pcsx2/Config.h` carries an 18-bit `GamefixOptions` bitfield**, every bit a
per-title switch that changes the emulator's own semantics:

| Quirk | What it changes |
| --- | --- |
| `GoemonTlbHack` | *"the game need to access unmapped virtual address... **tlb are preloaded at startup**"* — **MMU behaviour** |
| `VuAddSubHack` | Tri-ace titles need **`VU ADDI` bit-accurate** for their encryption |
| `VUSyncHack` | **makes microVU run behind the EE** to avoid register sync issues |
| `SoftwareRendererFMVHack` | **switches renderer** for FMVs |
| `DMABusyHack` | denies DMAC writes when busy — *"**this is correct behaviour** but bad timing can cause problems"* |
| `InstantDMAHack`, `GIFFIFOHack`, `VIFFIFOHack`, `VIF1StallHack`, `EETimingHack` | **timing and FIFO models** |

> **Nineteen instances across two forks. The category is not a proposal any
> more.** And **ARMSX2's is a shipped, named, persisted taxonomy** — which is more
> than this repo had for any of its other four patch kinds when they were
> recorded.

## The single strongest argument for "per-title, never global"

> **`XgKickHack`** — *"Erementar Gerad, adds more delay to VU XGkick instructions.
> **Corrects the color of some graphics, but BREAKS TRI-ACE GAMES and others.**"*

**One quirk that fixes one title and breaks another.** No global default is
correct. **That is the case for per-game overrides made by a shipped emulator, in
one comment**, and it is stronger than any argument this repo has written for the
same conclusion.

## A quirk ID is PERSISTED, so the list is append-only

**The comment on the first bit is the find:**

> *"**No reader**: `eeMulRound` and `emitDefectiveFmul` model the multiplier
> defect this patched one product of. **The bit stays because its `GamefixId`
> indexes `vu_capture`'s on-disk gamefix mask.**"*

**A quirk with no implementation left, kept because removing it would shift every
later index and invalidate every stored mask.**

> **That is the THIRD instance in ARMSX2 alone of "this enum is persisted, so it
> is append-only"** — after `mVUbuildOptionsSentinel`'s reserved tail, and
> `GSTextureUpscaleAlgorithm`'s *"entries can only ever be appended"*.

**So a quirk list is not just backend-declared, it is backend-declared AND
STABLE.** A backend that renumbers its quirks orphans every per-game record that
referenced them — **the same failure as renaming a settings key, which this repo
recorded from XenDroid this morning.**

## And one quirk exists for an x86 reason

> **`VUOverflowHack`** — *"Tries to simulate overflow flag checks (**not really
> possible on x86 without soft floats**)."*

**A quirk whose stated justification is a HOST limitation, not a guest one.**
Whether ARM64 changes that is **unexamined** — and it is exactly the shape of the
x86-detour audit this project already runs. **Recorded as a candidate, not a
claim.**

## What this changes

- **The fifth patch kind is confirmed**, with a shipped reference taxonomy to
  copy rather than design.
- **The contract needs quirk IDs to be stable and backend-declared**, not merely
  backend-declared.
- **`XgKickHack` is the example to quote** whenever somebody proposes making a
  per-game fix global.

## Limits

- **The sweep covered five forks with four vocabularies.** Cemu, azahar, Vita3K
  and melonDS returned zero, **which may mean they express the idea in words I
  did not search** — the search-for-a-name trap, and I have hit it twice today.
  **Treat those zeros as weak.**
- **The 18 bits were read from one header.** How each is applied, and whether any
  is dead beyond the one that says so, was not checked.
- **`VUOverflowHack`'s ARM64 status is unexamined.** The comment is PCSX2's, from
  its x86 era.
- **Nothing built or run, no device.**

## Sources

- ARMSX2 `pcsx2/Config.h:1385-1412`
- `research_log/20260825_2340_a_fifth_patch_kind_decided_at_load.md`
