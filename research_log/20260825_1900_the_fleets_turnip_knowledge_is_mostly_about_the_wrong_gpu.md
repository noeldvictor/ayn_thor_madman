# The fleet's Turnip knowledge is mostly about a GPU this device is not

**Goal: XenDroid has a Turnip driver workaround. Extraction candidate 9 is a
driver bug database. Add it.**

**Device-free: one commit diff. No device used.**

## The defect

> *"**Adreno 6xx: Turnip's shared-consts push delivery is broken there.**"*

Worked around with `TU_DEBUG=push_consts_per_stage`, **gated to Adreno 6xx
only**.

## And that gate is the finding

**Method: these are the Turnip defects THIS REPO has recorded, collected from its
own research logs of the last three days — not a search of the forks and not a
survey of Turnip's bug tracker.** Three items, each read in full where it was
found.

**Three Turnip defects are now recorded in this repo. TWO of them are about
Adreno 6xx, and this device is 7xx:**

| Defect | Measured on | This device |
| --- | --- | --- |
| **attachment self-read broken, both forms** (ARMSX2) | **Adreno 650, Mesa 26.1.2** | a740 — **unknown** |
| **shared-consts push delivery broken** (XenDroid) | **Adreno 6xx, explicitly gated** | a740 — **excluded by the gate** |
| `vkGetFenceStatus` blocks on an in-flight fence (xenia) | Turnip-over-KGSL, **not generation-gated** | plausibly applies |

> **The fleet's accumulated Turnip knowledge is disproportionately about a GPU
> generation the Thor does not have.** That is not surprising — 6xx parts are far
> more common in Android handhelds — **but it means a rule inherited without its
> provenance is likely to be a 6xx rule.**

**Consequence for extraction candidate 9, and it sharpens a design already
recorded.** ARMSX2's `GSGPUProfile` matches with a **confidence rank** —
`Vendor < Model < Driver < DriverVersion` — which is exactly the mechanism
needed. **The rule to add: every entry records the part it was MEASURED on, not
only the parts it applies to.** A workaround with no measured-on field cannot be
re-evaluated when the fleet moves to a different GPU, and **an unmeasured 7xx
entry inherited from a 650 costs performance for a defect that may not exist
here.**

**ARMSX2's attachment-self-read rule carries no version bound** — this repo
already records that it was written from one A/B rather than re-tested. **It is
the exact shape this rule is about.**

## A device fact this repo did not have

```c
fopen("/sys/class/kgsl/kgsl-3d0/gpu_model", "r")
```

parsed for its first digits, then `/ 100 == 6` for the generation.

**This repo records `/sys/class/kgsl/kgsl-3d0/gpubusy` as a cumulative busy
counter that needs no root. `gpu_model` is its sibling and was not recorded.**

> **It is the GPU analogue of `midr_el1`** — world-readable identification from
> sysfs, **available to native code before Vulkan is up**, which matters because
> a driver environment variable must be set before the driver reads it.

**rpcsx's `GpuDriverAdvisor` already reports `a7xx` and `Adreno 740`**, so the
capability exists in the fleet; **what `gpu_model` adds is that it works with no
Android API and before instance creation.**

## And it answers a question `DEVICE_QUEUE.md` entry 27 left open

**Entry 27 proposes an A/B of `GCM=1`**, an upstream Turnip shader optimisation
that ships default-off and is read with `debug_get_num_option("GCM", 0)`. **The
entry did not say HOW the app would set it.**

**XenDroid shows the mechanism, shipping:**

```c
setenv("TU_DEBUG", tu_debug.c_str(), 1);
XELOGI("Set TU_DEBUG={} for the Turnip Vulkan driver", tu_debug);
```

**The app sets the driver's environment from inside its own process, before
Vulkan initialises, and logs what it set.** `GCM` is read the same way, so the
same route works.

**Two details worth copying.** It **checks whether the user already specified the
flag** before appending, so an explicit setting is not duplicated or overridden.
And **it logs the final value** — which is the "verify from the emitted artefact"
rule applied to an environment variable, and exactly what entry 27's gate asks
for.

## Limits

- **One commit diff.** Nothing built or run, no device.
- **The 6xx defect is XenDroid's claim in a comment**, with no reproduction
  shown and no titles named.
- **"Two of three Turnip defects are 6xx" is a count of what THIS REPO has
  recorded**, which is three items found in three days. **It is not a survey of
  Turnip's bug surface.**
- **`gpu_model`'s exact format was not read on a device** — the parse is
  XenDroid's and assumes digits appear and encode the generation in hundreds.
- **No claim that the a740 is free of either 6xx defect.** The gate says XenDroid
  did not apply the workaround there; it does not say the defect is absent.

## Sources

- XenDroid, `[Vulkan] Force per-stage Turnip push constants on Adreno 6xx`
- `shared_layer/OWNED.md` candidate 9; `DEVICE_QUEUE.md` entries 26 and 27
