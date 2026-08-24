# "Thirteen for thirteen" does not survive being counted

**Goal: I claimed the pipelines column retro-predicts one of the thirteen refuted
manual predictions. Test it on the others — and the tally itself did not
survive.**

**Device-free: reading this repo's own record. No device used.**

## The tally disagrees with itself in three places

| Line | Says |
| --- | --- |
| `CLAUDE.md:3628` | *"**Ten** manual-derived predictions were measured here and ten were refuted"* — rpcsx's, quoted |
| `CLAUDE.md:3638` | *"**Thirteen for thirteen**"* |
| `CLAUDE.md:3538` | *"**Fourteenth** manual-derived prediction measured here and refuted"* |
| `CLAUDE.md:3587` | *"**Fourteen others** were measured and failed"* |

**Ten, thirteen, fourteen.** The number grew as items were added and **nobody
reconciled it.**

## And at least three counted items are not refutations

**The thirteen is built as rpcsx's ten plus this line:** *"xenia implemented all
three CPU leads and left them off by default; `EOR3`/`BCAX` fusion measured
`DEAD`; the `TBL2`-for-`TBX2` rewrite measured null."*

**Read what those actually are:**

| Counted item | What this repo records elsewhere | Refutation? |
| --- | --- | --- |
| **A710 lane-assembly stall** | *"**CONFIRMED AND FIXED 2026-08-24** — the FIRST manual-derived lead in this fleet not to be refuted"* | **NO — confirmed** |
| **Spill GPRs to the vector file** | *"off, **measured**: `UMOV` latency **2** against `LDR` **4**"* — **the manual was RIGHT**; it is off because the 360 guest squeezes 128 vector registers into 28 | **NO — manual confirmed, lever declined on scope** |
| **FPCR single mode** | *"off, **census first**"* | **NO — unmeasured** |
| `a64_vmx_nan_fixup_branchless` | *"off, pending a qemu differential"* | **NO — unmeasured** |
| **`EOR3`/`BCAX`** | `DEAD` — **0 of 1 fusable candidates** | **not a COST question** — the manual was right and the pattern was absent |
| **`TBL2` for `TBX2`** | null, 0.555 against 0.555 | **YES — and the pipe column predicts it** |
| **A510 shared VPU** | pairs scaled near-linearly | **YES** |

> **"Off by default" is not a verdict.** Two items are unmeasured. **One is
> recorded twenty lines earlier in the same file as CONFIRMED.** One had its
> manual claim measured and upheld, and was declined for an unrelated reason.

## The shape of the error is one this repo already corrected once

**`CLAUDE.md` records that quoting "57 wins" from the experiment ledger as 57
speedups is "wrong by an order of magnitude"**, because a `WIN` there means a
decisive result and 26 are milestones of a build-out whose premise was later
refuted.

> **This is the same error on the other side of the ledger.** An aggregate
> assembled from heterogeneous members, quoted as a prior, **where several members
> are not what the label says.**

**And it matters more than the WIN count did**, because this number is used to
decide **whether to chase a lever at all**.

## What the honest statement looks like

**The prior is real and it is not thirteen-for-thirteen.** Of the items this repo
NAMED and can audit:

- **Two are genuine refutations** — the A510 shared VPU and `TBX2`.
- **One is an applicability failure** — `EOR3`, where the manual was right about
  cost and there was nothing to fuse.
- **Two are unmeasured.**
- **Two are confirmations**, one of them explicitly the first lead in the fleet
  not to be refuted.

**rpcsx's ten are its own and are NOT audited here.** They may well all be
genuine; the point is that **the three this repo appended to reach "thirteen" do
not carry their weight.**

> **The useful prior survives**: a manual-derived, one-sided, single-instruction
> argument has a poor record. **The specific tally does not.**

## Limits

- **rpcsx's ten were not examined.** Its own document states them and this repo
  quotes the count; **auditing them needs its ledger read entry by entry.**
- **This is a reading of this repo's own text**, not a re-measurement of any
  lever. **Every underlying result stands** — only the label on the aggregate is
  wrong.
- **"Confirmed" for the A710 stall is that fork's word**, in a document this repo
  already quotes at length.

## Sources

- `CLAUDE.md:2294, 3538, 3586-3592, 3628-3640`
- `research_log/20260826_1120_the_pipelines_column_is_why_manuals_kept_failing.md`
