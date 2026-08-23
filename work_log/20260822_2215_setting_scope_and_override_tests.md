# Add setting scope, a safe override writer, and the first tests

**Goal: put ARMSX2's three per-game-override fixes into the contract, and prove
they are needed.**

Session 2026-08-22 22:15. Source:
[`../research_log/20260822_2203_armsx2_frontend_is_the_shell.md`](../research_log/20260822_2203_armsx2_frontend_is_the_shell.md).

**Done and verified on the JVM. No device involved, and none needed: this is
pure logic.**

---

## What changed

`app/shell/app/src/main/java/com/aynthor/shell/Backend.kt`.

**1. `SettingScope`.** `PER_GAME`, `PROMOTED`, `GLOBAL_ONLY`. `SettingSpec`
carries one, defaulting to `PER_GAME`.

**2. `resolve` takes a `SettingSpec` and a `global` tier.** The order is now
per-game, global, Thor profile, backend default. A `PROMOTED` or `GLOBAL_ONLY`
setting is **never read from the per-game tier**, so a stale override cannot
outlive the rule that put it there.

**3. `writeOverride`**, new. It returns both a per-game map and a **global
patch**, because a promoted field has to leave the per-game tier and land
somewhere. It implements three rules:

| Rule | What it prevents |
| --- | --- |
| sparse | a per-game copy of every field, which would block later global changes |
| **sticky** | an override that equals global being stored as nothing, then lost when global changes |
| **change-tracked** | a stale whole-object write clobbering a pinned value, permanently |

**4. `SETTINGS_SCHEMA_VERSION`**, because ARMSX2 carries seven one-time
migration keys and a schema that ships needs migrations.

## Test setup, which did not exist

The shell had **no test source set and no test dependency**. Added
`junit:junit:4.13.2` and `app/src/test/java/`. The resolver is pure Kotlin with
no Android dependency, so it runs on the JVM.

`app/src/test/java/com/aynthor/shell/SettingResolverTest.kt`, 8 tests.

## Verification, including the part that matters

**Green with the real implementation:**

```
com.aynthor.shell.SettingResolverTest: tests=8 failures=0 errors=0 skipped=0
BUILD SUCCESSFUL
```

**Then the naive implementation was swapped in and the tests re-run.** The naive
version is the design `CLAUDE.md` Track A step 4 specified: store exactly the
fields that differ from global right now.

```
RESULT tests=8 failures=4 errors=0
  FAIL: sticky - ARMSX2 bug 1, an override equal to global survives a global change
  FAIL: promoted - ARMSX2 bug 2, a process-wide setting is written to global, not nowhere
  FAIL: change-tracked - ARMSX2 bug 3, a stale whole-object write cannot clobber a pin
  FAIL: promotion carries only the changed field, never the resolved object
```

**The specified design fails four of eight tests, and the failures are exactly
ARMSX2's three reported bugs.** That satisfies the repo rule that a test must
fail before the change, and it is the evidence that the three rules are
necessary rather than defensive.

The real implementation was then restored and re-verified: 8 tests, 0 failures,
`assembleDebug` successful.

## Follow-up, same session: the native contract

**`shared_layer/thor_backend.h` had no settings at all**, which is worse than
disagreeing with the Kotlin side: `CLAUDE.md` lists settings in the **minimum**
contract, naming the key namespace and the override resolution order.

Added a settings section carrying `SettingType`, `SettingScope`, `SettingSpec`,
`SettingSource`, `kSettingsSchemaVersion`, the three rules as a comment block
with the ARMSX2 symptom for each, and two interface methods: `SettingCount` /
`SettingAt` to declare, and `ApplySetting` to receive **a fully resolved value**.
The backend is told the answer; it never resolves its own order and never reads
or writes a settings file.

**Syntax-checked, and this is the first time the header has been compiled:**

```
clang++ --target=aarch64-linux-android33 -std=c++20 -fsyntax-only thor_backend.h
1 warning generated  (#pragma once in main file, expected)
exit code: 0
```

NDK `29.0.14206865`, the version in the standard row. It compiles as C++20 on
the target triple. That is one data point toward the C++20 decision `CLAUDE.md`
marks unverified — for this header only, not for any fork.

## What is not done
- Nothing reads `SETTINGS_SCHEMA_VERSION`. It is a placeholder with a reason.
- **No screen calls `SettingResolver`.** It had no callers before this change
  either, so the contract is still unexercised by the UI.
- The fake backends do not declare a `PROMOTED` setting, so the scope split is
  untested against the shell's own data.
