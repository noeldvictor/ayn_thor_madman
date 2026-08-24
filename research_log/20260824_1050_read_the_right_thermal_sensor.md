# Two thermal quantities, one `max`, and a guard that favoured the slower arm

**Goal: read `rpcsx/docs/arm64/thermal.md`.**

**It records a measurement failure that this repo's own device rules could
reproduce exactly, and the failure is not a wrong number — it is a bias toward
whichever arm ran slower.**

## The device exposes two quantities and they differ by 30 C

| Sensor | Under load | Idle | What it is |
| --- | --- | --- | --- |
| `cpu-1-9` | **90.7 C** | **55.0 C** | **per-core junction (Tj)** |
| `cpu-1-10` | 83.9 C | — | junction |
| `cpuss-0` | 68.7 C | 49.4 C | CPU subsystem |
| `gpuss-*` | 43-46 C | — | GPU subsystem |
| AYN FanBase / on-device readout | **57-61 C** | — | **package** |

**A ~35 C swing with load is the junction signature.** Subsystem sensors beside
it move far less.

**This device exposes no `skin` zone at all** — nothing matches
`skin|case|shell|quiet` — so `/sys/class/thermal` has no package sensor, and a
guard written against one has nothing to fall back on.

**Roughly 90 C junction under load is ordinary for this SoC**, which throttles
nearer **95-105 C**. **The number the fan curve uses, the number shown to the
user, and the number a 72 C limit bounds are all package.**

## The failure: a `max` over a heterogeneous set

rpcsx's guard classified any zone matching
`cpu|gpu|soc|apss|cluster|silver|gold|prime|cpuss|ddr|memory|modem|pmic|xo` as
`silicon` and reported **the maximum**. That set includes `cpu-1-0` through
`cpu-1-10` — **the junction sensors** — so a package-shaped limit of 72 C was
applied to a junction maximum.

> **A 72 C limit on a junction maximum is not a thermal bound, it is a load
> detector.**

**Verified after the fix, at moderate load:**

```
silicon : 64.6 C from cpuss-2   (15 sensors, limit 72)  no violation
junction: 71.9 C from cpu-1-8   (14 sensors, limit 95)  no violation
```

**Under the old classifier that same moment would have reported 71.9 C against a
72 C limit — one tenth of a degree from stopping the run**, at an unremarkable
temperature.

**The fix was the classification, not the threshold**: `cpu-<cluster>-<core>`
became its own `junction` domain with `MaxJunctionTemperatureC = 95.0`, leaving
`silicon` to the subsystem sensors it was meant to describe.

## Why this matters more than a bad reading

**The bias is systematic.** A junction threshold trips sooner on faster cores,
so:

- the arm using the **big cores** tripped the guard **0.7 s in**;
- the arm pinned to the **A510s** did not, **because little cores run cooler at
  the junction.**

> *"The measurement was faithfully recording which arm ran on faster cores."*

**A510 pinning was then adopted to satisfy a limit that was measuring the wrong
quantity.** The decision rested on an artifact, and removing the pinning later —
for an unrelated reason — removed it by accident.

**And it nearly reversed a good change once:** an unthrottled compile was reported
as *"81.5 C, above the 72 C gate"* **when the device was at 57 C.**

**With the correct sensor the comparison inverts cleanly:**

| Configuration | Time | Package temp |
| --- | --- | --- |
| throttled: 3x A510, 2 LLVM threads | ~10 min | 51-58 C |
| **unthrottled: all cores, auto threads** | **~3 min** | **57-61 C** |

**Three times faster for about three degrees.**

## The rules this repo should adopt

1. **State which sensor every temperature came from.** A limit and a measurement
   from different sensors cannot be compared.
2. **Never `sort -rn | head -1` over every thermal zone.** A `max` over a
   heterogeneous set silently compares the wrong quantities.
3. **A thermal guard biased toward the slower arm is a confound that fakes a
   win** — the class rpcsx's spin prediction names explicitly. **Check the guard
   before trusting an A/B it participated in.**
4. **This repo's "about 5 W and 50 C" target needs its sensor named.** 50 C is
   plausible as package and is well below idle junction.

## Limits

- **Not reproduced by me.** All numbers are rpcsx's, on this device.
- **The package figure comes from the AYN FanBase readout**, not from
  `/sys/class/thermal`, because this device has no skin zone there.
- **The 95-105 C throttle point is stated, not measured** in that document.
- **The compile comparison is a build workload**, not a game, so the three-degree
  figure does not transfer to sustained play.

## Sources

- rpcsx `docs/arm64/thermal.md`, `tools/thor_debug_common.ps1`
