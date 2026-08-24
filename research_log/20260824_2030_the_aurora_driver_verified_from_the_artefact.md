# The Aurora driver, verified from the artefact: one file with two names, and its headline feature is an upstream env var

**Goal: assess `github.com/Balemuni/Balemunis-Aurora`, offered as the best new
Turnip build for the AYN Thor, against the pinned-driver decision.**

**No device used. The facts below come from the published artefacts and from
upstream Mesa, and each is named at the point it is used.**

## What the README claims

Mesa 26.3.0-devel, tuned for the Snapdragon 8 Gen 2 and named for the AYN Thor,
with **Global Code Motion in the IR3 compiler**, a **4 GB shader cache**, **512 KB
suballocator pools** and **Adreno 740 LRZ fast paths**. Two downloads: an
`Ultimate SD8Gen2` build and a `Universal AllAdreno` build.

## 1. The two downloads are ONE FILE

| asset | bytes | SHA-256 |
| --- | --- | --- |
| `Balemuni_Apex_Ultimate_SD8Gen2.zip` | 14,925,167 | `dbd1971d...49cd8f` |
| `Balemuni_Apex_Universal_AllAdreno.zip` | 14,925,167 | `dbd1971d...49cd8f` |

**Byte-identical.** The `meta.json` inside both says *"SD8 Gen 2 / Adreno 740"*,
because there is only one archive.

> **There is no SD8Gen2-specific build. The device-specific name is a label on a
> universal artefact.**

The shipped library is `vulkan.freedreno.so` — the generic Mesa name. The R8
build already in this project's candidate table ships `vulkan.ad07xx.so`, an
a7xx-specific name.

## 2. The build is upstream Mesa main, from two hours before release

The binary's own banner: **`26.3.0-devel (git-6dd2f2919e)`**.

That hash resolves upstream: Mesa commit `6dd2f2919e74a1e0...`, merged
**2026-08-20T19:33Z**. The release was published **2026-08-20T21:43Z** — about
**two hours later**. The commit itself is a `radeonsi` comment change, so it is
the tip of main at build time and nothing to do with Turnip.

**Mesa builds its version string from `git describe`.** A committed local patch
makes HEAD a local commit, and that hash does not resolve upstream. **This one
does.**

> **So the tree at build time was upstream main. Any change was an uncommitted
> working-tree edit** — which cannot be reviewed, because the repository holds no
> source.

## 3. The repository holds no source

Two files: `README.md` (3,675 bytes) and a Google site-verification page. **No
Mesa tree, no patch set, no build script, no CI.** Created 2026-08-04. 30 stars,
0 forks.

## 4. The headline feature is an upstream environment variable, default off

Upstream `src/freedreno/ir3/ir3_nir.c`, **at the exact commit this driver was
built from**:

```c
static int gcm = -1;
if (gcm == -1)
   gcm = debug_get_num_option("GCM", 0);
if (gcm == 1)
   progress |= OPT(s, nir_opt_gcm, true, true);
else if (gcm == 2)
   progress |= OPT(s, nir_opt_gcm, false, true);
```

**GCM is upstream, it is gated on an environment variable, and the default is
0 — off.** The literal `GCM` is present in the shipped binary.

> **A driver package cannot set its own environment.** Installing this driver
> does not enable GCM. **The app does, or nobody does.**

**This is a `DID_IT_APPLY.md` instance from outside the fleet**: a feature that
is real, is present, is advertised as included, and does not take effect —
because the switch that enables it lives where the shipping artefact cannot
reach.

### And it is not Aurora's to offer

`ir3_nir.c` has not changed upstream since **2026-07-20**. Checked directly
against the **K11MCH1 `Turnip_v26.0.0_R8`** build already in this project's
candidate table, built in **May 2026**:

| build | banner | `GCM` knob |
| --- | --- | --- |
| Aurora Apex | `26.3.0-devel (git-6dd2f2919e)` | **present** |
| **R8, May 2026** | `26.0.0-devel (git-5ac41be677)` | **present** |

> **The knob is three months older than this driver and is in a build the fleet
> already holds.** Testing GCM needs no new driver.

**That converts a "which driver" question into an environment A/B**, which is
cheaper and applies to whichever build is pinned. `DEVICE_QUEUE.md` entry 27.

### Method, and a tool default that produced a false negative

**Instrument: `llvm-strings` from NDK 29**, plus `sha256sum` on the archives and
the GitLab API for the Mesa commit. **No `grep` over a fork tree; the search
space is two binaries.**

**The first search for `GCM` returned nothing, and the string is there.**
`llvm-strings` defaults to a **minimum length of four characters**, and `GCM` is
three.

> **A tool default silently excluded the exact string under test.** Re-run with
> `-n 3` and it appears in both drivers. **This is the "prove the instrument
> before believing a zero" rule, met by a tool's default rather than by a bug.**

**Any search for a short symbol name — `GCM`, `LRZ`, `IDC`, `DIC`, `sve` — must
set the minimum length explicitly.**

## 5. The other three claims are not checkable from a stripped binary

`MESA_SHADER_CACHE_MAX_SIZE` is present in the binary and is an **upstream
environment variable**, so "4 GB shader cache" is either an instruction to the
user or a patched default, and the two are indistinguishable here. Suballocator
pool sizes and LRZ fast paths are compile-time code, and there is no source to
compare. **Recorded as unverified, not as false.**

## 6. The licence blocks bundling, and nothing else

The repository declares **no licence** — no `LICENSE` file, no SPDX field — and
the binary carries **no MIT permission notice** (searched; zero hits).

**Mesa is MIT, and MIT requires the copyright and permission notice to travel
with a redistribution.** This repo's driver rule already says to *"confirm the
licence of the specific build before shipping it"*.

> **A build that carries no notice and publishes no source cannot be bundled.**
> It can be installed and measured by anyone who wants to — that is the user's
> own copy, not redistribution.

## What is actually different, stated fairly

**One thing: it is a newer build of the same Mesa series than the current pin.**
The pin is `mesa-turnip-v26.3.0-20260803-r7`, dated 2026-08-03. This is
26.3.0-devel dated 2026-08-20 — **seventeen days newer, same series.**

That is a real if modest benefit, and it is the only verified difference.

## Limits

- **Nothing is measured.** No frames, no watts, no device. Whether this build is
  faster than the pin on any scene is unknown.
- **A stripped binary cannot prove the absence of a patch.** The version-string
  argument bounds what kind of change is possible; it does not exclude an
  uncommitted edit.
- **The R8 comparison is a build from a different packager**, used here only to
  date the GCM knob, not as a performance reference.
- **The GCM knob being present is not evidence it helps.** It is upstream and
  default-off, which usually means it was not a clear win everywhere.

## Sources

- `github.com/Balemuni/Balemunis-Aurora`, release `Balemuni`, published 2026-08-20
- Mesa commit `6dd2f2919e74a1e038485b1dd08eb062c4230ebb`, and `ir3_nir.c` at that ref
- `nethersx2-thor/NetherSX2-patch/.tmp_driver/Turnip_v26.0.0_R8.zip`, read only
- `shared_layer/DID_IT_APPLY.md`
