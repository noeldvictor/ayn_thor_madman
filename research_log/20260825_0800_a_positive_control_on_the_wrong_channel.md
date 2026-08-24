# A positive control on the wrong channel, and two sections that analysed code which never runs

**Goal: read `codegen.md`'s largest lever — *"the JIT is scheduled for a core
this device does not have"* — following the rule to read a document's own
corrections first. It has two, stacked.**

**The finding survives in a changed form. The three rules that fall out are worth
more than it.**

## The chain

**The claim.** `JITLLVM.cpp` runs the detected CPU name through
`sanitize_android_arm64_llvm_cpu()`, which downgrades an Armv9 core to
**`cortex-a78`** when LLVM would enable SVE and HWCAP reports none. **`-mcpu`
selects LLVM's scheduling model**, so *"instruction selection and ordering for
**all** JIT output are tuned for the wrong microarchitecture"* — and **the
profile puts 54% of cycles in that output.** Two sections of analysis followed.

**Check 1 passed.** Verified off-device: `-sve,-sve2` in the feature list **does**
suppress SVE for `-mcpu=cortex-a715`, so the downgrade is not load-bearing.

**Check 2 refuted the premise.** The override was built, installed and enabled —
**and the log still said `cpu=cortex-a78`. Neither warning fired.**

> **The SVE branch is never reached.** The value comes from a **hardcoded default**
> taken when `get_cpu_name()` returns empty.
>
> *"So the preceding two sections analysed a downgrade that does not happen.
> **This is checklist item 4 — establish reach before optimality — violated in
> the same session the checklist was written.**"*

> **"Reading a function and reasoning about its logic is not evidence that it
> runs."**

**Then corrected again**, because the explanation for the empty name was **"wrong
twice over"**: `get_cpu_name()` does not read `/proc/cpuinfo` at all — it reads
`midr_el1` per core already — and **the app can read that file.**

## Rule 1: a positive control on the wrong channel proves nothing

> *"Verified through **`run-as net.rpcsx.easy`**, not just an adb shell — the file
> is `-r--r--r--`, world readable, and returns `0x00000000411fd4e0` for cpu7. **My
> earlier sysfs check was run as *shell*, which proves nothing about the app;
> that is the "positive control on the wrong channel" trap this project has now
> hit three times.**"*

**This repo already has one instance of the same class and did not name it:**
*"Headless `adb shell perfetto` with KGSL ftrace events returns EMPTY on the
retail Thor. **Shell is uid 2000**, it can read tracefs but cannot enable
events."* **Same mismatch, opposite direction** — there the shell could do less
than expected, here it could do more.

> **`adb shell` and the app are different uids in different SELinux contexts.**
> A capability confirmed on one says nothing about the other. **Use
> `run-as <package>` when the question is about the app.**

**And it composes with the rule this session already adopted** — *prove the
instrument can return non-zero.* **A positive control also has to run where the
code will.**

## Rule 2: establish reach before optimality

**This repo has "ask whether the code is executed before asking for a better
instruction", attributed to two forks.** This is the same rule at a different
altitude and with a sharper failure: **two sections of correct analysis about a
branch that never executes.**

> **The tell was cheap and available: build the change, enable it, and read the
> log for the warning it should have printed.** It printed neither the old
> warning nor the new one.

**`DID_IT_APPLY.md` has twelve mechanisms for a setting that never takes effect.
This is the analytical twin: a code path that never takes effect, reasoned about
in detail.** The detector is the same — **instrument the thing you changed** —
recorded here this session from a different fork.

## Rule 3, and a device fact: every core identifies itself

```
/sys/devices/system/cpu/cpu0/regs/identification/midr_el1  0x411fd461  part 0xd46  Cortex-A510
/sys/devices/system/cpu/cpu5/regs/identification/midr_el1  0x412fd470  part 0xd47  Cortex-A710
/sys/devices/system/cpu/cpu7/regs/identification/midr_el1  0x411fd4e0  part 0xd4e  Cortex-X3
```

**World-readable, per core, no root**, and the part table already maps
`0xd46` A510, `0xd47` A710, **`0xd4d` A715**, `0xd4e` X3.

**Two uses beyond the JIT.** This repo records the Thor's 1+4+3 layout and which
index is which **from one fork's header**; `midr_el1` is the primary source and
costs one read. **And ARMSX2 claims the Thor line has an 865 variant** — a
question this repo left open as a scope decision. **MIDR identifies the CPU part
on any Thor**, so the SKU question is answerable on a second device without
guessing.

## The finding that survives, and it is a third answer to an old question

> **"A single `-mcpu` is wrong for a big.LITTLE target no matter which one is
> chosen."** SPU threads are pinned to A710/A715 while RSX runs on the X3, and
> **a JIT could reasonably pick its target per thread class.**

**`CLAUDE.md` asked this and answered it twice already**, and this is the third:

| Answer | Cost |
| --- | --- |
| **tune for the X3 and place hot code there** (this file, original) | needs affinity control **two forks do not have** |
| **the all-core gate** (azahar) | paid in **rejected candidates** |
| **per-thread-class `-mcpu` in the JIT** (rpcsx) | needs **MIDR detection and per-class code caches** |

**All three beat the original, and they are not exclusive**: the gate decides
which lowerings are acceptable at all; a per-class target decides scheduling for
threads already pinned by class.

**And the sequence rpcsx leaves for whoever takes it is the right one:** read
MIDR, map part numbers to LLVM CPU names, **keep the `-sve,-sve2` negation
(verified sufficient)**, and **only then A/B** — *"the measurement remains the
arbiter — this section has already been wrong once."*

## Limits

- **Everything here is rpcsx's**, including the MIDR values, which are from its
  device — the same Thor model, not re-read here.
- **The 54%-of-cycles figure is its profile on its title.**
- **Whether a per-thread-class JIT target helps is unmeasured**, and the document
  says so: nine predictions of that shape have been refuted there.
- **`run-as` requires a debuggable build**, which this repo already records as a
  build-configuration requirement for the per-context GPU counter route.

## Sources

- rpcsx `docs/arm64/codegen.md:595-740`
- `research_log/20260824_2350_the_all_core_gate.md`
- `research_log/20260824_2140_nosve_clears_the_backend_not_the_frontend.md`
