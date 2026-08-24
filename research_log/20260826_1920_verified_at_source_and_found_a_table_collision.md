# Verified at source, and found a table collision the transcription hid

**Goal: I have recorded "two hops from the source, unverified" three times today.
This repo holds Arm's PDFs. Verify the row that carries the matched prediction.
This proposes no lever.**

**Device-free: `pdftotext` on a guide already in the fleet. No device used.**

## The transcription is confirmed

**Cortex-A715 guide, ASIMD cryptographic table:**

```
Crypto SHA3 ops   BCAX, EOR3, RAX1, XAR   2   1   V0   -
```

**Latency 2, throughput 1, utilised pipeline `V0`.** Exactly what rpcsx
transcribed, what I recorded this morning, and what carried the prediction that
matched to 2% on three cores.

> **Three hops of transcription — Arm -> rpcsx -> this repo — and the numbers
> survived.** Worth knowing, because I had flagged it as a risk each time.

## And going to the source found a trap the transcription had hidden

**The same mnemonics appear TWICE in the same guide, with different latencies:**

| Line | Table | `BCAX`/`EOR3` latency |
| --- | --- | --- |
| 2384 | **ASIMD** cryptographic | **2** |
| 3845 | **§3.31 SVE Cryptographic, Table 3-48** | **4** |

> **The latency-4 row is the SVE form. This device has no SVE** — measured, and
> recorded in this repo from `/proc/cpuinfo`.

**So one of the two tables is entirely inapplicable here, and nothing in the row
says so.** A reader grepping the guide for `BCAX` gets **two answers**, and the
wrong one is **double the latency** for exactly the instruction that carries this
project's one matched manual prediction.

> **THE RULE: when quoting a guide row, name the TABLE, not just the guide.**
> ASIMD and SVE tables collide on mnemonics, and on this device the SVE tables
> describe a feature the SoC does not expose.

**This is the same shape as everything else this week** — a qualification that
exists in the source and is lost on the way out — **except here the loss happened
in the transcription chain rather than in a headline.** rpcsx quoted the right
table; **nothing in its document says which table it was.**

## What this changes

- **`CORTEX_X3_NOTES.md`'s new tables are verified for the row that matters**,
  and now carry the SVE warning.
- **This repo's SVE work gains a fourth instance.** It already records that the
  SoC exposes no SVE, that `-march=armv9-a` would select SVE paths in xxHash, and
  that `-mcpu=cortex-x3` defines SVE macros. **Now: the optimisation guides
  contain SVE tables that look like ordinary rows.**

## Limits

- **One row verified**, in one guide. **The other rows in the transcribed tables
  are still at two hops**, and the X3 rendering of this same row was split by a
  page break and could not be read cleanly.
- **The A715 was chosen deliberately** because today's correction says it is the
  applicable core — **but the transcribed tables in `CORTEX_X3_NOTES.md` are the
  X3's**, so the verification is of the same ROW in a different core's guide.
- **`pdftotext -layout` output is column-fragile.** Two renderings of the X3 row
  disagreed on visible columns; the A715 row was legible and is what is quoted.

## Sources

- `armsx2-thor/ARMSX2/docs/reference/arm/cortex-a715-software-optimization-guide.pdf`,
  lines 2384 and 3845 of the `pdftotext -layout` extraction
- `research_log/20260826_1120_the_pipelines_column_is_why_manuals_kept_failing.md`
