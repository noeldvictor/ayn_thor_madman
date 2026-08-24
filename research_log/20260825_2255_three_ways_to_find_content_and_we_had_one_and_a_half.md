# Three ways to find a person's content, and this project had one and a half

**Goal: XenDroid browses mounted volumes in its folder picker. I changed
`ContentResolver` today and it takes a list of roots that nothing supplies.
Compare.**

**Device-free: one commit diff. No device used.**

## The three approaches, and they are complementary

| Approach | Fork | What it answers |
| --- | --- | --- |
| **Search known conventions** inside a root | **Vita3K** | *where would content BE?* — **nine locations** for one title |
| **Convert what the person picked** | **eden** | `PathUtil.kt` turns a SAF `content://` URI into a real path, **including removable SD volumes** |
| **Enumerate what is mounted** | **XenDroid** | *what roots EXIST right now?* |

**This repo had the first two recorded and treated them as alternatives.** They
are not: **enumerate the volumes, offer them, convert whatever the person picks,
then search conventions within it.** Each answers a different question and a
content system needs all three.

## The enumeration, and it is small

```kotlin
val primary = Environment.getExternalStorageDirectory() ?: File("/")
val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
// per volume:
val dir = volume.directory ?: continue
```

**`StorageManager.storageVolumes` with `volume.directory`.** The `?: continue` is
the detail worth keeping: **a volume can be known and have no directory** —
present in the list, not currently usable.

> **That is the case my own `ContentResolverTest` already covers from the other
> end**: *"a person's SD card is not always mounted."* **XenDroid's enumeration is
> where that state originates**, and my test asserts what happens after it.

## A DELETE instance, by our own rule

**XenDroid gates the enumeration at API 30+ and falls back to primary-only
below.** Its comment says so: *"Primary storage plus mounted removable volumes
(API 30+; primary-only below)."*

> **The standard row sets `minSdk` 33.** So the pre-30 fallback is **machinery
> serving variability this device does not have** — the DELETE operation, applied
> to a UI helper rather than to a portability layer.

**Small, and worth noting because it is the first time that rule has come up in
the app layer rather than in a renderer or a recompiler.** The same test applies:
*what variability does this serve?*

## The gap in what I wrote today

`ContentResolver.candidates(roots, kind, titleId, extensions)` **takes the roots
as a parameter**, and I extended it this afternoon so extensions are
backend-declared. **Nothing in the shell produces the `roots` list.**

> **The resolver is the "search conventions" third of the problem, and it is the
> only third that exists.** Its own doc comment promises a diagnostic — *"looked
> in these six places"* — **which is only true if something enumerated the
> places.**

**Recorded, not built.** The enumeration is Android-API code with no unit-test
surface worth the name, and this phase prefers a recorded requirement to a
speculative implementation. **`ContentRoot` already carries a `label`, which is
exactly what a volume list supplies.**

## Limits

- **One commit diff, read for its mechanism rather than in full.** Nothing built
  or run, no device.
- **`StorageManager.storageVolumes` behaviour on the Thor specifically was not
  checked** — two internal displays are unusual, storage is not, but the device
  was not queried.
- **No claim that Vita3K's nine conventions are the right set for this project.**
  They are the Vita's.
- **The DELETE observation is about XenDroid's code**, and applies to ours only
  if we write the same fallback.

## Sources

- XenDroid, `[Android] Browse mounted volumes (USB drives, SD cards) in the
  folder picker`
- `app/shell/.../ContentResolver.kt`; `CLAUDE.md`, Vita3K's path resolver and
  eden's `PathUtil`
