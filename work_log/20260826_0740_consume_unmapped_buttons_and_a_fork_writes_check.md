# Consume unmapped gamepad buttons, and answering the fork-writes warning

**Goal: finish XenDroid's input cluster, and answer a `supervise` warning that has
stood all session.**

**Device-free. No device used.**

## The last input requirement, and it is vendor-specific in our favour

> *"Unmapped controller buttons (never `BACK`): **unhandled gamepad input is what
> OEM overlays latch onto.**"*

**A gamepad key event the app does not consume goes up the stack**, and vendor
software bound to gamepad combinations **pops its own UI over the running game.**

**This bites this project harder than it bit XenDroid.** The Thor is a **vendor
handheld** — this repo already reads AYN's own FanBase thermal readout, so AYN
ships on-device software — **and a gamepad-first app generates nothing but
gamepad events.** The failure would present as a bug in ours.

**Two details worth copying exactly:**

- **Never consume `BACK`.** Consuming it traps the person in the game.
- **Test `SOURCE_GAMEPAD` OR `SOURCE_JOYSTICK`.** A pad reports one or both, and
  **checking only one misses devices.**

**Recorded in `app/SCREENS.md` with the other five input requirements**, before
the input layer exists.

## The fork-writes warning, answered

**`supervise` has warned about modified forks all session**, and the check's own
message says *"confirm these are the user's own, not this session's."* **A warning
nobody answers is a warning nobody reads.**

**Checked:**

| Fork | Modified files | Verdict |
| --- | --- | --- |
| **azahar** | 14 — `cubeb_sink.cpp`, `thread_queue_list.h`, `vk_pipeline_cache.*`, `vk_resource_pool.*`, `vk_shader_disk_cache.*` | **not mine** |
| **rpcsx** | 1 — `SPULLVMRecompiler.cpp` | **not mine** |

> **Every file is in a subsystem I only READ from this session** — `git grep`,
> `git show`, `sed -n`. **None of those writes.**

**And the azahar set is recognisably its own current work**: the pipeline cache,
resource pool and shader disk cache are **the exact subsystems whose rejections I
read out of its `AGENTS.md` today.** Somebody is working there.

**One observation for the check itself**: `git diff --stat` on that fork produced
**fourteen lines of CRLF warnings before one line of output**, which is a Windows
artefact that makes the confirmation harder than it should be. **Not fixed —
noted.**

## Limits

- **The input requirement is recorded, not implemented.** `app/shell/` still has
  no input layer.
- **The OEM-overlay claim about the Thor specifically is inference** — from AYN
  shipping on-device software, not from observing an overlay. **XenDroid's claim
  is about Android handhelds generally.**
- **The fork check is a `git status` read**, which shows modification but not
  authorship. **The reasoning that they are not mine is that I ran no writing
  command against those trees.**

## Files

- `app/SCREENS.md` — a sixth input requirement
