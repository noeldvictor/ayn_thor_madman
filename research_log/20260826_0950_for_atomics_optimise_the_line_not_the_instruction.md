# For atomics, optimise the line, not the instruction — and the guides cannot tell you otherwise

**Goal: continue `microarchitecture.md`. One section bounds what this project's
`+lse` work can settle by reading.**

**Device-free: one section. No device used.**

## The optimisation guides contain no atomic data at all

**Searched both guides, method stated:**

| searched | X3 | A710 |
| --- | --- | --- |
| `CAS` | **0 real hits** | **0 real hits** |
| `LDADD` / `LDSET` / `SWP` | 0 | 0 |
| `LDXR` / `STXR` / `LDAXR` | 0 | 0 |
| "atomic" / "exclusive" | 0 | 0 |

**The apparent `CAS` hits are the substring inside "cases" and "broadcast", and
A710's lone `SWP` is `VSWP`, an ASIMD register swap.**

> **That is the same false-positive shape I hit yesterday** — `sse` inside
> "processes". **Two forks, two ISA-name substring searches, two false positives,
> both caught by reading the hit.**

> **"There is no latency, throughput or pipeline data for a single atomic
> instruction in either document."**

## The omission is coherent, and the reason is the finding

> **"An atomic's cost on a multi-core part is dominated by the COHERENCE STATE OF
> THE LINE it touches** — whether it is already held exclusively, how many other
> cores have a copy, how far away the point of coherence is. **None of that is a
> property of one core's pipeline, so a per-core optimization guide has nothing
> useful to say about it."**

**So the absence is not an oversight and no amount of further reading fixes it.**

## And that qualifies this repo's `+lse` thesis

**`CLAUDE.md` argues at length for `+lse`** — the device reports `atomics`, five
of six shipped binaries dispatch through `__aarch64_have_lse_atomics`, and
**xenia was the only one of those six binaries emitting LSE at scale** — counted
by disassembling them, and later qualified when Vita3K's binary turned out to be
three months old — and the outline call is *"a `bl` to a stub that
loads a global feature byte and branches, to decide something constant for the
life of the device."*

**All of that is about INSTRUCTION SELECTION.** This section says the cost is
somewhere else.

> **`DEVICE_QUEUE.md` entry 25 predicts FLAT on throughput with contention as the
> only arm likely to move. This explains WHY** — and it is a better reason than
> the entry gives.

**rpcsx's own instance makes it concrete**: its fallback contends on
**`g_range_lock_bits[0]`, a single 64-bit word**, so **every SPU atomic serialises
on one cache line across all eight cores regardless of which guest address it is
updating.** The fix gives each guest line its own reservation entry and therefore
its own cache line.

> **"The benefit is entirely in the coherence traffic, not in the instruction
> selected."**

**The rule: for atomics, optimise the LINE, not the INSTRUCTION.** `+lse` removes
a dispatch branch; **it does not move a contended line.** Both are worth doing and
only one of them is where the time is.

## A document taxonomy worth taking

> **The Arm Architecture Reference Manual (~14,000 pages) contains NO TIMING DATA
> WHATSOEVER**, because timing is per-implementation. *"Get it for encodings when
> that work starts; do not consult it about speed."*

**And the fork records an error made by reasoning from it**: the **ESR `ISV`
fields, which the architecture defines and this silicon reports as zero.**

> **Third instance in this repo of "the manual describes what a part MAY
> implement, not what this one DOES"** — after `/proc/cpuinfo` showing no SVE on
> cores that implement SVE2, and the A510 shared-VPU claim that did not
> reproduce.

## Limits

- **The zero is rpcsx's search of its vendored guides**, not re-run here. **It is
  a well-specified negative** — four vocabularies, both documents, false
  positives identified — **which is more than most negatives in this fleet.**
- **Nothing is measured.** The coherence argument is architectural reasoning, and
  rpcsx says its own item *"cannot be advanced by reading"* and needs a contended
  measurement it does not yet have a workload for.
- **rpcsx is out of the packed binary**, so `g_range_lock_bits` is not our code.
  **The shape — one global word serialising unrelated addresses — is what
  transfers.**
- **This does not argue against `+lse`.** It argues that `+lse` is a smaller
  lever than the line placement beside it.

## Sources

- rpcsx `docs/arm64/microarchitecture.md:235-284`
- `CLAUDE.md`, the `+lse` census; `DEVICE_QUEUE.md` entry 25
