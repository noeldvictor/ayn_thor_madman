# The strip task uses the module's NDK, and a missing pin ships unstripped libraries

**Goal: read XenDroid's `[Build] Pin the app module NDK so release packaging
strips native libraries`. It is three lines and it lands on the standard row.**

**Device-free: one commit diff. No device used.**

## The mechanism

```gradle
// Pin like :emulator-core - without this AGP wants its default NDK for the
// strip task; when that NDK is absent (CI) it packages libe.so UNSTRIPPED.
ndkVersion '29.0.14206865'
```

> **AGP's strip task uses the MODULE'S `ndkVersion`.** A module that does not pin
> one falls back to AGP's default — and **if that NDK is not installed, the strip
> silently does not happen.**

**No error. The build succeeds. The APK ships native libraries with full debug
symbols.**

**And it is per module, not per project.** XenDroid's `:emulator-core` was already
pinned; **the app module was not**, and the app module is the one that packages.

## Why it belongs beside a number this repo already has

**rpcsx measured that adding `x86_64` put 26 MiB compressed and 65 MiB
uncompressed of unreachable code into a 96 MiB APK — more than half the
payload.** This repo records that as the ABI lesson.

> **Unstripped libraries are the same class of bloat from a different cause**, and
> **the same failure mode: the build succeeds and nothing says the artefact is
> wrong.** rpcsx's was visible in a size comparison; this one is visible only if
> somebody looks for symbols.

**The check is one command** — `llvm-readelf --symbols` on the packaged `.so`, or
simply its size against a stripped build. **This repo already disassembles shipped
binaries routinely**, so the instrument is in hand.

## A convergence worth recording

**XenDroid pins `29.0.14206865`.** The standard row in `CLAUDE.md` specifies
**`29.0.14206865`**.

> **The same version string, chosen independently.** This repo picked it as
> "latest stable r29, installed on this box"; XenDroid arrived at the same pin.
> **Two projects, one target device, same NDK** — a small convergence, and this
> project treats convergence as evidence.

## What it means for the standard row

**The row names an NDK version and does not say WHERE to pin it.** That is now a
gap with a known consequence:

> **Every module that contains native code must pin `ndkVersion`, not just the
> one that builds it.** The module that PACKAGES is the one whose pin the strip
> task reads.

**Our own `app/shell/` has no exposure** — checked: no `ndkVersion`, no
`externalNativeBuild`, **no native code at all yet.** The requirement is recorded
before the code exists, which is the cheap end and where this project keeps
finding itself.

## Limits

- **One commit diff, three lines.** Nothing built, nothing measured, no device.
- **The claim that an absent NDK silently skips the strip is XenDroid's comment**,
  not reproduced here. **It is a specific, checkable claim and it was not
  checked.**
- **No fork was audited for unstripped libraries.** The `.so` files this repo has
  disassembled were not checked for symbol tables, and **that would be a cheap
  pass** over the six already on disk.

## Sources

- XenDroid, `[Build] Pin the app module NDK so release packaging strips native
  libraries`
- `CLAUDE.md`, the standard row and the ABI census
