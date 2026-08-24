# A feature probe that cannot fire, and why my `casp` census was the wrong instrument

**Goal: continue through rpcsx's `AGENTS.md` index. `docs/arm64/memory-model.md`
is next, and its second section corrects a conclusion I published this morning.**

## The finding: a macro nothing defines

`util/atomic.hpp` has **LSE2 fast paths for `atomic_t<u128>`**, guarded on
`ARM_FEATURE_LSE2`. **There is no ACLE feature macro for FEAT_LSE2**, so
`rx/types.hpp` inferred it from `__ARM_ARCH_8_4__`, `__ARM_ARCH_8_5__`,
`__ARM_ARCH_8_6__` or `__ARM_ARCH_9__`.

> **Clang on AArch64 defines none of those, at any `-march`.** Verified at
> `armv8.2-a`, `armv8.4-a` and `armv9-a`: the only things that appear are
> `__ARM_ARCH 8` or `9` and the `__ARM_FEATURE_*` family.

**So the macro was never set and every LSE2 path was dead code.** The comment
above the probe called itself a hack; the replacement did not work either.

> **A feature probe that cannot fire is indistinguishable from a feature the
> hardware lacks.**

**That is a twelfth mechanism for `DID_IT_APPLY.md`, and a new one.** The others
are a stale value, a wrong launch path, a second writer, a dispatcher that
misses. **This is a condition that is syntactically valid, compiles cleanly, and
can never be true.** Nothing warns, and the fallback is correct — only slow.

**Its detector is one this project used today for a different question:** check
`clang -dM -E` output rather than the comment above the probe. **That is exactly
how the `+nosve` result was obtained**, and the same instrument answers both.

## What the fallback cost, and why it is not only an instruction count

| operation | with LSE2 | what was running |
| --- | --- | --- |
| `load` | `ldp` + `dmb ish` | **`ldaxp`/`stlxp`/`cbnz` retry loop** |
| `store` | `dmb` + `stp` + `dmb` | falls through to `exchange`, same loop |
| `release` | `dmb` + `stp` | same loop |

> **`STLXP` takes the cache line in EXCLUSIVE state, so a thread that only wants
> to READ acquires it for writing and invalidates every other core's copy.**

**Four sites, and the blast radius is wider than the reservation path:**

| site | what it is |
| --- | --- |
| `vm::g_reservations` (x2) | SPU reservation stamps, shared by every SPU thread |
| **`spu_channel_4_t::values`** | **SPU mailboxes** — `sync_var_t` is `alignas(16)` and exactly 16 bytes, so count and all three queued values are one 128-bit atomic. **Every `try_pop`, `try_read` and `push`** |
| `s_cpu_bits` | the **global CPU-thread bitmask**, shared by every thread in the process |
| RSX label store | one `release`, cold |

**The mailbox is the sharpest: `try_read` is a pure peek, and peeking took the
line exclusive** on a structure written by one thread and read by another.

## And this makes my own census this morning the wrong instrument

**I counted `casp` across six shipped binaries, found zero in 25.9 million
instructions, and closed the Armv8.4 question on it.**

> **Without LSE2 a 16-byte atomic is not a `casp`. It is an `ldaxp`/`stlxp`
> loop. Counting `casp` alone under-detects the thing by construction** — it
> counts the *fixed* case and reports the *broken* case as absent.

**I did also count `ldxp`/`stxp`, and azahar showed 8 — which I dismissed as
negligible without reading.** Read now:

> **All 8 are in one function, `InputManager::NDKMotion::GetStatus()`** —
> a 16-byte atomic read of motion-sensor state. **One function, one thread, at
> most once a frame. Genuinely cold.**

**And rpcsx was never in the census at all**, because PS3 is out of the packed
binary. **So the one fork where 16-byte atomics are hot is the one I did not
measure**, and I generalised from a set that excluded it.

## The Armv8.4 decision survives, for a corrected reason

**Old reason:** nobody does 16-byte atomics.
**Correct reason:** **one fork in the packed binary does, in one cold function;
the fork where it is hot is out of the binary.**

**The decision is unchanged — do not raise the baseline — but the reasoning was
wrong and would have stayed wrong until a backend started using `atomic_t<u128>`
on a hot path.** The revisit trigger is now stated in the right instrument:
**count `ldaxp|stlxp|ldxp|stxp` as well as `casp`.**

**rpcsx's own decision is the mirror image and independently reached:** it moved
`-march` to `armv8.4-a`, *"the lowest baseline that architecturally guarantees
FEAT_LSE2"*, and **deliberately not `armv9-a`, because Armv9 mandates SVE2 and
this device advertises none** — the same conclusion as this repo's, from the same
constraint, in the same week.

**Two preconditions it checked before trusting the change**, both of which any
adopter must also check: **an aligned 16-byte `LDP` is only single-copy atomic
WITH LSE2**, so on a lower baseline the same change is a **data race** rather
than a slow path — hence a configure-time guard that refuses the combination;
and **the alignment has to be real**, which `alignas(16)` plus an
`alignas(4096)` array with 16-byte indices provides.

## Limits

- **Not reproduced.** rpcsx's four sites are read from its document, not from a
  disassembly I took.
- **azahar's 8 were read from the disassembly and attributed by symbol**, but the
  call frequency of `NDKMotion::GetStatus` is asserted from what it is, not
  measured.
- **eden and GameThor remain unmeasured** — they do not build here, so "one fork
  in the packed binary" means one of the six that do.
- **No device. Nothing timed.**

## Sources

- rpcsx `docs/arm64/memory-model.md:98-167`, `util/atomic.hpp`, `rx/types.hpp`
- rpcsx `tools/test_thor_arm64_lse2_atomics.ps1`
- `research_log/20260824_2230_five_of_six_forks_ship_a_runtime_atomic_dispatch.md`
