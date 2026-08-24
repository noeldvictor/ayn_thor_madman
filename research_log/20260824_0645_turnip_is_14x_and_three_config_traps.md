# Turnip measured 14x the stock driver, and three ways a shipped optimisation is silently off

**Goal: mine the 57 `WIN` entries in xenia's ledger — the record of what actually
worked on this device.**

**Four results. One validates a decision this repo made on reasoning alone. Three
are traps that make a measurement or a feature silently wrong, and all three
apply directly to the artifact store.**

## 1. The pinned driver is worth 14x, measured

**`CLAUDE.md` justifies pinning Turnip on determinism, extension coverage and one
bug surface. It has never had a performance number. Here is one.**

Same title, same HLE configuration, same session:

| Driver | Result |
| --- | --- |
| **Qualcomm proprietary, the system driver** | **~0.8 fps**, stable, renders correctly |
| **Turnip** | **11 fps** |

> **CONFIRMS Turnip is mandatory: the Qualcomm proprietary driver handles the
> native pass/GMEM ~14x worse.**

**Read the scope before quoting it.** This is **one title on one backend through
a native-HLE render path that leans hard on GMEM behaviour**. It is not a general
claim that Turnip is 14x faster at everything. **What it does establish is that
the gap can be more than an order of magnitude on a GMEM-heavy path**, which is
exactly the path this project's render work targets.

### And a general rule falls out of the same run

**The title runs stably on the slow driver and crashes intermittently on the fast
one.** xenia's conclusion:

> the JIT 'crash' is **NOT a universal LLVM codegen bug** (would crash on both) —
> it's a **GPU-TIMING RACE that Turnip's FASTER GPU completion exposes**; the slow
> Qualcomm driver (0.8fps) never hits the race window.

> **A faster driver exposes races a slower one hides. Speed changes correctness
> exposure.**

**That matters for everything this project intends.** A shared device layer, a
warm pipeline cache and a persisted code cache all make things faster. **Latent
races that a slow path never reached will start firing**, and they will look like
new bugs introduced by the shared layer.

**An operational hazard from the same entry:** Turnip's **driver load degraded
after roughly 30 load cycles in one day** — a "No Vulkan physical devices"
restart loop that a reboot and a cache clear did not fix. **Device sessions
should count driver loads.**

## 2. The stale-config trap: a persisted value beats a compiled default forever

**Three `rlwinm` fastpaths had `defaultEnabled=true` in code and a compiled
default of true, and were false on the device**, because the persisted
`xenia.config.toml` had been written back when they were genuinely default-off
pending validation.

The census, on the device's own config:

| Path | Count |
| --- | --- |
| shift / mask / general fastpath | **0 / 0 / 0** |
| generic slow path | **20,480** |

**100% of `rlwinm` translations were taking the expensive generic path.** Forced
on: 6,962 / 9,811 / 2,072 fastpath against 611 generic — **96.9% had been on the
slow path unnecessarily.**

**Cost, measured in a same-session A/B at equal 40C starts: +2.88%**, with
**11 of 11 intervals favouring on**, +2.4% to +3.8%.

> **IMPLICATION FOR EVERY MEASUREMENT: every device number taken this session was
> on a handicapped baseline. Check the persisted config before trusting any A/B.**

**This repo's device rules already say each arm needs a fresh process because
properties are cached per process. This is the stronger version: the persisted
config outlives the process, the install and the app update.**

## 3. The wrong-launch-path trap, and it explains the ANR

**The AOT object cache existed, the device held 111 MB of cached objects, and it
was off for every launch a person actually performs.**

The defaults block that enables `cpu_llvm_object_cache` was guarded by
`intent.getBundleExtra(EXTRA_CVARS) == null` — it applied **only when nothing
supplied cvars**. **But the launcher always attaches a bundle when a game starts
from the app.** So:

- **Headless `am start` runs got the cache** — which is why the cache directory
  was full, 13,885 objects and 59 MB.
- **Every GUI launch recompiled about 10,000 functions from scratch.**

**That is the ~60-second black screen**, and it is the same event as the ANR
recorded separately: **Android fired "isn't responding" at 18 seconds while the
app recompiled a cache it already had.**

> **LESSON: a lever can be defaulted correctly, allowlisted correctly, and still
> never apply, because the code path that sets it is gated on a condition the real
> launch path does not meet. Check WHICH launch path sets a default, not just
> that a default exists.**

**This is the exact failure mode a shipped artifact store would have**, and it
would be invisible: the cache directory fills from testing, every measurement
looks warm, and users get a cold start.

## 4. What to add to this project's rules

**Three checks before any device number is believed:**

1. **Read the persisted config**, not the compiled defaults.
2. **Confirm the lever applied on the launch path used for the run** — and that
   the run's launch path is the one people use.
3. **Count driver loads in a session**, because Turnip's loader degraded after
   about thirty.

**And one design rule for the artifact store:** **verify a cache hit, do not
infer one from the cache directory being non-empty.** xenia's verification was
explicit — *"ZERO cold LLVM begin compiles, a full warm hit"* — and that is the
only evidence that counts.

## Limits

- **The 14x is one title, one backend, one render path.** Do not restate it as a
  general driver speedup.
- **The 2.88% is xenia's number on Burnout**, same-session A/B, uncapped, on
  verified Turnip.
- **None of this was reproduced by me.** All four are read from xenia's ledger.
- **The driver-load degradation is one observation on one day**, not a
  characterised failure.

## Sources

- xenia `tools/exp_ledger.py`, entries `DECISIVE: Qualcomm-driver=0.8fps vs
  Turnip=11fps`, `rlwinm fastpaths disabled by STALE device config (-2.88%)`,
  `object cache never enabled on GUI launches (EXTRA_CVARS guard)`
- `research_log/20260824_0520_the_x86_detour_with_receipts.md` for the ANR entry
