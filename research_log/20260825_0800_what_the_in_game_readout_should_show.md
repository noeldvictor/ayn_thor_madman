# What the in-game readout should show, and an eleventh way a switch does nothing

**Goal: answer `app/SCREENS.md` open question 4 — what does the performance
readout show by default, when cross-run numbers are `CONFOUNDED` and a naive fps
counter invites exactly the wrong conclusion.**

**The fleet has the answer and it is not one number.**

## One number labelled "FPS" is ambiguous on a console

**ARMSX2 shows three speed numbers and they mean different things:**

| Field | What it counts |
| --- | --- |
| **FPS** | frames the graphics synthesiser actually drew |
| **VPS** | **vsyncs per second — the guest's own refresh** |
| **Speed** | percent of nominal |

**A game that renders at 30 into a 60 Hz display reads 30 FPS and 60 VPS, and
neither is wrong.** A single number labelled "FPS" collapses them, and which one
it collapses to decides whether the user concludes the emulator is slow or the
game is.

**`PerformanceMetrics` also already tracks `GetMinimumFrameTime` and
`GetMaximumFrameTime`**, which is exactly what `MEASUREMENT.md` says to report
instead of a mean — **the range is already computed and the OSD shows an
average.**

**And Vita3K's tooling has `--truncated_mean`, defaulting to the 90th
percentile.** Two forks already know the mean is the wrong statistic; **neither
puts that knowledge in the readout a person looks at.**

## So the default readout, decided

**Three lines, and none of them is a bare "FPS".**

1. **Guest rate and host rate together**, labelled as such. The backend declares
   which is which; the app does not guess.
2. **Frame time as a RANGE**, not a mean — the `[min..max]` rule this project
   already applies to every measurement it publishes. A mean hides exactly the
   judder that a stable 30 does not have.
3. **Thermal and power state**, because on a handheld a number without a
   temperature beside it cannot be compared to the same number ten minutes
   later.

**What NOT to show by default: a single large fps counter.** It is the field
most likely to be photographed and quoted, and it is the one this project's own
measurement rules say is untrustworthy across runs.

**The full ARMSX2 field list is worth taking as the OPTIONAL set** — 19 toggles
including resolution, CPU, GPU, GS stats, frame times, hardware info, inputs,
patches and texture replacements. **The default is the decision; the list is
not.**

## And the eleventh instance of "the switch does nothing"

**`ImGuiOverlays.cpp:286`, in ARMSX2's own words:**

> *"When every perf line is off, draw nothing and return **before** any draw
> call, so a line string cached before a pause can't linger on screen (**the
> rebuild block below is skipped while the VM is paused, which is why toggling
> in the menu looked inert**). Packed rather than a chain of ors because the
> shrink-to-fit below needs to notice **the set changing, not just emptying**."*

**Two distinct defects in one comment.**

- **The rebuild runs only while the VM runs, and the menu is only open while
  paused.** So every OSD toggle looked inert at the exact moment a person used
  it.
- **The invalidation noticed the set becoming empty and not the set changing**,
  so turning one line off and another on left the old text.

> **A setting reachable from an in-game menu must take effect WHILE PAUSED,
> because paused is the only state in which that menu is open.**

**That is a general rule for screen 3 and it is not in `SCREENS.md`.** It is the
eleventh mechanism for `DID_IT_APPLY.md`, and the first that is a **rendering
cache** rather than a configuration store: the live path exists, and it is
skipped in precisely the state where the change is made.

## What this does not settle

- **Which number is "guest rate" is per backend.** PS2 VPS is a vsync count;
  a DS or 3DS guest has two screens; a Wii U game may drive a GamePad screen at
  a different rate. **The backend declares its rates; the app labels them.**
- **Nothing here is measured.** No readout was built or timed, and the OSD
  itself costs frames — this project's own rule says screen-2 draws on change,
  and the same question applies to an overlay drawn every frame.
- **ARMSX2's field list was read from `Config.h` and `ImGuiOverlays.cpp`**, not
  from a running screen.
- **Six forks' OSDs were sampled by string search, not read.** Only ARMSX2's was
  opened.

## Sources

- ARMSX2 `pcsx2/Config.h:951-971`, `pcsx2/ImGui/ImGuiOverlays.cpp:286-300,529`,
  `pcsx2/PerformanceMetrics.h:69-75`
- Vita3K `--truncated_mean`, default 90th percentile
- `shared_layer/MEASUREMENT.md`, `shared_layer/DID_IT_APPLY.md`
