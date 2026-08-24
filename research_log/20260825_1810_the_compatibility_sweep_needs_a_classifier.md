# The compatibility sweep needs a classifier, xenia has one, and its two most important classes say the harness failed

**Goal: apply the "start at the fork's `AGENTS.md`" rule to xenia, which has 553
research documents and where this repo has read about a dozen.**

**Its index is 128 lines and it changed what I thought the fork was working on.**

## First, a correction to how this repo reads xenia

> *"Product priority: **Android usability and compatibility on AYN Thor**."*
> *"**Blue Dragon full-speed work is paused** unless the user explicitly
> restarts it."*

**This repo has built extensively on Blue Dragon material** — the frame anatomy,
the rearch refutation, the ~100 ms fragment-overdraw decomposition. **None of
that is invalidated. But the fork's ACTIVE knowledge has moved**, and its own
index says so.

**And it lists seven current notes this repo has never opened**, all on the
product side: Android launch and controller mapping, an OSD status list, a
**recent-games status list**, a title-geometry fix, a remote-debug test rig, a
**game status classifier**, and native guest-crash status.

> **The fork whose measurement discipline this project copies is now spending
> its time on the half of the problem this project calls the product.**

## The classifier, and why it is the missing half of a specified feature

`CLAUDE.md` specifies a **boot and compatibility sweep** — *"launch every game in
the library headless, record how far each reaches"* — and `STATIC_TRIAGE.md`
answers *"will it boot"* **before** running, from the dump's import table.

**Nothing answered "what happened" after.** `tools/thor/thor_android_game_status_
report.ps1` does, from a logcat, emitting key/value text:

```text
classification=project_sylpheed_guest_heap_rtlraiseexception
reason=Project Sylpheed title ID plus heap failure and RtlRaiseException
title_id=535107D4        media_id=2D2E2EEB
rtl_raise_exception=702DF8D0(E06D7363)
crash_pc=8245BDEC        crash_function=0x8245BD80-0x8245BE64
base_heap_release_count=1  physical_heap_count=2
android_runtime_count=0    native_signal_count=0
```

**Seven classes**, and the taxonomy is the design:

| Class | What failed |
| --- | --- |
| `android_or_native_process_crash` | **the app** |
| `xenia_guest_crash` | **the emulator** |
| `guest_heap_rtlraiseexception` | **a named compatibility class** |
| `project_sylpheed_guest_heap_rtlraiseexception` | **that class, for one title** |
| `launched_no_crash_marker` | **the game** — started, no fatal marker |
| **`no_xenia_runtime_evidence`** | **the HARNESS. The emulator never ran** |
| `unknown` | — |

## The two classes that matter most are about the instrument

> **`no_xenia_runtime_evidence` against `launched_no_crash_marker` is the whole
> value.** The first says the harness failed; the second says the game did.
> **Without that split, a broken launcher reads as a broken game, for every
> title in the sweep.**

**That is "prove the instrument can return non-zero" applied to a compatibility
sweep**, and it is the same failure as xenia's own HLE intercepts reading
`count=0` for weeks. **A sweep is an instrument, and its most likely failure is
that it measured nothing.**

**And the taxonomy REFINES rather than flattening.** A per-title class sits under
a general one, so a sweep can report "12 titles in the guest-heap class" while a
single title keeps its specific entry. **A flat enum forces a choice between
those.**

## What to take, and what to add

**Take the shape**: a log in, key/value out, **written beside the capture in the
same packet** so a result is never separated from its evidence. `AGENTS.md`
requires that packet — *"screenshot/video, logcat, build or APK hash, cvars/
settings, and a reproducible path"* — **which is a proof-packet definition this
repo's measurement discipline does not have.** It names what numbers to report
and not what must accompany them.

**Two additions this project can make that xenia cannot:**

- **`GuestActivity` splits `launched_no_crash_marker`.** A backend that declares
  its activity state distinguishes **stalled** from **loading** from **sitting
  in a movie**, which a log alone cannot. That class is currently the vaguest of
  the seven.
- **The static triage predicts, the classifier confirms.** A title whose import
  table says it needs 296 unimplemented functions and then returns
  `launched_no_crash_marker` **confirms the prediction cheaply**; one that
  crashes where triage said it was safe **is the interesting case**, and it is
  how the triage gets calibrated.

**And one operational rule from the same note, which is easy to get wrong:**
*"Because this was an opportunistic current-log screenshot rather than a
controlled repro, it intentionally did not clear logcat first; stale prior
markers remained visible."* **A classifier over an uncleared log can classify a
previous run.** `AGENTS.md` states the matching rule from the other side:
**capture before force-stopping or clearing.**

## Limits

- **The classifier was read, not run.** It is PowerShell over a logcat and needs
  no device, but it is xenia-specific: the classes are Xenia's markers.
- **Its class list is seven entries deep today** and the note's own "Next"
  section says exposing them in the launcher is undecided, pending *"how the app
  should safely ingest host-side evidence"*.
- **Six of the seven current notes named in `AGENTS.md` remain unread.**
- **Nothing measured, no device.**

## Sources

- xenia `AGENTS.md`
- xenia `docs/research/20260527-193200-android-game-status-classifier.md`
- xenia `tools/thor/thor_android_game_status_report.ps1` (named, not read)
- `shared_layer/STATIC_TRIAGE.md`, `app/shell/GuestActivity.kt`
