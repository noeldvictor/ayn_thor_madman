# The upscale algorithm sets: one axis is missing, and one entry is in the wrong pipeline

**Goal: verify `CLAUDE.md` Phase 4, which says to base the shared enum on
ARMSX2's `GSTextureUpscaleAlgorithm` and add `Super2xSaI` and `Quilez` from
melonDS-android.**

Session 2026-08-22 23:50. Queue item 7, the flagship feature, was the last one
still marked partial.

**Result: half the instruction is right, half is a category error, and the two
forks disagree on something more important than which algorithms exist.**

---

## ARMSX2's enum is the better starting point, and it says why

`pcsx2/Config.h`, `GSTextureUpscaleAlgorithm`, **24 entries grouped by the art
they suit**, each with a one-line note on what it does and when it is wrong:

- **Resample and sharpen** — Bilinear, Bicubic, Lanczos, LanczosCAS.
  "Neutral and nearly free; never makes art look wrong, only softer."
- **Pixel art and edge directed** — Scale2x, Eagle, SuperEagle, SaI2x,
  SuperSaI2x, HQx, xBR, xBRZ, SuperxBR, ScaleFX, MMPX, OmniScale. The comment
  explains *why* this group matters on PS2: "4MB of VRAM pushed games hard
  toward PSMT8/PSMT4".
- **Neural** — Anime4K, FSRCNN, SESR, ESPCN. "Best on painted and photographic
  textures, worst on crisp UI."
- **Appended later** — Nearest, Mitchell, SharpBilinear, ScaleForce.

**A rule worth taking, stated in the enum itself:**

> Appended after the first release of this enum. Order below this line is
> history, not grouping — **the enum is persisted as an integer, so entries can
> only ever be added.**

**That is the settings-migration problem in miniature**, and it connects to the
migration framework taken from melonDS-android earlier today. A persisted enum
is an append-only list forever. **Group in the UI, not in the numbering.**

---

## Correction 1: `Quilez` is a present-time filter, not a texture filter

melonDS keeps **two separate lists**, in `app/src/main/res/values/strings.xml`:

| Array | Entries |
| --- | --- |
| **`video_filtering_options`** | None, Linear, 2xBR, HQ2X, HQ4X, **Quilez** |
| the texture filter array | ..., 2xSaI lite, SuperEagle lite, MMPX lite, **Anime4K (DoG)**, **Super2xSaI strong**, SuperEagle smooth, Crisp gradient, Crisp edge AA, ScaleFX |

**`Super2xSaI` is in the texture list. `Quilez` is not — it is whole-frame
output filtering.**

`CLAUDE.md` therefore instructs adding a **present-time** filter to a
**texture-time** enum. **That is the exact category error the same document
warns about**, in the section arguing texture-time beats present-time:

> A present-time shader sees one finished frame. It cannot separate an anime
> portrait from a wall.

The claim was almost certainly built by grepping a string array without checking
which setting owned it. **Two lists, one grep.**

---

## Correction 2, and it matters more: melonDS's axis is cost, not algorithm

Read `melonDS-android-lib/src/HDTextureFilter.h`. The mode list:

```
3  Scale2x          9  Super2xSaI strong
4  HQ2x lite        10 SuperEagle smooth
5  2xSaI lite       11 crisp gradient
6  SuperEagle lite  12 crisp edge AA
7  MMPX lite        13 ScaleFX (faithful multi-pass whole-image port)
8  Anime4K lite
```

**Five entries are `lite`. One is `strong`. One is `smooth`.** Those are not
different algorithms — they are **the same algorithms at different cost and
strength points**. Two more, `crisp gradient` and `crisp edge AA`, are not named
after any classical algorithm at all.

| | ARMSX2 | melonDS |
| --- | --- | --- |
| Axis | algorithm identity | **cost and strength tier** |
| Entries | 24 | ~14 |
| Can express "xBR, but cheap" | **no** | yes |
| Can express "Lanczos" | yes | no |

**Merging these into one flat list loses melonDS's axis entirely**, and on a
handheld the cost axis is the one that decides whether a filter is usable. A
24-entry flat enum cannot say "MMPX at half the cost".

### So the shared type needs two fields, not one

```
algorithm  : Bilinear | xBRZ | MMPX | Anime4K | ScaleFX | ...
tier       : Lite | Standard | Strong
```

Not every pair exists, so the backend or the shared layer declares which are
implemented — the same declared-list pattern the contract already uses for
texture classes rather than a fixed enum.

**This is the same finding as the settings framework**: two forks built the same
thing, and **where they diverged is where the original design was
under-specified.** ARMSX2 under-specified cost; melonDS under-specified
algorithm identity.

---

## What to do

1. **Take ARMSX2's enum as the algorithm axis**, with its grouping comments and
   its append-only rule.
2. **Add a tier axis from melonDS.** Do not flatten `lite` variants into
   separate algorithm entries.
3. **Do not add `Quilez` to it.** It belongs to the present-time filter list,
   which is a different pipeline stage and a different setting key.
4. **`Anime4K` checked, and it changes the conclusion above.** See below.

---

## Read afterwards: the algorithm names are not a shared vocabulary

**The two forks use the name `Anime4K` for two unrelated techniques.**

| | ARMSX2 | melonDS |
| --- | --- | --- |
| What it is | **a neural network** | **a 3x3 texel kernel** |
| Evidence | `anime4k_x2.a2nn`, `anime4k_x4.a2nn`, `GSTextureUpscalerNN.cpp` | `Anime4KLiteTexel(a..i, subx, suby)`, built on `MMPXLiteTexel` |
| Sees | the whole texture | nine texels |

melonDS's is a hand-written pixel-art kernel with thin-detail protection that
borrows the name. **It is not Anime4K.**

**And its modes compose each other.** `CrispGradientTexel` blends
`Anime4KLiteTexel` with `SuperSAIStrongTexel`, then sharpens against a weighted
neighbourhood. `CrispEdgeAATexel` is built the same way, and there is a separate
`HeavyFilterTexel` / `FirstStageHeavyTexel` / `Recursive4xHeavyTexel` path.

**So melonDS's list is not a list of published algorithms at different cost
points. It is a set of hand-tuned recipes composed from primitives.**

### This kills the shared enum, not just flattens it

The conclusion earlier in this log — algorithm axis plus tier axis — **is not
enough.** Two axes still assume the algorithm names mean the same thing in both
forks, and they do not.

**A shared enum keyed on names would silently merge a neural network with a
nine-texel kernel**, and a user picking "Anime4K" would get something different
per backend with no way to tell.

**The filter list must be declared by the backend, not enumerated by the shared
layer.** `CLAUDE.md` already states exactly this argument for texture classes:

> **Texture classes are a declared list, not an enum.** ARMSX2 has two, melonDS
> three, Cemu none. **A fixed enum would impose one emulator's taxonomy on the
> rest.**

**The same sentence is true of filters, word for word**, and Phase 4 currently
says the opposite. What the shared layer owns is the **routing** — which class
goes to which filter — and the UI. **The filter list itself is backend
knowledge**, exactly like the class list.

**ARMSX2's enum stays valuable as a vocabulary and a set of grouping
comments.** It stops being the shared type.

## Not verified

**`CLAUDE.md` also claims the two algorithm sets "overlap by nine entries".**
Counting the lists above, the overlap by name is smaller, and the `lite`
suffixes make an exact comparison ambiguous. **That number should be re-derived
or dropped**, not repeated.
