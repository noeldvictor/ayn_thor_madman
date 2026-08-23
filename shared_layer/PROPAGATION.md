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

| Fork | Builds? | Tests? | Ready |
| --- | --- | --- | --- |
| melonDS-android | **yes**, 15 min 27 s | some | **yes** |
| Vita3K-Thor | **yes, once `x86_64` is dropped** — see below | a regression suite | **unblocked by a one-line change** |
| ARMSX2, azahar, eden, Cemu, xenia, GameThor | **never attempted** | varies | **unknown** |

**Six of eight are unknown.** That is the single biggest obstacle to this
operation and it is why Phase 0.3 matters.

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
| 6 | A migration must **freeze a DTO per version**, never deserialise with the current class | melonDS | GPL-3 | shared layer | contract + test | **READY** |
| 7 | A persisted enum is **append-only forever**; group in the UI, not the numbering | ARMSX2 | GPL-3 | shared layer | contract | **READY** |
| 8 | Validate the pipeline cache header and **log which field disagreed** | ARMSX2 | GPL-3 | shared device layer | test on a synthetic blob | **READY** |
| 9 | A `Presentation` is **not torn down** when the activity stops; re-attach every resume | ARMSX2 | GPL-3 | app shell | **applied**, device-verify queued | **DONE**, unverified |
| 10 | `yield` is a **no-op** on ARM; use `ISB` for spin backoff | rpcs3 → xenia | idea | every fork with a spin loop | A/B on device | **BLOCKED** — needs builds |
| 11 | Pass **real target features** to the compiler and to LLVM | xenia | BSD | six forks | build-flag guard | **READY** |
| 12 | A **content path resolver** must search many roots; users put files anywhere | Vita3K | GPL-2+ | app shell | unit test over fake roots | **READY** |
| 13 | A driver picker should **advise**, not list — verdicts, not options | rpcsx | **GPL-2 only** | shared driver manager | test on package names | **READY** |
| 14 | Mark fork divergence at line level with a **`NOTE(thor):`** comment | xenia | BSD | every fork | lint | **READY** |

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

**Five lessons have landed and all five were host-side, licence-free and
testable without a device.** That is not a coincidence — it is the ranking in
`UNIFICATION.md` section 5 playing out: the cheapest durable form is a test, and
host-side lessons are the ones a test can reach.

**Nine are `READY`** and need only the work.

**Five are `BLOCKED` on builds**, and four of those five are the render-side
attachment lessons — **the ones most likely to buy frames and watts on a
tiler.** The most valuable propagations are gated on the least glamorous work.

**Four are refused**, and three of the four were refused for the same reason:
somebody checked whether the guest architecture actually has the feature. **That
check is the difference between propagation and cargo-culting.**
