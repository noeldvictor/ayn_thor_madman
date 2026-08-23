# PERSIST is in four forks, not one, and three of them designed the same validity key

**Goal: test the PERSIST finding with a tool built to stop the failure that
produced it.**

`tools/capability_probe.py` searches each capability with **several independent
vocabularies** and refuses to report absence unless every probe misses. **It
found three things the same day's hand searches missed, and the first thing it
caught was its own author's blind spot.**

## The tool failed its own test first, which is the point

Its first run scored **xenia 0 of 5** on `persisted_code_cache`. **xenia has an
AOT precompiler**, already recorded in `shared_layer/TRANSLATION.md`. None of the
five probes used xenia's vocabulary, which is `aot` and `object_cache`.

**A sixth probe was added and xenia now hits.** That is the discipline working:
the tool states how many probes ran, so a thin definition reads as a weak
negative rather than a strong one.

## Correction: three forks persist translated code, not one

| Fork | What it persists | Key | Maturity |
| --- | --- | --- | --- |
| **rpcs3, through rpcsx** | **PPU LLVM objects, and an SPU native object cache** | **`ppu-<base57(sha1)>-<name>/`** | **production, the "Compiling PPU modules" step** |
| ARMSX2 | PS2 VU programs, `.vuprog` | content hash | built, three test files, **default off** |
| xenia | AOT precompiled code, LLVM object cache | — | **default off** |

**rpcs3's is the mature one and it is the strongest evidence this thesis has.**
`PPUThread.cpp:5638` builds the cache directory from the **SHA-1 of the module**:

```cpp
fmt::append(cache_path, "ppu-%s-%s/", fmt::base57(info.sha1),
            info.path.substr(info.path.find_last_of('/') + 1));
```

**Content addressing at production scale, shipped to a large user base for
years.** `jit_object_cache validated_cache` at line 6515 carries the validation.

**rpcsx is GPL-2.0-only. Take the idea, never the code.**

## The convergence: three independent designs of one validity key

**This repo already treats independent convergence as its strongest signal** —
three forks choosing Oboe, two reaching the same overlay API. **Here it happens
for the cache validity key, and it happens three times.**

rpcs3, `PPUThread.cpp:7043`:

```cpp
enum class ppu_settings : u32 {
    platform_bit,
    accurate_dfma, fixup_vnan, fixup_nj_denormals,
    accurate_cache_line_stores, reservations_128_byte, greedy_mode,
    accurate_sat, accurate_fpcc, accurate_vnan, accurate_nj_mode,
    ...
};
be_t<rx::EnumBitSet<ppu_settings>> settings{};
```

**Compare ARMSX2's 64-byte `mVUbuildOptionsSentinel`**, which carries codegen
switches, clamp modes, speedhacks and **three FPCR masks**.

**Both put floating-point behaviour in the cache key**, independently, on
different guests. **`accurate_dfma`, `fixup_vnan`, `fixup_nj_denormals`,
`accurate_fpcc` are the same class of decision as ARMSX2's FPCR masks.**

> **Two mature emulators, two guests, two teams: both concluded that FP semantics
> must be part of a persisted artifact's identity.** That is the strongest
> possible support for the rule this repo wrote as *whatever a block bakes in
> must appear in its key.*

**And the append-only discipline is visible in the fork's own history.** The
enum continues with **`thor_es_async_draw_barrier_v1` through `v8`** and
**`thor_es_dispatch_provenance_v1` through `v6`**. Each codegen change **added a
new bit rather than reusing one**, so an old cache can never be matched against
new codegen. **That is the reserved-tail rule, in production, being used during
active development.**

**One entry is directly relevant:** `arm64_codegen_v1`. **The ARM64 backend's
codegen version is in the cache key**, which is exactly what an ARM64-targeting
fleet needs.

## Correction: melonDS persists upscaled textures, and the output is a shareable pack

**`shared_layer/ARTIFACT_STORE.md` listed "Upscaled textures — mechanism exists:
no — nobody" when it was written. That was wrong within the hour.**

melonDS `GPU3D_Texcache.h:217`, comment verbatim:

> **Persistent disk cache of filtered textures: a self-populating pack** keyed by
> the same content hashes, one directory per filter/scale/game. First sight of a
> texture filters and stores it; later sessions load the stored image instead of
> re-running the filter.

**And the output is not a private cache format.** `HDTexPack.h` records that the
identity scheme and file layout are **shared with the desktop tooling**:

```
tex1_<W>x<H>_<texhash16>_<palhash16|none|$>_<fmt>.png
obj1_<W>x<H>_<tilehash16>_<palhash16|none|$>_<4|8|bmp>.png
bg1_8x8_<tilehash16>_<palhash16|none|$>_<4|8>.png
```

> **Playing the game writes a real HD texture pack that anybody else can
> install.**

**That is PERSIST and community pooling in one feature**, and it is the texture
analogue of Cemu's `ShaderCacheMerger`. It also partly answers the standing
problem that HD packs need somebody to make them.

## The corrected picture

**PERSIST is not a proposal. It is the fleet's existing practice, in four forks,
across three artifact classes, and nothing connected them.**

| Artifact class | Forks | Poolable |
| --- | --- | --- |
| Translated guest code | **rpcs3, ARMSX2, xenia** | content-addressed, so yes in principle |
| Guest shaders and pipelines | Cemu, eden, azahar | **Cemu ships a merger** |
| Filtered textures | **melonDS** | **already a standard pack format** |

## Limits

- **Nothing is measured.** No cache size, no hit rate, no launch time, in any
  fork.
- **rpcs3's cache being shareable between users was not verified**, only that it
  is content-addressed. **Do not claim community pooling for it.**
- **`spu_native_object_cache_enabled()` was found but not read.** Whether the SPU
  cache persists to disk is unconfirmed.
- **The probe set is still thin for some capabilities.** Three probes is the
  stated minimum and several capabilities have exactly three.
- **A false positive this tool surfaced and a human had to judge:** Vita3K's
  "program cache" hit is `VertexProgramCacheKey` in `SceGxm.cpp`, a **guest-side
  in-memory** GXM cache, not a persisted host cache. **The tool reports; it does
  not decide.**

## Sources

- rpcsx `app/src/main/cpp/rpcsx/rpcs3/Emu/Cell/PPUThread.cpp` lines 5638, 6515,
  7043; `android/src/rpcsx-android.cpp:2021`
- melonDS `melonDS-android-lib/src/GPU3D_Texcache.h`, `HDTexPack.h`
- ARMSX2 `pcsx2/arm64/microVU-arm64.cpp`
- `tools/capability_probe.py`
