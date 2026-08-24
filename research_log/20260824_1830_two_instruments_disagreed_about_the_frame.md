# Correction: three measurements, three answers, and the fork itself calls the confound open

**This corrects
[`20260824_0410_the_rearch_premise_was_refuted_on_device.md`](20260824_0410_the_rearch_premise_was_refuted_on_device.md),
written this morning, and the banner it put on `shared_layer/THOR_RENDER.md`.**

## What I recorded

From `xenia/docs/research/native-render-path-rearch.md`, **dated 2026-07-04**:

> **The driver `u_trace` already said BD's frame = ~90% fragment/draw EXECUTION**,
> ~6 ms EDRAM structure, ~1 ms tile-I/O [...] there is **NO 15-20x of emulation
> structure to remove.**

**I took that as the settled frame anatomy and propagated it into `CLAUDE.md` and
a "read this first" banner on `THOR_RENDER.md`.**

## What the ledger says five days later

`bd_field_tilestore_bound_not_fragment`, **`WIN`, 2026-07-09**, from per-pass GPU
timestamps on a non-frozen heavy field with stable medians:

| Quantity | Value |
| --- | --- |
| `gpu_frame_us` | 42,698 |
| `gpu_pass_us` (in-pass draws) | **12,568 — 29%** |
| **GAP** (between-pass EDRAM tile-store and barriers) | **30,130 — 71%** |

across **~43 passes**, with the CPU fence-blocked 33.6 ms waiting on the GPU.

**And it says explicitly what it overturns:**

> **CORRECTS the "GPU-fragment-bound" memory finding (it was wrong; only 29% is
> in-pass).**

> **BD field is EDRAM-TILE-STORE-bound, NOT foliage-overdraw-bound.**

**Same title. Five days apart. Opposite conclusions about where the time goes.**

## What still stands, and what does not

**Stands — these were direct A/Bs, not anatomy:**

- **Bindless resources regressed**, 129 ms to 161 ms, rendering pixel-perfect,
  with descriptor and pipeline bind counts unchanged. The reason is solid: the
  1,064 per-draw binds are **shared-memory and per-draw constants**, which
  bindless cannot remove.
- **Native GMEM-resident render targets measured `DEAD`**, with a specific
  mechanism and a retracted `CONFOUNDED` cross-run number.
- **Fragment levers cap low.** The 2026-07-09 entry agrees: fp16 slower, MSAA1
  slower, cheaper shadows flat — **all failed.**

**Does not stand as I wrote it:**

- **"There is only ~7 ms of structure to reclaim."** The later measurement puts
  **30 ms between passes.**
- **"Structure rearchitecture cannot reach 30 fps."** The later entry projects
  the opposite from its own numbers: **cut passes 43 to ~20, tile-store 30 ms to
  15 ms, giving 27.6 ms.**

**And the sequel is not a success either.** Attempts to exploit the tile-store
finding are in the same ledger: **`gpu_skip_edram_transfers` `CONFOUNDED`**,
**`gpu_vulkan_inpass_edram_transfers` `FLAT`** — twice, on 2026-07-05 and
2026-07-10.

> **So the fork's own position is unresolved, and this morning I presented one
> side of it as settled.**

## A third measurement, and it disproves a premise I quoted

**Found by continuing the audit.** `CORRECTION: half-width RT = NO change`,
**`OPEN`, 2026-07-06** — between the other two.

> **RIGOROUS DISPROOF:** `gpu_bd_native_rt_width=640` — half the native RT width,
> **half the fragments** — gave **818 ms against 823 ms. NO change.**
> **Fragment/pixel overdraw would scale with resolution. It does not.**

**So the cost is not fragment overdraw**, and the entry says so explicitly:
*"contradicts my prior 'alpha-test foliage overdraw' claim, which was from the
Qualcomm profile."*

**I quoted that overdraw claim this morning** — the line about alpha-test foliage
defeating early-Z so the Adreno's effective advantage is only 2-4x. **It was
already disproved in the same fork, two days after the document I took it from.**

**And the entry names its own confound.** The 823 ms profile is **on the Qualcomm
proprietary driver, 14x slower than Turnip**, so it is dominated by that driver's
handling of the native pass rather than by the game.

> **Its own verdict: "The TURNIP perf confound is GENUINELY UNIDENTIFIED."**
> Profiling on Turnip is blocked by the Turnip timing-race crash, so **the only
> stable profile available is the driver-confounded one.**

**That leaves three measurements and three answers:**

| Date | Instrument | Verdict |
| --- | --- | --- |
| 2026-07-04 | driver `u_trace` | ~90% **fragment execution** |
| **2026-07-06** | **half-resolution A/B** | **not fragment-bound at all** — 818 against 823 ms |
| 2026-07-09 | per-pass GPU timestamps | 71% **between-pass tile store** |

**The 14x driver figure survives** — it is used *within* that entry as the reason
the Qualcomm profile is unusable, so it is treated there as established. **What
does not survive is any anatomy derived from a Qualcomm-driver profile.**

## The finding that replaces it, and it is more useful

**Two instruments measured one frame and disagreed about where the time went.**

| Instrument | Verdict |
| --- | --- |
| the driver's `u_trace` | **~90% fragment execution** |
| per-pass GPU timestamps | **71% between-pass tile store** |

**The rule this repo took from that document — measure the frame anatomy before
designing a render path — survives and gets sharper:**

> **An anatomy is only as good as its instrument, and this fleet now has an
> instance of two instruments disagreeing by a factor of three about the same
> frame.** A render-path decision resting on one of them rests on the choice of
> instrument.

**That is a better warning than the one I wrote**, because it does not require
believing either number.

## What I did wrong, precisely

- **I read one document and did not query the ledger for later entries on the
  same subject.** `exp_ledger.py check` is one command and it is the tool that
  exists to prevent this.
- **The repo's own rule covers it**: *a newest failure outranks an older
  success.* **I applied it to results and not to measurements.**
- **The document I read carried a single date**, 2026-07-04, with no pointer
  forward — so nothing in it was wrong; **the omission was mine.**

## Corrections made

- This log, and the banner on `THOR_RENDER.md` rewritten to state the conflict
  rather than the refutation.
- `CLAUDE.md`'s measurement rule amended: **name the instrument with the
  anatomy**, and **check the ledger for a later measurement of the same thing.**

## Sources

- xenia `docs/research/native-render-path-rearch.md` (2026-07-04)
- xenia `tools/exp_ledger.py`, entries `bd_field_tilestore_bound_not_fragment`
  (2026-07-09), `gpu_skip_edram_transfers`, `gpu_vulkan_inpass_edram_transfers`
