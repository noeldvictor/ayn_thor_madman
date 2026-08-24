# Quote the cluster that runs the code, not the fastest one — and it explains a measurement from this morning

**Goal: read `microarchitecture.md`'s self-correction before anything else in it,
per the rule this session established.**

**Device-free: one section. No device used.**

## The correction

> **"Everything above was read out of the Cortex-X3 guide. The X3 is one core of
> eight, and `thor_power_probe.ps1` consistently shows it doing the LEAST work of
> the three clusters on this workload."**

```
A510       3 cores   0.54 cores busy   2016 MHz
A710/A715  4 cores   1.06 cores busy   2707 MHz   <- most loaded
X3         1 core    0.62 cores busy   3187 MHz
```

**And the A715 tables are TIGHTER than the X3's on exactly the instructions that
audit examined:**

| operation | X3 | **A715** |
| --- | --- | --- |
| **`MLA`, `MLS`** | 4(1), thr 2, `V02` | 4(1), thr **1**, **`V0`** |
| **`SSHL`, `USHL`** | 2, thr 2, `V13` | 2, thr **1**, **`V1`** |
| `CMEQ`, `AND`/`EOR`/`ORR` | 2, thr 4, `V` | 2, thr **2**, `V` |
| `SDOT`/`UDOT` 8-bit | 3(1), thr 4, `V` | 3(1), thr **2**, `V` |
| `ADDV` 8H | 4, thr 2 | **5**, thr 1 |

> **"Quoting the X3 because it is the fastest one is a mistake: for anything
> running on the mid cluster, the A715 and A710 guides are the applicable tables,
> and they are consistently more restrictive. The X3 is the right reference only
> for work pinned to `cpu7`."**

**Its own caveat, and it matters**: this *"does not overturn any conclusion,
because every one of them was settled by measurement rather than by the table."*
**Being wrong about the magnitude did not change a verdict.**

## It explains a measurement I recorded this morning, from a different fork

**azahar rejected fusing `MLA`/`MLS` into `MADD`/`MSUB`**, and its result was the
one I called the sharpest of the four because it broke the usual story:

> *regressed the dependent A510 path, **both A715 patterns**, and the independent
> A710 and X3 patterns*

**I recorded it as "the textbook fusion regressed everywhere" with no mechanism.**
The A715 table supplies one:

> **`MLA` is throughput 1 on a SINGLE pipe (`V0`) on A715.** A fused `MADD`
> contends for that one pipe; the split `MUL` + `ADD` can spread across more.

**Two forks, one measured and one read from a table, and they agree.** azahar
measured the regression on real silicon; rpcsx's table says why it should be
worse on exactly the cluster azahar named.

> **Neither fork cites the other, and neither knew.** That is the convergence
> signal this project already trusts, arriving as measurement-plus-mechanism
> rather than as two measurements.

## What this repo should change

**`CLAUDE.md`'s CPU leads are largely X3-derived** — *"From the Cortex-X3
optimization guide, distilled in `CORTEX_X3_NOTES.md`"* — and two of them, the FP
status stall and the vector-file spill, are X3 table readings.

**The load figures make the X3-centric reading doubly wrong**: it is **not where
the work is** (0.62 of one core against 1.06 across four), **and its tables are
looser than the cluster that is.**

> **The rule to adopt: a table quotation must name the core whose table it is,
> and a lever must name the cluster it would run on.** This repo already requires
> every performance claim to state the CPU cluster. **It does not yet require the
> same of a claim drawn from a manual.**

**And it strengthens the all-core gate**, which this repo adopted from azahar: if
the mid cluster's tables are consistently more restrictive, **a lowering accepted
on X3 numbers is accepted on the loosest evidence available.**

## Limits

- **The load figures are rpcsx's, on rpcsx's workload** — a PS3 title, and PS3 is
  out of the packed binary. **Another backend's thread placement could differ
  entirely.**
- **The tables are quoted from that document**, not read from the ARM guides
  here.
- **The `MLA` synthesis is mine.** azahar gives a measurement with no mechanism;
  rpcsx gives a table with no reference to azahar. **The link is plausible and
  untested** — and a single-pipe throughput-1 instruction is a sufficient
  explanation, not a proven one.
- **rpcsx's own caveat stands**: no conclusion in its document changed, because
  each was settled by measurement.

## Sources

- rpcsx `docs/arm64/microarchitecture.md:610-656`
- `research_log/20260824_2220_four_textbook_arm64_fusions_measured_and_rejected.md`
