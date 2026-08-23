# Going into the game code: it is the highest-ceiling lever, and it is proven

**Goal: answer four questions asked together — can we go into game code, can we
fix what we find, can we super-optimise per game, and do other emulators do
this?**

No device. Reading only.

## Short answers

| Question | Answer |
| --- | --- |
| Can we go into game code? | **Yes. xenia already does** — `SetupExtern` installs HLE intercepts over guest functions |
| Can we fix what we find? | **Yes. That is what a patch is**, and ~3,600 patch files already ship in this fleet |
| Can we super-optimise per game? | **Yes, and it is the most-tried lane here — with 39 recorded wins** |
| Do other emulators do this? | **All of them.** In nine incompatible formats |

## 1. Going into game code is already how the hard problems get solved

**It is also the answer to the API-boundary problem found earlier today.** You
do not have to reconstruct the API that PM4 erased — **you replace the guest code
that would have called it.**

xenia's verdict document says exactly this:

> **Byte signatures can recreate an API boundary**, as this fork's `SetupExtern`
> mechanism demonstrates, but signatures will vary with XDK version,
> compiler/linker output, inlining, title updates, and game wrappers. **A
> signature database is feasible, not magically universal.**

**And the machinery is real.** Twelve HLE cvars exist for one title:
`cpu_hle_intercept_addrs`, `cpu_d3d_hle_signatures`, `cpu_hle_ring_writer_addr`,
`cpu_hle_binonce_addr`, `cpu_hle_tiling_replay_addr`, `gpu_bd_hle_drop_resolve`,
`gpu_bd_hle_present_decoupled` and more.

## 2. The lane produces wins, and I nearly got this wrong

**Before checking the ledger I was about to write that per-game HLE mostly
failed. It does not.**

| Verdict | Count |
| --- | --- |
| **WIN** | **39** |
| DEAD | 33 |
| FLAT | 8 |
| CONFOUNDED | 5 |

**77 ledger entries touch HLE alone.** So this is simultaneously the most-tried
lane in the fleet and a lane with a **roughly even win rate** — far better than
the manual-derived CPU levers, which are **thirteen for thirteen refuted.**

> **Per-game analysis beats manual-derived micro-optimisation by a wide margin
> in this fleet's own record.**

## 3. Every emulator ships patches, and the fleet ships about 3,600 files

| Fork | Files | Format |
| --- | --- | --- |
| **rpcsx** | **2,676** | rpcs3 YAML and `.ncl` |
| **ARMSX2** | **591** | `pnach` |
| eden | 153 | content patches and mods |
| melonDS | 62 | AR codes |
| **GameThor** | 43 | **typed host-config fixes, as code** |
| azahar | 39 | cheats |
| Vita3K | 20 | cheats |
| **Cemu** | 15 | graphic packs — **two are this project's own**, Star Fox Zero infinite-life and super-shot |
| xenia | 1 | `.patch.toml` |

**This is the largest single asset in the fleet**, it is community-maintained,
and **it is in nine incompatible formats behind nine engines.**

**And "patch" still means four different things**, which this repo has recorded
twice: a **content patch** (updates, DLC), a **code patch** (modify guest
instructions), a **file mod** (replace assets), and a **host config fix**
(GameThor's 43, which touch no guest memory at all).

## 4. So what is actually missing

**Not the idea, and not the mechanism. Two things:**

**One engine across eight systems.** The engines are already read and ranked:
**Cemu's `GraphicPack2Patches` is a symbolic assembler with a relocating linker**
— 1,331 lines, two input formats, code caves, symbols resolved against a live
module, and `ResolvePresetConstant`, which feeds a **user setting** into a patch
as a value. **xenia's `.patch.toml` is the better authoring surface**, with
`emit_patch_toml.py` writing a patch straight out of Ghidra. **Take the engine
from one and the format from the other; neither needs writing.**

**And the analysis has to become affordable.** Per-game work has always been
limited by how many people will sit in Ghidra. **That is precisely what
[Foundation](../CLAUDE.md#foundation) point 3 says agents are for**, and the
fleet has already built the lanes: `thor-ghidra-static-lane` with its
**precompute-the-halt-map-before-the-fault** trick, `vita3k-ghidra-escalation`,
and xenia's Ghidra-to-TOML emitter.

## 5. The one caution, and it is the same one all day

**Per-game work is only as good as the bottleneck it aims at.** xenia's own
verdict:

> If unrelated guest CPU work already exceeds 33.3 ms, **A is a detour and a
> perfect renderer cannot deliver 30 fps.**

**And the fleet's own measurement rule**: *do not pick a lever from a manual —
profile first.* **Thirteen manual-derived predictions have been refuted here.**

**So the order is: profile, then analyse the game, then patch.** The 33 DEAD
entries in that ledger are mostly what happens when the order is reversed.

## What this suggests, concretely

1. **Build the shared patch engine.** Cemu's assembler, xenia's format. It is
   the highest-value shared subsystem after the ones already extracted, and
   **~3,600 existing files are its input corpus.**
2. **A patch carries its intent and its evidence.** Speed, fix, or change — and
   a speed patch carries the scene, the before and the after. **A speed patch
   without a measured reason is a guess**, and this fleet has 33 of those
   recorded as DEAD.
3. **Bind a patch to the right build.** Three forks solve this three ways —
   Cemu matches a loaded `RPLModule`, rpcsx hashes, eden uses a 32-byte
   `BuildID`, and **rpcs3's corpus is keyed by `PPU-<hash>` across 183 distinct
   hashes.** That is `DumpId` from `app/GAME_DATA.md`, in production.
4. **Point the agent at it.** The paused agent loop reaches the scene; the
   Ghidra lane finds the function; the emitter writes the patch. **Every piece
   exists in a different fork.**

## Limits

- **The verdict counts are from one fork's ledger**, and a ledger records what
  somebody chose to write down.
- **"~3,600 files" counts files, not distinct titles.** rpcsx's 2,676 includes
  many per-title variants.
- **No claim that any specific patch would help on the Thor.** None of this is
  measured here.
