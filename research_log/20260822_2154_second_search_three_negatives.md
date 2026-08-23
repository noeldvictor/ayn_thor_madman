# Second search on three negatives. All three were wrong.

**Goal: apply the repo's own rule to the three "nobody has this" rows written
into `BACKEND_STANDARD.md`, before they are trusted.**

Session 2026-08-22 21:54. The rule: *a negative is worth recording only after a
second search with different words.* Six negatives in this repo have already
been reversed.

**Result: three for three. Two of them are already built in ARMSX2, which is the
fork this project already calls its seed.**

---

## Method

First search used the row's own words: `hotkey`, `storage`, `resolve`, `LRZ`.

Second search used the **actions and the mechanisms** instead of the names:

| Row | Second-search terms |
| --- | --- |
| one hotkey set | `fast.?forward`, `rewind`, `save.?state`, `quick.?save`, `slot`, `screenshot`, `keybind`, `HotkeyManager` |
| storage accounting | `StatFs`, `usableSpace`, `walkTopDown`, `sumOf`, `Formatter.formatFileSize`, `directorySize`, `cache.?size` |

Scoped to `**/src/main/java/**` for the Android-side question, because desktop
Qt hotkeys answer a different question than a handheld does.

---

## Reversal 1: hotkeys

**`armsx2-thor/ARMSX2/.../com/armsx2/ui/settings/HotkeysTab.kt`, 139 lines.**

Its own header comment:

> Dedicated controller-hotkey binding tab. **Pulled out of the Pad tab so the
> hotkeys (menu, quick save/load, slot cycle, texture-dump toggle, fast forward,
> resolution ±, achievements, close game) have a home that's easy to find.**
> Binding happens via `ControllerMappings.captureHotkey` — tapping a row arms it,
> and the next button seen by `MainActivityRuntime.dispatchKeyEvent` is bound to
> it.

Three things matter here:

1. **`ControllerMappings.SysHotkey` is an enum.** The universal action list this
   project specified is already enumerated in a fork, and the tab renders from
   `SysHotkey.entries`. **Take the enum.**
2. **The arming interaction is solved.** Tap a row to arm, press a button to
   bind. No text entry, no key names, works with a controller in hand.
3. **The reason for the tab is this project's own complaint.** It was split out
   of the Pad tab so hotkeys are "easy to find". That is the RetroArch failure
   being fixed independently, inside the fleet, before this repo existed.

azahar also has a full desktop hotkey subsystem — `hotkeys.cpp`,
`hotkey_monitor.cpp`, `configure_hotkeys.cpp`, `configure_hotkeys_controller.cpp`
— and Dolphin has `HotkeyScheduler.cpp`.

**What is actually true:** no fork has **one hotkey set across several
backends**, which is trivially true because no fork has several backends. **The
row overstated it into "nobody has hotkeys", which is false.**

---

## Reversal 2: storage accounting

Present in pieces in at least five forks.

| Fork | What it does |
| --- | --- |
| **melonDS-android** | **shows the filter disk cache size in the preference summary**, scanned off the main thread, with the path |
| melonDS-android | `shaderLibraryManager.installedSizeBytes()` rendered with `Formatter.formatShortFileSize` |
| **ARMSX2** | `directorySize()` = `walkTopDown().filter(isFile).sumOf(length)` |
| **ARMSX2** | **pre-install free-space check** with a helpful failure message |
| Cemu | `Formatter.formatFileSize` helper on the Compose side |
| azahar / Borked3DS | `FileUtil.getFreeSpace` |
| melonds_HD_2 | `NdsRomCache` cache size |
| GameNative | `StatFs` |

melonDS's is the closest to the product idea, because it is **in the settings UI
where a person looks**, not in a log:

```kotlin
val sizeMb = withContext(Dispatchers.IO) {
    cacheRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() } / (1024L * 1024L)
}
cachePreference.summary = getString(R.string.filter_disk_cache_summary_with_size,
                                    cacheRoot.absolutePath, sizeMb)
```

ARMSX2's free-space check is the pattern for install flows:

```kotlin
val needed = pack.sizeBytes * 2 + FREE_SPACE_SLACK
val free = runCatching { root.usableSpace }.getOrDefault(0L)
if (free in 1 until needed)
    return Outcome(false, "Needs ~${needed/1024/1024} MB free, ${free/1024/1024} MB available")
```

**What is actually true:** the **measurement** is solved several times over. What
nobody has is the **unified per-game breakdown by category with a rebuildable
flag**, and the rule that the UI states the cost before the action. **That is a
much smaller gap than "nobody", and it is an aggregation problem, not a
measurement problem.**

---

## Reversal 3, unplanned: ARMSX2 already ships a second-screen Presentation

Not searched for. Found while confirming the other two.

**`SecondScreen.kt`, 576 lines, plus `SecondScreenTiles.kt`, 131 lines.** It
targets this exact device by name:

> Utility panel on a SECOND display — **Ayn Thor**, the Retroid dual-screen
> add-on, or anything else Android reports as an extra display. Shows live stats
> and the actions you otherwise have to pause the game to reach.

**It carries three lessons the app shell needs, each already paid for.**

**1. Do not put Compose inside a `Presentation`.**

> Built from plain Views, not Compose, on purpose. A `Presentation` is its own
> Window with its own decor view, and a ComposeView inside one only works after
> the ViewTree lifecycle/saved-state owners are attached to that decor view —
> **get it wrong and it throws at inflate time, on hardware almost nobody testing
> this has.**

**2. A `Presentation` is not torn down when the activity stops.**

> a Presentation belongs to the app's window token but is NOT torn down when the
> activity stops, so **the panel stayed up on the second display while the user
> was off doing something else entirely** — reported, and it also meant a stale
> FPS reading sitting on screen. Driven from the activity's onResume/onPause.

**3. Re-attach on every resume, not only on a foreground change.**

> the activity can come back on a different display than it left on
> (**dual-screen handhelds let you move the app**), and refresh() is the only
> thing that re-picks the target. Idempotent.

It also registers a `DisplayManager.DisplayListener` for add and remove.

**`app/shell/` has a `Screen2Presentation` written without any of this.** Three
bugs are pre-diagnosed, one of which reproduces only on hardware.

**This also corrects the dual-screen row**, which credited melonDS and azahar.
**Three forks, not two**, and ARMSX2's is the one that matches the Thor's case:
a `Presentation` on the second internal panel showing app content rather than a
guest screen.

---

## The one negative that survives

**Resolve and LRZ reporting: still nobody**, and it was already established by a
read rather than a listing. No fork plans render passes; eden hardcodes
`LOAD_OP_LOAD` and `STORE_OP_STORE`; nobody merges passes, uses input attachments
or resolves MSAA on-chip. **Nothing counts or reports a resolve.**

---

## Consequence

`BACKEND_STANDARD.md` said the three rows with no prior art "are the work" and
everything above them is conversion. **That was wrong for two of the three.**

The corrected position:

| Row | Actual state |
| --- | --- |
| one hotkey set | **ARMSX2 has the enum and the binding UX.** Generalise it |
| storage accounting | **measurement solved five times.** The gap is aggregation and the cost-before-action rule |
| resolve and LRZ reporting | **genuinely nobody.** This one is the work |

---

## Method note

**This is the seventh, eighth and ninth reversal in this repo, and the rate has
not improved: 9 of 10 negatives checked have been wrong.**

Two further observations:

- **Both reversals were in ARMSX2**, the fork this project already calls its
  seed and claims to have read most carefully. **Being the best-documented fork
  is not the same as being read.**
- **The searches that failed used the feature's name. The searches that
  succeeded used the actions.** "Hotkey" missed nothing here, but "storage"
  missed `walkTopDown` and `usableSpace` entirely, because nobody names a
  function after the category it belongs to.

**Rule to add: search for the mechanism, not the category.**
