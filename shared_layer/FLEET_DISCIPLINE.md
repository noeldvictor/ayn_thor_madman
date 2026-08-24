# What each fork is disciplined about

**Eight forks, eight working practices, read on 2026-08-24 in one pass.**

`CLAUDE.md` says the fleet's research directories are its richest unmined seam.
**The indexes are cheaper than that and denser: a fork's `AGENTS.md` is the
shortest document it has and the highest yield per line.** Reading all eight cost
under two hours and produced a correction to Foundation point 1, a measured
answer to a queued device experiment, and eleven rules this repo did not have.

**Of the eight read, none is disciplined about everything.** Each is rigorous in
one place and casual elsewhere, and **in every one of the eight the rigour sits
where that fork visibly got burned** — the index says so, usually with the
incident. **This is the map of who is good at what, so the next question goes to
the right fork.**

---

## The table

| Fork | Disciplined about | The one thing to take |
| --- | --- | --- |
| **xenia** | **proof of behaviour** | **the proof packet**: screenshot or video, logcat, build or APK hash, cvars and settings, a reproducible path — *"do not claim a game is fixed until the actual failing screen or route is shown working"* |
| **azahar** | **not undoing hand-tuned work** | **invariants asserted on EMITTED CODE** — expected instruction sequences, spill-freedom, and a linked function size of 568 bytes against a rejected 3,328 |
| **Vita3K** | **not going in circles** | **the anti-loop gate**: before any experiment, write the one reason this run is genuinely new — *"if the only difference is 'try it again', stop and choose instrumentation"* |
| **Cemu** | **rejecting things properly** | **a rejection with a revisit condition**, and *"measure the loading phase, not gameplay"* with the false negative that produced it |
| **rpcsx** | **knowing what an instrument can answer** | **use a cumulative counter wherever one exists** — 1.66 W of spread became 0.002 W |
| **ARMSX2** | **not assuming the hardware** | **gate on architecture detection**, because *"Thor ships both an 8 Gen 2 and an 865 variant"* |
| **GameThor** | **what NOT to harvest** | **a denylist AND a post-transplant scan** — analytics, ads, recommendation feeds, supporter prompts |
| **eden** | **reporting completely** | **the fullest field list**: timezone, upstream commit, host toolchain, firmware build, and **settings changed from defaults** |
| **melonDS** | **admitting what is unverified** | **"verification debt"** — a named, explicit list of claims not yet device-tested |

---

## The eleven rules this repo took

**Measurement** — all now in [`MEASUREMENT.md`](MEASUREMENT.md):

1. **Use a cumulative counter, never a spot reading.** An instantaneous
   `current_now * voltage_now` on an idle device spread **1.66 W**; differencing
   `charge_counter` spread **0.002 W**. *(rpcsx)*
2. **The harness is not thermally free.** The same run through an input-macro
   harness reached **70.7 C in ten seconds** and tripped its own guard. *(rpcsx)*
3. **Measure the loading phase, not gameplay**, for any CPU change — with the
   false negative that proves it: a real 3-instructions-to-2 win measured
   **7922 against 7897 ticks**. *(Cemu)*
4. **Identify the driver by logged metadata, not its banner.** Two builds that
   differ measurably present the same string. *(azahar)*
5. **Brightness is part of a power measurement**, and **audio breakup makes a
   power win a failure.** *(azahar)*
6. **The proof packet**, and **capture before force-stopping or clearing.**
   *(xenia)*
7. **Name the upstream commit**, so a result survives a rebase. *(eden)*

**Process:**

8. **A planned attempt is a blocker.** Resolve it before starting a nearby one.
   *(Vita3K)*
9. **A rejection needs a revisit condition**, or it is only discouragement.
   *(Cemu)*
10. **A harvest needs a denylist and a scan**, not only a ledger. *(GameThor)*
11. **Verification debt is a named list**, not an absence. *(melonDS)*

---

## Four detection heuristics, from four forks

**These are the transferable techniques, as opposed to the rules.**

- **"An orphaned comment — one that still describes behaviour the code no longer
  has — is the signature of a REVERTED FIX."** *(melonDS.* That is how one of
  its regressions was found.)
- **"Bisect by SETTING before bisecting by build."** *"Toggling one preference
  costs seconds, a build costs minutes."* *(melonDS)*
- **"Check the runtime counters to prove a path is even live before blaming
  it."** *"`planeFilters=0` in the logs killed a wrong hypothesis instantly."*
  *(melonDS — and it is [`DID_IT_APPLY.md`](DID_IT_APPLY.md)'s rule aimed at the
  code path under test rather than at the instrument.)*
- **"Auditing x86 CORRECTIONS has high yield where auditing opcodes has low
  yield."** *(rpcsx.* Six defects found that way; **the tell is an XOR against a
  sign-extended comparison.)*

---

## Two vocabularies, and this repo had only one

**Every verdict this project carried is about a NUMBER**: `DEAD`, `FLAT`, `WIN`,
`CONFOUNDED`, `migration-credit`, `route-miss`.

**Vita3K's are about a SYMPTOM**: `fixed`, `improved`, `unchanged`, `worse`,
**`mixed-supports-involvement`**, `contaminated-inconclusive` — and **`fixed` has
a definition**: original symptom gone, no neighbouring regression, no stale debug
toggles, proof on every affected platform.

> **A project that ships a product needs both.** A change can move a symptom
> without moving a counter, and the reverse.

---

## Where the disciplines disagree, and both are right

| Question | One fork | The other |
| --- | --- | --- |
| **stacking** | **rpcsx: one component per proof run** — protects attribution | **xenia: measure the COMPOUND, never one layer** — protects detection |
| **enabling an ISA feature** | **this repo: global `-march`** for a mandatory feature on a fixed device | **azahar: per-TU flags + a runtime gate**, *"never enable optional crypto ISA extensions globally"* |
| **where reports live** | **Vita3K: SQLite canonical**, *"markdown reports are legacy context only"* | **this repo: markdown**, because a correction made in a diff is visible and a row updated in place is not |

**None of these is a mistake by either side.** Each is correct for its regime,
and **the failure is applying one without knowing the other exists** — which is
what this document is for.

---

## Limits

- **Indexes only.** The research directories behind them — 553 documents in
  xenia alone — remain the larger seam.
- **azahar's index is 1,458 lines and roughly 100 were read.** Its 54 `Do not`
  invariants are a rejection ledger nobody has mined.
- **A fork writing a rule down is not evidence it follows it.** Nothing here was
  audited against the fork's actual commits.
- **melonDS has no `AGENTS.md` at either level** — checked by listing both
  `melonds_HD/` and `melonds_HD/melonDS-android/`. Its conventions live in a
  620-line workspace `CLAUDE.md` **one directory up from the fork**, which is why
  a survey looking inside each fork missed it. **`melonds_HD_2` was not
  checked.**

## Sources

Each fork's `AGENTS.md`, and `melonds_HD/CLAUDE.md`. Research logs
`20260824_1950`, `20260824_2020`, `20260824_2100`, `20260824_2140`,
`20260824_2220`, `20260824_2300`, `20260825_1810`, `20260825_1900`.
