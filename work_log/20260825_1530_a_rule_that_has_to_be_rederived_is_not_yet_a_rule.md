# A rule that has to be re-derived is not yet a rule

**Goal: the host-owns-it test has now been derived, applied and validated. It
lives in a research log and a code comment. Put it where the next person will
meet it.**

**No device. One documentation change.**

## The count that justifies the placement

**Six instances of one argument, each made from scratch:**

| # | Instance | Where |
| --- | --- | --- |
| 1 | texture classes are declared, not enumerated | `CLAUDE.md`, the native contract |
| 2 | filter algorithms — ARMSX2's `Anime4K` is a **neural network**, melonDS's a **nine-texel kernel** | the upscaling section |
| 3 | cheat memory regions — pnach `EE`/`IOP` against dmnt `MainNso`/`Heap` | the cheat section |
| 4 | **`AcceptedInput`** | **our own header** |
| 5 | **`ValidationError`** | **our own header** |
| 6 | **`ContentResolver.Kind`'s extension** | **our own shell** |

> **Instances 4, 5 and 6 were written AFTER the rule had been stated three
> times.** The argument was available and was not applied, because it existed
> only as three separate war stories.

**That is the case for putting the test at instance 1**, which is where somebody
designing the next contract will be reading.

## What was written

**The test, beside the texture-class bullet in `CLAUDE.md`:**

> **A fixed enum is correct when the HOST owns the concept, and wrong when the
> GUEST does.**

**With the acceptances, not only the rejections** — `UploadResult`,
`SettingScope`, `LifecycleOp`, `Counter`, `MetaField`, `ScreenLayout` — because
**a test that only ever rejects gives no guidance to somebody writing a new
enum**, and 32 of the 33 audited passed.

**And the resolution for the hard case**: when a concept is guest-owned but the
shell must still act on it, **carry both** — the small set usable without guest
knowledge, plus the guest's own value uninterpreted. **Already the answer for
cheat regions, and azahar's answer for button labels.**

## The meta-point, which is this project's own thesis turned inward

`CLAUDE.md` says a rule in a document does not stop duplication, because **an
agent skips what it does not read**, and that the fix must be structural.

> **This is the same failure in miniature, and the culprit was me.** Three
> statements of one argument in three sections did not stop me writing two more
> instances of it into a contract in one afternoon.

**The structural version would be a lint** — flag a fixed enum in a contract file
whose member names encode guest vocabulary. **That is not obviously possible to
write well**: `kAtSignNotAllowed` is detectable, `AcceptedInput` is not.
**Recorded as wanted and not attempted.**

## Limits

- **A documentation change. Nothing is enforced.**
- **The 33-enum audit covers this repo's two contracts only.** The forks were not
  audited for the same shape, and several of them are where the instances came
  from.
- **No claim that the test is complete.** `AppletKind` passes it and may still be
  too small; passing the test is necessary, not sufficient.

## Files

- `CLAUDE.md` — the test, beside instance 1
