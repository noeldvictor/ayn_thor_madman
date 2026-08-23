# GameThor: a SNAPSHOT dependency that no longer exists

**Goal: finish the attempted set for Phase 0.3 and check SDK levels fleet-wide.**

Session 2026-08-23 02:54.

**Result: GameThor cannot build, and the cause is a reproducibility failure
rather than a code or tooling one. And every fork is below the standard row on
SDK levels.**

---

## GameThor

```
BUILD FAILED in 27s
Task :app:generateDebugFeatureTransitiveDeps FAILED
  Could not resolve all artifacts for configuration ':app:debugRuntimeClasspath'
  Could not find io.github.joshuatam:javasteam:1.8.0.1-18-SNAPSHOT
```

**It depends on a `-SNAPSHOT`, and the snapshot is gone.** Searched Google's
Maven, Maven Central and the Sonatype snapshot repository; it is in none of
them.

**A `-SNAPSHOT` is a mutable, unpinned artifact.** Publishers overwrite and
expire them. **A build that depends on one is not reproducible by construction**
— it worked on the day it was written and there is no version to go back to.

**This is the fourth distinct failure class in Phase 0.3, and all four are
peripheral:**

| Class | Fork | Nature |
| --- | --- | --- |
| the fork's own recipe | Vita3K | wrong directory, AGP conflict |
| an ABI the device cannot run | Vita3K | `x86_64` has no prebuilt FFmpeg |
| host tools nobody declared | eden | `pkg-config`, `glslangValidator` |
| **an unresolvable SNAPSHOT** | **GameThor** | **the artifact no longer exists** |

**Nothing has failed inside an emulator.**

**And this one connects to the dependency thread from a third direction.**
`CLAUDE.md` wants pinned versions because the packed binary cannot link five
copies of a library. The build-time work showed the fleet also *compiles* those
libraries repeatedly. **GameThor shows a third cost: an unpinned dependency can
make a fork unbuildable at any moment, with no warning and no fix inside the
fork.**

**Not resolved.** Finding or republishing that artifact is a decision for
whoever owns the fork.

---

## SDK levels: every fork is below the row

The standard row is `minSdk` 33, `targetSdk` 37, `compileSdk` 37 — 33 exact,
because the device reports API 33 and a lower value buys nothing for a
one-device app.

| Fork | minSdk | targetSdk | compileSdk |
| --- | --- | --- | --- |
| **row** | **33** | **37** | **37** |
| ARMSX2 | — | **37** | **37** |
| eden | **24** | 36 | — |
| GameThor | **26** | **28** | 35 |
| Vita3K | 28 | 35 | 35 |
| azahar | 29 | — | — |
| rpcsx | 29 | 35 | — |
| Cemu | 30 | 35 | 36 |

**Only ARMSX2 meets the row anywhere.** `minSdk` spans **24 to 30**, and eden's
24 means it carries compatibility paths for Android 7.

**This is the same portability tax as a second ABI, paid in API guards instead
of object files.** A one-device app targeting API 33 needs none of it.

**Now checked by `tools/fleet_lint.py`**, so it stops being a thing somebody has
to remember.

## Phase 0.3, complete for the attempted set

| Fork | Result |
| --- | --- |
| **Cemu** | **~2 min 42 s** native |
| **ARMSX2** | **11 min 25 s** |
| **azahar** | **14 min 33 s** |
| **melonDS** | **15 min 27 s** |
| Vita3K | fails — recipe, then `x86_64`. **arm64-only builds** |
| eden | fails — two host tools |
| **GameThor** | **fails — vanished SNAPSHOT** |
| xenia | not attempted |

**Four of seven build.** Every failure is peripheral, and **two of the three are
one line each.**
