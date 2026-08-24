# A fleet-wide rejection index, because the ledger is xenia-only

**Goal: make four sessions of mined rejections usable. They were spread across
research logs, which is where a finding goes to be forgotten.**

## What was built

**[`shared_layer/REJECTED.md`](../shared_layer/REJECTED.md)** — about thirty
measured rejections from **azahar, xenia, rpcsx and Cemu**, grouped as CPU and
code generation, graphics, and audio/power/build. **Each row names the fork, the
number and the document that states it in full.**

**It is an index, not a copy**, in the same shape as `MEASUREMENT.md` and
`FLEET_DISCIPLINE.md`. **Nothing in it was measured by this repo.**

## Why it is not redundant with the experiment ledger

`CLAUDE.md` already says to adopt xenia's `exp_ledger.py`, and that stands. **But
the ledger holds xenia's experiments.** azahar's rejections — which are the
densest set found, and the only ones covering audio, the Android build and the
guest scheduler — **are in a fork's `AGENTS.md` and in no queryable store at
all.**

**The two are complementary and the file says to query both.** The ledger is
current and queryable for one fork. The index is diffable, carries the
reasoning, and covers four.

## The part that is more than a list

**Five recurring shapes account for nearly every row**, and naming them is what
makes the file usable on a lever that is not already in it:

1. **The target improved and the complete path did not.**
2. **It merely moved cost**, into a caller nobody profiled.
3. **The ceiling was below the noise floor.**
4. **The pattern does not occur.**
5. **Fewer instructions, deeper dependency chain.**

**And a sixth that is not about performance: a change can pass an enormous test
suite and be wrong** — 439,504 of 439,505 assertions, and a crash in one second.

**The file ends with five things to do before proposing an optimisation**, of
which two need only a profile and no device: **compute the ceiling**, and
**count applicability.**

## One deliberate inclusion

**azahar's `RSHRN` first-class IR operation, at 3.5x - 14.8x, is listed in the
rejection file.** It is an acceptance, and it is there so the file cannot be read
as "never touch codegen". **The distinction it draws is the useful one**: that
win removed a portable polyfill DAG rather than fusing two instructions.

## Limits

- **Not exhaustive, and the file says so.** It indexes what has been read —
  mostly four `AGENTS.md` files and one ledger. **xenia has 553 research
  documents and 75 `OPEN` entries not represented.**
- **Several rows quote manuals rather than measurements.** They are marked.
- **A rejection can go stale**, and the forks' own conditional wording is
  preserved rather than flattened into a verdict.
- **No tool enforces reading it.** `supervise.py`'s `dead-levers` check asks for
  a ledger query; it does not know about this file. **That is the obvious next
  step and was not done here.**

## Files

- `shared_layer/REJECTED.md` — new
- `CLAUDE.md` — linked from the experiment-ledger section
