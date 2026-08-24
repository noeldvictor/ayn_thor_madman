# Cleared 26 GB of superseded build trees, with three safety checks

**Goal: the user reported storage pressure and authorised clearing stale build
output.**

**No device. Deletion of gitignored build artefacts only.**

## The situation

**105.6 GB of build output across the fleet**, counted as directories over
500 MB. The largest single item is **rpcsx's `.cxx` at 24.40 GB** — a fork that is
**deferred and out of the packed binary**.

**azahar's own `AGENTS.md` already has the rule**, and it is one I read earlier
the same day:

> *"Keep generated Android storage bounded... **Do not leave tens of gigabytes of
> reproducible build output behind**"*, and *"verify that only the active release
> hash remains under `.cxx/RelWithDebInfo`."*

## What was removed, and why it is the zero-judgement category

**Seven CMake configuration-hash trees, each STRICTLY SUPERSEDED** — a newer hash
of the **same fork and the same configuration** remains in place:

| Freed | Tree | Superseded by |
| --- | --- | --- |
| 8.13 GB | rpcsx `13603r6x` (Aug 18) | `4t2n445z` (Aug 20) |
| 8.13 GB | rpcsx `2v3i5c1h` (Aug 8) | same |
| 3.02 GB | azahar `5u1i4b41` (Aug 20) | two Aug 23 hashes |
| 2.56 GB | dolphin `r1t2wem5` (**May 9**) | Aug 21 |
| 2.35 GB | Vita3K `android/.cxx` (**May 18**) | `android/app/.cxx`, new layout |
| 0.82 GB | melonDS `5y1i6xk2` (Jul 7) | Aug 23 |
| 0.75 GB | ARMSX2 `app/.cxx` (**May 8**) | `platforms/android/app/.cxx`, new layout |

**184 GB free -> 210 GB free. 81% -> 78% used.**

## Three checks, and the middle one is the important one

1. **Every target confirmed `git check-ignore`d before deletion.** A gitignored
   path is build output by definition.
2. **`git status --porcelain` captured BEFORE and AFTER in all six forks** —
   `1, 14, 0, 0, 0, 0` both times. **Identical.** No tracked file was touched, so
   **working rule 1 holds: the forks were not modified.**
3. **The newest hash per fork and config was kept**, so nothing needs a full
   rebuild.

> **Check 2 is what makes this defensible.** "I only deleted build output" is a
> claim; **an unchanged `git status` in every fork is evidence.** The rule this
> repo applies to measurements — verify from the artefact, not the intent —
> applies to destructive actions too.

## What was deliberately NOT removed

- **azahar's two Aug-23 hashes.** Both same-day, and **azahar has 14 modified
  source files** — somebody is mid-task. Deleting the active one costs a rebuild.
- **Debug trees** — ARMSX2 8.78 GB, Cemu 4.90 GB, melonDS 1.06 GB. azahar's rule
  says clear stale Debug, but **ARMSX2's and Cemu's pairs are both dated Aug 23**,
  so the date cannot say which is live.
- **`dolphin-thor` entirely, ~11 GB.** `CLAUDE.md` lists it as **obsolete, "do
  not invest work in these forks"**, which makes it the largest safe win
  remaining. **Offered and not taken without a reply** — deletion is destructive
  and one authorisation does not extend to the next target.
- **`build/` directories, ~35 GB.** They contain `outputs/apk`.

## The reusable part

**Superseded configuration hashes are the zero-judgement category.** They need no
knowledge of what is active, only that a newer hash of the same fork and config
exists. **Enumerate `.cxx/*/*` with dates and sizes, keep the newest per
fork+config, delete the rest** — and the enumeration is worth doing before any
build, not only when a disk fills.

## Limits

- **Only directories over 500 MB were counted**, so 105.6 GB understates the
  total.
- **Sizes are from the pre-deletion enumeration**, not re-measured per file as it
  was removed.
- **Nothing was rebuilt afterwards to confirm the kept hashes still work.** The
  claim is that a newer hash exists, not that it is functional.

## Files

- None in this repo. The change was to the machine's build output.
