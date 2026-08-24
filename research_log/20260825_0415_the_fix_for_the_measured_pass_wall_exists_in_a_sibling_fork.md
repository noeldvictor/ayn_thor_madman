# The fix for the measured pass wall exists in a sibling fork, and a three-line bug that reads as a hang

**Goal: read the GPU half of xenia's 2026-08-06 ledger burst. It turns out to
answer a problem this repo has measured and had no fix for.**

**Device-free: a ledger query. Everything below is xenia's reading of XenDroid,
not mine.**

## The problem this repo already measured

`CLAUDE.md` records, from 159 gameplay frames: **45 EDRAM ownership transfers per
frame**, `pass_break_rt_change` **27**, and **24 of the 45 change no format at
all** — moves, not reinterpretations. **A third of that title's passes exist only
to service them.**

**xenia states the mechanism plainly:**

> *"xenia's EDRAM resolve **ENDS the render pass**, copies, and begins another.
> On a TBDR every pass begin is **a GMEM tile store plus reload**... `local_read`
> lets the resolve read the CURRENT attachment **on-tile** so the pass never
> breaks."*

## The fix exists, in a sibling fork, as sixteen commits

**XenDroid — which this repo lists only as a reference clone — has a coherent
16-commit series implementing in-pass EDRAM resolves through
`VK_KHR_dynamic_rendering_local_read`.** xenia's sweep found its own tree **27
commits behind**, and:

> *"OUR TREE HAS NEITHER: grep for `dynamic_rendering_local_read` and
> `in_pass_resolve` in `src/xenia/gpu/vulkan` returns NOTHING."*

**And it names why this matters beyond one fork:** *"This is precisely the gap
... why XenDroid outruns us on the Thor."*

### Step 1 is done and device-verified

> **"DEVICE-VERIFIED AVAILABLE ... Burnout on verified Turnip 26.3.0 reports
> `dynamic_rendering_local_read=true`."**

**Declared through the existing promoted-extension machinery, with the feature
struct mirroring how `EXT_rasterization_order_attachment_access` is already
handled.** One portability note worth keeping: **pre-1.4 Vulkan headers require
the KHR-suffixed names** — `VkPhysicalDeviceDynamicRenderingLocalReadFeaturesKHR`.

### This bears on an open device-queue entry, and does NOT settle it

**`DEVICE_QUEUE.md` entry 26 asks whether in-pass attachment self-read works on
this device**, because ARMSX2's driver profile marks **both** forms broken on
Turnip/Adreno — measured on an **Adreno 650 with Mesa 26.1.2**.

**This adds one fact: the extension is exposed and enabled on an Adreno 740 with
Turnip 26.3.0.** It does **not** add a correctness result.

> **And this project has a recent, expensive lesson about exactly that
> confusion.** azahar confirmed all four `extended_dynamic_state3` blending
> features on the physical Thor, rendered correctly, and measured nothing — its
> rule: **"extension availability is not optimization evidence."**
>
> **`local_read` being available is a precondition, not a result.**

## The second item is independent, smaller, and takes itself

**`904374971`: hoist shared-memory uploads out of render passes.** ~162 lines
across 7 files, **explicitly independent of the resolve chain.**

**The argument is the elegant part:**

> a shared-memory upload issued **during** a render pass forces the pass to END,
> and on a TBDR that is a GMEM store and reload. **But pages never invalidated
> SINCE THE CURRENT GPU SUBMISSION OPENED cannot have been legitimately read by an
> already-recorded command**, so their upload can be safely reordered to the
> **HEAD** of the submission's command buffer — executing before any pass begins.

**A correctness argument that buys a reordering**, rather than a heuristic. It
needs submission-scoped `invalidated_in_submission` tracking, an
`OnGpuSubmissionOpened` hook, and a submission-head deferred command buffer
replayed before the main one.

## The safety mechanism is the part to copy

xenia's port order ends with:

> **`c13b9be1f` — gate by proven roundtrip class and COUNT rejections. "Keep that
> counter, it is how they made it safe."**

**A fast path that silently declines is indistinguishable from one that is not
running** — which is this project's `DID_IT_APPLY` problem and its
prove-the-instrument rule in one. **A rejection counter turns "did it apply" into
a number.**

**`thor_backend.h` already reached the same conclusion from the other side**:
every decline reason is separate, because ARMSX2 learned that *"nothing got
upscaled"* has half a dozen causes. **Same rule, different subsystem, third
independent arrival.**

**And the hardening series is worth reading as a checklist of what breaks**:
reject resolves whose mapped rect leaves the render area; keep `loadOp` discard
bits out of framebuffer and pipeline keys; scope input-attachment index mapping
per draw; destination matching, strip row offset, 2D texel origin.

## And a three-line bug that reads as a hang — this one is OURS

**Taken by xenia in the same sweep, from XenDroid `4b416cd83`:
`FLAG_KEEP_SCREEN_ON`.** Its reason:

> **gamepad play generates NO touch events, so the display timeout still fires**;
> a sleeping panel stops the activity, **the `SurfaceView` loses its surface, and
> the presenter silently DROPS every guest frame while the emulator keeps
> running** — *"which reads as a hang and already cost a full session."*

**This is an app-shell requirement for this project specifically, and it is not
in `app/SCREENS.md`.**

**`CLAUDE.md` requires gamepad-first navigation** — *"every screen must be
drivable without touching the glass, and touch is the addition rather than the
assumption."* **That decision GUARANTEES this bug.** A person playing with the
Thor's physical buttons generates no touch events for the whole session.

**And the failure mode is the worst kind: the emulator keeps running and every
frame is discarded.** Not a crash, not a stall — **a silent frame drop that
presents as a hang.** It is a `DID_IT_APPLY` sibling: **the work happened and the
output went nowhere.**

**One rejected item from the same sweep is worth recording as rejected**, because
it stops the question: XenDroid pins its main process out of the cached band,
which **does not apply** — it runs the emulator in a separate `:emu` process, and
xenia runs it in the main process, so there is no cached-band launcher to
protect. **This project packs everything into one binary, so xenia's situation is
ours, not XenDroid's.**

## Limits

- **Everything here is xenia's ledger**, its reading of XenDroid's commits, and
  its device probe. **Nothing reproduced, no device used, no commit read by me.**
- **The in-pass resolve series is NOT PORTED and unmeasured.** xenia says
  cherry-picks will not apply, because XenDroid vendors xenia-edge and its own
  GPU has diverged. **It is a multi-session hand port with no number attached.**
- **`local_read` availability is not a correctness or performance result**, and
  the ARMSX2 Turnip attachment-self-read rule is not answered by it.
- **The upload-hoist correctness argument is quoted, not verified.** Whether
  "never invalidated since the submission opened" is sufficient in xenia's own
  tree was not checked.
- **`FLAG_KEEP_SCREEN_ON` is three lines and the reasoning is sound, but the
  failure was xenia's, on its shell.** Whether this repo's `app/shell/` has the
  same exposure was not tested — **it has no game running behind it yet.**

## Sources

- xenia `tools/exp_ledger.py check "dynamic_rendering_local_read"`
- `DEVICE_QUEUE.md` entry 26
- `research_log/20260824_2000_the_fork_had_a_routing_table_and_i_did_not_read_it.md`
- `shared_layer/DID_IT_APPLY.md`
