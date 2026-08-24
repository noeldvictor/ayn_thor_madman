# Did it apply?

**The most repeated failure in this fleet is not a wrong value. It is a correct
value that never took effect.**

Eleven instances are now recorded, across five forks, found independently by
different people at different times. **They are not twelve bugs. They are one
disease with twelve mechanisms**, and this project has been accumulating defences
against them one at a time without noticing they were the same thing.

> **A setting that exists is not a setting that applies.**

This document is the index: every known mechanism, how it presents, and what
detects it. **Read it before designing anything a person can configure.**

---

## The user-visible symptom is always the same

**A control moves and nothing happens.** Sometimes the control is a switch in a
settings screen, sometimes it is a compile flag, sometimes it is a default in
code. The person is never told, because from the software's point of view
nothing went wrong.

**The second-worst property is that it is silent. The worst is that measurements
taken through it are false**, which is how it stops being a product bug and
becomes a research bug.

---

## The twelve mechanisms

### Configuration layer

| # | Mechanism | Found in | Presented as |
| --- | --- | --- | --- |
| 1 | **Sparse is not sticky.** A per-game value equal to global is stored as nothing, so a later global change silently takes the game's setting with it | ARMSX2 | cheats turned off per game came back on |
| 2 | **A whole-object write clobbers a pinned value.** Screens write the entire settings object, so a stale snapshot overwrites a field the user just set | ARMSX2 | a per-game FPS cap came back as 0 **and stayed 0**, surviving the fix to the writers |
| 3 | **A process-wide setting offered per game.** One server, one loaded driver, one device — the per-game tier cannot hold it | ARMSX2 (PINE), and the **per-game GPU driver override** here | the switch reads as enabled until the process restarts |
| 4 | **The value was persisted and the live path never fired.** Correct on the next boot, wrong now | ARMSX2 | an imported texture pack did not appear until the next game boot |
| 5 | **A second writer runs later.** The config file holds the value, and a profile applier overwrites it on every launch | rpcsx | freeing the CPU affinity mask looked like it did nothing — a separate compile-thread cap was still 2 |
| 6 | **The gate is too wide.** A restriction meant for one class of thing catches another | ARMSX2 | hardcore mode dropped every on-disk patch, killing everything the Patch Manager wrote, "with no message explaining why" |
| 7 | **The feature is not a value at all.** It is a cross-cutting property of every clock, cache or path in the backend | Vita3K | fast-forward shipped twice and gameplay ran at real time both times |
| **8** | **A rendering cache whose rebuild is skipped in exactly the state where the change is made.** The live path exists; the change happens while it is not running | ARMSX2 | **every OSD toggle looked inert**, because the line strings rebuild only while the VM runs and the menu is only open while paused. Its invalidation also watched the enabled set **emptying** rather than **changing**, so one line off and another on left the old text |

### Build and launch layer

| # | Mechanism | Found in | Presented as |
| --- | --- | --- | --- |
| 9 | **A persisted config beats a compiled default, forever.** Written back when the default was genuinely off | xenia | three validated `rlwinm` fastpaths were off on the device. **−2.88%**, and every measurement that session was on a handicapped baseline |
| 10 | **The default is set on a launch path the real launch does not take** | xenia | the AOT object cache was enabled only when no cvar bundle was supplied, and the launcher always supplies one. **111 MB of cache sat unused while every real launch recompiled ~10,000 functions** |
| 11 | **The dispatcher never reaches the handler.** The lever is configured, allowlisted and installed, and the call goes past it | xenia | desktop HLE intercepts returned `count=0` for weeks — **"ALL desktop diag intercepts were FALSE NEGATIVES"**, and the fix **corrected an earlier research conclusion built on that zero** |

| **12** | **A feature probe that CANNOT FIRE.** A guard on a macro nothing defines: syntactically valid, compiles cleanly, never true | rpcsx | **every LSE2 fast path for `atomic_t<u128>` was dead code**, so SPU mailboxes, reservation stamps and the global thread bitmask all ran an `ldaxp`/`stlxp` loop — and **`try_read`, a pure peek, took the cache line EXCLUSIVE** |

> **A feature probe that cannot fire is indistinguishable from a feature the
> hardware lacks.** There is no ACLE macro for FEAT_LSE2, so the code inferred it
> from `__ARM_ARCH_8_4__` and friends — **which clang on AArch64 defines at no
> `-march` at all.** Detector: **read `clang -dM -E`, not the comment above the
> probe.** That is the same instrument that answered the `+nosve` question here.

**And two near-misses that belong here:** Vita3K's `USE_LTO` defaulted to
`RELEASE_ONLY`, which covers a configuration neither shipped build uses, so LTO
meant "never"; and eden declares a `PatchCacheKey` type, gives it a hash
specialisation, and **uses it nowhere.**

---

## Why #11 is the expensive one

**Ten of these cost frames or convenience. The eleventh costs the truth.**

An unfired lever makes every measurement taken through it a false negative, and a
false negative reads exactly like a real one: the feature does not help, the
capability is absent, the fork does not do this. **The conclusion gets written
down, cited, and built on.**

> **An instrument that can return zero must be proved able to return non-zero.**

`tools/capability_probe.py --self-test` does this: a positive control per fork
and a negative control that must never match. **Any tool here that reports
absence needs the same** — `hle_coverage.py`, `vk_capability_census.py` and
`bug_class_sweep.py` all report absence, and only the last has the property by
construction, because every one of its classes ships with a case that already
paid.

---

## What detects each one

| Mechanism | Detector |
| --- | --- |
| 1, 2 | `SettingResolver.writeOverride` — sparse, sticky, **change-tracked**. The obvious implementation passes none of the three tests |
| 3 | `SettingScope.PROMOTED` + `liveChangeable = false`, so the UI says "needs restart" **before** the change |
| 4 | `SettingResolver.applyPlan` — the live-apply set is **derived from the specs**, never enumerated. ARMSX2's is a hand-written chain of `!=` comparisons and a new setting silently gets no live apply |
| 5 | `bug_class_sweep.py --class setting_written_by_multiple_writers` |
| 6 | `IntegrityPolicy.allowsPatch` — the gate keys on **intent**, not on "is this a patch" |
| 7 | `TimeScaleSupport.scaled` — a backend **declares** which clock domains it moves, and one that HLEs guest time from the host clock without declaring it is **refused** |
| 8 | **Rebuild on the state the change happens in.** A setting reachable from an in-game menu must take effect WHILE PAUSED, because paused is the only state that menu is open in — and invalidate on the set CHANGING, not on it emptying |
| 9 | `bug_class_sweep.py --class stale_default`; and **read the persisted config, not the compiled default**. **Query the experiment ledger too** — `python <xenia>/tools/exp_ledger.py check "<lever>"` — because a lever recorded `DEAD` while silently disabled was never really tested |
| 10 | `bug_class_sweep.py --class wrong_launch_path`; and **verify a hit, never infer one from a non-empty cache** |
| 11 | **A positive control.** `capability_probe.py --self-test` |
| 12 | **`clang -dM -E`.** Grep for `#if defined(X)` where nothing defines `X` |
| build flags | `tools/emitted_flags.py` — the flags that reached the compiler, from `compile_commands.json`, **with `--dates`**, because a stale build describes an artefact rather than the source |
| target claims | `tools/target_check.py` — 4 probes, proven against 4 real traps |

---

## The four rules

1. **Verify from the emitted artefact, not from the source that asks for it.**
   The compile line, the disassembly, the running config — never the build file
   or the default in code.
2. **Derive, never enumerate.** Any second list that must be kept in step with a
   first list will fall out of step. The live-apply set, the guarded-feature
   set, the clock domains.
3. **Declare, do not infer.** A backend that must do something states that it
   does. An undeclared capability fails at integration, which is cheap; an
   inferred one fails in a user report, which is not.
4. **Prove the instrument can return non-zero before believing a zero.**

---

## Limits

- **Twelve instances, five forks.** Cemu, azahar, melonDS and GameThor have
  contributed none, which almost certainly means nobody has looked rather than
  that they are clean.
- **Every instance here was found by reading, not by a tool.** The tools were
  written afterwards, from the instance. **None of them has yet caught a new
  one**, and that was tested rather than assumed on 2026-08-25: four classes run
  to exhaustion across nine forks, every non-zero hit outside the known cases
  read and dismissed. **The attempt found two instrument defects instead** — a
  class whose pattern matched only the vocabulary of the case it came from, and
  a vendored filter that missed a fork vendoring outside the usual directory
  names. **Both would have made a future sweep lie.** See
  `work_log/20260825_0705_ran_the_sweep_to_exhaustion.md`.
- **Nothing is measured.** The two percentages quoted (−2.88%, and the object
  cache) are xenia's device measurements, not reproduced here.

## Sources

Research logs `20260824_0645`, `20260824_1500`, `20260824_2320`, `20260825_0030`,
`20260825_0245`, `20260825_0410`; `CLAUDE.md` Track A step 4; ARMSX2
`pcsx2/Patch.cpp:366`; rpcsx `docs/arm64/ledger.md`; Vita3K
`reports/20260510_172815_*`.
