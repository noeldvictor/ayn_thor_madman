# Target features are permission, not speedup — and xenia already measured it

**Goal: verify a lint result that contradicted this repo's own documentation.**

Session 2026-08-23 01:50. Found by writing `tools/fleet_lint.py`, whose first
accurate run reported that **xenia sets both `-march` and `-mtune`** — which
`CLAUDE.md` says no fork does.

**Result: the documentation was wrong, and reading the file that proved it
turned up a measured answer to an experiment already queued for the device.**

---

## 1. The correction

`CLAUDE.md` states: *"melonDS is the only fork that sets `-mtune` ... xenia is
the only fork that raises `-march`. **No fork does both.**"*

**xenia does both**, in `premake5.lua`:

```lua
buildoptions({"-march=armv8.2-a+lse+crypto+sha3+crc+dotprod",
              "-mno-outline-atomics",
              "-mtune=cortex-a710"})
```

**And it is very nearly the line `THOR_TARGET.md` recommends**, arrived at
independently. That is another convergence result, like Oboe.

**Two differences from the recommendation, and both are deliberate on xenia's
side:**

- xenia adds **`-mno-outline-atomics`** and **`+crypto`**, neither of which
  `THOR_TARGET.md` names.
- xenia tunes for **`cortex-a710`**, not `cortex-x3`.

**Why the earlier survey missed it:** it searched `CMakeLists.txt` and `*.cmake`
files. **xenia uses premake.** The same class of error as every other search
failure today — the mechanism was right, the file type was wrong.

## 2. The finding: the features buy permission, not speed

xenia's comment records an experiment this repo had **queued for the device**:

> **MEASURED AFTER ENABLING THEM** (disassembled the built `libxenia-app.so`,
> 1,779,182 instructions): clang emitted **ZERO eor3/bcax/rax1/xar, ZERO
> aes*/sha*, ZERO crc32*, ZERO udot/sdot.** So these flags change the compiler's
> **PERMISSION** and nothing else — they are a **PREREQUISITE for hand-written
> intrinsics, NOT a free codegen win. Do not claim a speedup from this line.**

It even records a **correction to its own earlier comment**, which had implied
clang would fuse C++ into `EOR3`/`BCAX`. It does not.

**`DEVICE_QUEUE.md` item 4 predicted `FLAT` and asked for a device A/B. xenia
already got `FLAT`, by a better method, and needed no device.**

### The method is the transferable part

**Disassemble the built binary and count instructions.**

| | Device A/B | Disassembly |
| --- | --- | --- |
| Needs the Thor | yes | **no** |
| Scene-dependent noise | **yes, several times a second** | none |
| Answers "did the compiler emit it" | indirectly, at best | **exactly** |
| Repeatable | approximately | **deterministically** |

**For any question of the form "does this flag change what is emitted", the
disassembly is strictly better than a benchmark**, and this repo's measurement
discipline had not considered it.

**It also separates two questions the repo had merged**: *did the compiler emit
the instruction* and *did the program get faster*. **Only the second needs the
device.**

### What it means for the compile target

**The target in `THOR_TARGET.md` stays**, and its justification changes.

It was justified as matching the silicon. **That was right, and it is not a
performance claim** — the correct reason is that **hardware AES and SHA
intrinsics will not compile without `+crypto`**, and `SDOT` cannot be
hand-written without `+dotprod`. **The flags unlock work; they do not do it.**

**So the ARMSX2 opportunity recorded earlier is real but is one step further
away than implied.** ARMSX2 emits no `SDOT` and has baseline flags. Fixing the
flags will not make clang emit `SDOT`. **Someone has to write the intrinsics**,
and the flags are what lets that compile.

## 3. A hazard: `+fp16` may not be safe

`THOR_TARGET.md` recommends `+fp16`. **xenia deliberately excludes it:**

> NOT adding `+fp16`: FP16 is excluded for guest FP everywhere in this tree
> (it black-screens as guest geometry) and there is no host-side case.

**Read carefully, this is guest-side and xenia-specific.** It is about how xenia
handles Xenon floating point, not a statement that `+fp16` is unsafe on the
device — the device reports `fphp` and `asimdhp`.

**But it is a warning, and `THOR_TARGET.md` should carry it.** A fork that lowers
guest FP through half precision can lose geometry, and one fork has already been
bitten.

## 4. And a warning about ISA baselines

> these DO move the ISA baseline, unlike `-mtune` — a build with them **SIGILLs
> on an arm64 device lacking them.** Thor-targeted by intent.

**That is the same trap as `armv9-a`, one level down.** `-mtune` is always safe;
`-march` is not. It is safe here **only because the APK runs on one device**, and
that safety disappears the moment anything is shared with a non-Thor target.

---

## Method note

**This came from the lint disagreeing with the documentation.** The lint was
written to encode findings, and its first accurate run overturned one of them.

**That is the argument for the executable form, demonstrated on itself.** A
document cannot contradict you.
