# API translation needs an API boundary, and only some consoles have one

**Goal: research the GPU side. `CLAUDE.md` names "translate the guest API rather
than emulate the guest GPU" as the large architectural lever, citing RE2 Remake
running on this Thor through GameNative and DXVK. Is that available to the
fleet?**

No device. Reading only.

## Result

**It is available to two backends, declined by one, and structurally
unavailable to four.** The deciding property is **whether the console shipped
its graphics API as a separate module that the emulator can intercept with
logical resource identity intact** — and that is a property of the console, not
of the emulator.

**And xenia, the fork that named this direction, is one of the four that
cannot.**

## xenia already wrote the verdict, and this repo had not read it

`docs/research/20260711-rexglue-gpu-dxvk-for-360-verdict-56sol.md`, 200 lines.
**Its answer to "is Xbox 360 D3D9 easily portable to Vulkan" is no**, for three
precise reasons.

**1. There is no stable host-visible API boundary.** DXVK works because it
**replaces `d3d9.dll`** — a stable, high-level interface. On the 360 the XDK's
D3D9-like runtime is **linked into the XEX and becomes ordinary PPC game code**.
By the time xenia sees anything, `SetRenderTarget`, `Resolve` and
`DrawIndexedPrimitive` **have already been lowered into PM4 registers and command
buffers.**

**2. PM4 has discarded logical resource identity.** There are no
`IDirect3DSurface9`-style handles — only EDRAM address, pitch, format, MSAA
state and resolve destinations. **Render-target identity must be inferred, and
height is not even directly specified.** The same EDRAM range may be reused for
unrelated surfaces or reinterpreted under another format.

**3. Xbox D3D9 semantics are not PC D3D9 semantics.** Predicated tiling,
explicit EDRAM resolves, exponent bias, destination swap, 7e3 colour, 20e4
depth, Xenos microcode, endianness, arbitrary shader vertex fetch.

> **BD's D3D9 usage is translatable, but only after reconstructing the
> high-level resource/state API that PM4 erased. The "easy" part starts after
> that reconstruction.**

**And "AOT-recompile the GPU draws" is called a category error**, because PM4
buffers are runtime data: draw count, indices, constants, visibility, texture
addresses and ordering all depend on live game state.

## The correction this forces on CLAUDE.md

**`CLAUDE.md` cites RE2 Remake on GameNative/DXVK as the existence proof for
this direction. xenia's own document says it proves less than that:**

> RE2 proves the Thor can run a much larger modern renderer. **It does not prove
> whether BD's current 100 ms is GPU execution, GPU starvation, or guest CPU
> work.**

**And the document is explicit about the precondition:**

> If unrelated guest CPU work already exceeds 33.3 ms, **A is a detour and a
> perfect renderer cannot deliver 30 fps.**

**So the existence proof shows the device is capable. It does not identify the
bottleneck**, and this repo has been using it as if it did.

## The three-way split, verified by reading each fork

| Backend | Guest graphics reaches the emulator as | Boundary |
| --- | --- | --- |
| **GameThor** | `d3d9.dll`, **replaced** by DXVK | **API, by replacement** |
| **Vita3K** | **`SceGxm`, HLE'd with identity intact** | **API** |
| **Cemu** | GX2 intercepted, **then lowered to PM4** | **available, declined** |
| eden | NVN through `nvdrv`/`nvnflinger` | partial, not read |
| **xenia** | **PM4 only** — the XDK is inside the XEX | **none** |
| **ARMSX2** | GS through GIF packets | **none** |
| **melonDS** | direct register writes | **none** |
| **azahar** | GSP command lists | **none** |

### Vita3K is the clean case

`sceGxmDraw` validates against a `SceGxmContext` carrying `vertex_program`,
`fragment_program` and scene state, and returns `SCE_GXM_ERROR_NULL_PROGRAM` or
`SCE_GXM_ERROR_NOT_WITHIN_SCENE`. **Logical resource identity is present at the
call.** `SceGxm.cpp` is 5,710 lines of it.

**That is DXVK's situation**, and it is why Vita3K is an API translator whether
or not it calls itself one.

### Cemu has the boundary and gives it up

**This is the surprise, and it corrected my own hypothesis mid-read.** Cemu
intercepts GX2 — 35 files under `src/Cafe/OS/libs/gx2/` — so the boundary
exists. **But `GX2DrawIndexedEx` writes PM4 packets:**

```cpp
void GX2DrawIndexedEx(GX2PrimitiveMode2 primitiveMode, uint32 count, ...) {
    GX2ReserveCmdSpace(3 + 3 + 2 + 2 + 6);
    gx2WriteGather_submit(
        pm4HeaderType3(IT_SET_CTL_CONST, 2), 0, baseVertex, ...
```

**The API-level information is available at the call and thrown away one line
later**, and the Latte command processor reads the PM4 back.

**This is not obviously a mistake.** GX2 is a thin wrapper over Latte, a Wii U
game can bypass GX2 and write command buffers itself, and matching the console
is what an emulator is for. **But it does mean Cemu is the one fork where "keep
the API-level information" is a decision rather than an impossibility.**

## What this changes

1. **Stop citing RE2/DXVK as the direction for the fleet.** It is the direction
   for backends that have an API boundary. **xenia, which named it, does not
   have one.**
2. **Record the boundary as a property of each backend.** It predicts which
   optimisations are even expressible, and it belongs beside the guest ISA in
   `PATTERNS.md`.
3. **Vita3K is the fork to study for this**, not GameThor — GameThor gets its
   boundary by replacing a DLL, which no console emulator can do.
4. **Cemu is the one place where the question is open.** Whether keeping GX2's
   identity instead of lowering to PM4 would pay is unmeasured, and it is a
   large change.

## What this does not say

- **No claim that API translation is faster.** xenia's document says the
  opposite is possible: if guest CPU already exceeds the frame budget, a perfect
  renderer changes nothing.
- **eden was not read.** Its NVN path through `nvdrv` may or may not preserve
  identity.
- **The 3DS and DS cases were classified from file layout**, not from reading
  their command paths.
- **Nothing here is measured.** This is a structural survey, and the whole point
  of the xenia verdict is that structure does not tell you where the time goes.
