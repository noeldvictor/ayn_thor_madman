# A self-test for the guard that gates every commit

**Goal: I wrote that "a guard without a control is not a guard." Audit which of
this repo's tools have one. This proposes no lever.**

**No device. One tool.**

## The audit, and it is not a coincidence

| Tool | `--self-test` |
| --- | --- |
| `capability_probe.py`, `hle_coverage.py`, `vk_capability_census.py`, `dead_guard.py` | **yes** |
| `bug_class_sweep.py`, `emitted_flags.py`, `fleet_docs_index.py`, `fleet_lint.py`, **`supervise.py`**, `target_check.py` | **no** |

> **The two tools whose defects I found this session — `supervise.py` and
> `bug_class_sweep.py` — are both in the second row.** The four with self-tests
> are the four where a control had already been added.

**`supervise.py` is the worst omission**: it gates every commit, it has seven
checks, and **two of them were broken this session** — `queue-stale` silently,
`dead-levers` by a crash.

## What it does

**`supervise.py --self-test` gives every check an input it MUST flag and one it
MUST pass**, and reports any check that cannot tell them apart. **Exit 1 if any
fails.**

## It found two faults on its first run, and both were MINE

**Neither was in a check. Both were in my fixtures**, which is its own lesson —
**a control that does not exercise the thing it targets proves nothing.**

1. **A Windows backslash.** `os.path.join("research_log", ...)` produces
   `research_log\...`, and two checks filter on `"research_log/"`. **They skipped
   the fixture entirely and reported clean.** Git always emits forward slashes,
   so the production path was never wrong — **only the test was.**
2. **A fixture in a directory the check excludes.** `check_dead_levers`
   deliberately skips `research_log/`, so a fixture there **could never fire.**
   Fixtures now carry their directory.

## Three checks are SKIPPED, and saying so is the point

`fork-writes` reads git status in each fork; `device-queue` reads
`DEVICE_QUEUE.md` itself; `session-log` depends on the **mix** of changed paths
rather than their content. **A synthetic path cannot exercise any of them.**

> **A self-test that pretended to cover them would always pass**, which is the
> disease this whole exercise is about. **They are named, with the reason, so the
> gap is visible rather than assumed closed.**

## The meta-control

**A self-test without its own control is a guard without a guard.** So:

- **`check_negatives` was deliberately broken** — its match condition forced
  true. **Result: `[FAIL] negatives did not discriminate: fires`, exit code 1.**
- **Restored: four checks `OK`.**

**It discriminates, and that is now demonstrated rather than assumed.**

## Limits

- **Four of seven checks are covered.** Three are skipped for stated reasons and
  **those remain unguarded.**
- **The fixtures are one input each.** A check could pass both and still be wrong
  on a third case.
- **Five other tools still have no self-test**, including `bug_class_sweep.py`,
  which had a defect this session. **Not done here.**

## Files

- `tools/supervise.py` — `--self-test`, `SELF_TESTS`, `SELF_TEST_SKIP`
