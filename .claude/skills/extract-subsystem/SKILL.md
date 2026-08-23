---
name: extract-subsystem
description: Use before moving any subsystem from a fork into the shared layer. Walks the read-first rule, the five extraction steps, the licence gate, and the build guard that makes duplication structurally impossible. Triggers on "extract", "share this", "move to shared layer", "consolidate", "deduplicate", "unify".
---

# Extracting a subsystem

**Read before you claim. Every "no fork has this" claim made in this project
has been wrong, six times.**

## Step 0: prove the duplication is real

**This step exists because it has reversed three plans.**

Open every implementation. Do not work from file listings, similar names or a
capability row somebody else wrote.

| Looked like | Turned out to be |
| --- | --- |
| Three forks duplicating an LRU cache | three designs: map with a counter, static array with no allocation, intrusive list with ticks |
| Six forks duplicating a driver picker | four concerns: install, launch wiring, remote catalogue, capability detection, suitability assessment |
| Shared texture cache hashing | guest-specific; it hashes guest formats and palettes |
| Seven Vulkan device layers | **genuinely duplicated**, because device setup has no guest semantics |
| Eight pipeline caches | **genuinely duplicated**, and the shape is fixed by the Vulkan API |
| Two upscale algorithm sets | **two different axes**: ARMSX2 by algorithm, melonDS by cost tier |
| Seven forks setting thread affinity | **three of those hits were GUEST kernel code** |

### Separate host-side from guest-side before you count

**An emulator implements the guest's API as a feature**, so a search for a host
mechanism finds the emulated console's version of it.

Searching for `sched_setaffinity` and `CPU_SET` returned seven forks. Cemu's hit
was the Wii U `coreinit` thread API, azahar's was a 3DS kernel syscall, and
rpcsx's was Orbis kernel emulation. **Three of seven were the guest.** A fourth
was inside a vendored library's test app.

**The corrected answer was different in kind**: four forks set host affinity and
**two set none at all**.

### Ask where the boundary already is

**Before designing a boundary, check whether a fork has already drawn one.**
Measured across the fleet on 2026-08-22:

| Layer | Separated by | Everyone else |
| --- | --- | --- |
| Vulkan device | **xenia, `ui/vulkan/` vs `gpu/vulkan/`** | fused into the renderer |
| Code cache | **xenia, `a64_code_cache_posix.cc`** | fused into the emitter |
| Pipeline cache | **nobody** | fused into guest pipeline state |

**Where one fork has already separated it, the extraction is "take that module,
then unpick the others from their own version".** The second half is per-fork
and is where the cost actually is. Say so in the plan, because "merge seven
implementations" describes the wrong work.

Ask three questions and answer them in writing:

1. **Do they do the same job, or the same-sounding job?**
2. **What does each one know that the others do not?** That knowledge is the
   guest side and it stays in the backend.
3. **Would one implementation be a regression for any of them?** azahar's LRU
   avoids allocation on purpose; a map-backed replacement would be slower.

**If any implementation would regress, this is not an extraction.** It may be a
shared *interface* with per-fork implementations, or nothing at all.

Record the answer either way. A negative result stops the next agent repeating
the work.

## Step 1: licence gate

Extracted code keeps the licence of its source. The shared layer inherits the
most restrictive one.

| Source | Shared module can be |
| --- | --- |
| xenia-thor, BSD | anything |
| Cemu-thor, MPL-2.0 | MPL-2.0 or GPL |
| azahar-thor, Vita3K-Thor, GPL-2.0-or-later | GPL-2.0-or-later, or GPL-3.0 |
| ARMSX2, melonDS-android, eden-thor, GameThor, GPL-3.0 | GPL-3.0 only |
| rpcsx-ui-android, GPL-2.0-only | **never; cannot be linked** |

**Check per file, not per fork.** azahar is GPL-2.0-or-later, but its
`static_lru_cache.h` is Boost, inherited from `boost::compute`. A GPL-3.0 fork
may hold a permissive file.

**When quality is close, take the permissive source.**

**Ideas are not copyrightable.** Read rpcsx freely and take techniques. Never
copy its code.

## Step 2: write the contract against the pattern

**Not against one fork's version.** The contract for texture upload is not
ARMSX2's `GSTextureUpscaler` with the names changed.

See [`shared_layer/PATTERNS.md`](../../../shared_layer/PATTERNS.md) for the
eight pipelines and, for each, what is shared and what must stay in the
backend.

The shared side takes an **opaque handle** for anything guest-specific. A
texture cache takes a key the backend computed; it does not compute one.

## Step 3: extract, keeping the best implementation as the base

Compose where the concerns differ. The driver manager is four concerns from
four forks, not one fork's version winning.

Carry the reasons across, not just the code. When two forks disagree and each
states a reason, record both. ARMSX2 keeps its upscaler out of the cache
because "inventing a second owner of that lifetime is how it gets corrupted";
melonDS puts packs inside the cache entry. That disagreement is information.

## Step 4: convert one fork, and measure

**One fork. Not all of them.**

Prove the contract survives contact before converting a second. If the shared
version is slower, the extraction failed and reverting one fork is cheap.

Use `thor-measure`. State the expected signature first.

## Step 5: delete, depend, guard

Only after step 4 passes.

1. **Delete** the fork's implementation. Do not leave it unused; dead code is
   an invitation.
2. **Depend.** The fork's build links the shared module and implements the
   contract, nothing more.
3. **Guard.** The build fails if the subsystem reappears. A new file under a
   deleted path, or a symbol duplicating an owned one, is a build error and not
   a review comment.
4. **Record** in `shared_layer/OWNED.md`: what it covers precisely enough to
   write the guard, which forks are converted, and the paths deleted from each.

**A subsystem is either owned or not owned.** Half-converted is how duplication
returns.

## Order by risk, not by value

0. **Things nobody has built.** They have no incumbent, nothing to reconcile
   and no licence question, so they are the cheapest of all. **Frame pacing is
   the example**: no fork uses Swappy or Vulkan display timing.
1. Things with no guest semantics at all. The Vulkan device layer qualifies:
   instance creation, device selection, queues and allocation cannot encode
   guest behaviour. **Take xenia's; it already separated it, and it is BSD.**
2. The GPU driver manager, composed from its four owners. **Folds into 1.**
3. **The driver pipeline cache blob only.** The shader translation cache in the
   same files is guest-specific and stays. The blob dies on a driver swap; the
   translation cache survives one.
4. Texture upload. The flagship feature lives here. **It needs two axes** —
   algorithm and cost tier — because the two forks that built it chose
   different ones.
5. **The code cache, not code translation.** There is no shared recompiler.
   Executable memory, W^X and icache invalidation are host-side and are about
   2% of a backend, so this **folds into 1** rather than standing alone.

## Expect maintenance wins, not frame wins

xenia's ledger records many incremental GPU levers as `DEAD` or `FLAT`.
Duplication costs maintenance whatever it costs in frames: six driver managers
is six bugs.

**Where an extraction claims frames, prove it per subsystem against the
ledger.** Do not promise a speedup a shared renderer has never delivered.

## Price the cost to the test harness

**Fixtures are savestates.** A change that alters guest state layout invalidates
every fixture for that backend at once — the golden images survive, but nothing
can reach the scene that made them.

**This will happen**, because extraction changes backends on purpose, and
ARMSX2 already carries a `SaveStateLegacy.cpp` from the last time its format
moved.

So before converting a fork:

- Check whether the change touches serialised guest state.
- If it does, **regenerate the fixtures in the same commit**, and make the
  harness fail loudly on a version mismatch. **A quietly wrong fixture is worse
  than a missing one**, because its golden-image diff reads as a rendering
  regression.
