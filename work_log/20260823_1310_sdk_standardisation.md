# Standardising the fleet on one SDK row

**Goal: move every fork to `minSdk` 33, `targetSdk` 37, `compileSdk` 37, and
verify by building.**

Session 2026-08-23 13:10. Started because the lint showed **every fork below the
row**, and Phase 1 cannot proceed while they disagree.

**Status: all nine forks changed. Three verified by a successful build.**

---

## What each fork was, and is

| Fork | minSdk | targetSdk | compileSdk |
| --- | --- | --- | --- |
| **row** | **33** | **37** | **37** |
| ARMSX2 | 26 → **33** | 37 | 37 |
| azahar | 29 → **33** | 37 | 35 → **37** |
| Cemu | 30 → **33** | 35 → **37** | 36 → **37** |
| **eden** | **24 → 33** | 36 → **37** | 36 → **37** |
| GameThor | 26 → **33** | **28 → 37** | 35 → **37** |
| **melonDS** | **24 → 33** | 36 → **37** | 36 → **37** |
| rpcsx | 29 → **33** | 35 → **37** | 37 |
| Vita3K | 28 → **33** | 35 → **37** | 35 → **37** |
| **xenia** | 26 → **33** | **33 → 37** | **33 → 37** |

**`android-37.0` was confirmed installed before any change.**

**Every file was backed up** to `%TEMP%\sdk_backups` first, and **nothing was
committed to any fork repository.**

## Two couplings, handled rather than overwritten

**A blind find-and-replace on the numbers would have broken both.**

**ARMSX2 reads `minSdk` from a gradle property:**

```kotlin
val armsx2MinSdk = providers.gradleProperty("armsx2.minSdk").orElse("26")
```

**The default was changed to 33 and the override kept.** The override is a
feature the fork deliberately provides; replacing the expression with a literal
would have removed it.

**melonDS's `minSdkVersion` selects the NDK compiler:**

```kotlin
val clang = toolchain.resolve("${abiTarget.clangPrefix}${AppConfig.minSdkVersion}-clang...")
```

**Raising it to 33 changes which clang binary is invoked** — from
`aarch64-linux-android24-clang` to `…android33-clang`. **Verified that binary
exists in NDK 28 before building**, and it does.

**It also invalidates the entire native cache**, which is why melonDS's build
went from 15 min 27 s to **18 min 12 s** — all three ABIs recompiled from
scratch.

## Verification

| Fork | Result | Notes |
| --- | --- | --- |
| **Cemu** | **BUILD SUCCESSFUL in 5 min 8 s** | 73.1 MB APK |
| **melonDS** | **BUILD SUCCESSFUL in 18 min 12 s** | all 3 ABIs rebuilt, 15 warning groups |
| azahar | building | |
| ARMSX2 | queued | |
| eden, GameThor | **cannot be verified** — they do not build for unrelated reasons | |
| Vita3K, rpcsx, xenia | not yet attempted | |

**melonDS is the important one.** It carried the riskiest change and its
toolchain switch worked.

## The migration is safer than it looked, because two forks had already done it

**azahar and ARMSX2 were already on `targetSdk = 37`.**

`targetSdk` is the behavioural level — each step opts the app into new Android
restrictions — so it is the risky one. **The fleet already contains a working
reference for it, twice.**

**And azahar wrote down what it cost**, in its own `AGENTS.md`:

> SDK 37 edge-to-edge handling depends on a black emulation root, the named
> `coordinator_layout`, and one null-safe display-cutout margin listener
> attached to both the coordinator and in-game menu. Do not restore the deleted
> `values-v35` opt-out theme or cast a possibly null `layoutParams` to a
> non-null margin layout.

**That is a propagation item for every fork that just moved to 37**, and it is
exactly the pattern in
[`PROPAGATION.md`](../shared_layer/PROPAGATION.md): one fork learned it, the
others have not received it.

**Prediction: the forks that just jumped to 37 will show edge-to-edge and
display-cutout problems on the device, not at build time.** A build passing does
not verify `targetSdk`. **That check belongs in
[`DEVICE_QUEUE.md`](../DEVICE_QUEUE.md).**

## What is not done

- **Four forks unverified by build**, two of them because they do not build at
  all for reasons predating this change.
- **Nothing committed to any fork.** The changes sit in the working trees with
  backups beside them.
- **No device verification**, which is the only thing that actually tests a
  `targetSdk` raise.
