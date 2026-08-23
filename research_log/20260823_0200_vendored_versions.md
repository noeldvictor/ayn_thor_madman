# Vendored library versions: mostly divergent, but two forks already agree

**Goal: give Open Decision 2, dependency unification, its first data.**

Session 2026-08-23 02:00. `CLAUDE.md` records that the packed binary cannot link
five copies of one library and that **four different versions is worse than four
copies**, then leaves the versions unrecorded.

**Result: most shared libraries are pinned to different commits, but azahar and
Vita3K already agree on two of them — which gives the unification an order.**

---

## Pinned commits, read from each fork's git tree

| Library | azahar | Cemu | Vita3K | xenia |
| --- | --- | --- | --- | --- |
| `fmt` | `e424e3f2` | — | `1be298e1` | `27e3c0fe` |
| `Vulkan-Headers` | `409c16be` | `9b9fd871` | — | `31aa7f63` |
| **`glslang`** | **`fc9889c8`** | — | **`fc9889c8`** | `f4f1d8a3` |
| `imgui` | — | `f65bcf48` | `cb16568f` | `81160fee` |
| **`xxHash`** | **`e626a72b`** | — | **`e626a72b`** | `4c881f79` |

**azahar and Vita3K pin the identical commit for `glslang` and for `xxHash`.**

**That is unlikely to be coincidence.** Both descend from lineages that took
their externals from the same place, and it is the same shared-ancestry effect
[`ANCESTRY.md`](../shared_layer/ANCESTRY.md) documents for source files, showing
up in dependency pins.

## What it gives the decision

**Unification cost is not uniform, and the cheap ones are identifiable.**

| Library | Forks to move | Cost |
| --- | --- | --- |
| **`glslang`** | **xenia only** — azahar and Vita3K already agree | **cheapest** |
| **`xxHash`** | **xenia only** | **cheapest** |
| `fmt` | two of three | medium |
| `Vulkan-Headers` | two of three | medium |
| `imgui` | two of three | medium |

**Start with the two where two thirds of the fleet has already converged.**

**And `glslang` is the one that matters most**, because three forks need it **at
run time** — azahar, Cemu and Vita3K emit GLSL text and compile it. See the
shader translation entry in [`PATTERNS.md`](../shared_layer/PATTERNS.md).
**Two forks agreeing on the version of the one runtime-critical shared library
is the most useful fact in this table.**

## Two honest limits

**A different SHA does not mean far apart.** Two adjacent commits differ as much
as two years do, by this measurement. **The distances here are unknown**, and
the table proves only agreement where the SHAs match.

**Only four forks are covered.** ARMSX2 and eden report zero submodules — they
vendor by copying, so there is no pinned commit to read and the version has to
come from a version header instead. **Not done.** melonDS has three submodules
and only `xxhash` matched.

## Method

```sh
git ls-tree -r HEAD | grep -iE "\t.*/<lib>$"
```

A submodule appears in the tree as a `commit` entry, so the pinned SHA is
readable **without cloning the submodule or having it checked out.** That is
what made this cheap.
