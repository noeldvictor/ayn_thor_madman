# The backend standard: what a Thor core must deliver

**The feature north star.
[`THOR_TARGET.md`](../hardware_ref/thor/THOR_TARGET.md) says what every fork
compiles for. This says what every backend must do.**

Written 2026-08-22. It is the acceptance test for "is this backend finished",
and it exists because [`thor_backend.h`](thor_backend.h) says how a backend
plugs in and says nothing about what it must deliver.

**A backend that meets section 1 is one product with the others. A backend that
does not is a separate emulator sharing a launcher**, which is the thing this
project refuses to build.

---

## 0. What a backend must not do

Read this first. It is shorter than the requirements and it decides more.

| A backend never | Because |
| --- | --- |
| creates a Vulkan instance, device or queue | one device, one memory budget |
| owns a thread pool | one scheduler knows there is a single X3 |
| picks a GPU driver | one pinned Turnip is the reference configuration |
| defines a hotkey | **one hotkey set works on every system, always** |
| draws a settings screen, a file picker or a system dialog | a person must not be able to tell which backend asked |
| decides where a guest screen appears | the app routes across two panels |
| writes outside the paths the app gives it | storage accounting depends on it |
| ships its own upscaler, cheat engine or patch engine | those are owned subsystems. See [`OWNED.md`](OWNED.md) |

**Every one of these is something at least one fork does today.** The standard
is a list of habits to remove more than a list of features to add.

---

## 1. Required

**No backend ships without these.** They are what make the app one product.

### 1.1 Lifecycle and state

- Load, run, pause, stop.
- **Save state and load state.** Not optional. The universal hotkey set is
  built on it, and so is the test harness, which uses a savestate as a fixture
  rather than playing to a scene.
- **Deterministic from a loaded state.** The same state plus the same input
  produces the same frame. This is what makes golden-image comparison possible
  at all.

### 1.2 Settings

- **Every setting has a stable key, a type, a default and an owner.**
- **Every setting is overridable per game.** No global-only settings. A setting
  without a key cannot be overridden, so a setting without a key is a defect.
- The backend does not resolve the override. The order is per-game value, then
  Thor profile, then backend default, resolved in one place.

### 1.3 The device

- Accepts `DeviceHandles` and `DeviceFacts`. Stores them. Creates nothing.
- Respects the queue lock. The queue is shared, and melonDS already learned it
  needs one.
- Uses the shared allocator, the shared pipeline cache and the shared upload
  path.

### 1.4 Declarations

The backend declares; the app renders. A backend that declares nothing still
runs, with less UI.

| Declares | What the app does with it |
| --- | --- |
| guest screens: name, size, aspect, touch, required | dual-screen routing |
| texture classes, as a **list** and not an enum | per-class filter routing |
| storage categories: path, and **whether rebuildable** | the storage view, and never offering to delete a save |
| settings: key, type, default | per-game overrides |
| counters | the performance overlay, and measurement |

### 1.5 Honesty

- **Report resolves and LRZ breaks.** Do not make the app infer them. They are
  the most common way to lose frames on a tiler.
- **Give a specific decline reason.** "Nothing got upscaled" has half a dozen
  causes needing different fixes, which ARMSX2 already learned.

---

## 2. Expected

**Ship these unless the guest makes them meaningless.** State the reason when
you skip one.

- **Cheats**, through the shared engine. The backend supplies six callbacks:
  memory read, memory write, keys down, pause, resume, log. **Every backend in
  the fleet can implement those**, which is why the engine is shared.
- **Code patches**, through the shared engine.
- **Guest system applets** routed to the host: text entry, user selection,
  error dialog. A backend that draws its own keyboard has failed section 0.
- **Guest accounts**, where the guest has them. **Six of eight forks already
  have an implementation**, so this is core, not an edge case.
- **A touch overlay** through the shared one. The backend declares its elements,
  each with a **kind** as well as a name, because Cemu's `BUTTON_BLOW_MIC`
  proves not every element is a button.
- **Physical controller attach and detach.** The Thor has real buttons. An
  overlay drawn permanently over a game on a device with physical controls is
  wrong, and only Vita3K has solved it.

---

## 3. Declared extensions

**Do not force these on every backend.** A PS2 recompiler, a Wii U graphic pack
system and a DS plane compositor do not fit one shape, and forcing them into one
is what makes libretro slow.

A backend declares what it has. The app shows UI only for what is present.

| Extension | Who already has it |
| --- | --- |
| graphic packs, with preset constants feeding patches | Cemu |
| separate filter planes: OBJ, BG, 3D | melonDS |
| frame generation, with its own pacer | ARMSX2, 31 files |
| dual guest screens | melonDS, azahar, Cemu |
| guest accounts | six of eight |
| trophies, friends list, overlay video | rpcsx |

---

## 4. The performance bar

**A backend is measured, not argued about.**

| Rule | Value |
| --- | --- |
| Frame budget at 60 Hz | **16.6 ms** |
| Frame budget at 120 Hz | 8.3 ms. **Both panels support 120 Hz and the device is currently capped to 60 by a user setting.** Check the cap before trusting any pacing number |
| Sustained power | roughly **5 W** |
| Sustained temperature | roughly **50 C** |
| Run length when heat matters | **15 minutes or more** |

Every performance claim names the **CPU cluster**, the **driver build** and the
**commit**. A claim missing any of the three is not a result.

**Watts, not only frames.** A change that holds fps and lowers temperature is a
win.

---

## 5. The quality gate

From the rule that a change without a test does not land:

1. A **savestate fixture** for at least one scene.
2. A **golden frame** for that scene, compared automatically. A test that needs
   a person to look at a screenshot is not a test.
3. A **performance record** for that scene: fps, 1% low, frametime, watts,
   temperature.
4. A **boot check**, so an upstream harvest that breaks a console is caught
   without anybody playing it.

---

## 6. Where the fleet stands

**Nothing meets section 1 today**, because the shared layer it depends on does
not exist yet. The useful question is what each backend already has, so the work
is conversion rather than invention.

| Requirement | Already somewhere in the fleet |
| --- | --- |
| save states | every fork |
| savestate test fixtures | Vita3K |
| golden image comparison | ARMSX2, melonds_HD_2 |
| per-class texture routing | ARMSX2, melonDS, azahar |
| typed settings with stable keys | azahar and eden, twice, fully diverged |
| dual-screen routing | melonDS, azahar |
| touch overlay | azahar, Vita3K, Cemu |
| cheat engine | eden VM, azahar manager, rpcsx flat |
| code patcher | Cemu assembler, xenia TOML |
| guest accounts | six of eight |
| second-screen `Presentation` | **ARMSX2**, 707 lines, naming the Thor |
| hotkey set and binding UX | **ARMSX2** `SysHotkey` enum + `captureHotkey` |
| storage measurement | melonDS, ARMSX2, Cemu, azahar, GameNative |
| **storage aggregation per game, per category** | **nobody** |
| **resolve and LRZ reporting** | **nobody** |

**The three negatives here survived a second search with different words.** An
earlier version of this table claimed three different rows had no prior art;
**all three were wrong**, and two were already built in ARMSX2. See
[`../research_log/20260822_2154_second_search_three_negatives.md`](../research_log/20260822_2154_second_search_three_negatives.md).

**Search for the mechanism, not the category.** The failed searches used the
feature name. "storage" misses `walkTopDown` and `usableSpace`, because nobody
names a function after the category it belongs to.

---

## What this document is not

It is not a schedule, and it does not say a backend must meet the bar before the
shared layer exists. It says what "finished" means, so a backend can be judged
against something other than opinion.

**It is unproven.** The contract behind it has been exercised against three
deliberately divergent fake backends in `app/shell/` and against no real one.
Expect section 1 to change when the first backend is wired up, and change it
there rather than arguing it here.
