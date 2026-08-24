# The cross route works, and `-static` is the next obstacle

**Goal: I wrote "a route existing is not a route working" about the qemu route
for entry 13. Test the half I was uncertain about.**

**Device-free, and no fork touched. One temporary file in WSL, removed.**

## The test

```
aarch64-linux-gnu-g++ -O2 -std=c++20 -static -o t t.cpp
file t          -> ELF 64-bit LSB executable, ARM aarch64
qemu-aarch64 t  -> "running as aarch64"
```

**The program prints which branch of `#if defined(__aarch64__)` it compiled**, so
the result establishes both halves at once: **the compiler defines the ARM64
macro, and the binary executes.**

> **That is precisely what entry 13 needs.** `XE_ARCH_ARM64` is what gates
> xenia's a64 backend into the build, and the emitter then runs — **and inflation
> is a count of what the emitter EMITS, not of how fast it runs**, so emulated
> execution is sufficient.

## `-static` is load-bearing, and naming it is the point

**qemu-user running a DYNAMICALLY linked ARM64 binary needs the ARM64 sysroot
and `QEMU_LD_PREFIX`.** A static link sidesteps that.

**I used `-static`, so this test does not prove the dynamic case**, and
**xenia's cross build will be dynamic.** Either `QEMU_LD_PREFIX` points at the
aarch64 sysroot, or the harness is linked static.

> **Recorded so it is met deliberately rather than discovered as a crash**, which
> is the same reason rpcsx fixed a branch offset in disabled code: *"whoever
> enables it is debugging one thing instead of two."*

## What is now established, and what is not

| | Status |
| --- | --- |
| an ARM64 cross compiler exists here | **tested** |
| it defines `__aarch64__` | **tested** |
| qemu-user executes the result | **tested** |
| the dynamic-linking case | **not tested**, and `-static` avoided it |
| xenia's `--linux-arm64` premake path configures | **not tried** |
| the PPC harness runs under qemu with threads | **not tried** |

**The uncertainty has moved from "is there an ARM64 host" to "does this
particular fork cross-build".** That is a much smaller and more specific
question, and it is a build question rather than a hardware one.

## Why this was worth doing before the big build

**A cross-build of xenia is a long job.** Establishing the toolchain first means
**a failure in that build is a xenia problem, not an unknown.** The alternative
is starting a long build and, on failure, not knowing which of three things
broke.

**And it cost one temporary file.** No fork was touched, nothing was installed,
and the file was removed.

## Limits

- **A ten-line program is not xenia.** Threads, dynamic linking, filesystem
  access and signal handling are all untested under qemu-user here.
- **`uname -m` in this WSL reports `x86_64`.** The ARM64 capability is emulation.
  **Any timing taken this way is meaningless** — which is why this route serves
  an instruction count and nothing else in the queue.
- **The WSL path-translation failure noted earlier recurs**; the command must run
  from a translatable directory.

## Files

- `DEVICE_QUEUE.md` entry 13 — toolchain half marked tested, `-static` named
