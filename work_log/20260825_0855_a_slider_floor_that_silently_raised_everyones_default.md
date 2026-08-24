# A slider floor that silently raised everyone's default, and the schema check that catches it

**Goal: XenDroid ships a settings schema WITH TESTS. This project built a
`SettingSpec` and has no schema test. Compare them.**

**No device. A source read, a change to this repo's own app shell, and a test
run.**

## The bug XenDroid shipped

Its regression guard says it plainly:

> *"every IntRange default must be in `[min, max]`, else **the slider silently
> coerces the persisted default to a different value** (the texture-cache bug)."*

**And the instance is recorded in the test beside it:**

> `GPU|texture_cache_memory_limit_soft` — *"min deliberately diverges from the
> legacy XML (512): **that floor was above the real TOML default (384), which
> would have silently coerced the default upward.**"*

> **A slider whose minimum sits above the configuration default raises every
> user's setting, with no message and no failure.** The config says 384, the
> widget cannot represent 384, so the user gets 512 and nothing says so.

**That is `DID_IT_APPLY` mechanism 14 — a substituted value — arriving through
the UI instead of through an API**, which is why the check belongs in the schema
rather than in a screen.

## Our `SettingSpec` could not even express it

**Read before changing anything.** `Backend.kt`'s `SettingSpec` carried `key`,
`label`, `type`, `group`, `default`, `options`, `liveChangeable`, `scope` —
**and no bounds at all.**

> **So an `INT` setting had no declarable range, which means the range is
> invented by whoever draws the slider — and an invented range is a
> GUARANTEED candidate for this bug rather than merely an exposed one.**

**The gap was in the contract, not in a screen.**

## What was added

**`min: Double?` and `max: Double?` on `SettingSpec`**, null meaning *the backend
declares no bound and the UI must not invent one.*

**`validateSchema(specs): List<SchemaDefect>`**, device-free, backend-free,
game-free. Six rules, each a bug somebody shipped rather than a tidiness
preference:

| Rule | What happens without it |
| --- | --- |
| **a numeric default must lie in `[min, max]`** | **the widget silently coerces it** — XenDroid's bug |
| **an ENUM default must be one of its options** | the picker cannot display it, so it chooses something else |
| an ENUM must have options | an empty picker |
| **a key must be unique** | resolution order is undefined, and this project resolves per-game over global BY KEY |
| `min <= max` | an unsatisfiable range |
| bounds only on numeric types | somebody misread the type |

**It returns every defect rather than throwing on the first**, so a backend
author fixes one list instead of iterating.

**And the message says what HAPPENS, not that the value is invalid** — *"default
384.0 below min 512.0 — would be coerced"*. A test asserts that wording, because
*"invalid"* would not tell the reader their users' settings are being changed.

## Result

**`SchemaValidationTest`, 11 cases. The suite is 226 tests, 0 failures**, up from
215. Nothing regressed.

**The XenDroid bug is reproduced as a test against our own schema** —
`a_default_below_its_own_floor_is_a_defect` uses 384 and 512, the real numbers.

## Four more things worth taking from their test, not taken yet

- **Pin the inventory with exact counts.** `total_entry_count_is_115`, plus per
  type — **83 Bool, 10 IntRange, 20 ListChoice, 2 Action.** Adding a setting then
  fails the build until somebody updates the count deliberately. **Structural,
  not advisory**, which is this project's stated preference.
- **`Action` as a setting type**, which we do not have. Theirs are
  `Vulkan|vulkan_lib_path` and `Logging|dump_session_logs` — **the driver picker
  is an Action in the settings schema.** This project's per-game driver override
  needs exactly that shape.
- **`removed_no_op_settings_stay_removed`** — a test asserting a deleted key
  stays absent. **A setting that does nothing is the `DID_IT_APPLY` symptom
  itself**, so once removed it should be pinned.
- **A deliberately UNEXPOSED setting, documented in the test.**
  `Display|host_present_from_non_ui_thread` is *"intentionally absent (must be
  true on Android — forced natively, **false black-screens the app** — so it is
  not a valid user choice)"*. **That is a scope this project's `SettingScope`
  does not have: not global-only, but not-a-choice.**

## Limits

- **The validator is not wired into anything yet.** No screen or backend calls
  it, so it guards the schema only when a test does. **Wiring it into backend
  registration is the obvious next step and was not done.**
- **`min`/`max` are `Double?` for both INT and FLOAT.** That is simpler than two
  pairs and loses exact integer semantics above 2^53, which no setting will
  reach — **stated so the choice is visible.**
- **No backend declares bounds yet**, because no real backend exists. The
  validator has nothing real to check.
- **XenDroid's counts and keys were read, not verified against a build.**
- **The four items above are recorded, not implemented.**

## Files

- `app/shell/.../Backend.kt` — `min`, `max`, `SchemaDefect`, `validateSchema`
- `app/shell/.../SchemaValidationTest.kt` — 11 cases
