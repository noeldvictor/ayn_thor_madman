# The cross-fork synthesis holds at source: `MLA` really does narrow to one pipe

**Goal: yesterday I claimed azahar's measured `MADD` regression on A715 was
explained by `MLA` being single-pipe there, and marked the link "plausible rather
than proven". Verify it in Arm's guides. This proposes no lever.**

**Device-free: `pdftotext` on two guides already in the fleet. No device used.**

## Read directly from both PDFs

**Cortex-X3, ASIMD multiply accumulate:**

```
ASIMD multiply accumulate   MLA, MLS   4(1)   2   V02   1
```

**Cortex-A715, same instruction group:**

```
ASIMD multiply accumulate   MLA, MLS   4(1)       V0    1
```

> **`V02` on the X3. `V0` on the A715.** **Two pipes narrowing to one**, read from
> Arm's own tables rather than from a transcription.

## So the synthesis holds

**azahar measured** that fusing `MLA`/`MLS` into `MADD`/`MSUB` **regressed both
A715 patterns**, and recorded no mechanism.

**I proposed one from rpcsx's transcription**: the fused instruction contends for
a single pipe while the split `MUL` + `ADD` can spread. **Marked plausible, not
proven.**

> **It is now read at source.** A measurement from one fork and a table from
> another, joined by a third party, **and the primary document agrees.**

**That is the strongest chain this project has assembled**: a device measurement,
an independent table reading, and a source verification — **none of the three
parties aware of the others.**

## One honest gap in the reading

**The throughput column did not render cleanly for the A715 row.** The X3 emitted
four values — `4(1) | 2 | V02 | 1` — and the A715 emitted three, with the
throughput position blank.

**rpcsx says A715 `MLA` is throughput 1**, and the rendering is consistent with
that without being decisive. **The PIPE column is unambiguous in both, and the
pipe column is the claim.**

> **METHOD NOTE: `pdftotext -layout` renders these tables inconsistently between
> guides.** The same table in two documents produced different column counts.
> **Read a neighbouring row as a control before trusting a column position** —
> the row above the A715 entry showed its throughput value on a CONTINUATION
> line, which is how the column count differs.

## Limits

- **Two rows verified**, in two guides. **The rest of the transcribed tables are
  still at two hops.**
- **The throughput figure for A715 `MLA` is NOT verified**, only its pipes.
- **The synthesis explains a regression; it does not predict its size.** azahar's
  numbers remain azahar's.
- **Nothing measured here.** This is a document reading.

## Sources

- `cortex-x3-software-optimization-guide.pdf` line 1469 of the extraction
- `cortex-a715-software-optimization-guide.pdf` line 1498
- `research_log/20260826_0820_quote_the_cluster_that_runs_the_code.md`
