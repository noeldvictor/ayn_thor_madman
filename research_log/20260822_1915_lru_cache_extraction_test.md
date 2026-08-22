# The LRU cache extraction test

Goal: run the cheapest possible test of the extraction machinery on what
looked like the clearest duplication in the fleet.

Date: 2026-08-22, 19:15 EDT.

## Result: it is not duplication. I was wrong.

`capability_inventory.md` and `shared_layer/PATTERNS.md` both recorded three
LRU caches as "the same data structure, three times, no guest hardware
involved, nothing to preserve per fork". **That is false.**

Reading them shows three different designs solving three different problems.

| | ARMSX2 | azahar-thor | eden-thor |
| --- | --- | --- | --- |
| File | `common/LRUCache.h` | `src/common/static_lru_cache.h` | `src/common/lru_cache.h` |
| Lines | 122 | 113 | 139 |
| Type | `LRUCache<K, V>` | `StaticLRUCache<K, V, Size>` | `LeastRecentlyUsedCache<Traits>` |
| Storage | `std::map`, or `StringMap` for string keys | `std::array`, statically allocated | intrusive list plus an item pool |
| Ordering | `last_access` counter, `uint64` | `std::list` of key and index | `Item*` next and prev, tick based |
| Capacity | runtime, resizable | **compile time template parameter** | grows through the pool |
| Allocation | dynamic | **none** | pooled |
| Eviction | on insert, or manual | on insert | by tick |
| Licence | GPL-3.0+ | **Boost Software License** | GPL-2.0-or-later |

### They are not interchangeable

- **azahar's is statically allocated on purpose.** Its header says so: an
  array instead of a map so elements are statically allocated, and insert and
  get merged into one `request` method. That is a hot-path design that avoids
  allocation. Replacing it with a map-backed cache would be a regression.
- **eden's is tick based with an intrusive list.** It tracks a `TickType`, not
  an access counter, so entries can be invalidated by emulated time rather
  than by access order alone. A counter-based cache cannot express that.
- **ARMSX2's is the general-purpose one**, resizable at run time with an
  optional manual eviction mode.

Consolidating them would mean either shipping three policies behind one name,
or forcing one design onto three different constraints. The second is a
performance regression dressed as cleanup.

## What this changes

### 1. Apparent duplication is not always duplication

**Counting implementations is not evidence of waste.** The inventory found
three files with similar names and recorded them as one capability written
three times. Reading them took ten minutes and reversed the conclusion.

**Rule to adopt: read every implementation before recording a duplication.**
A capability row that was never read should say so.

### 2. The extraction order changes

`CLAUDE.md` ordered extraction by risk and put the LRU cache first, as the
cheapest proof of the machinery. **It is no longer a candidate at all.** The
GPU driver manager moves to first.

That one still looks genuine: six forks, one GPU, and the variation is in the
UI rather than the algorithm. But **it has not been read either**, so treat
that as unproven until it is.

### 3. The licence gate got a data point

azahar's is under the **Boost Software License**, inherited from
`boost::compute`, not the GPL that covers the rest of that fork. **A fork's
licence is not uniform.** A file may be more permissive than its repository,
and the extraction licence gate must be checked per file rather than per fork.

That cuts both ways: a GPL-3.0 fork may hold a permissively licensed file that
is safe to extract into a shared module usable by anything.

## Method note

This took one command per file and about ten minutes. It reversed a conclusion
that two documents had already recorded as fact and that an extraction plan was
built on.

**The cheap test was worth running precisely because it was cheap.** Run the
same test on the GPU driver manager before extracting it.

## Next

1. Read all six GPU driver manager implementations before treating them as one
   capability.
2. Sweep `capability_inventory.md` and mark every row as read or unread. A row
   recorded from a file listing is a hypothesis, not a finding.
3. Correct `shared_layer/PATTERNS.md`, which repeats the wrong claim under
   Support patterns.
