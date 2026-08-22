# Reading the texture caches

Goal: read pipeline 2 across the fleet before the texture upload extraction is
planned in detail. Pipeline 2 is where the flagship feature lives.

Date: 2026-08-22, 20:15 EDT.

## Result: more genuinely alike than the LRU caches, but the key differs

Unlike the LRU caches, these do share a concept. Every one of them is a
content-addressed lookup with eviction. The divergence is in **what forms the
key** and **where replacement hooks in**.

| Fork | Type | Lines | Keyed on |
| --- | --- | --- | --- |
| ARMSX2 | `GSTextureCache` | 660 | `HashCacheKey`: `TEX0Hash`, `CLUTHash`, TEX0 and TEXA registers, region width and height |
| melonDS-android | `Texcache` | 592 | `TexCacheEntry`: `TextureHash[2]`, `TexPalHash`, RAM start and size, width and height as log2 |
| Cemu-thor | `LatteTexture` | 368 | physical address and data. `LookupTextureByData`, `LookupTexturesByPhysAddr` |
| Vita3K-Thor | `TextureCache` | 177 | hash, plus `SamplerCacheInfo` and `YUVConversionCache` |
| azahar-thor | `CustomTexManager` | 98 | **not a cache.** A replacement asset manager. |
| xenia-thor | decomposed | — | `texture_address`, `texture_info`, `texture_util`, `texture_extent` |

## The real shared concept: data hash plus palette hash

**ARMSX2 and melonDS independently arrived at the same key design.**

```
ARMSX2   HashCacheKey  { TEX0Hash, CLUTHash, TEX0, TEXA, region_w, region_h }
melonDS  TexCacheEntry { TextureHash[2], TexPalHash, RAMStart, RAMSize, ... }
```

Both are **content hash of the texel data, plus a separate content hash of the
palette, plus format and geometry**. Both target paletted-heavy machines: the
PS2 pushed games toward PSMT8 and PSMT4 because of 4 MB of VRAM, and the DS is
paletted throughout.

ARMSX2 even provides `WithRemovedCLUTHash()` and `RemoveCLUTHash()`, so the
same texel data can be looked up independently of its palette. That is a real
insight and melonDS does not have it.

**This is the strongest shared-key candidate in the fleet.** It is not
universal: Cemu keys on physical address, and Vita3K folds sampler and YUV
state into the cache.

## The finding that matters: two opposite integration philosophies

The flagship feature is per-class routing and upscaling. The two forks that
have it disagree about where it belongs, and the disagreement is explicit.

### melonDS integrates it into the cache entry

```cpp
HDTexPack* TexPack = nullptr;
HDTexPack* FilterCache = nullptr;
```

Both live inside `Texcache`. Pack replacement and filtering are part of the
cache, not layered on it.

### ARMSX2 keeps it deliberately separate and pure

From the `GSTextureUpscaler` header:

> This module is deliberately pure: it decides what to do and scales pixel
> buffers, but never touches the hash cache. GSTextureCache owns insertion,
> because the cache map is private to it and inventing a second owner of that
> lifetime is how it gets corrupted.

**These are opposite answers to the same design question, each with a stated
reason.** melonDS gets a simpler data path. ARMSX2 gets one owner of cache
lifetime and a testable pure module.

**The shared layer must choose, and this is the decision to make first.** The
ARMSX2 position is better argued and it is the one that survives a shared
implementation, because a shared upscaler cannot own six different caches'
lifetimes. Take the pure module and let each backend's cache own insertion.

## Where each fork is genuinely different

- **Cemu tracks overlapping textures.** `LatteTextureSliceMipDataOverlap_t`,
  `LatteTextureRelation`, `getSliceMipArrayIndex`. The Wii U lets textures
  alias in memory at slice and mip granularity, which no other fork models.
- **Vita3K folds in YUV conversion and sampler caching.** `YUVConversionCache`,
  `SamplerCacheInfo`. Those are not texture caching; they are in the same class
  because the Vita needs both at the same point.
- **azahar's `CustomTexManager` is not a cache at all.** It is a replacement
  asset manager with a `Material` model, async upload through a thread worker,
  and a `TickFrame()`. It sits beside the surface cache rather than being one.
  **It is the best model in the fleet for pack loading**, which is a different
  job from caching.
- **xenia decomposes** into address, info, util and extent rather than one
  class. That decomposition is closer to what a shared layer needs than any
  single monolithic cache.

## What is shared and what is not

**Shared:**

- Content-addressed lookup with a hash key.
- Eviction and a memory budget.
- The upload path and the point where replacement or upscaling hooks in.
- Pack loading and the async upload worker. Take azahar's model.
- Statistics and decline reasons. Take ARMSX2's, which separates every reason.

**Not shared:**

- What forms the key. Paletted machines want data plus palette; Cemu wants
  physical address; Vita3K wants sampler state folded in.
- Overlap and aliasing models. Guest-specific.
- Format conversion, including YUV.

**Contract shape this implies:** the shared cache takes an opaque key the
backend computes, not a key the shared layer defines. The backend supplies a
hash and a class; the shared layer owns storage, eviction, budget, replacement
and upscaling.

## Correction to PATTERNS.md

Pipeline 2 currently says the shared part includes "hashing". **It should not.**
Hashing is guest-specific: it hashes guest texel formats and guest palettes.
The shared layer takes the hash; it does not compute it.

## Next

1. Fix pipeline 2 in `shared_layer/PATTERNS.md`.
2. Decide integrated against pure for the upscaler. Recommendation above:
   pure, following ARMSX2's stated reason.
3. Read `azahar` `material.h` before designing pack loading. Materials carry
   more than colour and no other fork models that.
