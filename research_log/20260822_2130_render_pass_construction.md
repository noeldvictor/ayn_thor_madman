# How the forks actually build render passes

Goal: settle whether a shared render graph would take something the backends
own. This was the weakest joint in the architecture and the one blocked on
"needs device measurement".

Date: 2026-08-22, 21:30 EDT.

## Result: it takes nothing, because no fork plans passes

**The concern was wrong.** THOR_RENDER.md commitment 2 was written cautiously,
saying a shared render graph "is the one place where the shared layer takes
something a backend owns today, and it needs measurement per backend before it
is taken."

Reading the code shows the backends do not own pass planning. They own
**format-keyed render pass creation**, which is a different and much smaller
thing.

### eden: one subpass, unconditional load and store

`src/video_core/renderer_vulkan/vk_render_pass_cache.h`, 57 lines. The entire
key:

```cpp
struct RenderPassKey {
    std::array<VideoCore::Surface::PixelFormat, 8> color_formats;
    VideoCore::Surface::PixelFormat depth_format;
    VkSampleCountFlagBits samples;
};
```

**Formats and sample count. Nothing else.** No load or store op, no subpass
structure, no dependency information.

The `.cpp` fills in the rest as constants:

```cpp
.loadOp         = VK_ATTACHMENT_LOAD_OP_LOAD,
.storeOp        = VK_ATTACHMENT_STORE_OP_STORE,
.stencilLoadOp  = has_stencil ? LOAD_OP_LOAD  : LOAD_OP_DONT_CARE,
.stencilStoreOp = has_stencil ? STORE_OP_STORE : STORE_OP_DONT_CARE,
...
.subpassCount        = 1,
.inputAttachmentCount = 0,
.pResolveAttachments  = nullptr,
```

Four things follow, and each is a tiler anti-pattern:

1. **Colour always loads and always stores.** Every pass reads every attachment
   in from system memory and writes it back, whether or not anything needed it.
2. **Always exactly one subpass.** Merging is impossible by construction.
3. **Zero input attachments.** A later pass can never read an earlier one
   on-chip; it must go through system memory.
4. **No resolve attachments.** MSAA cannot resolve inside the pass.

This is a portable, conservative design. It is inherited from yuzu, which
targeted desktop GPUs where none of these choices costs much.

Note the licence header: the file is Eden GPL-3.0-or-later over a yuzu
GPL-2.0-or-later original. **Per-file licence checking confirmed again.**

### Vita3K: also format-keyed

`renderer/vulkan/pipeline_cache.h`:

```cpp
std::map<vk::Format, vk::RenderPass> render_passes[2][2][2];
std::map<vk::Format, vk::RenderPass> shader_interlock_pass;
```

A format-keyed lookup with three boolean dimensions. Again a cache, not a
plan.

## What this changes

**Commitment 2 becomes additive rather than subtractive.**

| | Was assumed | Actually |
| --- | --- | --- |
| What backends own | pass structure, tuned | format-keyed pass creation |
| A shared graph would | take it away, risky | add planning nobody has |
| Evidence needed first | per-backend measurement | far less |

The risk that a shared render graph would be worse than each backend's tuned
structure **does not apply**, because there is no tuned structure. There is one
subpass and unconditional load and store.

This does not make a render graph free. It makes it **additive**, which is a
much smaller commitment than replacing something that works.

## The immediate win, and it needs no graph at all

**Choose load and store operations correctly.**

`LOAD_OP_LOAD` on an attachment the pass fully overwrites should be
`LOAD_OP_DONT_CARE` or `LOAD_OP_CLEAR`. `STORE_OP_STORE` on an attachment
nobody reads afterwards should be `STORE_OP_DONT_CARE`.

On a tiler a clear is a tile operation, while a load is an external read of
every tile. Depth in particular is very often stored when nothing reads it.

This is:

- **Small.** A change to how one struct is filled in.
- **Measurable.** Bandwidth and resolve count move directly.
- **Per backend.** No shared layer required, so it can be tested before any
  architecture lands.
- **Exactly the kind of thing the experiment ledger exists for.**

**Do this before building a render graph.** If correcting the ops moves
nothing, a graph that automates the same decision will also move nothing, and
that is worth knowing cheaply.

## Correction to record

`shared_layer/THOR_RENDER.md` and `shared_layer/thor_backend.h` both state that
render pass structure stays with the backend because taking it would be
premature. **The reasoning behind that caution was wrong**, though the
conclusion is still fine for now: leave passes with the backend until the load
and store experiment has run.

## Next

1. Run the load and store op experiment on one backend. Predict lower
   bandwidth and fewer resolves at identical output.
2. Read Cemu and azahar render pass construction. Neither was found by the
   search used here, so they name things differently and are still unread.
3. Only then decide whether the shared layer plans passes.

## Cemu and azahar, read

Both name their pass construction differently, which is why the first search
missed them.

### Cemu is better than eden on depth, and it is a free win

`src/Cafe/HW/Latte/Renderer/Vulkan/CachedFBOVk.cpp`:

```cpp
m_vkColorAttachments[i].loadOp  = VK_ATTACHMENT_LOAD_OP_LOAD;
m_vkColorAttachments[i].storeOp = VK_ATTACHMENT_STORE_OP_STORE;

m_vkDepthAttachment.loadOp    = VK_ATTACHMENT_LOAD_OP_DONT_CARE;   // default
m_vkDepthAttachment.storeOp   = VK_ATTACHMENT_STORE_OP_DONT_CARE;  // default
m_vkStencilAttachment.loadOp  = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
m_vkStencilAttachment.storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;

// then, conditionally:
m_vkDepthAttachment.loadOp  = VK_ATTACHMENT_LOAD_OP_LOAD;
m_vkDepthAttachment.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
```

**Cemu defaults depth and stencil to `DONT_CARE` and only promotes them to
load and store when the case needs it.** eden loads and stores depth
unconditionally.

That is the exact optimisation the cheap experiment proposes, already
implemented in one fork. **It is a free win to propagate**, and it is the
clearest example yet of the project's thesis: one fork solved it, the others
never heard.

### Cemu already supports dynamic rendering

`CachedFBOVk` carries `InitDynamicRenderingData()`, `GetRenderingInfo()` and a
`VkRenderingInfoKHR`, so it can run through `KHR_dynamic_rendering` as well as
classic render passes. Nothing else in the fleet appears to.

It also tracks which pipelines depend on an FBO, under a spinlock, so pipelines
can be invalidated when the FBO changes.

### azahar gives Anime4K its own render passes

`src/video_core/renderer_vulkan/vk_blit_helper.h`:

```cpp
vk::RenderPass anime4k_xy_renderpass;
vk::RenderPass anime4k_luma_renderpass;
```

Its pass cache is called `RenderManager`, which is why a search for
"render_pass" missed it.

**Anime4K costs two extra full-screen render passes here.** On a tiler each is
a load and a store of the whole target unless carefully arranged. That is a
concrete, measurable cost attached to the project's flagship feature, and it is
a strong candidate for subpass merging.

## Four forks, four different answers

| Fork | Pass cache keyed on | Colour ops | Depth ops | Subpasses | Dynamic rendering |
| --- | --- | --- | --- | --- | --- |
| eden-thor | formats, samples | LOAD / STORE | LOAD / STORE | 1, fixed | no |
| Cemu-thor | FBO identity | LOAD / STORE | **DONT_CARE by default** | — | **yes** |
| Vita3K-Thor | format, 3 bools | not read | not read | not read | no |
| azahar-thor | `RenderManager` | not read | not read | plus 2 Anime4K passes | no |

**Nobody merges passes. Nobody uses input attachments. Only Cemu defaults depth
to `DONT_CARE`.**

## Revised next steps

1. **Propagate Cemu's depth default.** It is already written, already correct
   for a tiler, and eden does the opposite. Check ARMSX2, Vita3K and azahar
   too.
2. **Measure azahar's two Anime4K passes.** The flagship feature has a known
   pass cost and nobody has priced it.
3. **Read Cemu's dynamic rendering path.** It is the only one, and dynamic
   rendering changes what a shared graph would even look like.
4. Read Vita3K and azahar load and store ops, still unknown.

## All four read. Vita3K is the best, eden the worst.

Completed 2026-08-22. Ranked by how tiler-correct the attachment operations
are.

### 1. Vita3K — the only one that tracks transient attachments

`vita3k/renderer/src/vulkan/pipeline_cache.cpp`:

```cpp
.loadOp  = is_color_transient ? eDontCare : eLoad,
.storeOp = is_color_transient ? eDontCare : eStore,
...
load_op  = force_load  ? eLoad  : eClear;
store_op = force_store ? eStore : eDontCare;
```

**A colour attachment that does not need to persist gets `DontCare` on both
load and store.** Depth defaults to `eClear` and `eDontCare` unless something
forces otherwise.

This is the correct tiler behaviour and **no other fork does it.** It is also
the design that pairs with `LAZILY_ALLOCATED` memory, since a transient
attachment that never leaves tile memory needs no backing allocation at all.

### 2. Cemu — right on depth, wrong on layout

Depth and stencil default to `DONT_CARE` and are promoted only when needed.
Colour is always `LOAD` and `STORE`.

Its dynamic rendering path carries a separate cost:

```cpp
m_vkColorAttachments[i].imageLayout = VK_IMAGE_LAYOUT_GENERAL;
m_vkColorAttachments[i].resolveMode = VK_RESOLVE_MODE_NONE;
```

**`VK_IMAGE_LAYOUT_GENERAL` works for everything and is optimal for nothing.**
On Adreno it can disable framebuffer compression, where
`COLOR_ATTACHMENT_OPTIMAL` would not. `RESOLVE_MODE_NONE` means no on-chip MSAA
resolve either.

So Cemu is ahead on depth and possibly behind on layout. **Both need
measuring; neither is obvious from reading alone.**

### 3. azahar — clears when clearing, always stores

`src/video_core/renderer_vulkan/vk_render_manager.cpp`:

```cpp
const vk::AttachmentLoadOp load_op = is_clear ? eClear : eLoad;
.loadOp = load_op, .storeOp = eStore,                    // colour
.stencilLoadOp = eDontCare, .stencilStoreOp = eDontCare, // colour's stencil
...
.loadOp = load_op, .storeOp = eStore,                    // depth
.stencilLoadOp = load_op, .stencilStoreOp = eStore,      // depth's stencil
.pInputAttachments = nullptr,
```

Using `eClear` when the pass clears is correct and eden does not do it.
Everything is stored unconditionally.

### 4. eden — unconditional load and store on everything

Already recorded above. `LOAD_OP_LOAD` and `STORE_OP_STORE` as constants, one
subpass, no input attachments, no resolve attachments.

## The propagation list, in order of expected value

Every item is already written somewhere in the fleet. None needs invention.

| Take | From | Give to | Why |
| --- | --- | --- | --- |
| Transient colour attachments, `DontCare` both ways | Vita3K | everyone | The only correct answer, and it pairs with `LAZILY_ALLOCATED` memory |
| Depth and stencil `DontCare` by default | Cemu | eden, azahar | Depth is very often stored when nothing reads it |
| `eClear` instead of `eLoad` when the pass clears | azahar | eden | A clear is a tile operation; a load is an external read |
| `COLOR_ATTACHMENT_OPTIMAL` instead of `GENERAL` | — | Cemu | `GENERAL` can disable framebuffer compression |

**Nobody merges passes. Nobody uses input attachments. Nobody resolves MSAA
on-chip.** Those three remain genuinely unbuilt, and they are what a shared
render graph would add.

## What this settles for the architecture

1. **The shared layer's attachment policy is already designed**, by Vita3K. Do
   not invent one. Take `is_color_transient` and generalise it.
2. **A shared render graph adds three things nobody has**: pass merging, input
   attachments, on-chip resolve. That is a clean scope.
3. **The four-way spread is itself the argument for the shared layer.** Four
   forks, four different answers to the same question, and the best answer is
   not the newest fork or the most active one.
