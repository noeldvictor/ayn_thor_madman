---
name: thor-measure
description: Use before and during ANY measurement on the AYN Thor - fps, frametime, watts, temperature, GPU counters, screenshots, or an A/B comparison. Encodes the device connection, the traps that silently produce fake numbers, and the rules that make a result trustworthy. Triggers on "measure", "benchmark", "profile", "capture", "A/B", "is it faster", "how many fps".
---

# Measuring on the Thor

**A wrong measurement is worse than no measurement.** Every trap below has
already produced a fake number in this project.

## Connect

**Never run a bare `adb` command.** A Quest 2 also answers adb on this machine.
A command without `-s` either fails or reaches the wrong device.

**Do not hardcode the address.** It changes with the DHCP lease. It was
`192.168.1.3:5555` on 2026-08-22 and `192.168.1.33:5555` in an older script.

Resolve by model:

```sh
THOR=$(adb devices | awk '/device$/{print $1}' | while read s; do
  [ "$(adb -s "$s" shell getprop ro.product.model | tr -d '\r')" = "AYN Thor" ] \
    && echo "$s"
done)
```

Reconnect with `adb connect <ip>:5555` after the device sleeps. Treat a dropped
Wi-Fi connection as normal, not as a fault.

**Check what is running before you foreground anything.** The Thor is a device
somebody uses. `am start` interrupts a game in progress.

## The traps

### Power readings are fiction while charging

`dumpsys` reports `status=Charging` and `current_now` flips sign between
consecutive idle samples. Measured on one idle run: -36988, +225591, +165897,
+224859, -16846 uA.

**Gate every power measurement on `status=Discharging` and refuse to report
otherwise.** Wi-Fi adb exists so the cable can come out.

`xenia-thor/tools/thor/power_affinity_ab.sh` already implements the gate. Copy
it.

### Cross-run comparison is confounded

Scene complexity swings several times a second, so two separate runs are not
comparable.

**Trust only:**

- an in-place alternating A/B inside one run, on a busy frame
- screenshot correctness
- byte-identical comparison
- code facts

**`CONFOUNDED` is a verdict.** A number that cannot be trusted gets labelled,
never discarded and never promoted to a win.

### No temperature rise means the run did not happen

An idle or menu scene produces a number and no heat. **A run with no
temperature rise is invalid whatever the counter said.** Record the temperature
delta with every result.

### Short runs measure a cold device

Thermal behaviour settles over minutes. **Run 15 minutes or more when heat
matters.** A two-minute run measures a device nobody plays on.

### `screencap -d` takes the SurfaceFlinger display ID

Not the Android display id. `-d 0` and `-d 4` write a **zero-byte file and
report success**.

| Panel | Android displayId | SurfaceFlinger ID |
| --- | --- | --- |
| Built-in Screen | 0 | `4630946441858561667` |
| Screen-2 | 4 | `4630946482288158084` |

**Treat a zero-byte PNG as a failure.** Confirm the IDs with
`dumpsys SurfaceFlinger --display-id`; they may differ after a firmware change.

### Stream captures, do not stage them

`screencap -p /sdcard/x.png` writes zero bytes, probably scoped storage. Use:

```sh
adb -s "$THOR" exec-out screencap -d <sf-id> -p > local.png
```

### Git-Bash mangles remote paths

`/sdcard/x.png` becomes `C:/Program Files/Git/sdcard/x.png`.

```sh
export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'
```

Then the **local** path must be Windows-style, because `adb.exe` cannot read
`/c/Users/...`. One command line, two path styles.

Prefer PowerShell for Android work. See `CLAUDE.md`, Conventions and hazards.

### Do not pull a capture during the run

The transfer competes with what you are measuring. Pull after the run ends.

### Check the refresh cap before any pacing work

Both panels support 120 Hz and the device is currently capped to 60 by a user
setting. A pacing measurement taken now measures the setting, not the hardware.

### ADPF is off on this device

The persisted config sets `gpu_adpf_performance_hints` false, overriding a
compiled default of true. An arm that does not force it measures nothing.

Also: **measure without ADPF first**, or the hint is tuned against an unknown.

## Before the run

0. **Read [`DEVICE_QUEUE.md`](../../../DEVICE_QUEUE.md).** There is one physical
   Thor, so device time is the scarce resource and analysis is not. **If the
   thing you are about to measure is already queued, run the queued version**,
   which already carries its prediction and its gates.

   **And put a new experiment in the queue rather than in a research log.** A
   log is where an experiment goes to be forgotten; the queue is what gets read
   when the device is free.

1. **Query the experiment ledger.** `python tools/exp_ledger.py check "<keyword>"`
   in xenia-thor. A `DEAD` or `FLAT` verdict means do not re-run it.
2. **State the expected signature.** Name what the numbers should do if the
   change works. A run with no prediction cannot fail.
3. **Name the scene.** Which game, which point, how you get there.
4. **Name the CPU cluster** for any CPU claim. The 8 Gen 2 is 1+4+3 and
   "faster on the X3" and "faster on the A510" are different results.
5. **Check the metric matches the claim.** Some wins do not move an average.
   A frame-pacing change should hold average fps and **cut frame-time
   variance**, so a run recording only fps will read as `FLAT` while being a
   win. Record 1% low and frame-time spread whenever smoothness is the point.
   A bandwidth change may move **watts and temperature and not frame time at
   all**.

## What to record

Every result carries:

- the fork and the commit
- the driver build
- the scene and how it was reached
- charge state and battery level
- temperature before and after
- the expected signature, and whether it happened
- a verdict: `WIN`, `DEAD`, `FLAT`, `GFX-LOSS`, `CONFOUNDED`, `OPEN`

**Watts, not only frames.** The stated target for this device is roughly 5 W
and 50 C. A change that holds fps and lowers temperature is a win.

Write it to `work_log/` as you go, and add it to the ledger the moment you have
it.

## Existing tools

Do not write new capture tooling before reading these:

| Tool | Fork |
| --- | --- |
| `power_affinity_ab.sh` | xenia-thor, gates on Discharging |
| `bd_adpf_ab.sh` | xenia-thor |
| `thor_gpu_profile.ps1`, `thor_gpu_perfetto.ps1` | xenia-thor |
| `xenia-thor-adb-gpu-stage-split` skill | per-stage Adreno split over adb |
| `xenia-snapdragon-profiler-gpu-metrics` skill | hardware stage metrics |
| `xenia-thor-evidence-discipline` skill | mandatory before stating a number |
| `run-thor-regression-suite.ps1` | Vita3K-Thor, on-device suite |
| `thor_mcp_server.py` | xenia-thor, drives all of the above |
