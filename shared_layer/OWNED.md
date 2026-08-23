# Owned subsystems

**The ledger of what the shared layer owns. It is the input to the build
guard.**

**A subsystem is either owned or not owned. There is no partly owned.** A
half-converted subsystem is how duplication returns.

---

## Nothing is owned yet

**The owned list is empty on 2026-08-22.** That is the honest state, and this
file exists so the emptiness is visible rather than assumed.

Nothing can be owned before the toolchain is unified, because seven C++ runtimes
cannot share native code. See the standard row in `CLAUDE.md`.

**Do not add a row here as a plan.** A row means the extraction is done, the
fork copies are deleted and the guard is in place. Candidates live in the queue
below.

---

## The mechanism

When the shared layer takes a subsystem, the fork loses the ability to have one.

1. **Extract** into the shared layer.
2. **Delete** the fork's implementation. Do not leave it unused. Dead code is an
   invitation.
3. **Depend.** The fork links the shared module and implements the contract,
   nothing more.
4. **Guard.** The build fails if the subsystem reappears: a new file under a
   deleted path, or a symbol duplicating an owned one, is a build error, not a
   review comment.

An agent that tries to add a texture filter to a fork should find no directory,
no build target and a failing build. **That is cheaper than any review**, and it
is the point: a rule in a document does not stop an agent that never read it.

---

## The row format

Every owned subsystem records these. The first three feed the guard.

| Field | Why |
| --- | --- |
| **Subsystem** | the name used everywhere, in every fork |
| **Guard paths** | paths that must not reappear in a converted fork |
| **Guard symbols** | symbols that must not be redefined |
| **Covers** | precise enough to write the guard from |
| **Does not cover** | the backend's half. Prevents scope creep |
| **Licence** | the most restrictive source licence. See below |
| **Extracted from** | fork and commit, per source |
| **Converted** | fork, commit, and the paths deleted |
| **Not yet converted** | remaining forks |
| **Depth decision** | why this depth, recorded before extracting |
| **Evidence** | the research log that proved the duplication |

**Licence rule: the shared layer inherits the most restrictive licence among its
sources.** A module built from ARMSX2 code is GPL-3.0 and only GPL-3.0 backends
can link it. Prefer the least restrictive source when quality is close.

---

## The queue

Ordered by risk, not by value. **Each entry needs a read that proves the
duplication before it moves.**

| # | Candidate | State | Note |
| --- | --- | --- | --- |
| 0 | **Vulkan device layer** | **read, proven** | Seven forks each built one. Three vendor an allocator separately. **Cannot be guest-specific**, and the packed binary requires it |
| 1 | **Touch overlay** | **read, proven** | azahar and Vita3K ship the same four classes from the same 2013 Dolphin ancestor. Eight method names survived twelve years of independent drift |
| 2 | **GPU driver manager** | **read** | Four concerns, not six copies. Folds into the device layer rather than standing alone |
| 3 | Shader and pipeline cache | not read | Measurable, user-visible, no renderer internals needed |
| 4 | Settings framework | **read** | azahar and eden: same design twice, **code fully diverged**. Extraction is a rewrite guided by two references |
| 5 | Cheat engine | **read** | Three architectures on one axis: flat poke, polymorphic, bytecode VM. Take the whole axis |
| 6 | Code patch engine | **read** | Cemu's symbolic assembler as the engine, xenia's TOML as the authoring format |
| 7 | Texture upload and per-class routing | partially read | The flagship feature. Safe only after the test harness |
| 8 | Code translation | not read | Last. The deepest reach into a core |

### Rejected candidates

**Recording a rejection matters as much as recording an extraction.** Both stop
the question being re-argued.

| Rejected | Why | Evidence |
| --- | --- | --- |
| **LRU cache** | Three different designs for three constraints, not one structure written three times | [`../research_log/20260822_1915_lru_cache_extraction_test.md`](../research_log/20260822_1915_lru_cache_extraction_test.md) |
| **Texture cache hashing** | Guest-specific. ARMSX2 and melonDS key on data plus palette; Cemu keys on physical address | `CLAUDE.md` |
| **A shared renderer** | xenia's ledger records incremental GPU levers as `DEAD` or `FLAT`. Build the layer **beneath** a renderer | `CLAUDE.md` |
| **Render pass structure** | Tiler-critical and unmeasured. Flattening it spills GMEM | [`THOR_RENDER.md`](THOR_RENDER.md) |

---

## Before you add a row

1. **Read every implementation.** A capability recorded from a file listing is a
   hypothesis. Four duplication claims in this repo shrank on reading.
2. **Search for a shared ancestor, not a shared feature.** Shared ancestry
   predicts duplication; shared purpose does not. See
   [`ANCESTRY.md`](ANCESTRY.md).
3. **Measure the drift.** Shared ancestry after enough years means shared
   *design*, not shared *code*, and that changes extraction into a rewrite.
4. **Pass the licence gate.**
5. **Record the depth decision and its reasons**, before extracting.
6. **Convert one fork and prove the contract.** Do not convert every fork at
   once.

The procedure is in [`../.claude/skills/extract-subsystem/`](../.claude/skills/extract-subsystem/).
