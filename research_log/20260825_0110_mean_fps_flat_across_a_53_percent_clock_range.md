# Mean FPS is flat across a 53% GPU clock range, P95 is not, and a power mode silently changed the fan

**Goal: read azahar's remaining `Do not` invariants outside the optimisation
clusters. Two are different in kind and both are useful here.**

**Measured by that fork on the physical Thor. No device used here.**

## 1. The number that argues for P95 over mean

**Super Mario 3D Land attract loop, 2x internal resolution:**

| Power mode | GPU clock | Mean FPS | **P95 frame time** |
| --- | --- | --- | --- |
| **High Performance** | **615 MHz** | 59.256 | **20.673 ms** |
| Performance | 550 MHz | 58.397 | **27.626 ms** |
| **Standard** | **401 MHz** | 57.935 | **27.460 ms** |

**All three had zero intervals over 50 ms and clean audio.**

> **The GPU clock ranges over 53% and mean FPS moves 2.3%. P95 frame time moves
> 34%.**

**This project already names 1% lows and frame time alongside fps.** This is the
number that shows why: **a mean-FPS gate would have passed all three modes as
equivalent and picked the coolest**, and azahar's own conclusion is that
**"neither lower mode was a free speed-preserving swap."**

**And the two lower modes are indistinguishable from each other** — 27.626 ms
against 27.460 ms at a 37% clock difference — **while the top mode is separated
from both.** That is not a smooth curve; it looks like a threshold.

## 2. A search order for the power target, which this project lacks

> **"Keep Standard first for the <=6-W search, then test Performance and High
> only when the fixed target misses its speed gate."**

**Start at the cheapest mode and escalate only on a missed gate**, rather than
measuring all modes every time. **`DEVICE_QUEUE.md` has 27 entries and no
convention for which power mode an arm runs in**, which means arms taken on
different days may not be comparable.

**And the stated budget is `<=6 W`**, which matches the ~5 W target this repo
already records for xenia.

## 3. A COUPLED SETTING — a thirteenth `DID_IT_APPLY` mechanism

> **"Changing High Performance to Standard on this firmware also reset fan mode 4
> to 1; always read and restore both settings explicitly."**

**The twelve mechanisms already catalogued are all forms of a setting NOT taking
effect. This is the opposite and it is worse:**

> **The setting applied, and so did a different one nobody asked for.**

**The measurement consequence is direct.** A power-mode A/B with fan mode
silently changing underneath **is a two-variable experiment reported as one**,
and the fan is the variable that most affects sustained thermals. **Every
thermal comparison across power modes on this firmware is suspect unless the fan
mode was read back.**

**There is no tool for this.** The coupling is a property of the firmware, not of
the code, so no sweep finds it. **The only detector is reading back every setting
you did not touch, before and after.**

**Recorded as mechanism 13 in
[`shared_layer/DID_IT_APPLY.md`](../shared_layer/DID_IT_APPLY.md).**

## 4. A Phase 1 blocker: Gradle's configuration cache

> **"Do not pass Gradle `--configuration-cache` for Android packaging.
> `app/build.gradle.kts` runs command-line Git during configuration, and Gradle
> 8.13 rejects that while storing the cache EVEN AFTER native and APK tasks
> succeed."**

**The failure arrives after the build works**, which is the expensive shape: the
APK is produced and the invocation still fails.

**This matters for Phase 1**, which moves every fork to Gradle 9.6.1 or newer.
**Configuration cache is the headline feature of modern Gradle and the obvious
thing to enable when chasing build times** — and at least one fork's build script
cannot use it as written. **The cause is generic**: running Git during
configuration to stamp a version. **Any fork doing the same has the same
problem.**

**The ordinary Gradle build cache and the native CMake/Ninja cache are
unaffected**, so the recommendation is `--no-configuration-cache`, not disabling
caching.

### Swept: four of eight forks have the same blocker, for the same reason

**Method: `git grep` for `exec {`, `providers.exec`, `ProcessBuilder` and a
literal `git` in every fork's `*.gradle` and `*.gradle.kts`, vendored trees
excluded, then every hit read.**

| Fork | Configuration-time process |
| --- | --- |
| **Cemu, melonDS, azahar, eden** | **raw `ProcessBuilder`, running `git`** |
| ARMSX2, xenia, Vita3K, GameThor | none found |

**Not one of the four uses `providers.exec` or a `ValueSource`**, which are the
configuration-cache-compatible APIs. azahar's is the plainest: **`versionName =
ProcessBuilder("git", "describe", "--always", "--long")` inside
`defaultConfig`** — configuration time by construction. eden and melonDS wrap
theirs in a helper and Cemu inlines it, but all four are the same shape.

> **Half the fleet cannot use Gradle's configuration cache as written, for one
> identical reason: stamping a version from `git` during configuration. The fix
> is the same in all four, and the standard row's Gradle 9.6.1 has the API for
> it.**

**This belongs in Phase 1 and in the build-location decision**, where build time
is the thing being weighed. **It is a shared fix with a shared cause** — the
unit-of-work argument again, in the build system.

**Not verified: whether Gradle 9.x still rejects it.** azahar's note is about
8.13. **The pattern is incompatible by design rather than by version**, so it
almost certainly still is, but no 9.x build was run to confirm.

## 5. Three device-etiquette rules worth taking

- **"A `.ninja_deps` access or sharing failure can mean another Gradle/CMake
  build still owns the active configuration: wait for that owner instead of
  deleting the cache or starting a parallel build."** **A lock contention read as
  corruption** — the same class as this repo's rule about the device being busy.
- **"Wall-powered measurements are useful for sustained thermals but are not
  battery-discharge watt measurements."** **Independent agreement with rpcsx's
  floor rule**, from a different fork.
- **Strip a large native test executable before pushing it to
  `/data/local/tmp`, and remove both copies immediately after.**

## And one place two forks disagree

**azahar benchmarks the Super Mario 3D Land ATTRACT LOOP.** This repo carries
xenia's standing rule that **"attract mode is not gameplay"** and that a
benchmark scene must be the workload.

**Both are defensible and the distinction is what the number is FOR.** azahar
uses the attract loop for a **relative** comparison between power modes, where a
fixed, repeatable, low-variance scene is exactly what is wanted. xenia's caution
is about **absolute** claims — "this title runs at N fps" — where an attract loop
understates the real workload.

> **A deterministic attract loop is a good instrument for A/B and a bad one for
> "is this playable".** Say which question is being asked.

## Limits

- **Every figure is azahar's, one title, one scene, one firmware.** Nothing
  reproduced, no device used here.
- **The FPS/P95 table is at 2x internal resolution**, which is a specific load;
  the threshold shape may not hold at 1x or 3x.
- **"This firmware"** is azahar's own qualifier on the fan-mode coupling. **It may
  already be fixed, or may differ per unit.** It was not re-checked.
- **The Gradle claim is about Gradle 8.13 and azahar's build script.** Whether
  9.x still rejects it, and which other forks run Git during configuration, was
  not checked.

## Sources

- azahar `AGENTS.md:89-95, 1435-1452`
- `shared_layer/DID_IT_APPLY.md`
