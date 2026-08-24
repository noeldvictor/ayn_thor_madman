# There is an ARM64 execution environment on this machine, and I checked the wrong shell

**Goal: apply rpcsx's "re-read your own blockers" rule to `DEVICE_QUEUE.md`,
which holds 27 entries all waiting on a device somebody else is using.**

**Device-free. Three `command -v` calls.**

## Entry 13's blocker is wrong

**Entry 13 is "the number the whole IR question turns on"** — instruction
inflation, which the literature says predicts slowdown by regression. Its stated
blocker:

> *"The a64 backend only runs on ARM64. A desktop xenia build uses the x64
> backend, which would measure the wrong emitter. **There is no ARM64 host here
> other than the Thor.**"*

**The first two sentences are true.** `ppc_testing_main.cc` selects the a64
backend inside `#elif XE_ARCH_ARM64`, so on an x86-64 host it is not compiled in
at all.

**The third is false.** Checked in WSL Ubuntu on this machine:

```
/usr/bin/qemu-aarch64
/usr/bin/aarch64-linux-gnu-g++
/usr/bin/ccache
```

**`aarch64-linux-gnu-g++` is the exact compiler xenia's `--linux-arm64` cross
build names** — `premake5.lua` says so in a comment. **And `qemu-aarch64` is the
runner xenia's own ABI-poison-test proposal already assumes**, down to the
warning that ORC segfaults under qemu-user.

> **The route is: cross-build the PPC test harness for ARM64 Linux, run it under
> `qemu-aarch64` with `--cpu=arm64` and the disassembly flags, count.** No Thor.

**What is NOT established**, and it is the whole remaining risk: **whether that
cross build succeeds, and whether the harness runs under qemu-user.** Those are
the next steps, not a conclusion. **A route existing is not a route working.**

**And one thing the entry says stays true either way**: a desktop x64 build
measures the wrong emitter. **The correction is that "desktop" and "x64" are not
the same thing here.**

## And I made the same mistake thirty minutes ago

**I ran `which ccache` in Git-Bash, got nothing, and wrote that neither ccache
nor sccache is "on PATH here", adding ccache to the known-host-tools list as
missing.**

**`/usr/bin/ccache` exists in WSL on this machine.**

> **I checked one environment and generalised to the machine.**

**That is the "positive control on the WRONG CHANNEL" trap, which I recorded from
rpcsx earlier today** — *"`adb shell` and the app are different uids in different
SELinux contexts; a capability confirmed on one says nothing about the other."*
**Git-Bash and WSL are different environments, and a tool absent in one says
nothing about the other.** Third instance of that rule today, and this time it
was mine.

**The ccache conclusion is only PARTLY wrong, and the distinction matters.** The
Android NDK build runs under **Windows** Gradle, so a WSL ccache does not serve
it — **the recipe still needs a Windows-side ccache.** What was wrong was the
scope of the sentence, not the recommendation.

## What this changes

- **Entry 13 is no longer device-blocked.** Its blocker becomes "cross-build and
  qemu-user, both tools present, neither tried".
- **The known-host-tools list needs an environment column.** "Present on this
  machine" is not a fact; **"present in the environment that runs the build" is.**
- **The same re-read is owed to the other 26 entries.** One was checked and one
  was wrong.

## Limits

- **Three `command -v` calls.** Nothing was built, cross-compiled or run under
  qemu.
- **`uname -m` in WSL reports `x86_64`**, as expected — the ARM64 capability is
  emulation, not hardware. **Timing measured under qemu is meaningless**;
  **inflation is a static count of emitted instructions, which is why this route
  serves entry 13 and would not serve a benchmark.**
- **No claim that the cross build works.** xenia's `--linux-arm64` path exists in
  `premake5.lua` and has not been exercised here.
- **WSL path translation failed** on the first attempt with a `D:\` drive in the
  environment; the check had to run from a translatable directory. **A future
  attempt will meet the same thing.**

## Sources

- `DEVICE_QUEUE.md` entry 13
- xenia `premake5.lua`, `src/xenia/cpu/ppc/testing/ppc_testing_main.cc:205-232`
- `research_log/20260825_1950_ccache_and_the_setting_that_makes_it_hit.md`
