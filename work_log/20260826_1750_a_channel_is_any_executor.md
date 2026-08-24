# A channel is any executor, not just a uid

**Goal: the wrong-channel rule has now caught me twice in one session, in forms
that have nothing to do with `adb`. Generalise it where it lives. This proposes
no lever.**

**No device. One rule sharpened.**

## Three channels, and only the first was recorded

**The rule entered this repo as an ANDROID observation**, from rpcsx: *"`adb
shell` is uid 2000 in a different SELinux context from the app."* **Two more
turned up this session, and neither involves a device at all.**

| Channel | The mistake | The consequence |
| --- | --- | --- |
| **uid / SELinux** | checked a sysfs file from `adb shell`, concluded it about the app | rpcsx's — *"proves nothing about the app"* |
| **environment** | `which ccache` in Git-Bash returned nothing | **wrote that the machine lacks it; `/usr/bin/ccache` exists in WSL** |
| **regex engine** | validated a pattern with Python's `re` | **`git grep -E` rejects it; the detector returned ZERO for every fork and the self-test said `OK`** |

> **A "channel" is any EXECUTOR** — a uid, a shell, an interpreter, a regex
> engine, a compiler. **Validate with the thing that will RUN it.**

## Why the third is the worst

**The first two produced a wrong ANSWER. The third produced a wrong ANSWER FROM A
TOOL BUILT TO PREVENT WRONG ANSWERS**, and reported clean while doing it.

**And it defeated a self-test written the same hour**, for the express purpose of
proving that guard could fire. **The self-test fired correctly on every input it
could construct in Python; the failure was outside Python.**

## The cheap form of the rule

**Before trusting a control, ask what actually executes the thing under test**,
and make the control go through that:

- the app, not the shell -> **`run-as <package>`**
- the build's environment, not yours -> **check inside it**
- **`git grep -E`, not `re.compile`** -> **run the pattern through `git grep`**

**The fix in each case was one line and none of them was obvious before the
failure.**

## And the exemption I added two hours ago did not fire on this file

**`dead-levers` warned on this very log, which opens with "This proposes no
lever."** The exemption was there. It did not match.

> **The phrase had WRAPPED across a line break** — *"This proposes
no lever."* —
> and the pattern matched a literal space.

**Markdown wraps at about 80 columns, so ANY multi-word magic phrase eventually
splits.** The exemption was correct and unreachable for a reason that has nothing
to do with its logic.

**Fixed to be whitespace-tolerant** (`proposes\s+no\s+lever`), with two
controls: **a wrapped disclaimer now satisfies it, and a bare proposal still
fires.**

> **Fourth guard defect today, and the third whose failure mode was "the rule
> exists and cannot fire."** A backspace byte, a lookahead the engine rejects,
> a fixture in an excluded directory, and now a phrase split by word wrap.
> **Every one was invisible in the source.**

## Limits

- **Three instances, two from one session, all mine or rpcsx's.** No claim about
  frequency.
- **The generalisation is a framing**, not a new detection method: **nothing
  automatically finds a wrong-channel control.**
- **The `bug_class_sweep` self-test now uses `git grep`**, but the other tools'
  self-tests were **not re-examined** for the same trap.

## Files

- `shared_layer/MEASUREMENT.md`
