# Three forks use input attachments; one truly merges passes. Both claims were wrong

**Goal: re-check "nobody merges passes, nobody uses input attachments" with a
probe that uses more than one vocabulary.**

**Both halves are wrong, and they are wrong by different amounts. Twelfth
absolute negative in this repo to fail.**

| Fork | Input attachments | **Multi-subpass merge** |
| --- | --- | --- |
| **xenia** | yes, with a `subpassInput` shader variant | **YES — a 2-subpass merged feedback pass** |
| **ARMSX2** | yes, depth `subpassLoad` in tile | **no — `MAX_SUBPASSES = 1`**, a self-dependency |
| **rpcsx** | yes, a real non-zero array | **no — `subpassCount = 1`** |
| Cemu, eden | zero initialisers, as previously found | no |

**"Nobody uses input attachments" is wrong three times over. "Nobody merges
passes" is wrong once.**

**The distinction matters and is not pedantry.** ARMSX2 reaches the same
tile-memory benefit **without** a subpass boundary, by using an ordered in-tile
read. **A merge is one way to keep data in tile memory; it is not the only
way.**

**This is the most consequential correction so far**, because merging passes to
keep data in tile memory is the single largest structural win available on a
TBDR, and this repo believed the fleet had none of it.

## What the previous survey did

`CLAUDE.md`: *"Nobody merges passes. Nobody uses input attachments — verified
2026-08-23 by counting `pInputAttachments` across seven forks and reading every
hit; Cemu's and eden's are `inputAttachmentCount = 0` initialisers."*

**The reading was correct. The vocabulary was one word wide.** A render pass
declares input attachments through `pInputAttachments`, but the interesting half
is spelled elsewhere:

- **`subpassInput` / `subpassLoad`** in the shader
- **`vkCmdNextSubpass`** in the command stream
- **`VK_EXT_rasterization_order_attachment_access`** for the ordered case

**Found with `tools/capability_probe.py`**, four probes, on the run after it was
written.

## xenia: a merged two-subpass feedback pass, with a shader variant

`gpu_vulkan_feedback_merge`. `deferred_command_buffer.h:516`:

> **BD input-attachment merge (Inc3): advance to the next subpass of a merged
> 2-subpass feedback render pass (producer in subpass 0, the same-pixel composite
> consumer reading it as an input attachment in subpass 1).**

**Three parts, and all three are the hard parts:**

1. **The shader translator emits the variant.**
   `spirv_shader_translator.h:605` carries
   `PixelShaderModification.feedback_input_...`, and
   `spirv_shader_translator_fetch.cc` reads *"the producer RT at this fragment's
   position"* — the texture fetch becomes a `subpassInput` read. Its comment
   notes there is **no descriptor-binding collision** because it is a distinct
   shader variant.
2. **The command buffer can repoint a recorded pass.**
   `deferred_command_buffer.h:67` — *"retroactively repoint an already-recorded
   BeginRenderPass"* — and line 85 validates the captured position. **That is the
   same retroactive-patch trick `CLAUDE.md` already records for load and store
   ops**, reused for a second purpose.
3. **It is a real subpass advance**, `CmdVkNextSubpass`, not a second pass.

**What it replaces:** ending the pass, resolving the colour target to system
memory, and starting a new pass that samples it. **On a tiler that round trip is
the cost.**

## ARMSX2: tile-native ordered depth feedback, called "mobile ROV"

`GSDeviceVK.cpp:3882`, comment verbatim:

> **Mobile tile-native ordered depth feedback ("mobile ROV"), opt-in via HWROV.
> Reads the depth buffer in-tile (`subpassLoad` on a depth input attachment)
> instead of copying it to a colour RT (`DoBeginDSAsRT`), so SW-Z / DATE /
> alpha-test / AA1 depth passes fuse in-pass rather than round-tripping.**

**It states its own requirements and its own escape hatch:**

- It needs an ordered in-tile depth read: **ROAA on the depth aspect** on the
  `framebuffer_fetch` path, **or** a render-pass self-dependency on the
  `texture_barrier` path.
- **Gated behind `HWROV` so toggle-off is byte-for-byte the well-tested
  fallback.** That is the same default-off, byte-identical discipline xenia uses
  for frame generation.
- **Mali r44p1 excludes itself** by having `texture_barrier` forced off.
- **Read at device init**, so the depth half applies on game restart — a
  live-changeability note of exactly the kind this project's settings scope rule
  needs.

**ARMSX2 also runs an input-attachment feedback descriptor path** on the case
where `texture_barrier` is on and feedback-loop layout is unavailable, and its
device banner prints whether it is active.

## The two connect to a measured device fact

**ARMSX2's mobile ROV wants `VK_EXT_rasterization_order_attachment_access`.**

- `tools/vk_capability_census.py`: **ARMSX2 and Vita3K are the only two forks
  that request ROAA.**
- xenia's device audit, 2026-06-20, probed the Thor and recorded
  **`roaa_color=1 roaa_depth=1`** — and noted the same probe read **false** three
  days earlier on an older Turnip build.

> **So ARMSX2's tile-native depth path is capability-available on this device,
> and it is opt-in and unmeasured.**

That is a device experiment with a named flag, a stated fallback, and a
byte-identical off state. It goes to `DEVICE_QUEUE.md`.

## What this changes in the plan

`CLAUDE.md` says a shared render graph would **add** planning nobody has. **That
was already corrected once** when xenia turned out to plan render passes. **It
needs correcting again**: xenia also **merges** passes, with a shader variant and
a retroactive command-buffer patch, and two more forks reach tile-local reads
through input attachments without merging.

**The shared layer's job here is not to invent pass merging. It is to stop it
being written a fourth time**, and to give the existing designs one place to
live. **xenia's is BSD**, so its mechanism is usable anywhere; ARMSX2's is
GPL-3.0, which the app already is.

**The four-fork attachment-ops ranking in `CLAUDE.md` is also incomplete**, since
it omitted both of these forks' merging behaviour and ranked only load and store
ops.

## Limits

- **Both are off by default.** `gpu_vulkan_feedback_merge` and `HWROV` are
  opt-in, and neither is measured on the Thor.
- **Both are title-shaped.** xenia's is named for one game, Blue Dragon.
  ARMSX2's targets specific PS2 idioms — SW-Z, DATE, alpha-test, AA1. **Neither
  is a general pass-merging framework**, and this log does not claim one exists.
- **Cemu and eden's `pInputAttachments` remain zero initialisers**, as the
  earlier survey found. **The negative was wrong about the fleet, not about those
  two forks.**
- **azahar hit the merge-vocabulary probe and the hit was not resolved.** Treat
  azahar as **unchecked**, not as absent.
- **rpcsx also requests `VK_KHR_dynamic_rendering_local_read`**, which
  `vk_capability_census.py` reported as requested by nobody. **That census does
  not cover rpcsx at all**, and reads one device-layer file per fork. Its
  no-user list is a floor, and this is the second time that has bitten.
- **Nothing is measured.**

## Sources

- xenia `src/xenia/gpu/vulkan/deferred_command_buffer.h`,
  `src/xenia/gpu/spirv_shader_translator.h`,
  `src/xenia/gpu/spirv_shader_translator_fetch.cc`
- ARMSX2 `pcsx2/GS/Renderers/Vulkan/GSDeviceVK.cpp`
- xenia `docs/research/20260620-adreno-turnip-feature-gap-audit.md`
- `tools/capability_probe.py`, `tools/vk_capability_census.py`
