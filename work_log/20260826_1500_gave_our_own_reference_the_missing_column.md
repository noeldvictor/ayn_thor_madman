# Gave our own reference the column its rules demand

**Goal: I recorded that `CLAUDE.md`'s sharpened rule — *name the pipes of the
instructions you remove and add* — **could not be met from this repo's own
hardware reference**, because that reference has no pipe column. Fix it.**

**No device. One reference file.**

## What was added

**`CORTEX_X3_NOTES.md` now ends with the utilised-pipelines column** for the
instructions this fleet's codegen actually selects — three tables:

- **Cortex-X3 rows** for the bitwise, saturating, count, narrow and table-lookup
  families, including the two that trap: **`BCAX`/`EOR3` at throughput 1 on
  `V0`**, and **`TBL` confined to `V01` while `TBX` uses all of `V`.**
- **Reductions**, where **width changes both columns** — a 16-byte reduce is
  latency 4 at throughput 1, against 2 and 2 for a 4-lane one, **and every form
  is pinned to `V13`.**
- **The A715 deltas**, which are consistently tighter, **with the cluster-load
  figures that say the mid cluster is where the work is.**

## The provenance is two hops and the file says so

**These rows are transcribed from rpcsx's `microarchitecture.md`, which
transcribed Arm's guides.** The file carries that in a block quote, with the
instruction to **verify a row against the guide before betting a lever on it.**

> **This repo requires every borrowed number to name its fork.** A borrowed
> TABLE is the same obligation, and it is easier to forget because a table looks
> like a fact.

## Why this closes a real gap rather than a tidy one

**The rule as sharpened this morning is a demand**: a lever that cannot name the
pipes has not been analysed. **A demand that cannot be met from the project's own
reference is a demand nobody will meet** — they will quote latency, as they have
been doing, because that is what the file contains.

**Three of the four consolidated misreads were pipe-column failures.** The file
that failed to supply the column is the file every one of those leads was drawn
from.

## Limits

- **Two hops from the source, and unverified against Arm's PDFs**, which this
  repo holds under `hardware_ref/thor/cpu/`. **Checking one row against the guide
  would establish the transcription; it was not done.**
- **The instruction list is what rpcsx's codegen selects**, not what this
  project's will. **It is a starting set, biased toward SPU work.**
- **No A710 rows**, only A715 deltas — and the Thor has both.
- **Nothing measured.** These are table values.

## Files

- `hardware_ref/thor/cpu/CORTEX_X3_NOTES.md`
