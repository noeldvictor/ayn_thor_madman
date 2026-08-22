# Reading the GPU driver managers

Goal: read all implementations before treating them as one capability, as the
LRU cache test required.

Date: 2026-08-22, 19:45 EDT.

## Result: four concerns, not six copies

The inventory recorded "six forks each wrote a GPU driver picker, same feature
six times, no per-emulator variation to preserve, the lowest-risk extraction in
the fleet."

**Wrong again.** They are four different concerns that happen to touch the same
subject.

| Fork | Lines | What it actually does |
| --- | --- | --- |
| xenia-thor `GpuDriverManager.java` | 582 | Install, select, and **wire into launch**. Sets `gpu_vulkan_driver*` cvars the native loader reads. Downloads packages. |
| azahar-thor `GpuDriverHelper.kt` | 467 | Install, plus a **remote catalogue and recommendation engine**: `getRecommendedDriverOptions`, `downloadRecommendedTurnipDriver`, `downloadDriverAssetPackage`. |
| eden-thor `GpuDriverHelper.kt` | 267 | Install, plus **device capability detection**: `isAdrenoGpu`, `supportsCustomDriverLoading`, `getSystemDriverInfo`, `initializeFreedrenoConfigEarly`. |
| rpcsx-ui-android `GpuDriverAdvisor.kt` | 284 | **Suitability assessment.** Judges a package against the device. |
| Vita3K-Thor | — | vendors `libadrenotools`, no picker |
| Cemu-thor | — | vendors `libadrenotools`, no picker |

The shared driver manager is a **composition of four concerns**, each taken
from the fork that does it best. It is not "pick one and delete five".

| Concern | Best source | Why |
| --- | --- | --- |
| Package install and storage | any, they agree | ADPKG zip, `meta.json` plus a `.so` |
| Launch wiring and safe fallback | xenia-thor | a bad package can never brick a launch; the native loader falls back to the system driver |
| Remote catalogue and recommendation | azahar-thor | the only one that fetches and recommends |
| Device capability detection | eden-thor | the only one that asks whether custom loading is even supported |
| Suitability assessment | rpcsx-ui-android | see below |

## Correction: rpcsx already validates the GPU generation

I recorded twice that no fork validates whether a driver matches the GPU, and
wrote it into `app/SCREENS.md` as a job nothing does today. **rpcsx has done it
for a while.**

`GpuDriverAdvisor.kt` header:

> The driver list previously showed the same paragraph of advice next to every
> entry, so a package built for a different Adreno generation looked exactly
> like one built for this device. Installing the wrong one typically fails at
> boot or corrupts rendering, and the only clue was in the package name.

What it does:

- `Verdict { INCOMPATIBLE, RISKY, COMPATIBLE, ... }`.
- `deviceTarget()` returns `a7xx`, `Adreno 740` on the Thor.
- `claimedFamilies()` recovers the target family from the package name and
  description. It handles `a7xx` style strings, `Adreno 740` model numbers, and
  Qualcomm **"Gen N" marketing**, mapping Gen 1 and 2 to a7xx and Gen 3 and
  later to a8xx.
- `mesaAtLeast()`, `isTurnip()`, `parseLiveDriver()`.

And it is honest about its limits:

> AdrenoTools metadata carries no field naming the target GPU, so the family
> has to be recovered from the package name and description. That is a
> heuristic, and it is reported as such: an unrecognised package is never
> presented as verified, only as unknown.

**That is the right design and it should be taken as-is.** The a8xx packages
sitting on the device are exactly the case it was written for.

## New find: `ThorPerformanceProfile`

`rpcsx-ui-android/app/src/main/java/net/rpcsx/performance/ThorPerformanceProfile.kt`,
165 lines. Unrecorded until now.

- `isThorTarget()` detects the device.
- `applyStartupDefaults()` applies Thor-specific settings at startup.
- `PROFILE_VERSION = 14`, persisted, so the profile re-applies when it changes.
- `ApplyResult` reports `changedSettings` and `failedSettings` rather than
  failing silently.

**This is shared-layer item 3, the Thor hardware profile, already built.** It
was listed as a thing to design.

### A second affinity lesson, learned separately

```kotlin
// Keep the full SoC available to Android and the OS scheduler. Restricting
// the entire process to CPUs 3-7 also pins Java, audio, compiler, and
// service threads onto the same five cores as PPU/SPU/RSX work.
private const val PERFORMANCE_CORE_MASK = 0xFF
```

xenia found guest threads hard-pinned to the A510 cores while the X3 sat idle.
rpcsx found that restricting the whole process to the big cores is also wrong,
because it drags the Java, audio and compiler threads onto the same cores as
emulation work.

**Two forks, two opposite-looking affinity findings, neither aware of the
other.** Both belong in the shared scheduler design.

## The meta-lesson

**Every claim of the form "no fork has this" that I have made has been wrong.**

| Claim | Reality |
| --- | --- |
| No fork has differential testing | melonds_HD_2 `renderer_cases/` does |
| The on-device MCP is design only | xenia has an implemented server |
| No fork validates driver GPU family | rpcsx `GpuDriverAdvisor` does |
| Three forks duplicate an LRU cache | three different designs |
| Six forks duplicate a driver picker | four different concerns |
| The Thor hardware profile is to be designed | rpcsx `ThorPerformanceProfile` exists |

The cause is consistent: **the inventory was built from file listings, and a
file listing cannot tell you what a file does.**

Rules that follow:

1. **Never write "no fork has this" without a search that names its method.**
2. **A capability row from a listing is a hypothesis.** Mark it unread.
3. **Reading is cheap.** Each of these corrections took under fifteen minutes
   and each reversed a plan.

## Next

1. Mark every inventory row as read or unread.
2. Read the remaining texture-cache implementations before the shader cache
   and texture upload extractions are planned in detail.
3. Take `GpuDriverAdvisor` and `ThorPerformanceProfile` as the starting point
   for the shared driver and hardware profile, rather than designing either.
