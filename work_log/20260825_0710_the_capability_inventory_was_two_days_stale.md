# The capability inventory was two days stale, and fourteen capabilities were missing

**Goal: `CLAUDE.md` calls the capability inventory the reality check for the app
shell and says to read it before building anything. It was last consolidated on
2026-08-23. Check whether it is current.**

## The check

**Searched the inventory for each capability's own vocabulary**, not for a fork
name:

| Term | Occurrences in the inventory |
| --- | --- |
| `GSGPUProfile`, `DriverBug` | **0** |
| `hardcore` | **0** |
| `TimeStretcher`, `SoundTouch` | **0** |
| `shader_float16` | **0** |
| `dynamic_rendering_local_read` | **0** |
| `autoSaveOnExit` | **0** |

**Six vocabularies, all zero.** The inventory had none of the 2026-08-24 or
2026-08-25 findings.

## Why this matters more than it looks

**A stale capability inventory does not fail loudly.** It answers *"which fork
already has this?"* with a confident, incomplete answer — **which is exactly the
failure this project has recorded eighteen times** under "read before you claim".

**A row that is ABSENT reads the same as a capability that does not exist.**
That is the same shape as this week's silent-no-op finding: **absence of output
is not a result.**

## What was added

**Fourteen rows**, each read rather than listed: ARMSX2's driver bug database,
Vita3K's device-layer capability table, the fp16 census, melonDS's rewind,
the integrity mode, auto-save on exit, fast forward as a time scale, azahar's
Eco Turbo and its below-100% audio stretch, its SoundTouch work, XenDroid's
in-pass resolve series and upload hoist, `FLAG_KEEP_SCREEN_ON`, and the
constant-VA arena.

**Plus one whole-fleet property**: the AAPCS `v8`-`v15` contract, correct at all
four emitter boundaries read.

## Two things the update revealed

**XenDroid holds three capabilities and this repo lists it only as a reference
clone.** It supplies the in-pass EDRAM resolve series, the upload hoist and
`FLAG_KEEP_SCREEN_ON` — **one of which is a three-line fix for a bug this
project's own gamepad-first requirement guarantees.**

**azahar's audio work was invisible to earlier surveys because they searched for
BACKEND NAMES** — Oboe, cubeb. Its time stretcher, its unity bypass and its
below-100% stretching are none of those. **Same trap as the frame-pacing survey
that searched for Swappy and missed Cemu's four-part implementation.**

## Limits

- **This is an additive update, not a re-survey.** The pre-2026-08-23 rows were
  not re-read, and any of them may have drifted.
- **Every new row is sourced from another fork's code or documents**, read this
  week. **None was built or run here.**
- **The XenDroid rows are xenia's reading of XenDroid's commits**, not mine — no
  XenDroid commit was opened.
- **No quality value above `shipped` is claimed for anything**, and `shipped`
  here means "works in that fork", not "measured by us".

## Files

- `capability_inventory.md`
