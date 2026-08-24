# Our own compile target omits `+rcpc`, and it is the one flag that changes codegen by itself

**Goal: chase a bug class found in rpcsx — a feature guarded on a macro the
compiler never defines — and check the fleet.**

**The class is rpcsx-only. The check found something better: this project's own
recommended compile line omits a feature the device has, and unlike every other
target feature this repo has studied, the compiler uses it automatically.**

## The bug class, verified on our own toolchain

rpcsx's `atomic_t<u128>` LSE2 fast paths were guarded on `ARM_FEATURE_LSE2`,
inferred from `__ARM_ARCH_8_4__`, `__ARM_ARCH_8_5__`, `__ARM_ARCH_8_6__` or
`__ARM_ARCH_9__`. **Clang defines none of those.**

**Tested directly with this box's NDK 30 clang, `aarch64-linux-android33`:**

| `-march` | `__ARM_ARCH` | Numbered `__ARM_ARCH_8_x__` |
| --- | --- | --- |
| `armv8.2-a` | **8** | **none** |
| `armv8.4-a` | **8** | **none** |
| `armv9-a` | 9 | **none** |

**So the guard was never true and every LSE2 path was dead code.** The fallback is
an `LDAXP`/`STLXP`/`CBNZ` retry loop, and **`STLXP` takes the cache line in
exclusive state — so a thread that only wants to read acquires it for writing and
invalidates every other core's copy**, on a structure shared by every guest
thread.

**Swept the fleet for the same shape.** `__ARM_ARCH_8_x__`, `__ARM_ARCH_9__` and
`__ARM_ARCH_8__` across all nine forks' own source, vendored trees excluded:
**rpcsx 2 sites, every other fork zero.** A legitimate empty, and confirmed
because the compiler behaviour was tested rather than assumed.

**There is also no `__ARM_FEATURE_LSE2` macro at any `-march` tested**, and
`-march=armv8.2-a+lse2` is rejected by clang. **LSE2 must be detected at run time
through HWCAP**, not at compile time.

## The better finding: `+rcpc` is missing from `THOR_TARGET.md`

**`hardware_ref/thor/THOR_TARGET.md` recommends:**

```
-march=armv8.2-a+crc+lse+fp16+dotprod+sha3+i8mm+bf16 -mtune=cortex-x3
```

**Compiled with exactly that line, the macros defined are** `ATOMICS`, `BF16`,
`CRC32`, `DOTPROD`, `FP16_VECTOR_ARITHMETIC`, `MATMUL_INT8`, `SHA3` — **and not
`RCPC`.**

**`-mtune` changes scheduling, not features.** Only `-march` and `-mcpu` add
them.

**The device has it.** `CLAUDE.md`'s own recorded `/proc/cpuinfo` line carries
**`lrcpc` and `ilrcpc`** — LRCPC and LRCPC2.

**`+rcpc` is accepted by clang and defines the macro.**

## And it is the exception to this repo's "permission, not speed" rule

**`CLAUDE.md` records xenia's result**: enabling `+crypto+sha3+crc+dotprod` and
disassembling 1,779,182 instructions produced **zero** `eor3`, `sha*`, `crc32*`
or `udot`. *"These flags change the compiler's PERMISSION and nothing else."*

**`+rcpc` does not behave that way. Measured on this box, same source, two
flags:**

```
# -march=armv8.2-a+...+bf16 -mtune=cortex-x3      (THOR_TARGET as written)
    ldar    w0, [x0]

# ... +rcpc
    ldapr   w0, [x0]
```

**The compiler substitutes it by itself, with no intrinsic and no source
change.**

**The scope, tested precisely:**

| C++ memory order | Without `+rcpc` | With `+rcpc` |
| --- | --- | --- |
| **`acquire` load** | `ldar` | **`ldapr`** |
| `seq_cst` load | `ldar` | `ldar` — **unchanged, and correctly so** |
| `relaxed` load | `ldr` | `ldr` |
| `release` store | `stlr` | `stlr` |

**`LDAPR` provides RCpc, which is exactly what C++ `memory_order_acquire`
requires**, so the substitution is correct. **A `seq_cst` load needs RCsc and
keeps `LDAR`**, which is why that row does not move.

## What to do

- **Add `+rcpc` to the recommended line.** It costs nothing, the device has it,
  and the compiler uses it unprompted.
- **Record the exception**: the permission rule holds for every flag studied
  except this one.
- **Note where it will and will not help.** Code using `memory_order_acquire`
  benefits automatically. **Code that uses `seq_cst` everywhere gets nothing** —
  and rpcs3's `atomic_t::load()` is `SEQ_CST` by construction, so that fork would
  need to relax to acquire where correct before seeing any of it.

## Limits

- **Whether `LDAPR` is actually faster than `LDAR` on this SoC is not measured.**
  It is architecturally weaker, so it should be no worse, **and "should" is not a
  measurement.** Queued.
- **The macro tests are on NDK 30's clang.** The standard row pins NDK 29; the
  behaviour is not expected to differ but was not re-tested there.
- **The LSE2 sweep covers the fleet's own source only.**
- **rpcsx's blast-radius claim was read, not reproduced.**

## Sources

- rpcsx `docs/arm64/memory-model.md`, `util/atomic.hpp`, `rx/types.hpp`
- `hardware_ref/thor/THOR_TARGET.md`
- NDK 30 clang, `aarch64-linux-android33`, tested directly on this box
