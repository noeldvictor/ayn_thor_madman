---
name: supervise
description: Use DURING and BEFORE finishing any multi-step task in this repo. Detects the repeated unproductive cycles this project runs - unverified negative claims, re-argued settled decisions, re-run dead experiments, capability-shaped searches, and work leaking into a fork. Runs tools/supervise.py. Triggers on any audit, survey, fleet-wide claim, or session that changed more than one document.
---

# Supervise

**Run this while you work, and again before you report.**

## Why this exists

**NVIDIA's AVO architecture has four parts, and this project already has
three.** From the 2026 write-up of AVO reaching 100.00 RHAE on ARC-AGI-3:

| AVO part | Here |
| --- | --- |
| Main agent loop — inspect, plan, implement, evaluate | already |
| **Persistent memory** — carry forward prior results and reasoning so the agent resumes rather than reconstructs | already: `research_log/`, `work_log/`, `capability_inventory.md`, xenia's `exp_ledger.py`, `DEVICE_QUEUE.md`, `PROPAGATION.md`, `OWNED.md` |
| Domain tools — compilers, profilers | already: the builds, `llvm-nm`, `fleet_lint.py` |
| **Supervision layer** — monitor the trajectory for **stagnation or repeated unproductive cycles** and redirect | **missing, and this skill is it** |

**Only the fourth transfers.** The rest of AVO's results — 183 levels, 6,624
environment actions, kernels beating FlashAttention-4 by up to 10.5% — are a
different benchmark on different hardware. **Do not quote them here.** The idea
that transfers is the shape: a second loop that watches the first one for
cycles it cannot see from inside.

**This project needs it more than most**, because its failure modes are already
written down as *repeated* cycles rather than one-off mistakes:

- `CLAUDE.md` carries a table titled **"Every claim of the form 'no fork has
  this' made in this repo has been wrong."** It had nine rows. Three more were
  added on 2026-08-23.
- A fleet-wide SDK migration edited **nine forks** and was **reverted in full.**
- xenia's experiment ledger exists because levers were **re-run** after being
  recorded `DEAD`.
- Five audits on 2026-08-23 found the fleet had already done the work. **The
  cause was the same every time: searching for a capability name instead of a
  mechanism.**

## Run it

```sh
python tools/supervise.py            # report
python tools/supervise.py --strict   # exit 1 on any FAIL
python tools/supervise.py --check negatives
```

**It reads only added lines**, from `git diff` and untracked files. Scanning
whole documents reports the correction tables as the disease.

**It never touches a fork.**

## The six loops

### 1. The unverified negative — `FAIL`

**The most reliable failure mode this repo has.**

A new line claims "no fork has X", "nothing else does", "only X implements
this", and no method is named within ten lines.

**Redirect: search again with different words, then write down both searches.**
A negative is worth recording only after the second search. If the second search
confirms it, say so and name both patterns; if it refutes it, you just avoided a
row in the corrections table.

**This caught a real error on 2026-08-23.** A research log said "No other fork
does this. ARMSX2, Cemu and melonDS spill to the stack" on the strength of
reading xenia alone. The second search confirmed it — but it was unverified when
written, and three earlier claims in the same shape were wrong.

### 2. Work leaking into a fork — `WARN`

Working rule 1: **all work stays in this repo.** Measurement is not
modification; building, reading and linting leave no trace.

**Redirect: revert the fork and write the finding here.** If a build needed a
file changed, back it up and put it back.

### 3. A device experiment with no prediction — `FAIL`

**A run with no prediction cannot fail.** Every `DEVICE_QUEUE.md` entry states
what the numbers should do if the change works.

**Redirect: write the expected signature before the run, not after.**

### 4. No log written — `WARN`

**Write the log as you go. Do not write it at the end from memory.** Research
goes in `research_log/`, change goes in `work_log/`.

### 5. A fleet-wide claim with no instrument — `WARN`

A log claims something about "the fleet" or "every fork" without naming how it
looked.

**Redirect: name the mechanism you searched for, not the capability.** A tuning
flag is a `DEFINE_bool` with a paragraph of reasoning. A contract is a header. A
dependency list is a CPM cache. **None of them answers to a feature name.**

### 6. An experiment proposed without querying the ledger — `WARN`

**Redirect:** `python <xenia>/tools/exp_ledger.py check "<keyword>"` before
proposing anything. The ledger holds `DEAD` and `FLAT` verdicts precisely so a
dead lever is not re-run.

## What the script cannot see, and you must

**Stagnation is the half a regex cannot catch.** Watch for these yourself:

- **Re-reading a file you already read this session.** The information was
  there; the question was wrong.
- **Three searches returning nothing.** The instrument is wrong, not the fleet.
  Change from a name to a mechanism.
- **Widening scope after a dead end.** A second dead end usually follows.
- **Re-opening something under `Settled` in `CLAUDE.md`** without new evidence.
  A settled decision is re-argued only by a measurement, never by an argument.
- **Writing a plan longer than a week.** The project is in deep exploration and
  says so; a long plan on an unsettled contract is a month lost.

**The redirect for all five is the same: stop, state what you actually know,
and pick a different instrument.**

## When a check is wrong, fix the check

**On 2026-08-23 this script reported a false positive** — it failed
`DEVICE_QUEUE.md` entry 3 for having no prediction, when entry 3 states its
predictions in a table column headed `Prediction` and the pattern demanded bold
markers.

**The document was right and the tool was wrong.** That is the same lesson
`CLAUDE.md` already records about the ABI lint and Cemu: **when a tool and a
document disagree, read the actual line.**

Fix the check and record why in a comment beside it. **A check nobody trusts is
worse than no check**, because it trains you to skip the output.

## Do not

- **Do not treat a `WARN` as a blocker.** It is a prompt to look, not a refusal.
- **Do not quote AVO's benchmark numbers as if they apply here.** Nothing in
  this repo has been measured against them.
- **Do not add a check without evidence** that the loop it catches has actually
  run in this repo, more than once. Name the evidence in the check.
