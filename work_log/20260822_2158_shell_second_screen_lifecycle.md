# Fix the shell's Screen-2 lifecycle from ARMSX2's lessons

**Goal: apply the three second-display lessons found in ARMSX2 to
`app/shell/`, before the layout work goes further.**

Session 2026-08-22 21:58. Source:
[`../research_log/20260822_2154_second_search_three_negatives.md`](../research_log/20260822_2154_second_search_three_negatives.md).

**Built and verified. Not run on the device.**

---

## What was wrong

`MainActivity` attached its `Presentation` in `onStart` and dismissed it in
`onStop`, picked the target with `displayId != Display.DEFAULT_DISPLAY`, and
registered no display listener.

| # | Defect | Consequence |
| --- | --- | --- |
| 1 | `onStart` / `onStop` | A `Presentation` is not torn down when the activity merely pauses. Screen-2 keeps showing stale content while the person is elsewhere. **ARMSX2 shipped this bug and fixed it.** |
| 2 | target is "not `DEFAULT_DISPLAY`" | **If the activity is moved to Screen-2, the panel is placed on the display the activity is already on.** The display to avoid is the host display, not display 0 |
| 3 | no `DisplayManager.DisplayListener` | A display appearing or disappearing at run time is never noticed |
| 4 | attach not idempotent | A second call builds a second `Presentation` and leaks the first |
| 5 | no re-render after attach | A fresh `Presentation` starts blank, so Screen-2 goes empty after any re-attach |

Defect 2 is the one that was not in ARMSX2's comments. It follows from them:
ARMSX2 says the activity "can come back on a different display than it left on",
and once that is true, `DEFAULT_DISPLAY` is the wrong thing to exclude.

## What changed

`app/shell/app/src/main/java/com/aynthor/shell/MainActivity.kt`.

- Moved to `onResume` and `onPause`.
- Target is now `displayId != display?.displayId`, the activity's own display.
- Registered a `DisplayManager.DisplayListener` on attach, unregistered on
  pause.
- `attachSecondDisplay()` is idempotent: it tracks `presentationDisplayId` and
  returns early when the panel is already on the right display.
- After a new `Presentation` shows, it re-renders `screen2Title` and
  `screen2Lines`.
- Dismissal is centralised in `dismissPresentation()` and wrapped in
  `runCatching`, because a `Presentation` self-dismisses when its display goes
  away and the stale reference would otherwise throw.

**Lesson 1 needed no change.** The shell already used classic Views inside the
`Presentation` and documented why, reaching ARMSX2's conclusion independently.

## Verification

```
gradlew.bat compileDebugKotlin --rerun-tasks
BUILD SUCCESSFUL in 8s
app-debug.apk   61,099,608 bytes   2026-08-22 21:58
```

`--rerun-tasks` was used because an incremental build reported `UP-TO-DATE` and
proved nothing.

## What is not verified

**Everything that matters.** This is a compile, not a run.

Untested on the device:

- whether the panel now disappears when the app is backgrounded
- whether moving the activity between the two panels re-targets correctly
- whether the re-render leaves Screen-2 showing the right content
- whether the listener fires at all on two internal displays, which may never
  add or remove

**Add these to the device queue.** The last one may show the listener is
unnecessary on this hardware, in which case it should be removed rather than
kept for symmetry.
