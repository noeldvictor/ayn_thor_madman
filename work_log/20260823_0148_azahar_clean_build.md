# azahar clean build, and the second Phase 0.3 number

**Goal: continue Phase 0.3 with the third Tier 1 fork.**

Session 2026-08-23 01:48.

**Result: BUILD SUCCESSFUL in 14 min 33 s. Its recipe worked unchanged.**

---

## The numbers

| | |
| --- | --- |
| **Clean build** | **14 min 33 s** |
| APK | **27.7 MB** |
| Variant | `vanillaRelWithDebInfoLite` |
| ABIs | **`arm64-v8a` only** |
| JDK | **17** |
| Gradle | **8.14.5** |
| NDK | 29.0.14206865 |
| Exit | 0 |

**The fork's own recipe in `AGENTS.md` worked unchanged**, like melonDS's and
unlike Vita3K's. It also states the ABI rule itself: *"The Android APK target
for this repo is the AYN Thor, so keep `abiFilter` set to `arm64-v8a` only. Do
not build x86_64 unless the user explicitly asks for it."*

**azahar has uncommitted local work** in three files. Building is read-only, so
nothing was disturbed and nothing was committed to the fork.

## Two forks, and the ABI difference is visible in the artifact

| | melonDS | azahar |
| --- | --- | --- |
| Clean build | 15 min 27 s | **14 min 33 s** |
| **APK** | **55.5 MB** | **27.7 MB** |
| **ABIs** | **three** | **one** |
| Variant | debug | relWithDebInfoLite |
| Gradle | 9.5.0 | 8.14.5 |
| JDK | 21 | **17** |

**Similar build time, half the APK.** The variants differ so the times are not
strictly comparable — **which is the point already recorded: a build time means
nothing without its ABI list and its variant.**

**The APK size is the cleaner signal**, and it lines up with rpcsx's
measurement that a second ABI is more than half the payload.

## The toolchain spread is widening

| Fork | JDK | Gradle | NDK |
| --- | --- | --- | --- |
| **standard row** | — | **9.6.1+** | **29.0.14206865** |
| melonDS | 21 | 9.5.0 | 28.0.13004108 |
| **azahar** | **17** | **8.14.5** | 29.0.14206865 |
| Vita3K | 21 | 9.4.1 | 27.3.13750724 |

**Three forks, three Gradle versions, three NDK versions, and now two JDK
versions.** azahar is the furthest from the row on Gradle and the only one that
needs JDK 17 — and its `AGENTS.md` says to keep it on 8.14.5 *"unless a later
upstream merge changes them"*, so moving it is a decision against the fork's own
standing instruction.

**That is what the toolchain migration actually consists of**, and it is the
first concrete evidence of its cost.

## Phase 0.3 so far

| Fork | Result |
| --- | --- |
| melonDS-android | **15 min 27 s**, 3 ABIs, 55.5 MB |
| **azahar** | **14 min 33 s**, 1 ABI, 27.7 MB |
| Vita3K | **fails** — recipe wrong, then `x86_64`. arm64-only builds |
| ARMSX2, Cemu, eden, xenia, GameThor | not attempted |

**Two of three attempted forks built. Both that built had a working recipe;
the one that failed had a wrong one.**
