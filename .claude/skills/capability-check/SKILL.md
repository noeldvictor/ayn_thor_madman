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

Forks to search, in the order most likely to already have it:

1. `xenia-thor-workspace/xenia-thor` — the most developed fork. 137 tooling
   scripts, 29 agent skills, an MCP server, an experiment ledger.
2. `armsx2-thor/ARMSX2` — the seed of the shared layer.
3. `azahar-thor/azahar`, `melonds_HD/melonDS-android` — the most complete
   renderer feature sets.
4. `cemu-thor-experiment` — the most capable pack and patch format.
5. `psvita/Vita3K-Thor` — the on-device regression suite.
6. `melonds_HD/melonds_HD_2` — dropped as a target, **not** as a source. It
   holds `renderer_cases/`.
7. `eden-thor`, `gamethor`, `ps3-thor/rpcsx-ui-android`.

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
- **Does not exist.** Say what you searched for and where. A negative result
  needs its method stated, or the next agent repeats the search.

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
