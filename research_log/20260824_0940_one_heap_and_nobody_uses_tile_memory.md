# One heap, 11.4 GB, all device-local — and nobody backs a transient attachment with tile memory

**Goal: read `rpcsx/docs/arm64/uma-bar-heap.md`, then check the fleet.**

**Two findings, both in pipeline 2 — the shared upload path this project calls
its flagship — and both are the DELETE operation rather than a merge.**

## The device has one heap, and every memory type is device-local

`VkPhysicalDeviceMemoryProperties` dumped on the Thor by rpcsx:

| Type | Flags | Heap |
| --- | --- | --- |
| 0 | `DEVICE_LOCAL, HOST_VISIBLE, HOST_COHERENT` | 11,441 MB |
| **1** | **`DEVICE_LOCAL, HOST_VISIBLE, HOST_COHERENT, HOST_CACHED`** | 11,441 MB |
| 2 | `DEVICE_LOCAL, HOST_VISIBLE, HOST_CACHED` | 11,441 MB |
| **3** | **`DEVICE_LOCAL, LAZILY_ALLOCATED`** | 11,441 MB |

**One heap. Types 0 to 2 are the same 11.4 GB of DRAM described three ways.**

**There is no separate VRAM to stage into.**

rpcs3's own device layer classifies host-visible-and-device-local memory as a
scarce "BAR heap" and **parks it for future use** — with a comment that is
**correct for a discrete PC GPU**, where that category is the PCIe aperture and
historically 256 MB. **Here it printed `Detected 11441 MB of BAR memory`.**

> **That is not an aperture. It is the machine.**

### The consequence for the upload path

**Every fork writes to a host-coherent staging heap and copies into device-local
memory.** On this device **both are the same DRAM**, so the copy moves bytes from
one part of physical memory to another **for no architectural reason** — and
texture and vertex uploads are the volume traffic in a frame.

**Type 1 is the one that matters**: device-local, host-visible, coherent **and
cached**. Cached is what makes a direct write plausible; **an uncached or
write-combined mapping would have sunk the idea.**

**Every fork stages**, counted by files touching a staging, stream or upload
buffer:

| Fork | Files |
| --- | --- |
| eden | **32** |
| xenia | 27 |
| Cemu | 22 |
| ARMSX2 | 21 |
| azahar | 13 |
| rpcsx, melonDS | 8 |
| Vita3K | 5 |

**This is the same shape as every other DELETE candidate in this project**: the
staging buffer exists to serve a discrete GPU with separate VRAM. **The Thor does
not have one.**

**The honest limit, stated by rpcsx and not weakened here:** *"the precondition is
proven; the payoff is not."* **Host-visible does not mean fast to write**, and
cached-and-device-local still has to beat cached staging plus a driver-side copy.
**That is an A/B, not a deduction.**

## Type 3 is the tile memory. Searched eight forks; the one hit is a log string

**Searched all eight forks' own source for `LAZILY_ALLOCATED` and
`eLazilyAllocated`, vendored trees excluded, and read the single hit.**

`DEVICE_LOCAL | LAZILY_ALLOCATED` with no host visibility **is Adreno's on-chip
tile memory.** An attachment backed by it — a depth or MSAA target never sampled
outside its pass — **need never touch DRAM at all.**

**The full transient-attachment recipe has three parts, and the fleet has at most
two.**

| Part | Who does it |
| --- | --- |
| `VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT` on the image | **Vita3K only** — `renderer/src/vulkan/creation.cpp` |
| `LOAD_OP_DONT_CARE` / `STORE_OP_DONT_CARE` | **Vita3K only** |
| **`VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT` memory** | **nobody** |

**rpcsx's single `LAZILY_ALLOCATED` match is a log-formatting string** in its
memory-type dump — `(f & VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) ? "LAZY " : ""`
— **not a use.** Searched all eight forks' own source, vendored trees excluded.

> **Without part 3, a transient attachment still gets real DRAM.** The
> `DONT_CARE` ops save the store traffic; **the lazily-allocated memory saves the
> allocation and the residency.** Vita3K does the first half of a two-half
> optimisation and this repo has been praising it as complete.

**This gives `CLAUDE.md`'s propagation row "transient colour attachments, from
Vita3K, to everyone" its hardware justification — and extends it.** The row
should read: **take Vita3K's transient tracking, and add the memory type nobody
binds.**

**It also pairs with the `LOAD_OP_CLEAR` result.** rpcsx measured that conversion
at **no saving** and offered two explanations, one being that Turnip may already
fold it. **Backing an attachment with lazily-allocated memory is a different
lever** — it changes where the attachment lives, not which op is recorded — so
the null result does not carry over.

## Two things to do with this

1. **Add both to `DEVICE_QUEUE.md`.** The direct-write upload path is an A/B with
   a clear precondition and an honest unknown. The transient-memory binding is
   smaller and more likely to pay, because it removes an allocation rather than
   racing a driver optimisation.
2. **Add the memory-type dump to the device baseline.** `CLAUDE.md` records the
   Vulkan API version, the vendor ID and the GPU clocks from xenia's baseline. **It
   does not record the memory types**, and they decide the whole upload
   architecture.

## Limits

- **Nothing here is measured.** rpcsx's document says so about its own half, and
  the fleet census is a file count plus a read of the one interesting hit.
- **The staging-file counts are files, not call sites**, and a fork may stage for
  reasons unrelated to uploads — readback, for example, genuinely needs a host
  path.
- **Whether Turnip actually honours `LAZILY_ALLOCATED` by keeping an attachment
  in GMEM was not verified.** Drivers may allocate anyway. **That is the first
  thing to check, and it is a probe, not an A/B.**
- **Vita3K's transient tracking was read through `CLAUDE.md`'s quotation and a
  file-level search**, not line by line.

## Sources

- rpcsx `docs/arm64/uma-bar-heap.md`,
  `app/src/main/cpp/rpcsx/rpcs3/Emu/RSX/VK/vkutils/device.cpp`
- Vita3K `vita3k/renderer/src/vulkan/creation.cpp`
- Qualcomm *Snapdragon Mobile Platform OpenCL General Programming and
  Optimization*, section 7.4, cited there
