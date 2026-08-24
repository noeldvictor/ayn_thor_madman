# A correct measurement can support the wrong decision, and two throttles in series look like one

> ## SHARPENED 2026-08-25, AND THE HEADLINE UNDERSTATED IT
>
> **The 71.1 C number was not "real but insufficient". It was measuring the wrong
> quantity.** The same fork's `docs/arm64/thermal.md` — which its `ledger.md`
> does not link, and which I found a day later — says the guard classified
> **per-core JUNCTION sensors** as `silicon`, took their **maximum**, and
> compared it against a **72 C package-shaped limit**.
>
> > **A 72 C limit on a junction maximum is not a thermal bound, it is a load
> > detector.** Junction routinely passes 72 C on this SoC under any sustained
> > work and is unremarkable until roughly 95-105 C.
>
> **So the A/B was faithfully recording which arm ran on the faster cores**, since
> little cores have lower junction temperatures. **The A510 pinning default was
> adopted to satisfy a limit that was measuring the wrong quantity.**
>
> Measured after the fix, at moderate load: `silicon 64.6 C from cpuss-2 (limit
> 72)` against `junction 71.9 C from cpu-1-8 (limit 95)`. **Under the old
> classifier that is 71.9 against 72 — one tenth of a degree from stopping a
> run, at an unremarkable temperature.**
>
> **The section below still stands as a rule.** An A/B that answers "which arm is
> cooler" still does not answer "which default is better". **But this case is a
> stronger and different lesson: the instrument was wrong, not merely narrow.**
>
> **And I found it late for a reason worth recording.** I read `ledger.md`,
> treating it as the fork's routing table. **It is the audit ledger. The routing
> table is `AGENTS.md`**, which indexes `thermal.md` twice, once with the exact
> line that would have saved a day: *"Junction versus package sensors, and the
> guard that compared a limit against the wrong one."*
>
> > **Start at the fork's `AGENTS.md`. This repo's own federation rule already
> > said so — "those files are the source of truth for their fork" — and a
> > ledger is not an index.**


**Goal: read the rest of rpcsx's `docs/arm64/ledger.md`, its own index of
unfinished work, having just established the rule that a fork's routing table
comes before its topic files.**

**Three lessons, and the first one is an axis this repo's measurement discipline
does not have.**

## 1. The A510 pinning was measured correctly and reversed anyway

The old default pinned startup cache and PPU compile workers to the three A510
cores. Both arms from a 34.7 C preflight:

| cache workers | first runtime sample | outcome |
| --- | --- | --- |
| ordinary scheduler | `71.1 C` | **thermal guard stopped it 0.7 s in** |
| A510 cluster | `53.8 C` | peaked `67.8 C`, survived 9.5 s |

**That reads as settled, and it was wrong as a default.** Watching a real cold
PPU recompile with the pinning in place: **78 modules, roughly ten minutes, at
51-58 C the whole time, with the guard at 72 C.** Fourteen to twenty-one degrees
of headroom went unused for ten minutes **to avoid a hot case the guard already
exists to catch.**

The fork's own summary:

> **A measurement can be correct and still support the wrong decision.** The
> 71.1 C number was real. What it did not capture was **how often that case
> arises, what the alternative costs when it does not, or that a guard already
> handled it.** An A/B that answers "which arm is cooler" does not answer "which
> default is better".

**This repo's measurement discipline is entirely about getting a trustworthy
number** — noise floors, sensor naming, fresh processes, `[min..max]`, energy per
frame. **Every rule protects the measurement. None protects the decision.**

The three questions an A/B does not answer:

- **How often does the bad case arise?** A defence against a rare case is paid
  on every run.
- **What does the defence cost when the case does not arise?** Here: ten minutes
  at half the available thermal headroom.
- **Is something else already handling it?** Here: the thermal guard, which costs
  nothing until it fires.

> **Pre-emptively throttling every run to avoid occasionally reaching a limit
> spends a large certain cost against a small uncertain one.**

**It generalises past thermals**, and this repo has the same shape queued: a
conservative default that avoids a rare failure at a cost paid always. The
pinned-driver fallback, the `DONT_CARE` promotions, the cache-invalidation
conservatism.

## 2. Two throttles in series look like one

Freeing the affinity mask would have looked like it did nothing, because
compilation concurrency was **also** capped by `Max LLVM Compile Threads`:

```
thread_count = llvm_threads ? min(llvm_threads, limit()) : limit()
```

Set to **2**, so compilation ran two-wide regardless of how many cores the mask
allowed.

> **When a throttle is removed and nothing gets faster, look for the second
> throttle before doubting the first.**

**Two independent limiters in series are common precisely because each was added
for a different reason at a different time.** That is the same generator as this
repo's duplication finding: independent answers to different questions that
accumulate into one path.

## 3. The setting was written in three places and two overwrote the third

`config.yml` holds the value. **`ThorPerformanceProfile` calls
`setSetting("Core@@Max LLVM Compile Threads", "2")` on every boot**, and the
game's entry in `GameSettingsDatabase` carries it in the managed profile.

> **Editing the config alone is silently undone on the next launch.**

**This is a new bug class for `tools/bug_class_sweep.py`, and it is the runtime
twin of a rule this repo already has.** `emitted_flags.py` exists because a
*compile* setting that exists is not a setting that applies. **The same is true
of a runtime setting, and the mechanism is different: not a wrong default but a
second writer that runs later.**

**It is also this project's own risk, named.** The per-game override design has
**three tiers** — per-game, promoted, global — and ARMSX2 already shipped two
bugs in exactly this area: a value that read as enabled until the process
restarted, and a per-game FPS cap that came back as 0 and stayed 0.
**`ConfigStore`'s change-tracking is the defence, and a profile applier that
writes on every boot bypasses it.**

**Added as bug class `setting_written_by_multiple_writers`.**

## The fourth item: ARM does not hand you the fault decode

Not a measurement lesson, but the strongest negative in the document and it kills
a design that sounds obviously right.

x86 fault-based MMIO emulation needs a **250-line instruction decoder**,
`decode_x64_reg_op`, to recover "which register, what size, load or store" from
the faulting instruction. **The obvious hope is that AArch64 needs none of it**,
because `ESR_EL1` carries `ISV`, `SAS`, `SSE`, `SRT`, `SF` and `WnR`.

**Measured on device with a deliberate `PROT_NONE` fault:**

```
ldr  w9,  [p]   esr=0x92000007  isv=0  wnr=0
str  w13, [p]   esr=0x92000047  isv=0  wnr=1
stp  x17, x18   esr=0x92000047  isv=0  wnr=1
```

**`ISV = 0` for every case**, including the simplest. `ISV = 1` is essentially
reserved for **stage-2** aborts, the virtualization case; ordinary stage-1
userspace translation faults do not populate the syndrome, so `SAS`, `SRT`, `SF`
and `SSE` are all zero and mean nothing.

**What is reliable is the exception class and `WnR`, and nothing else.**

**Why it matters here:** any backend using fault-based guest MMIO or fastmem on
ARM64 must decode the faulting instruction itself. **The register-fields table in
the ARM manual is true and unreachable from userspace**, which is the
`/proc/cpuinfo`-over-the-manual rule again, one level deeper: **a field can exist
architecturally and be unpopulated in the only mode we run in.**

## Limits

- **Every number here is rpcsx's**, on rpcsx's workload. Nothing reproduced.
- **PS3 is out of the packed binary and rpcsx is GPL-2.0-only.** Lessons transfer;
  code does not.
- **The `ISV` probe is one kernel on one device.** It is consistent with the
  architecture's intent, which is why it is believable, but it is n=1 on the
  platform.
- **The A510 reversal is a default change, not a measured speedup.** No compile
  time is quoted before and after.

## Sources

- rpcsx `docs/arm64/ledger.md`, "Open opportunities, ranked", items 1-4
- rpcsx `AArch64Signal.cpp::_read_ESR_EL1`, `decode_x64_reg_op`
- `shared_layer/MEASUREMENT.md`, `tools/bug_class_sweep.py`
