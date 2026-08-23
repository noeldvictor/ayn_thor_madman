# Mining rpcsx's and Vita3K's skills: the measurement discipline this repo lacks

**Goal: read the skills in rpcsx and Vita3K, which the fleet census found and
nobody had opened.**

No device. Reading only. **rpcsx is GPL-2.0-only, so what follows is the
technique and the measured facts, not its text.**

## Census correction

**The counts were files, not skills.** rpcsx has **18 distinct skills**, Vita3K
has **9**.

**rpcsx:** `thor-measurement-validity`, `thor-experiment-ledger`,
`ps3-speed-proof-gate`, `ps3-rsx-experiment-gate`, `thor-windows-android-ab`,
`thor-scene-route`, `thor-screenshot-burst`, `thor-game-workup`,
`thor-ghidra-static-lane`, `thor-spu-codegen-hotpath`, `ps3-research-scout`,
`ps3-spu-contract-compiler`, `ps3-continual-harness-refiner`,
`ps3-debug-knowledge`, `thor-adb-operator`, `thor-game-controller`,
`thor-rsx-vulkan-audit`, `codex-goal-loop`.

**Vita3K:** `vita3k-debug-rag`, `vita3k-perf-profiler`,
`vita3k-regression-ledger`, `vita3k-render-experiment-gate`,
`vita3k-render-debug`, `vita3k-ghidra-escalation`, `vita3k-input-automation`,
`vita3k-thor-android-loop`, `vita3k-windows-render-loop`.

## 1. The missing numbers: measured noise floors

**`CLAUDE.md` says cross-run comparison is untrustworthy. It never says by how
much. rpcsx measured it.**

| workload | spread of **one** configuration |
| --- | --- |
| gated title screen | **+/-0.2%** |
| restored savestate | **+/-5%** |
| pressing through cutscenes | **about +/-50%, unusable** |

> **A claim smaller than the floor of its workload is not a result.**

**This is the quantitative backbone this repo's measurement rules have been
missing.** A 3% win on a savestate run is noise. **Every prediction in
`DEVICE_QUEUE.md` should be checked against these.**

## 2. Measurement traps worth taking verbatim as rules

**Reporting:**

- **Never quote n=1.** One arm read **10351 mW against 7545** and was reported
  as a win.
- **Report `[min..max]`, not the mean. Overlapping ranges mean "not
  distinguishable".**
- **Never pool arms that differ in more than one setting.**
- **Read the lever back off the device and print it with every arm.**
- **Print evidence the run was in the state you meant to measure.**
- **Grep for a fatal error before you read a profile.**
- **A measured win is not automatically a correct default.**

**Harness construction, and these are obscure:**

- **Each arm needs a FRESH PROCESS.** Device properties are read once into a
  static and cached for the process lifetime. **Same class as the finding that a
  per-game driver override needs a process restart.**
- **Run the harness from a frozen copy.** Editing a script while bash executes it
  killed a run with `unexpected EOF` **at a line with no syntax error**.
- **Restore every property and config on every exit path, including interrupt.**
- **Measure the thread, not the process.** `rsx::thread` is **0.51 of 2.90
  cores**, so a lever saving 30% of that thread moves the process total by 5%
  and hides in the noise.

**Profile reading, and the first one would invalidate conclusions:**

- **`unknown[+X]` is the JIT arena, not a symbol.** All recompiled guest code
  collapses onto one entry under `--sort symbol`, `--sort vaddr_in_file` **and**
  `report-sample`. **Never quote a percentage against it as if it were a
  function.**
- **Check whether a `grep` was truncated before concluding something is absent.**
  A `head -12` hid an entire upstream subsystem behind local probe code.

**That last rule is this repo's own most repeated failure**, and another fork
had already written it down.

## 3. Vita3K's experiment gate is a duplicate-detector plus a packet

**Before running anything**, `debug_knowledge.py` searches recent and long-term
memory and **checks the exact planned hypothesis** against past attempts:

> If a comparable attempt already failed or was inconclusive, **do not repeat it
> unchanged.** Either add instrumentation, change one controlled condition, or
> record why the old result is now superseded.

**Then `renderer_experiment.py start` opens a packet** carrying case, title id,
platform, subsystem, **a one-variable hypothesis**, the expected outcome, the
exact scene, and a baseline artifact — closed with a status and result.

**It uses a fixed outcome vocabulary**: `fixed`, `improved`, `unchanged`,
`worse`, `mixed-supports-involvement`, `contaminated-inconclusive`.

**Three of its rules belong in this repo:**

- **One variable per experiment.**
- **Prefer A/B/A when a live toggle exists.** `CLAUDE.md` asks for in-place
  alternating A/B; this names the pattern.
- **Stop after two failed or inconclusive guesses in the same subsystem.** The
  next move is instrumentation, dumps, RenderDoc or Ghidra — **not a third
  guess.**

**And the line that separates a probe from a fix:**

> A diagnostic prop, draw skip, shader hash guard, or forced depth/texture path
> **is not a fix until it becomes emulator-semantics code and passes regression
> checks.**

## 4. Three forks built an experiment ledger independently

| Fork | Mechanism |
| --- | --- |
| **xenia** | `tools/exp_ledger.py`, SQLite, verdicts `DEAD`/`FLAT`/`WIN`/`GFX-LOSS`/`CONFOUNDED`/`OPEN` |
| **Vita3K** | `debug_knowledge.py` + `renderer_experiment.py`, SQLite, packet manifests |
| **rpcsx** | `thor-experiment-ledger`, two ledgers |

**Three independent implementations of one idea is this repo's strongest
signal** — the same shape as Oboe, the touch overlay and the Dolphin overlay
API.

**And Vita3K's "stop after two failed guesses" is the supervision layer built
here today from the AVO paper.** It already existed in the fleet. **That is the
sixth time today.**

## What to change

1. **Put the noise floors in `CLAUDE.md` and check every `DEVICE_QUEUE.md`
   prediction against them.**
2. **Add the profile-reading traps**, especially `unknown[+X]`.
3. **Add "stop after two failed guesses in one subsystem"** to
   `tools/supervise.py` as a stagnation rule — it is exactly what that tool is
   for, and a fork wrote the rule first.
4. **Record the three-way ledger convergence** in the capability inventory.

## Limits

- **Three skills read of 27.** `thor-game-workup`, `ps3-speed-proof-gate`,
  `thor-windows-android-ab` and `vita3k-debug-rag` look valuable and are unread.
- **The noise floors are rpcsx's, for PS3 workloads.** Whether a Wii U title
  screen is as stable as a PS3 one is unmeasured — **but the ordering will
  hold**, and the cutscene figure is a warning for any fork.
- **rpcsx is GPL-2.0-only.** Procedures and measured facts are usable; its files
  are not.
