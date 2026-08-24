# Use a cumulative counter, never a spot reading; and the harness is hot enough to trip its own guard

**Goal: follow the rule written an hour ago and start at a fork's `AGENTS.md`.**
It indexes nine topic documents; `docs/arm64/instruments.md` — *"what each
measuring tool can and cannot answer"* — is the direct gap in
`shared_layer/MEASUREMENT.md`, which indexes rules and not instruments.

**Four findings, and three of them change rules this repo already has.**

## 1. An instantaneous battery reading is noise, and the fix is 800x

**This repo's rule was about the CABLE.** `CLAUDE.md`: *"Any wattage read from a
USB-attached session is fiction"*, later softened to *"a floor, not fiction"*,
with every power measurement gated on `status=Discharging`.

**The real problem is the SAMPLE, not the cable.** Measured five times on an
**idle** device with `current_now * voltage_now`:

> **0.300, 0.588, 1.447, 1.914, 1.961 W — a spread of 1.66 W**, larger than the
> effect the instrument exists to detect. Over the same five runs `cores_busy`
> held between **0.473 and 0.487**, so the CPU metrics were solid and only the
> wattage was junk.

**`charge_counter` is cumulative**, in microamp-hours. Differencing it across the
window gives the mean current over the window rather than one instant.
Re-measured four times, idle: **0.627, 0.629, 0.629, 0.628 W — a spread of
0.002 W.** **Roughly 800x tighter.**

> **Use a cumulative counter wherever one exists.** Every metric in that probe
> that works is a difference of two cumulative readings; **every one that had to
> be rewritten started as an instantaneous sample.**

**And the subtraction this repo might have reached for has no basis on this
device.** Computing `usb_in - battery_charge` to read power while plugged in
returned a **negative wattage**. `usb/current_now` sat **frozen at 447000**
across three seconds while `battery/current_now` swung from `-1130521` to
`-770160`: it is **the negotiated input limit**, not a measurement, and the
`ucsi` source node reports 0. **There is nothing to subtract.**

**Third instance of a rule this fleet keeps rediscovering:** a negative wattage,
a 10 ns `WFE`, an `ESR` decode that is always zero — **when a probe returns a
physically impossible number, the probe is what is broken.** It is not a
surprising result to be explained.

## 2. The harness is not thermally free, and it trips its own guard

**Same build, same game, same settings, two routes:**

| route | temperature |
| --- | --- |
| direct boot | `44-57 C`, never leaves the fifties |
| **through the input-macro harness** | **`59.4 -> 58.6 -> 61.0 -> 64.6 -> 70.7 C` in about ten seconds, and it trips the early stop** |

**The observer is the difference.** Its per-sample `adb shell` spawns walk
**roughly fifty `thermal_zone*` entries**, the sustain loop adds a poll per
second **exactly when the device is hottest**, and **each readiness poll takes a
1080p `screencap`.**

> **Treat harness temperatures as an upper bound that INCLUDES THE OBSERVER.**
> When the question is about the emulator rather than the route, boot directly.

**That is why the cumulative-counter property matters beyond precision.** A probe
reading cumulative counters costs **two adb round trips regardless of window
length**, so a five-minute run costs what a five-second one does. **An
instrument whose cost scales with the window cannot watch a long run**, and this
project's own rule says to run 15 minutes or more when heat matters.

## 3. A 4-second thermal sweep aliases the spike

Sampled every 2 s on a direct boot:

```
2s:56.2  4s:60.2  6s:46.2  8s:47.8  10s:44.9 ... 60s:55.0 ... 90s:56.6
```

**The transient peaks at `60.2 C` and collapses to `46.2 C` two seconds later.**
An earlier 4-second sweep reported a `57.8 C` peak and **simply missed it**.

> **Sample at 2 s or finer.** A 4 s sweep aliases the boot transient.

## 4. The three-input bitwise lane is not simply dead — it depends on the shape

**`CLAUDE.md` records `EOR3`/`BCAX` fusion as `DEAD` on zero candidates**, and
counts the instruction-repurposing lane three-for-three empty.

**`tools/bcax_bench.c` measures the instruction itself on this silicon**, with no
game and no boot — NDK, `-march=armv8.2-a+sha3 -static`, pushed to
`/data/local/tmp`, one CPU index. BCAX against the two-op form, best of five:

| shape | X3 | A715 | A510 |
| --- | --- | --- | --- |
| **latency, serial chain** | **1.96x** | **2.01x** | **2.00x** |
| throughput, 4 independent chains | **0.94x** | 1.00x | **2.02x** |

> **The two shapes disagree, and which one applies decides whether the change is
> worth making.** The big cores have enough vector pipes to issue the old pair in
> parallel, **so a wider instruction wins nothing there and can lose slightly.**
> It wins **when the result feeds the next instruction.**

**So the lane's verdict needs a qualifier this repo does not have.** A `DEAD`
from zero candidates is about applicability. **This is about shape**: even with
candidates, a throughput-bound use on a big core gains nothing, and the same
instruction is a **2x latency win** when the consumer is the next instruction.
rpcsx checked its own lowering rather than assuming — its `SHUFB` emits `bcax`
immediately followed by the `tbx` that consumes it, **so the latency row is the
one that describes it.**

**And the method is the transferable part**: a standalone microbenchmark, on the
device, per core, that answers a codegen question **without booting a game**.
This project has no such harness and its `DEVICE_QUEUE` is full of questions
shaped exactly like this one.

## Limits

- **Every number is rpcsx's, on rpcsx's device and workload.** Nothing
  reproduced here, and no device was used.
- **The BCAX table is one instruction on one microbenchmark.** It says nothing
  about whether any fork has candidates — which is the separate question `DEAD`
  answered.
- **The power figures are idle-device figures.** The spread under load is not
  quoted and may differ.
- **`charge_counter` availability was not checked on this device by me.**
- **Five of the nine topic documents remain unread**: `codegen.md`,
  `microarchitecture.md`, `memory-model.md`, `spin.md`, `spurs-halt.md`.

## Sources

- rpcsx `AGENTS.md:13-19` (the index), `docs/arm64/instruments.md:9-120`
- rpcsx `tools/thor_power_probe.ps1`, `tools/bcax_bench.c`
