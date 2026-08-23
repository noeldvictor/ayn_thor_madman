# The propagation ledger

**Lessons one fork learned that the others have not received.**

**This is the operation the project was founded to do.** See
[`UNIFICATION.md`](UNIFICATION.md) section 8: PROPAGATE has the highest value
and the lowest licence cost, because **an idea is not expression** and crosses
walls that code cannot.

## The rules

1. **A propagation lands with a test, or it does not land.** Without executable
   feedback it is a guess wearing a commit message. This is
   environment-in-the-loop at the smallest scale.
2. **Order by whether the target builds**, not by how valuable the lesson is.
   **A fork that does not build is blocked for propagation.** See the build
   column.
3. **Take the idea, not the file, when the source licence forbids the file.**
   rpcsx is GPL-2.0-only and out of the binary; its lessons are still available.
4. **Record a refusal.** A lesson that does not transfer is a finding — see
   Espresso and `SDOT` below.

## Can the target take a propagation today?

**Measured, not assumed. Updated as Phase 0.3 proceeds.**

| Fork | Builds? | Blocker | Ready for propagation |
| --- | --- | --- | --- |
| **melonDS-android** | **yes**, 15 min 27 s | — | **yes** |
| **azahar** | **yes**, 14 min 33 s | — | **yes** |
| Vita3K-Thor | **yes, arm64-only** | its recipe is wrong; `x86_64` fails | **one line away** |
| **eden** | **no** | **`pkg-config`, then `glslangValidator`** — host tools, and the NDK ships neither by that name | **blocked on the box, not the fork** |
| ARMSX2 | in progress | — | — |
| Cemu, xenia, GameThor | not attempted | — | **unknown** |

**Two forks are ready today.** That is two more than when this ledger was
written, and it came from the least glamorous work in the project.

**And the blockers are worth separating**, because they need different fixes:

| Blocker class | Example | Fix |
| --- | --- | --- |
| the fork's own recipe | Vita3K builds from the wrong directory | correct the recipe |
| an ABI the device cannot run | Vita3K, and four others carry it | one line in `abiFilters` |
| **host tools nobody declared** | **eden needs `pkg-config` and `glslangValidator`** | **install, or declare and check** |

**None of them is in an emulator.** After four attempts, every obstacle has been
peripheral — and that is good news for the migration and bad news for estimating
it, because each is a separate accident rather than one systematic difference.

**Vita3K was unblocked by testing a prediction.** The log
[`../work_log/20260823_0027_vita3k_build_attempt.md`](../work_log/20260823_0027_vita3k_build_attempt.md)
predicted that removing `x86_64` from `abiFilters` would make it build.
**Tested 2026-08-23 01:39: it does.** The change was reverted rather than
committed to the fork, because that is the fork's file and nobody asked for the
edit — but **the finding is that one line separates Vita3K from being an agent
target.**

---

## The ledger

**Status values:** `DONE` — landed with a test. `READY` — target builds, nothing
blocks it. `BLOCKED` — target does not build or is unattempted. `REFUSED` — read
and found not to transfer.

### Host-side. These cross every guest and every licence.

| # | Lesson | From | Licence | To | Verify with | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | A per-game override must be **sticky**, or a later global change silently takes it | ARMSX2 | GPL-3 | shared layer | **unit test — written** | **DONE** |
| 2 | Pinning needs **change-tracking**, or a stale whole-object write makes a wrong value permanent | ARMSX2 | GPL-3 | shared layer | **unit test — written** | **DONE** |
| 3 | Some settings are **process-wide** and must be promoted to global, not written per-game | ARMSX2 | GPL-3 | shared layer | **unit test — written** | **DONE** |
| 4 | A physical controller must **hide** the touch overlay | Vita3K | GPL-2+ | shared layer, every backend | **unit test — written** | **DONE** |
| 5 | Building non-arm64 ABIs doubles the native compile and can **break the build** | rpcsx | **GPL-2 only** | five forks | **build guard — written for the shell** | **DONE** for the shell |
| 6 | A migration must **freeze a DTO per version**, never deserialise with the current class | melonDS | GPL-3 | shared layer | **contract — and the hazard is designed out** | **DONE** |
| 7 | A persisted enum is **append-only forever**; group in the UI, not the numbering | ARMSX2 | GPL-3 | shared layer | contract | **READY** |
| 8 | Validate the pipeline cache header and **log which field disagreed** | ARMSX2 | GPL-3 | shared device layer | test on a synthetic blob | **READY** |
| 9 | A `Presentation` is **not torn down** when the activity stops; re-attach every resume | ARMSX2 | GPL-3 | app shell | **applied**, device-verify queued | **DONE**, unverified |
| 10 | `yield` is a **no-op** on ARM — **measured 0.36 ns, identical to `nop`**, on this SoC | rpcs3 bench | idea | every fork | already measured | **DONE**, as a fact |
| 10b | **Do not swap `yield` for `ISB` alone.** `ISB` costs **32x** a `yield`, so it multiplies every hand-tuned backoff. rpcsx **+23%**, xenia **`CONFOUNDED`**, Cemu's time-based attempt **worse on the Thor** | rpcsx, xenia, Cemu | idea | every fork | **the three A/Bs already exist** | **DONE**, as a refusal |
| 11 | Pass **real target features** to the compiler and to LLVM | xenia | BSD | six forks | build-flag guard | **READY** |
| 12 | A **content path resolver** must search many roots; users put files anywhere | Vita3K | GPL-2+ | app shell | **unit test — written** | **DONE** |
| 13 | A driver picker should **advise**, not list — verdicts, not options | rpcsx | **GPL-2 only** | shared driver manager | **unit test — written, on the real file names** | **DONE** |
| 14 | Mark fork divergence at line level with a **`NOTE(thor):`** comment | xenia | BSD | every fork | lint | **READY** |
| 21 | **Park on the address**: `SEVL`/`WFE` + `LDAXR` beats any spin. Reached **independently twice** — ARMSX2 `MonitoredWait` and dynarmic `EmitSpinLockLock` | **dynarmic** | **0BSD** | shared layer, eden | unit test on wake correctness | **READY** — source is 0BSD and already in three forks' trees |
| 22 | **Never `CLREX` on the way out** of a monitored wait — clearing the monitor is itself a wake event. Measured **3.5 wake-ups per wait against 6708** with one added | ARMSX2 | GPL-3 | anything using tier 1 | wake-count assertion | **READY** |
| 23 | **Calibrate the spin budget at run time** rather than fixing an iteration count, so the cost of the backoff instruction stops mattering | ARMSX2 `MeasurePauseTime` | GPL-3 | shared layer | unit test on the calibration loop | **READY** |
| 24 | eden's `Common::SpinLock` still spins on `yield` in **`k_slab_heap`** and **`KThread::m_context_guard`**, while dynarmic's correct one is **already in the same binary**. The fix is a **DELETE** | audit | — | eden | build after removal | **BLOCKED** — eden does not build |
| 25 | **Bake the guest FP environment into the compiled block as an immediate**, so recompiled code never reads `FPCR` | ARMSX2 | GPL-3 | xenia, any ARM64 backend | block-cache reuse test | **READY** |
| 26 | **Hash the FP environment into the block-cache key.** Baking a mode in (25) makes the block wrong when the mode changes; ARMSX2's mVU sentinel is the guard | ARMSX2 | GPL-3 | anything doing 25 | **unit test — written** | **DONE** |
| 27 | A guest can have **more FP mode registers than ARM64 has** — Xenon `FPSCR` + `VSCR.NJ` against one `FPCR`. An `FPCR` write that changes control fields **introduces a barrier** | xenia | BSD | any ARM64 backend | switch census | **READY** |

### Guest-side and render-side. These need the target to build.

| # | Lesson | From | To | Verify with | Status |
| --- | --- | --- | --- | --- | --- |
| 15 | Depth and stencil should default to **`DONT_CARE`** | Cemu | eden, azahar | GPU counters, watts | **BLOCKED** |
| 16 | **Transient attachments** get `DontCare` both ways and pair with `LAZILY_ALLOCATED` | Vita3K | everyone | bandwidth measurement | **BLOCKED** |
| 17 | Use `eClear` rather than `eLoad` when the pass clears | azahar | eden | GPU counters | **BLOCKED** |
| 18 | `COLOR_ATTACHMENT_OPTIMAL` rather than `GENERAL` | — | Cemu | validation layers | **BLOCKED** |
| 19 | Dolphin has added **40 emitter methods** melonDS never received, including the whole `YIELD`/`WFE`/`SEV` family | Dolphin, GPLv2+ | melonDS | emitter unit tests | **READY** — melonDS builds |
| 20 | A **preset constant** can feed a value into a code patch, linking settings to patches | Cemu | shared patch engine | test on a sample patch | **READY** |

### Refused, and why. **A refusal is a finding.**

| Lesson | Refused for | Reason |
| --- | --- | --- |
| `SDOT`/`UDOT` for guest vector lowering | **Cemu** | Espresso is a 750CL derivative with **no VMX at all** — only paired singles. No guest-side counterpart. |
| rpcs3's SVE2 work | **every fork** | **The device exposes no SVE.** Dead here regardless of guest. |
| `EOR3`/`BCAX` three-input bitwise | **Cemu** | Espresso's bitwise work is **scalar GPR**, not vector. |
| Dolphin's newer vector instructions | melonDS | **Dolphin never added `SDOT` or `EOR3` either.** The whole lineage lacks them; melonDS is not behind. |

---

## What this ledger says

**Seven lessons have landed and all seven were host-side, licence-free and
testable without a device.**

### The tests caught two real bugs in a reimplementation, which is the whole point

**Propagating an idea rather than a file means rewriting it, and a rewrite drops
details whose purpose is not visible.** Both bugs were in the driver advisor and
both were caught by the tests before the code was committed.

**1. A `normalize()` step that looked incidental was load-bearing.** rpcsx
replaces separators with spaces before matching. Skipping it meant `gen` never
matched `Turnip_Gen8_V33`, because **`_` is a word character and there is no
boundary before `Gen`** — so **the exact driver that must be rejected assessed
as merely RISKY**.

**2. The original regex quietly assumed one input format.** rpcsx requires the
literal `tm` in `adreno\s*\(?tm\)?\s*(\d{3})`, which is right for the Vulkan
renderer string — literally `Adreno (TM) 740`. **It fails on a package filename,
where a human writes `Adreno 740`.** Our version makes `(TM)` optional, which is
an improvement on the source rather than a copy of it.

**Neither bug is visible by reading.** Both are visible in one second of test
output. **That is the argument for "a propagation lands with a test" stated as
evidence rather than as a rule.** That is not a coincidence — it is the ranking in
`UNIFICATION.md` section 5 playing out: the cheapest durable form is a test, and
host-side lessons are the ones a test can reach.

**Six are `READY`** and need only the work.

### Item 6 was closed by designing the hazard away rather than by defending it

**Trying to write the test showed there was nothing to test.**

melonDS needs frozen DTOs — `Rom21`, `RomConfigDto25`, `RomDto31` — because it
**deserialises into typed objects**, so the shape of the current class sits in
the read path and changing it breaks old data silently.

**A flat map of stable key to string has no shape to break.** A migration
renames a key or rewrites a value, and a key it does not recognise passes
through untouched. The typed view is built from the map **after** migration, so
a stale type can never be applied to old data.

**So the propagation is not "copy the frozen-DTO discipline". It is "adopt the
storage shape that makes the discipline unnecessary".** That is a better outcome
than the lesson asked for, and it was only visible because someone tried to
write the failing test first.

### Two landed while writing this ledger, and both improved on their source

**Item 12, Vita3K's content path resolver.** Taken as a shape — enumerate roots,
build candidates, resolve, report — and **generalised from cheats to any content
kind**, because the app needs the same search for packs, mods, saves and ROMs.

**Two changes to the design.** It now **reports which root answered**, because
the storage view has to say where content actually is and "no cheats found" is
not actionable while "looked in these six places" is. And **`has()` is defined
in terms of `locate()`** rather than being a second function: Vita3K ships
`find_*` and `has_*` separately, which is two chances to disagree, and **a badge
that says a game has cheats when the loader finds none is worse than no badge.**

**Item 13, rpcsx's driver advisor.** rpcsx is GPL-2.0-only, so this is a
reimplementation from the shape rather than a copy — **which is exactly the case
`UNIFICATION.md` makes for technique crossing walls that code cannot.**

**It catches a hazard that is live on this device.** `CLAUDE.md` records
`Turnip_Gen8_V33.zip` and `a8xx-gen8-V24.zip` sitting in the Thor's storage, and
both target **a8xx** while the Thor is **a7xx**. The tests assert against those
exact filenames.

**The part worth stealing is decoding the marketing name.** Qualcomm sells a8xx
as "Gen 8", so `Turnip_Gen8_V33` claims a8xx **without ever saying so**, and a
picker matching only `a8xx` would install it.

**And it keeps rpcsx's honesty**: AdrenoTools metadata carries no target-GPU
field, so this is a heuristic over a filename. It is allowed to answer `RISKY`,
and **the provisional pin `turnip_mrpurple_T30-toasted` assesses as `RISKY`, not
`COMPATIBLE`** — nothing in its name proves the family. Saying so is more useful
than pretending to know.

**Five are `BLOCKED` on builds**, and four of those five are the render-side
attachment lessons — **the ones most likely to buy frames and watts on a
tiler.** The most valuable propagations are gated on the least glamorous work.

**Four are refused**, and three of the four were refused for the same reason:
somebody checked whether the guest architecture actually has the feature. **That
check is the difference between propagation and cargo-culting.**
