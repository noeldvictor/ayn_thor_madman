# `dead_guard.py`: two false positives fixed it, one hit survives, and the fleet is clean

**Goal: rpcsx's dead `ARM_FEATURE_LSE2` is the twelfth mechanism in
`DID_IT_APPLY.md` and the only one with a mechanical detector. Build it.**

## What it does

Reports **preprocessor guards on macros that nothing in the fork ever defines** —
a branch that is syntactically valid, compiles cleanly, warns about nothing, and
**can never be true**.

**The discriminator that makes it tractable:** real compiler and platform
predefines almost always begin with an underscore. **rpcsx's did not** —
`ARM_FEATURE_LSE2` looks like a compiler macro and is a project macro. So the
tool reports guards on identifiers that do not start with `_`, are never
`#define`d in the fork, and never appear in a `-D`.

**`--features` restricts to hardware-feature vocabulary**, which is the
high-value subset: **a dead `ENABLE_DISCORD_RPC` is a feature toggle and
harmless; a dead `ARM_FEATURE_LSE2` silently selects a slow path nobody can
see.**

## Two false positives, and each one made the tool real

**1. `ARMSX2_HAS_LIBRASHADER`, 7 guards across the Metal, OpenGL and Vulkan
device layers.** librashader is a flagship feature, so this looked like a find.

**It is defined. 360 of 1967 translation units carry it**, read from
`compile_commands.json`.

> **Fix: take definitions from the build OUTPUT, not the build FILES.** That is
> ground truth, it is the same instrument `emitted_flags.py` uses, and it is the
> rule this repo applies everywhere else — **verify from the emitted artefact.**

**2. `CITRA_HAS_SSE42`, 19 guards in azahar.** Its `math_util.cpp` turned out to
be **exemplary** code: it defines `CITRA_HAS_NEON` beside the SSE macro and
guards the SIMD path on `#if defined(CITRA_HAS_SSE42) || defined(CITRA_HAS_NEON)`.

**And `CMakeLists.txt:213` defines it.** So why did the tool miss it?

> **A bug in my own tool. `_grep` used `git grep -o`, which prints ONLY THE
> MATCHED SUBSTRING** — so scanning build files for `add_compile_definitions`
> returned that literal keyword and **never the macro name inside the
> parentheses.**

**The definition scanner had been blind for every fork.** Found by reading a
hit, which is the rule the tool's own header states.

## After both fixes: one hit across seven forks

| Fork | feature guards | never defined |
| --- | --- | --- |
| xenia | 26 | **0** |
| Cemu | 21 | **1** — `MBLOCK_DEBUG_ASSERT`, a deliberate debug opt-in in the expanded-heap allocator |
| ARMSX2 | 3 | 0 |
| azahar | 2 | 0 |
| melonDS | 1 | 0 |
| Vita3K, eden | 0 | 0 |

**No dead hardware-feature guard in the packed-binary fleet.** rpcsx had one and
fixed it.

## A two-sided self-test, on the real case

Per today's rule — **an instrument that can return zero must be proved able to
return non-zero** — `--self-test` uses rpcsx as a control in both directions:

- **`ARM_FEATURE_LSE2` must appear as a guard** → the guard scanner works.
  **Passes: 4 uses.**
- **and must NOT be reported dead** → the definition scanner works, because
  rpcsx fixed it with `add_compile_definitions`. **Passes.**
- **2,030 guard macros seen across the fleet**, so the scanner is not silently
  empty.

**A tool reporting "no dead guards anywhere" while unable to see a guard at all
would look exactly like a clean fleet.** That is the failure this control
removes, and it is the same failure that made xenia's HLE intercepts read
`count=0` for weeks.

## The score, three attempts in

**Three classes built today, three sweeps, zero new instances — and five
instrument defects fixed:**

| Attempt | Defect it exposed |
| --- | --- |
| `wrong_launch_path` | a pattern that matched only its own case; a vendored filter blind to one fork's layout |
| `path_as_identity` | no way to exclude a path inside a log line |
| **`dead_guard.py`** | **definitions read from build files rather than build output; and `-o` truncating the definition scan** |

> **The tooling is converging on trustworthy. The instances still come from
> reading something nobody had read.** Both halves of that sentence are worth
> keeping.

## Files

- `tools/dead_guard.py` (new), `--features`, `--min-uses`, `--self-test`
