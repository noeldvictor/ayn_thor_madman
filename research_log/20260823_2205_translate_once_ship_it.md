# The Rosetta model is already built in this fleet, on both sides, and nothing connects them

**Goal: keep looking for the big missing thing at the intersection of
unification, Vulkan and the Rosetta model.**

**Found it, and it is not a technique to invent. It is a rule that is already
proven twice inside the fleet and has never been stated.**

> **Every artifact an emulator derives at run time is a pure function of the
> guest content and the host configuration. Fix the host configuration and every
> one of those artifacts becomes cacheable across launches, poolable across
> users, and shippable.**

**This project fixes the host configuration harder than anything else in
emulation**: one device, one GPU, one pinned driver, one ABI, one toolchain.
**Every one of those pins was chosen for a different reason.** Together they buy
this.

## What is already built

| Side | Fork | What it persists | Poolable between users |
| --- | --- | --- | --- |
| **CPU** | **ARMSX2** | translated VU programs, content-addressed and relocatable | **yes, by construction** |
| **GPU** | **Cemu** | `shaderCache/transferable`, plus `precompiled` | **transferable yes, and it ships a merger** |
| GPU | eden | the guest shader environment per shader | yes |

**Neither side knows the other exists**, and this repo recorded neither.

## The CPU half: ARMSX2 has a persisted JIT, with tests

**This corrects a negative claim made one hour earlier in this session.** A
search for `SaveCodeCache`, `code_cache.*persist` and `AotCache` returned
nothing across seven forks. **A second search using different words found it
immediately.** Tenth time.

`pcsx2/arm64/microVU_ProgCache-arm64.{h,inl}` and
`microVU_Persist-arm64.{h,inl}`, **2,576 lines**, plus three test files. The
production path, from the test file's own header:

> compile (recording on) -> `mVUreset` -> `SaveAllPrograms` writes INDEX +
> `.vuprog` payload -> next dispatch misses contentMap + per-PC deque ->
> `mVUsearchProg` calls `mVUProgCache::TryLoadProgram` -> payload read +
> `HydrateProgram` -> **program runs with ZERO block compiles and a bit-identical
> post-state.**

**It solves the hard problem, which is relocation:**

> Constant-VA arena layout + a **placement-relative fixup table** make the
> persisted vixl output **relocatable** (absolute `armEmitCall` /
> `armMoveAddressToReg` targets are patched on load); any miss or payload
> rejection falls back to a normal recompile.

**And the payloads are content-addressed** — `<root>/<hh>/<hash32hex>.vuprog`,
written tmp-then-rename. **Content addressing is what makes one user's entry
mergeable into another's**, which is the same property Cemu's shader cache has.

**Failure is designed for.** A corrupt payload, a missing payload file and a
telemetry-only index entry each degrade to a clean recompile, and each has a
test.

### The validity key is the best engineering read in the fleet so far

`mVUbuildOptionsSentinel` in `pcsx2/arm64/microVU-arm64.cpp` builds a **64-byte
fixed-layout snapshot** of everything that changes emitted code:

- eleven build-time codegen switches, one byte each
- eight clamp-mode bits for VU0 and VU1
- eight speedhacks and gamefixes that gate emit shape
- **three FPCR bitmasks**, because mVU emits `MSR FPCR` loads
- **the recording flag itself**, because recording changes the emitted forms —
  canonical `movz`+`movk` for self-block pointers, forced-long cross-chunk
  conditional branches

Three rules travel with it, and all three are worth taking:

- **`static_assert(sizeof(Snapshot) == 64)`**, so layout drift is a compile error
  rather than a silently mismatched cache.
- **A reserved tail**, so adding an option does not shift the fields below it.
- **Reclaim a reserved byte only where zero means "feature off, old
  behaviour"**, so the off-state key stays bit-identical and **enabling a feature
  does not evict the cache of every user who never turns it on.**

**This is the rule this repo wrote independently in
`app/shell/.../BlockCacheKey.kt`** — whatever a block bakes in must appear in its
key. **ARMSX2 has the production version, and it has one rule the Kotlin does
not: the zero-state compatibility rule above.** Take it.

**It also confirms a lead `CLAUDE.md` records as unmeasured.** The FPCR masks are
in the key *because mVU emits FPCR writes*. The Cortex-X3 guide says `FPCR` is
**not renamed** and its reads are non-speculative and in-order. **So ARMSX2 is a
confirmed instance of the guest-FP-status lead, not a hypothetical one.**

## The GPU half: Cemu already pools caches between users

`src/tools/ShaderCacheMerger.cpp`, 145 lines. It opens
`shaderCache/transferable/<titleId>` and
`shaderCache/transferable/merge/<titleId>`, copies every entry the main cache
does not already hold, reports how many were added, and deletes the source.

**That is community cache pooling, shipped.** It is why Wii U shader caches
circulated for years.

**And it only works on the transferable half.** The `precompiled` half is the
driver's compiled output, and on desktop every user has a different GPU and
driver, so it cannot be shared.

> **The Thor changes exactly that one line.** One pinned Turnip build means the
> precompiled half is valid for every user of this app. **And the precompiled
> half is the one that costs the compile stall.**

See [`research_log/20260823_2110_shipped_pipeline_cache.md`](20260823_2110_shipped_pipeline_cache.md)
for the Vulkan side and the Steam Deck precedent.

## What this changes

**The unification argument has been about removing duplicate code.** Nine
candidates were read and most shrank, which this repo recorded honestly.

**This is a different kind of unification, and it does not shrink.** It is not
"seven implementations become one". It is **one cache serving seven backends and
every user**, where the thing being shared is not source but computed results.

Restating the operations table in `CLAUDE.md`: this is not UNIFY, DELETE, ISOLATE
or PROPAGATE. **It is a fifth: PERSIST.** Ask of any hot path: *is its output a
pure function of guest content and host configuration?* If yes, it should be
computed once for everybody, not once per launch per user.

Candidates, ranked by whether the mechanism already exists:

| Artifact | Mechanism exists | Where |
| --- | --- | --- |
| Translated guest code | **yes** | ARMSX2, VU only |
| Guest shader to SPIR-V | **yes** | Cemu, eden, azahar |
| Driver pipeline blob | **yes** | all eight forks call `vkGetPipelineCacheData` |
| Upscaled textures | **no** | the upscalers exist; nothing persists the output |
| Per-game known-good settings | partly | GameThor's 29 fixes, as code not data |

## Limits, stated

- **ARMSX2's cache covers the VU only**, not the EE or the IOP recompiler. It is
  a proof that the shape works on this fleet's hardest case, not a finished CPU
  cache.
- **It is default-off, and the fork says why in the same line.**
  `pcsx2/Pcsx2Config.cpp:463`:

  ```cpp
  EnableVUProgramCache = false; // default off; opt-in until the on-disk cache
                                // is validated on the target hardware
  ```

  **The target hardware is the Thor.** So ARMSX2 has queued a device experiment,
  stated its own gate, and not run it. **That is a `DEVICE_QUEUE` entry written
  by the fork itself**, and it is entry 17.
- **Nothing is measured.** No boot time, no cache size, no hit rate, no frame
  data. **The claim is that the mechanism exists and is tested, not that it is
  fast.**
- **Self-modifying guest code is the standing risk** for any persisted code
  cache. **Searched the ProgCache header for `self-modif`, `SMC` and
  `invalidate` and found only a test-harness comment**, so how ARMSX2 handles it
  is **unread, not absent.** The content hash over guest program bytes is the
  likely answer and was not confirmed.
- **Shipping a cache raises questions this log does not answer**: what a cache
  built by one person and run by another means for trust, and how a cache is
  invalidated when the app updates. ARMSX2's ABI-version handshake is the
  starting answer for the second.

## Sources

- ARMSX2 `pcsx2/arm64/microVU_ProgCache-arm64.h` and `.inl`,
  `microVU_Persist-arm64.h` and `.inl`, `pcsx2/arm64/microVU-arm64.cpp`,
  `tests/ctest/core/recompilers/mvu_progcache_disk_tests.cpp`
- Cemu `src/tools/ShaderCacheMerger.cpp`,
  `src/Cafe/HW/Latte/Renderer/Vulkan/VulkanPipelineStableCache.cpp`
- eden `src/video_core/shader_environment.cpp`
- `hardware_ref/thor/cpu/CORTEX_X3_NOTES.md` for the FPCR behaviour
