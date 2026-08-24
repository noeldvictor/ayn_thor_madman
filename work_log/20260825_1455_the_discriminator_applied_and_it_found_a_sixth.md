# The discriminator applied to the rest of the shell, and it found a sixth

**Goal: I derived a test an hour ago — a fixed enum is correct when the HOST owns
the concept and wrong when the GUEST does — and then said the cheat spec, patch
spec and `GameData.kt` were not audited. Apply it.**

**Device-free: an audit of 26 enums, one change, a test run. No device used.**

## The audit

**26 `enum class` declarations across the shell's contract files, all listed and
the borderline ones read.** Most are plainly host-owned and pass without
argument: `ArtifactTier`, `StoreVerdict`, `WriteTarget`, `PresentMode`,
`HostDisplay`, `CacheVerdict`, `ScaleDenial`, `ClockDomain`, `OverlayVisibility`,
`HotkeyDenial`, `IntegrityMode`, `PatchBinding`.

**Three needed reading.**

| Enum | Verdict |
| --- | --- |
| `GameData.MetaField` — TITLE, REGION, DEVELOPER, GENRE... | **passes.** Metadata is what the SHELL displays, so the shell owns the list. Taken from EmulationStation, a frontend, not a guest |
| `Model.ScreenLayout` | **passes.** `CLAUDE.md` is explicit that the app owns dual-screen routing and the backend only declares its screens |
| **`ContentResolver.Kind`** | **FAILS** |

## The sixth instance

```kotlin
enum class Kind(val dirName: String, val extension: String) {
    CHEAT("cheats", "txt"),
    TEXTURE_PACK("textures", "zip"),
    MOD("mods", "zip"),
    SAVE("saves", "sav"),
}
```

**The KIND is ours** — the shell decides that cheats, texture packs, mods and
saves are the categories it shows and where it keeps them. **The EXTENSION is
not.**

> **This repo's own cheat survey found SIX formats** — `pnach`, `ncl`, `mch`, AR
> codes, VitaCheat `.psv`, Atmosphere `dmnt` — **and `CLAUDE.md` records save
> formats as "irreducibly per-backend".**
>
> **A single hardcoded `"txt"` could find one of the six.** The resolver's own
> doc comment promises *"looked in these six places"* as its diagnostic value,
> and it was looking in the right places for the wrong filename.

## The change

**`candidates()` and `locate()` now take the extensions**, backend-declared, in
precedence order. The enum keeps `dirName` — our directory layout — and its
extension list is renamed to `defaultExtensions` with a comment saying it is **a
fallback for a caller with no backend to ask**, such as a library scan before a
backend is chosen.

**One ordering decision, stated in the code:**

> **Root is the outer loop.** A nearer root's second-choice extension beats a
> further root's first choice, because **precedence between roots is what a
> person configured and precedence between extensions is not.**

**Three tests added**: every declared extension is tried, root-outer precedence
holds, and `locate` finds a non-default extension. **Suite 236, 0 failures**, up
from 233.

## What the audit says about the discriminator itself

**It was derived from a failure and it correctly PASSED two enums that look
suspicious.** `MetaField` is a fixed taxonomy taken from another project, and
`ScreenLayout` describes guest screens — **both would have been flagged by a
cruder rule like "any enum describing content".** The ownership test separated
them.

> **A test that only ever rejects is not a test.** This one accepted 25 of 26.

## Limits

- **The patch spec and cheat spec were checked for enums and have none of this
  shape** in the shell; `PatchIntent` and `IntegrityMode.GuardedFeature` are
  host concepts. **The native header's `CheatSpec`/`PatchSpec` equivalents were
  not re-audited** beyond the seven enums covered yesterday.
- **`defaultExtensions` is still a guess.** The five cheat extensions come from
  this repo's survey, not from any backend, and no backend declares extensions
  yet because none exists.
- **Nothing here is measured or run on a device.** It is a contract change
  behind unit tests.

## Files

- `app/shell/.../ContentResolver.kt`
- `app/shell/.../ContentResolverTest.kt` — 3 new cases
