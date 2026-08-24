# Consolidated ten instances of "configured and never applied" into one index

**Goal: this session found the same failure four more times in four different
guises. Check whether it is one disease and write the index if so.**

## What was built

**[`shared_layer/DID_IT_APPLY.md`](../shared_layer/DID_IT_APPLY.md).** Ten
mechanisms across five forks, split into a configuration layer and a build-and-
launch layer, each with how it presents and what detects it.

**The finding that justified the document: this project had been accumulating
defences one at a time without noticing they were the same thing.** Counted
after the fact, the repo already held **seven separate detectors** for the same
disease, written on seven different days:

| Built | For |
| --- | --- |
| `SettingResolver.writeOverride` | sparse, sticky, change-tracked |
| `SettingScope.PROMOTED` + `liveChangeable` | a process-wide setting offered per game |
| `SettingResolver.applyPlan` | persisted but never live-applied |
| `IntegrityPolicy.allowsPatch` | a gate that is too wide |
| `TimeScaleSupport.scaled` | a feature that is not a value |
| `tools/emitted_flags.py` | a build flag that never reached the compiler |
| `tools/target_check.py` | a target line that does not emit what it claims |
| `tools/bug_class_sweep.py` | three of the mechanisms, as sweeps |
| `capability_probe.py --self-test` | an instrument that cannot return non-zero |

**Nine detectors, one disease, and no document said so.**

## The one that is not like the others

**Nine mechanisms cost frames or convenience. The tenth costs the truth.**
xenia's `kExtern` dispatch bug made every desktop HLE diagnostic return
`count=0`, and the fix **corrected an earlier research conclusion built on that
zero**. A false negative reads exactly like a real one and gets cited.

**That produced the fourth rule — prove the instrument can return non-zero — and
it is the only rule here with an executable form**,
`capability_probe.py --self-test`.

## What was NOT done

- **No new instance was found.** The document indexes what was already known.
- **No tool was written.** One line was added to mechanism 8's row telling the
  reader to run `exp_ledger.py check` alongside reading the persisted config,
  **because a lever recorded `DEAD` while silently disabled was never really
  tested** — which is xenia's `rlwinm` case exactly, where three fastpaths sat
  off on the device and cost 2.88%.
- **Four forks appear in none of the ten rows**: Cemu, azahar, melonDS,
  GameThor. **That is a statement about this repo's own logs, not about those
  forks** — checked by reading the ten rows, whose sources are five forks.
  **The likely reason is that nobody has looked**, since every instance here was
  found while reading a fork's own research directory and those four have the
  smallest ones. **The document says this rather than implying they are clean.**

## The honest limit, recorded in the document

**Every one of the ten was found by reading. The tools were written afterwards,
from the instance. None of them has yet caught a new one.** Until one does, the
tooling is a record of past failures rather than a working detector.

## Files

- `shared_layer/DID_IT_APPLY.md` (new)
- `CLAUDE.md` — a section above "Read before you claim", pointing at it
