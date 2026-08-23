# xenia plans render passes, and it does it retroactively

**Goal: verify "no fork plans render passes", the highest-value of the eleven
unverified negatives `tools/supervise.py` found in
[`CLAUDE.md`](../CLAUDE.md).**

No device. Reading only.

## Result: wrong, and it is the most consequential miss so far

**xenia does the most sophisticated render pass planning in the fleet.** It is
GMEM-aware, proven by analysis rather than assumed, per-attachment, and it has
safe fallbacks.

**Two claims fall together:**

| Claim | Verdict |
| --- | --- |
| "no fork plans render passes" | **WRONG** |
| "Nobody resolves MSAA on-chip" | **WRONG** — xenia uses `pResolveAttachments` |
| "Nobody uses input attachments" | **holds** — every hit in every fork is a zero-initialiser |
| "Nobody merges passes" | **holds** — one subpass everywhere |

**This is the sixteenth absolute negative in this repo to be wrong.**

## Why the fleet ranking in CLAUDE.md is incomplete

`CLAUDE.md` ranks four forks on attachment-operation correctness — Vita3K 1,
Cemu 2, azahar 3, eden 4. **xenia was not in that table**, and on this evidence
it belongs at the top.

**The earlier survey read the render pass *cache*.** xenia's planning is not in
its cache; it is in `vulkan_command_processor.cc`, driven by draw analysis, and
the cache is only where the resulting variant is fetched. **Same instrument
error as every other miss today.**

## What xenia actually does

### 1. On-chip MSAA resolve, with the store elided

`vulkan_render_target_cache.cc:3198`:

```cpp
subpass.pResolveAttachments = bd_color_resolve ? bd_resolve_refs : nullptr;
```

and, crucially, the multisample colour is **not stored**:

```cpp
// BD color-resolve: the MSAA color need not be stored (only the resolved 1x
// is kept) - DONT_CARE lets the tiler skip the MSAA store.
attachment.storeOp = (cvars::gpu_edram_passes_dont_care || bd_color_resolve)
                         ? VK_ATTACHMENT_STORE_OP_DONT_CARE
                         : VK_ATTACHMENT_STORE_OP_STORE;
```

**That is the whole point of an on-chip resolve on a tiler**: resolve inside the
pass and never write the multisample buffer to system memory. **It is the single
most valuable MSAA optimisation on this GPU** and the repo recorded it as absent.

### 2. `LOAD_OP_DONT_CARE` proven by replaying the guest's vertices

`gpu_edram_passes_dont_care_safe`:

> when a render pass's FIRST draw provably overwrites the entire render area
> unconditionally (a one-rectangle rectangle-list draw — the guest clear idiom —
> with always-pass depth write or replace-mode full-mask color write, **verified
> by replaying its vertex positions on the CPU**), begin the pass with
> `VK_ATTACHMENT_LOAD_OP_DONT_CARE` for the proven attachments, skipping their
> GMEM tile loads

**Per-pass and per-attachment**, and it names its own hazard: the raw
`gpu_edram_passes_dont_care` diagnostic "elides all loads AND stores and corrupts
titles that need the contents". **Any uncertainty falls back to loading.**

### 3. A depth path that knows `STORE_OP_NONE` is not `DONT_CARE`

`gpu_vulkan_skip_unused_depth_store`: when draws provably never test or write
depth — `RB_DEPTHCONTROL` `z_enable`, `z_write_enable` and `stencil_enable` all
off — begin depth with `loadOp=DONT_CARE` and `storeOp=NONE`.

> **`STORE_OP_NONE` (Vulkan 1.3 core) PRESERVES the depth EDRAM memory** (unlike
> `STORE_OP_DONT_CARE`, which would undefine it and corrupt aliasing render
> targets)

**That distinction is subtle and correct**, and nothing else in the fleet makes
it.

### 4. The mechanism: patch the pass begin retroactively

**This is the part worth taking wholesale.**

xenia records `vkCmdBeginRenderPass`, accumulates per-attachment coverage while
the pass runs, and then **patches the already-recorded begin command** once it
knows what every draw did:

```cpp
VkRenderPass variant = render_target_cache_->GetHostRenderTargetsRenderPass(
    retro_pass_key_, load_dont_care_mask, depth_none);
deferred_command_buffer_.PatchBeginRenderPassTargets(begin_pos, variant,
                                                     retro_depth_framebuffer_);
```

**It works because of a Vulkan rule the comment names:**

> Load/store ops do not affect Vulkan render pass compatibility, so the recorded
> framebuffer and all pipelines stay valid. Never breaks a pass — an ineligible
> pass simply keeps its original begin.

**So the decision does not have to be made before the pass starts.** That
removes the hardest constraint on planning attachment operations, which is that
a renderer usually cannot know at `BeginRenderPass` what the pass will contain.

`RetroCoverage` tracks per attachment an interval list with `x0`/`x1`, a
`complete` flag and a **`poisoned`** flag, so a partial union and a
disqualifying draw are distinguished. There is a diagnostic that logs exactly
where the chain breaks — "no contributors at all vs partial union vs poisoned".

## What this changes

**[`THOR_RENDER.md`](../shared_layer/THOR_RENDER.md) commitment 2 rests on a
false premise.** `CLAUDE.md` argues:

> A shared render graph therefore **adds** planning nobody has, rather than
> replacing tuned structure. That is a much smaller commitment than assumed.

**The second half is now wrong.** xenia has tuned structure, and a shared graph
that ignores it would replace analysis with a lookup.

**The revised position:**

1. **Take xenia's retroactive-patch mechanism as the shared graph's core.** It
   is BSD, it is already written, and it solves the ordering problem that makes
   pass planning hard.
2. **Take its three provers** — full-area first draw, unused depth, resolve —
   as the first three rules the graph knows.
3. **Keep the propagation list.** Vita3K's transient attachments, Cemu's
   `DONT_CARE` depth default and azahar's `eClear` are still real and still
   missing from the forks that lack them.
4. **Stop describing the fleet as unplanned.** One fork planned; six did not.

## What this does not say

- **No claim that any of it is faster on the Thor.** Every flag read here is
  **off by default** and marked `EXPERIMENTAL (Thor/TBDR)`. Nothing is measured.
- **No claim these generalise.** They are proven against **Xenon guest state** —
  `RB_DEPTHCONTROL`, the guest clear idiom, EDRAM tile rounding. **The provers
  are guest knowledge; the mechanism is not.**
- **`bd_color_resolve` is a per-title path**, named for Blue Dragon. How broadly
  it applies was not read.

## Method

Counted `pInputAttachments`, `pResolveAttachments` and subpass symbols across
seven forks, excluding vendored trees, **then read every hit** rather than
trusting the count. Cemu's and eden's input-attachment hits are
`inputAttachmentCount = 0` initialisers; xenia's resolve hit is real. Then read
`vulkan_render_target_cache.cc`, `gpu_flags.cc` and
`vulkan_command_processor.cc`.

**Reading the hits is what separated this from the earlier survey**, which
counted and ranked without opening xenia at all.

## The ten still unchecked

- "Vita3K tracks transient attachments and nothing else does"
- "nobody has priced Anime4K's two dedicated passes"
- "xenia is the only fork using the device's vector features at all"
- "melonDS-android is the only fork with a verified build recipe"
- "almost no emulator does performance-as-a-test"
- and five more
