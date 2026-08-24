# The guards are subject to the same disease

**Goal: I noticed in passing that a control had caught a broken guard for the
third time. Counting properly, it was five, all in one session.**

**No device. One consolidation. This proposes no lever.**

## Five, and three failed silently

**Every one is a tool written to CATCH a failure, and every one had the failure
it exists to catch:**

| Guard | Defect | Reported |
| --- | --- | --- |
| `supervise.py` `queue-stale` | `\b` written as a **literal backspace byte** | **`OK` on every input** |
| `supervise.py` `dead-levers` | alternatives became `re.compile`'s second argument | crashed |
| `hle_coverage.py --self-test` | referenced names not in the module | crashed |
| `bug_class_sweep.py` `near_absent` | did not check the **match line** | **false positive on correct code** |
| `dead_guard.py` | `git grep -o` dropped the macro name | **wrong answer, silently** |

> **`DID_IT_APPLY.md` mechanism 12 is "a feature probe that CANNOT FIRE".** These
> are that mechanism **in the verification layer** — and a detector that cannot
> fire reports clean on everything.

**Three failed silently. A crashing guard is a good day.**

## What actually caught them

**Positive controls, run from habit rather than suspicion.** In every case I had
no reason to doubt the tool — I had just written it.

**This repo's rule is stated about MEASUREMENT instruments**: *an instrument that
can return zero must be proved able to return non-zero.*

> **Verification instruments need it more.** A broken measurement produces a
> wrong number that somebody may question. **A broken guard produces silence,
> which nobody questions.**

## And the exemption needs its own control

**`queue-stale` needed three controls**: it fires; it stays quiet with no
citation; **and it stays quiet when the queue WAS updated.**

**Only the third tests the exemption**, and a guard that never stops firing is as
useless as one that never fires — **it becomes ritual, which is the failure I
named for `queue-stale` and then committed against `dead-levers` two hours
later.**

## What this does not claim

- **The five are mine, from one session.** No claim that the fleet's tools have
  this rate, and **no other project's guards were examined.**
- **Four of the five were caught within minutes**, so the cost was small. **The
  cost of the fifth, had it shipped, is unbounded** — `queue-stale` would have
  reported `OK` forever.
- **It is not an argument for fewer guards.** It is an argument that **a guard
  without a control is not a guard**, and that the control is cheap enough to be
  unconditional.

## Files

- `shared_layer/DID_IT_APPLY.md` — the five instances and the habit
