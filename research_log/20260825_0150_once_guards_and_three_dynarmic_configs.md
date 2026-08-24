# The once-only log guard, verified and mostly closed; and three forks configure one JIT three ways

**Goal: read Vita3K's `20260821-arm64-review-of-the-vita3k-tree.md` in full and
test its two sweepable findings against the fleet.**

## 1. The once-only guard: real, verified by emitted code, and it does not generalise

**Vita3K's Finding 2** is that `LOG_ONCE` expands to
`std::atomic_flag::test_and_set()`, which is a **sequentially-consistent
read-modify-write executed every time control reaches the site, forever** — not
only the first time. `RET_ERROR` expands to one, so every HLE stub returning an
error pays it, and `TextureCache::upload_texture` alone contains six.

**That is what its 12,074 `__aarch64_swp1_acq_rel` call sites were.**

**Verified here by compiling the three idioms**, NDK 29, `-std=c++20`,
`-march=armv8.2-a+lse`, and reading the emitted fast path:

| idiom | emitted steady state |
| --- | --- |
| `std::call_once` | **`adrp`, `add`, `ldar`, `cmn`, `b.eq`** — an acquire LOAD |
| **`atomic_flag::test_and_set()`** | **`swpalb`** — an acquire-release swap, every call |
| **the C++20 fix**: `test(relaxed)` then `test_and_set()` | **`ldrb`, `tbnz`** — falls through to `swpalb` only on the first pass |

**The fix is only available because the standard row is C++20.** `atomic_flag`
had no `test()` before C++20, so `test_and_set()` was the *only* way to ask, and
every codebase written against C++17 reaches for it. **This is the same shape as
the x86-detour findings: an idiom that was correct for its constraints, carried
past them.**

### But the fleet is mostly not exposed, and that is the useful half

**Swept all eight forks** with
`git grep --recurse-submodules -E 'test_and_set'` over `*.cpp *.h *.hpp`,
vendored trees removed, then **read the definitions**:

| Fork | `test_and_set` | Dominant once-idiom |
| --- | --- | --- |
| **rpcsx** | **53** | one `LOG_ONCE` using `.exchange(true)` — **one file**, raw sockets |
| Vita3K | 4 | **the macro Finding 2 fixed** |
| eden | 2 | **16 `std::call_once`** |
| xenia | 2 | `std::call_once` |
| Cemu | 1 | `std::call_once` |
| ARMSX2 | 0 | **5 `std::call_once`**, plus 2 plain `static bool logged_once` |
| azahar, melonDS | 0 | — |

> **`std::call_once`'s fast path is an acquire load, not an RMW**, so the
> fleet's dominant idiom is already the cheap one. **The lane closes for six
> forks**, and rpcsx's single instance is in one non-hot file.

**Source occurrences are the wrong unit here.** Vita3K's **4** occurrences
produced **12,074 call sites**, because the macro is defined once and used
everywhere. **Count expansions, not definitions** — the same lesson as
`unknown[+X]` being the JIT arena rather than a function.

### A side result: `+rcpc` reaches a real primitive

`std::call_once`'s fast path is **`ldar` without `+rcpc` and `ldapr` with it.**
The flag added to `THOR_TARGET.md` yesterday was justified from a synthetic
probe; **this is a ubiquitous library primitive that changes because of it.**
**No speed claim** — `DEVICE_QUEUE.md` entry 23 still owns that.

## 2. Three forks share one JIT and configure it three ways

**Vita3K's Finding 4** is that it leaves `fastmem_exclusive_access` at the
default `false`, so guest `LDREX`/`STREX` — which the Vita kernel uses for every
lock — fall out of the fastmem arena into callbacks. It asks for a measured A/B
and for the unsafe knobs to be "surfaced as a per-game config toggle rather than
enabling globally".

**eden already built exactly that, in this fleet, on the same library.**

| Fork | Memory model | `fastmem_exclusive_access` | Configuration surface |
| --- | --- | --- | --- |
| **eden** | fastmem arena | **on, tied to fastmem being available** | **a `CpuAccuracy` tier plus ~10 named per-option settings** |
| Vita3K | fastmem arena | **off** (the default) | none |
| **azahar** | **page table** | not applicable | none |

**eden's default is the instructive part**, because it is derived rather than
chosen:

```cpp
config.fastmem_exclusive_access = config.fastmem_pointer != std::nullopt;
config.recompile_on_exclusive_fastmem_failure = true;
```

Then every option is individually revocable by a named setting —
`cpuopt_fastmem_exclusives`, `cpuopt_recompile_exclusives`,
`cpuopt_ignore_memory_aborts` — and `unsafe_optimizations` is reached **only**
through the `Unsafe` accuracy tier, whose members are separately named
(`cpuopt_unsafe_unfuse_fma`, `cpuopt_unsafe_host_mmu`).

> **That is this project's per-game override design, already implemented, for
> the exact subsystem Vita3K says needs it.** A tier for the person who wants
> one switch, named sub-options for the person who does not, and a default that
> follows from a fact rather than from taste.

**And azahar is the reminder that a shared library is not a shared
configuration.** It uses `config.page_table`, so it takes the per-access lookup
rather than the arena, and the whole exclusive-access question does not arise
there. **Three users, three memory models.**

**For the backend contract**, this is a concrete example of a **declared
per-backend extension**: a CPU-accuracy tier with named sub-toggles is
meaningful for the three dynarmic backends and meaningless for ARMSX2's
emitters. **It must not become a shared enum**, for the same reason texture
classes and filter lists did not.

## Limits

- **Nothing measured on device.** Vita3K states this of its own findings 1 and
  2 as well: *"no speedup is claimed for either"*.
- **The emitted-code comparison is one compiler, one libc++, one flag set.** A
  different `call_once` implementation could use an RMW.
- **The `test_and_set` sweep counts source occurrences**, and the section above
  says why that is the wrong unit. The definitions were read; the expansion
  counts were not computed for any fork but Vita3K's, which measured its own.
- **eden's guards were read, not built.** eden does not build on this machine.
- **azahar's `page_table` choice is read from one file**; whether it can use
  fastmem was not investigated.

## Sources

- Vita3K `docs/research/20260821-arm64-review-of-the-vita3k-tree.md`
- eden `src/core/arm/dynarmic/arm_dynarmic_32.cpp:185-270`
- azahar `src/core/arm/dynarmic/arm_dynarmic.cpp:359-368`
- rpcsx `.../sys_net/lv2_socket_raw.cpp:17`
