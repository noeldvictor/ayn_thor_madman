---
name: capability-check
description: Use BEFORE writing any feature, subsystem or tool for the AYN Thor fleet. Answers "which fork already has this?" against capability_inventory.md and the nine forks on disk. Prevents the root failure of this project, which is agents rebuilding what another fork already has. Triggers on any request to add, build, implement or design a feature.
---

# Capability check

**Run this before you write a feature. Every time.**

## Why this exists

Agentic coding accelerates duplication. A feature that used to be built once,
because it was expensive, now gets built in three forks in a week.

That already happened here, repeatedly:

- Three forks built per-class texture filtering separately. ARMSX2, melonDS
  and azahar.
- Six forks each wrote a GPU driver picker for one GPU.
- Two forks implemented ADPF separately.
- Two forks wrote a game patch format.

Every one of those was written by somebody who did not know the others
existed. The cost is not the wasted work. The cost is nine versions to
maintain and no shared improvement.

## The procedure

### 1. Read the inventory first

Read `capability_inventory.md` in this repo. Search it for the feature by
name, and by what it does rather than what it is called. A capability is often
named differently in each fork:

| Same idea | ARMSX2 | melonDS-android |
| --- | --- | --- |
| Per-class texture routing | `TextureClass` | `HdFilterTarget` |

If the inventory names it, stop. Read the implementation it points at.

### 2. Search the forks directly

The inventory is incomplete and says so. Search the forks before concluding
nothing exists.

Use `git ls-files` inside each fork. A recursive grep over `Documents/` times
out, because ARMSX2 carries a build tree with a vendored librashader checkout.

```sh
cd <fork>
git ls-files | grep -iE '<term1>|<term2>'
git grep -lI "<phrase>" -- '<path>/*'
```

Search for the **behaviour**, not your chosen name. For a texture filter, try
`filter`, `upscal`, `texture`, `shader`, `scale`, and the algorithm names.

#### Search for the MECHANISM, not the category

**This is the rule that has failed most often.** A filename search finds what
somebody chose to name after the feature. Nobody names a function after the
category it belongs to.

Measured on 2026-08-22, three times in one session:

| Searched | Missed | Found by |
| --- | --- | --- |
| `*pipeline_cache*`, `*shader_cache*` | **melonDS and rpcsx** | `vkCreatePipelineCache`, `VkPipelineCache` — **all eight forks had it** |
| `storage` | **five forks** | `StatFs`, `usableSpace`, `walkTopDown`, `formatFileSize` |
| `hotkey` | ARMSX2's action list | `fast.?forward`, `rewind`, `quick.?save`, `slot` |

**So: name the API call, the syscall, or the concrete verb the code must use.**
A feature can be renamed. A `vkCreateDevice` cannot.

#### Two searches before any negative

A negative is worth recording **only after a second search with different
words**. Ten negatives in this repo have been checked and **nine were wrong**.

State both searches in the result. "I searched X and Y, in these paths, and
found nothing" is a finding. "No fork has this" is not.

#### Never characterise a fork from its documentation

**ARMSX2 was filed as a research fork for months** because its `docs/` hold
research notes on upscaling, neural models and ARM64. Nobody listed its Kotlin.
It has **63,111 lines of Android frontend** and already held the per-game
override system, the hotkey enum and a second-screen `Presentation`.

**melonDS-android had never been opened at all.** It has **78,033 lines**, the
only layered architecture in the fleet, the only settings migration framework,
and the only cheat manager UI.

**A fork's `docs/` describes what somebody wrote down, not what the fork does.**
And a code comment can be stale: xenia's pipeline cache carries a comment saying
disk persistence is "a follow-up step", two lines above the code that does it.

#### Count every candidate before writing a superlative

"The most complete X in the fleet" was written twice on 2026-08-22 and **both
times it was wrong**, the second time while correcting the first. A census takes
one command. Run it.

Forks to search, in the order most likely to already have it:

**Order corrected 2026-08-22 from a measured census, not from reputation.**

1. `melonds_HD/melonDS-android` — **the largest frontend in the fleet, 78,033
   lines.** The only layered architecture, the only settings migration
   framework, the only cheat manager UI. **Had never been opened before
   2026-08-22.**
2. `armsx2-thor/ARMSX2` — the seed of the shared layer, and **63,111 lines of
   frontend**. Holds the per-game override system with three fixed bugs, the
   hotkey enum, and a second-screen `Presentation` naming the Thor.
3. `xenia-thor-workspace/xenia-thor` — **the best tooling, not the biggest
   shell.** 137 scripts, 29 agent skills, an MCP server, an experiment ledger,
   and the only fork that separated its Vulkan device layer from its renderer.
   Its frontend is the **smallest** Tier 1 one at 12,334 lines.
4. `azahar-thor/azahar` — dual-screen layouts, applets, hotkey subsystem.
5. `cemu-thor-experiment` — the most capable pack and patch format.
6. `psvita/Vita3K-Thor` — the on-device regression suite.
7. `melonds_HD/melonds_HD_2` — dropped as a target, **not** as a source. It
   holds `renderer_cases/`.
8. `eden-thor`, `gamethor`, `ps3-thor/rpcsx-ui-android`.

### 3. Check the experiment ledger for anything performance-related

If the feature is a performance lever, it may already have been tried and
recorded dead.

```sh
python tools/exp_ledger.py check "<keyword>"   # in xenia-thor
```

Verdicts: `DEAD` and `FLAT` mean do not re-run it. Read the recorded result
and build on it.

### 4. Report before you build

State the answer in one of these forms:

- **Exists in N forks.** Name them and the paths. Extract, do not write. Go to
  `CLAUDE.md`, How to build the shared layer.
- **Exists in one fork.** Name the path. Read it, then decide whether to
  extract it or to write a better one, and say which and why.
- **Does not exist.** Say what you searched for and where, **and give both
  searches**. A negative result needs its method stated, or the next agent
  repeats the search. **Nine of the ten negatives recorded in this repo have
  been wrong**, so treat your own negative as the least reliable result you can
  produce.

## Counting traps

If you measure a fork rather than only searching it, **every headline number in
this repo was wrong on its first pass.** All four causes, with the fix:

| Trap | Example | Fix |
| --- | --- | --- |
| vendored build tree | Cemu looked like **7.2M lines**; `src/android/` holds a vcpkg tree | exclude `.cxx`, `vcpkg`, `build/` |
| generated code | xenia looked like **1.33M**; `gpu/shaders/bytecode/` has 20k-line SPIR-V headers | exclude `bytecode`, `*ShaderData.h` |
| guest code counted as host | melonDS's "Vulkan layer" looked like **123k**; most is a DS rasterizer and shader arrays | read the biggest filenames |
| a generic name in a library | grepping Cemu for `createInstance` returned **Boost ICU headers** | filter build dirs first |

**After counting, list the largest files and read their names.** One command,
and it reversed the conclusion twice in one session.

## Licence gate

Extracted code keeps the licence of its source fork, and the shared layer
inherits the most restrictive one.

| Source | Shared layer can be |
| --- | --- |
| xenia-thor, BSD | anything |
| Cemu-thor, MPL-2.0 | MPL-2.0 or GPL |
| azahar-thor, Vita3K-Thor, GPL-2.0-or-later | GPL-2.0-or-later or GPL-3.0 |
| ARMSX2, melonDS-android, eden-thor, GameThor, GPL-3.0 | GPL-3.0 only |
| rpcsx-ui-android, GPL-2.0-only | **cannot be linked** |

**When two forks have the same capability at similar quality, take the
permissively licensed one.** The GPU driver manager exists in six forks and
xenia's is BSD, so extracting xenia's keeps the shared module reusable.

**Never copy from `rpcsx-ui-android`.** It is GPL-2.0-only and cannot share
the binary. Read it for ideas; ideas are not copyrightable.

## Record what you find

Add anything new to `capability_inventory.md` immediately. Do not wait for a
survey. Include the fork, the path, and a quality value from the table at the
top of that file.

**A capability you found and did not record will be searched for again.**
