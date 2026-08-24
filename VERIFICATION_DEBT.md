# Verification debt

**Everything this project asserts that has not been verified the way it would
need to be.** Taken from melonDS, which keeps such a list and states its purpose
plainly: *"nothing below is device-tested."*

**This is not [`DEVICE_QUEUE.md`](DEVICE_QUEUE.md).** That queue holds
**experiments somebody proposed**. This holds **claims already being relied
on**, which is a larger and quieter set — **a claim can be load-bearing without
anyone having proposed to test it.**

---

## The rule that makes the list useful

**Say WHICH KIND of verification is missing.** melonDS's entries are precise
about this, and the precision is what stops an entry being read as "unknown":

> *"NEON master brightness: **bit-exactness PROVEN**, not argued. Modelled both
> the scalar and the NEON lane arithmetic and compared over the entire valid
> RGB6 space x factors 0..16 — **4,456,448 pairs, 0 mismatches**. Its
> **PERFORMANCE is still unmeasured**."*
>
> *"cortex-x3 scheduling: **compiles and is confirmed in the compile line**, but
> the gain is **UNMEASURED**."*

**Four states, and they are not degrees of the same thing:**

| State | Means |
| --- | --- |
| **argued** | derived from a manual or a document. **Thirteen of thirteen manual-derived predictions in this fleet were refuted** |
| **read** | somebody opened the code. Rules out a wrong claim about what exists; says nothing about behaviour |
| **emitted** | the compiler or the binary was checked. Proves the code exists in the artefact |
| **measured** | run on the device, with a workload that could have produced a different answer |

> **Most of this repo sits at "read". Almost nothing sits at "measured".**

---

## The debt, 2026-08-24

### Verified as EMITTED, unmeasured as speed

| Claim | State | What is missing |
| --- | --- | --- |
| **`+rcpc` gives `ldapr` instead of `ldar`** | **emitted**, `target_check.py` probe 1, both NDK 29 and 30 | **no timing.** `DEVICE_QUEUE.md` 23 |
| **`+lse` gives `ldaddal` instead of an outline call** | **emitted**, probe 3; and measured in six shipped binaries | **no timing.** Queue 25, and its FLAT prediction is a prior, not evidence |
| **No SVE macro is defined by the target line** | **emitted**, probe 4 | nothing missing — this one is complete |
| **`std::call_once` improves from `ldar` to `ldapr` under `+rcpc`** | **emitted** | **no timing, and no count of how often it runs** |
| **The standard row's NDK, ABI, `minSdk`, Gradle values** | **argued** | **no fork has been migrated to them.** C++20 verified for exactly one file |

### Verified as READ, not exercised

| Claim | What is missing |
| --- | --- |
| **The backend contract** (`thor_backend.h`, `Backend.kt`) | **nothing implements it.** Proven against three fake backends, which is design validation and not integration |
| **`TimeScale`, `IntegrityMode`, `GuestActivity`, `ArtifactStore`, `FramePacing`, `SettingResolver`** | 211 unit tests, **zero backends** |
| **The cover-art tier model** | **no dump was opened**; icon sizes are quoted from format specifications |
| **The cheat VM mapping across six formats** | **no cheat was executed** |
| **Static triage from import tables** | **no import table has been read.** The Vita3K stub census is a count of the emulator's side only |
| **eden's fastmem exclusive-access default** | read in the source; **eden does not build here** |

### Taken from another fork's measurement, not reproduced

**These are the most dangerous entries, because they LOOK measured.**

| Number | Whose | On what |
| --- | --- | --- |
| **`isb` 11.42 ns against `yield` 0.36 ns** | rpcsx | this SoC — **the strongest of these** |
| `OSGetTime` 7.8% + 3.6%; `clock_gettime` 23% | Cemu | one Wii U title |
| **−30.2% CPU, 4.4x startup** | Cemu | its own two commits, its own loading phase |
| **generic R8 8.022% / Sysmem 9.775% / PurpleVK 8.008%** | azahar | one 3DS scene at one clock |
| BD frame anatomy; the ~2-3x emulation tax | xenia | one Xbox 360 title, and **two of its own instruments disagreed** |
| `rlwinm` +2.88%, stackpoint prolog +2.04% | xenia | one title, same-session A/B |

**Rule: every one of these must name its fork and its workload wherever it is
quoted.** They are evidence about a fork, and only a prior about this project.

### Argued only

| Claim | Why it is still here |
| --- | --- |
| **The whole CPU-lead list** — FP status stalls, the A710 lane-assembly stall, A715 branch density, the A510 shared VPU | **manual-derived, and this fleet's record on manual-derived predictions is 0 for 13** |
| **PMULL for texture swizzle** | **no fork's swizzle code has been read** |
| **A shared render graph pays** | the frame anatomy that would decide it is one title, and contested |
| **Turnip attachment self-read is broken on the a740** | **measured on an Adreno 650 with Mesa 26.1.2.** Queue 26 |
| **The Thor is one machine** | **ARMSX2 says the line has an 865 variant.** Recorded as a scope decision |

---

## How to use this

- **Before quoting a number in a decision, find it here.** If it is in the last
  two sections, say whose it is.
- **When something moves state, move it here** — and **delete the row when it
  reaches `measured`,** because a debt list that only grows stops being read.
- **A new claim that cannot be placed in one of the four states is not ready to
  be relied on.**

## Limits

- **Assembled from this repo's own logs**, so a claim nobody wrote down is not
  here.
- **The four-state model is this document's**, not a fleet convention. melonDS
  distinguishes proven from unmeasured; the middle two states are ours.
- **No audit was done of whether each row's cited log still says what this table
  says it says.**
