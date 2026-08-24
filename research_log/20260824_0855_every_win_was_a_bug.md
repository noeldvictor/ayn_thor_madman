# Every real win in this fleet was a bug, not an optimisation

**Goal: read `rpcsx/docs/arm64/adreno-tiler.md`, the GPU counterpart to its
x86-isms sweep.**

**It ends with the most strategically important sentence found in the fleet, and
today's evidence supports it from three forks at once.**

> **This emulator's ARM64 and GPU paths are already well matched to the hardware.
> The wins have come from code that was broken, not code that was slow.**

## The immediate finding: rpcsx never clears, it always loads

`VKRenderPass.cpp:276-295`, unconditionally, no parameter and no variation —
colour and depth and stencil all `LOAD_OP_LOAD` and `STORE_OP_STORE`. Every
attachment op in its whole Vulkan backend:

```
3  LOAD_OP_LOAD        3  STORE_OP_STORE
1  LOAD_OP_DONT_CARE   1  STORE_OP_DONT_CARE
0  LOAD_OP_CLEAR
```

**`LOAD_OP_CLEAR` is used zero times**, so even a full-screen clear unresolves the
attachment into GMEM first, then clears it.

**This adds a fifth fork to `CLAUDE.md`'s attachment-ops ranking, at the bottom.**
The existing table covers Vita3K, Cemu, azahar and eden. **rpcsx is worse than
eden**, which at least loads and stores rather than loading in order to clear.

## And then they fixed it, and it bought nothing

**They built the `LOAD_OP_CLEAR` conversion, proved it correct, proved it applies
to 100% of clears in the workload, and measured no saving.**

The instrument, because frame time could not see it — Folklore is capped at 60 fps
with RSX at ~1.4% — was **`/sys/class/kgsl/kgsl-3d0/gpubusy`**, a cumulative
busy/total pair that **resets on read**, driven by `tools/thor_gpu_busy_ab.sh`:

| Arm | GPU busy | Clock |
| --- | --- | --- |
| `loadop_clear=0` | **12.39%** | 615 MHz |
| `loadop_clear=1` | **12.65%** | 615 MHz |

**No saving, and the clock is identical in both arms**, so devfreq is not
absorbing a win. The direction is very slightly wrong, inside noise.

**Two explanations it explicitly does not separate:**

- **Turnip may already do it.** A driver is free to notice that a pass begins with
  a full-surface `vkCmdClearAttachments` and fold it into the load op. **If Mesa
  does that, an app-level version is redundant by construction.**
- **The workload is too light** — a 2D title screen on a GPU that is 87% idle.

**Verdict: default-off**, and reopening it needs a heavier scene **and** a check
of whether Turnip already folds the clear, **in that order, because if the driver
does it the scene will not matter.**

### This qualifies two items on this repo's propagation list

`CLAUDE.md` currently lists **"depth and stencil `DontCare` by default"** and
**"`eClear` instead of `eLoad` when the pass clears"** as propagation items.

**They are still correct, and they may buy nothing.** The one fork that measured
the second one got 12.39% against 12.65%. **Mark both as correctness-and-hygiene
propagations, not performance ones, until somebody checks what Turnip already
folds.**

## The pattern, tested against today

**rpcsx counts six vendor-derived predictions refuted in that effort. xenia counts
ten manual-derived predictions refuted, and this repo has recorded fourteen.**

**Now list what actually paid, across everything read today:**

| Win | What it was |
| --- | --- |
| **74% of cycles in a nop-spin** | a **timing constant** correct on x86, wrong by 1300x here |
| **rlwinm fastpaths off, +2.88%** | a **stale persisted config** overriding a compiled default |
| **AOT object cache never enabled** | a **guard on the wrong launch path** |
| **guest core N pinned to host core N** | a **guest index used as a host index** on big.LITTLE |
| **AES hardware never asked for** | a feature **never requested** |
| **ARMSX2 frame generation at fp32** | an **extension the device layer never requests** |
| **eden's NCE patches re-derived every launch** | a **cache key declared and never used** |

**And what did not pay:**

| Refuted | Why |
| --- | --- |
| native render path rearch | frame anatomy: **~7 ms of structure existed to reclaim** |
| bindless resources | **regressed** 129 ms to 161 ms |
| `EOR3`/`BCAX` fusion | **0 of 1 candidates** — the pattern does not exist |
| `TBL2` for `TBX2` | 0.555 against 0.555 |
| `LOAD_OP_CLEAR` conversion | **12.39% against 12.65% GPU busy** |
| A510 shared vector unit | **pairs scale near-linearly**, no halving |
| `ISB` for `yield` | **+23% regression** when swapped alone |

> **Two columns, and the split is total. Every win was something broken. Every
> refutation was something merely slow.**

## What this means for this project

**1. It supports `CLAUDE.md`'s existing position and reframes it.** That section
says to expect maintenance wins rather than large frame wins, on the evidence
that xenia's ledger holds many `DEAD` entries. **The reframing is sharper: the
frame wins exist, and they are in broken code, not in slow code.**

**2. It changes what the shared layer is for.** The stated case is one Vulkan
device, one cache, one budget, one scheduler. **The stronger case is that a
shared layer makes a broken thing visible in eight backends at once.**

Every item in the win column above is a **class**, not an instance:

- **A guest index used as a host index** — found in xenia, then eden.
- **A timing constant assuming a fast counter** — found in rpcsx; **Vita3K has the
  cure and nobody else was checked.**
- **A capability the device layer never requests** — ARMSX2's fp16, and the
  census says the fleet agrees on almost nothing.
- **A cache that exists and is off on the path people use** — xenia's object
  cache, and **this project is designing a store with exactly that failure mode.**

**3. It gives the extraction queue a different ordering principle.** Not "which
subsystem is duplicated most", but **"which class of bug would a shared owner
have caught in eight places".**

**4. It is a warning about this project's own instincts.** Most of what this repo
has proposed is optimisation — a render graph, an upscaler, instruction
repurposing, an IR decision. **The fleet's record says that lane is where the
refutations live.**

## Limits

- **This is one sentence from one fork's GPU review**, generalised by me against
  today's reading. **rpcsx wrote it about its own codebase, not about the fleet.**
- **The win/refutation table is assembled from documents, not from a systematic
  census.** A systematic one would need every `WIN` entry in xenia's 57 read, and
  57 have not been read.
- **"Broken" is doing work in that sentence.** A stale config and a wrong core
  index are clearly bugs; **an extension never requested is arguably a missing
  feature.** The line is not sharp.
- **The `gpubusy` result is one title, one scene, one arm each.** rpcsx did not
  claim otherwise, and neither does this.
- **`/sys/class/kgsl/kgsl-3d0/gpubusy` is a device instrument this repo has never
  recorded** — cumulative busy/total, resets on read, needs no root.

## Sources

- rpcsx `docs/arm64/adreno-tiler.md`, `tools/thor_gpu_busy_ab.sh`
- rpcsx `app/src/main/cpp/rpcsx/rpcs3/Emu/RSX/VK/VKRenderPass.cpp`
- the day's other logs, `research_log/20260824_*.md`
