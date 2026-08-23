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
