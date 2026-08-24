# xenia built this project's render rearchitecture, measured it, and refuted its premise

**Goal: mine the fleet's own research corpus, starting with the documents that
record negative verdicts.**

**`tools/fleet_docs_index.py` indexes 1,139 tracked markdown files across nine
forks — nearly double the estimate in `CLAUDE.md` — and lists 51 that record a
`DEAD`, `FLAT`, `REFUTED` or `NO-OP` verdict.**

**The first one opened is `xenia/docs/research/native-render-path-rearch.md`,
and it is this project's `THOR_RENDER.md` bet, built and measured on this device
in July 2026.**

## What xenia set out to do, and why it is our plan

Its opening line is a user mandate: **"NO MORE INCREMENTAL LEVERS, REARCH."**

The premise, stated there:

> BD's ~15-20x emulation gap is a **COMPOUNDING STACK of ~2x emulation taxes**,
> not one bug. Every single-layer lever read FLAT because removing ONE layer
> leaves the other four dominating. The rearch removes the WHOLE stack.

**Its five taxes are `THOR_RENDER.md`'s agenda almost item for item:** in-shader
vertex fetch instead of native input assembly; 1,272 state changes per frame
context-rolling the tiler; EDRAM round trips across 42 render passes instead of a
GMEM-resident pass; guest-to-SPIR-V shader bloat with fp32 where fp16 would do;
and format and bandwidth waste.

## What it measured

| Brick | What | Result |
| --- | --- | --- |
| 0 | native vertex input | shipped, **FLAT alone** |
| **1** | **bindless resources** | built, **pixel-perfect, and REGRESSED 129 -> 161 ms** |
| **2** | **native GMEM-resident render targets** | built, **DEAD** |
| 3 | lean fp16 shaders | not built |

**Brick 1's failure is the informative one**, because it says why the model was
wrong:

> **the 1064 descriptor binds are NOT texture binds** — they are set-0
> (shared-mem) + set-1 (per-draw CONSTANTS), inherent per-draw data bindless
> can't touch; the 208 pipeline binds are shader/render_pass variants. **So the
> "1272 state-change tax" is NOT collapsible via bindless textures.**

**Brick 2 died with a specific mechanism and a retracted number:** a 1-sample
host image is half the tile footprint the guest addresses at guest-MSAA, so
content wrapped and two windmills rendered — and the earlier **"cap=1 = -42%"
was a `CONFOUNDED` cross-run number.**

## The refutation, and it is the part this repo must absorb

> **The driver `u_trace` already said BD's frame = ~90% fragment/draw EXECUTION
> (the intrinsic foliage rendering), ~6 ms EDRAM structure, ~1 ms tile-I/O.**
> [...] **there is NO 15-20x of emulation structure to remove.** [...] The
> "compounding stack of emulation taxes" model was WRONG: **the taxes sum to
> ~2-3x, not 15-20x; the rest is intrinsic.**

And the hardware figure this repo also relies on gets qualified:

> **The 10-20x hardware figure is PEAK on friendly workloads** — BD's alpha-test
> overdraw foliage defeats the GPU's early-Z/HSR (every layer shaded), so the
> Adreno's **EFFECTIVE advantage for THIS workload is ~2-4x**, which the
> emulation's ~2-3x eats.

**Structure rearchitecture had about 7 ms to reclaim, and the one brick that
attacked the largest slice made it worse.**

## What this does and does not say about `THOR_RENDER.md`

**It does not cancel the shared render path.** It bounds it, on evidence, for the
first time.

**What it establishes:**

- **Measure the frame anatomy before designing a render path.** The split between
  emulation structure and intrinsic rendering work decides whether structural
  work can win, and it is measurable on this device.
- **`CLAUDE.md`'s existing position is now much better supported.** It already
  says to expect maintenance wins rather than large frame wins. **The evidence
  was "the ledger has DEAD entries"; it is now a frame anatomy.**

**What it does not establish, and this matters:**

- **It is one title on one backend**, and Blue Dragon's alpha-test overdraw
  foliage is close to a worst case for a tiler. **The ratio it measured is a
  property of that workload.**
- **The ratio is what generalises, not the number.** For a heavy 3D guest,
  intrinsic rendering dominates and structure work cannot win. **For a light
  guest — DS, 3DS, PS2 2D — intrinsic work is small, so the same fixed structural
  overhead is a much larger share.**

> **The uncomfortable consequence: the shared render layer is most likely to pay
> on the LIGHT backends, not the heavy ones.** That is the opposite of where this
> repo has been pointing it, and it is testable per backend with the same
> instrument.

## The instrument, which this repo does not have and needs

**xenia has a headless per-stage GPU profiling methodology for the Thor**,
`.agents/skills/xenia-thor-adb-gpu-stage-split/SKILL.md`. It answers
binning/vertex against fragment against stall, and per-draw cost, **over plain
adb, with no root and no desktop GUI.**

Its routes: **Mesa/Turnip's freedreno perfetto counter producer**,
**gfxreconstruct capture and replay profiling**, **in-engine per-pass Vulkan
timestamps**, and the AGI CLI.

**It also names the trap.** Headless `adb shell perfetto` with **KGSL ftrace
events returns empty on the retail Thor**: shell is uid 2000, it can read tracefs
but cannot enable events, and there is no `su`. **The kernel-ftrace route is dead
headless.** The working routes use the GPU driver's own per-context counters,
which a **debuggable** app is allowed, or offline replay.

**Two consequences for this project:**

1. **`CLAUDE.md`'s measurement discipline names fps, frame time, watts and
   temperature. It does not name the per-stage GPU split**, which is the number
   that decides every render-path question. Add it.
2. **The per-context counter route requires the app to be debuggable.** That is a
   build-configuration requirement for the unified app and it is recorded
   nowhere.

## And a conflict between two forks' stacking rules, unrecorded until now

**`CLAUDE.md` carries rpcsx's stacking rules**: one new component per proof run,
and only after each is individually clean on the same route.

**xenia's rule is the opposite**, and it is stated as the lesson of the failures
above:

> **RULE: build + measure the COMPOUND, never one layer (that's the trap that
> killed every lever).** [...] One-brick A/B is meaningless here (the stack hides
> it).

**Both are right, in different regimes.** rpcsx's rule protects attribution: with
independent components, stacking blind hides which one regressed. xenia's rule
protects detection: with multiplicative taxes, each component alone is below the
noise floor.

> **Decide which regime you are in before choosing a stacking rule, and say
> which one you used.** A repo that carries only one of these will use it in the
> wrong regime.

**xenia's own outcome is the caution against its own rule**: the compound of
bricks 0 and 1 regressed, and the anatomy showed the premise was wrong from the
start. **The frame anatomy would have said so before either brick was built.**

## Limits

- **One document, one title, one backend, read once.** Fifty more documents carry
  negative verdicts and are unread.
- **The `u_trace` frame anatomy is quoted from this document; the trace itself
  was not seen.**
- **No number here was reproduced.** All of it is xenia's measurement, on xenia's
  workload, in July 2026.
- **The light-backend hypothesis is a hypothesis.** Nothing has measured the
  structure-to-intrinsic ratio on melonDS, azahar or ARMSX2.

## Sources

- xenia `docs/research/native-render-path-rearch.md`
- xenia `.agents/skills/xenia-thor-adb-gpu-stage-split/SKILL.md`
- `tools/fleet_docs_index.py`
