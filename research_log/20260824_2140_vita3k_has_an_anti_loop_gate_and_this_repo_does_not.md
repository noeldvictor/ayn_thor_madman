# Vita3K has an anti-loop gate, an attempt fingerprint and a definition of "fixed" — this repo has none of the three

**Goal: read Vita3K's `AGENTS.md`, the last Tier 1 index unread.**

**Its experiment discipline is the most developed in the fleet, and it is aimed
squarely at the failure mode this repo has been demonstrating all day.**

## Twelve mechanisms, and the six this repo does not have

| Mechanism | Here? |
| --- | --- |
| stop after two failed guesses in one subsystem | **yes**, mined earlier |
| a diagnostic toggle is not a fix | **yes** |
| prefer A/B/A when a live toggle exists | **yes** |
| one variable per experiment | **yes** |
| **an active-case LOCK** | **no** |
| **an attempt FINGERPRINT, checked before the run** | **no** |
| **a planned attempt is a BLOCKER** | **no** |
| **an experiment PACKET that refuses out-of-scope work** | **no** |
| **a stable outcome vocabulary with a DEFINITION of `fixed`** | **no** |
| **a compatibility ledger keyed on commit** | **no** |
| **leave the workspace knowable** | **no** |
| **SQLite as the canonical store, not markdown** | **no, and it is a challenge** |

## The four that would have changed today

**1. The anti-loop gate.**

> *"Before any renderer/core experiment, read the case history and produce a
> short preflight summary: current commit, platform, scene, debug props,
> matching prior attempts, what those attempts proved, and **the one reason this
> run is genuinely new. If the only difference is 'try it again,' stop and
> choose instrumentation or documentation instead.**"*

**This repo has "query the ledger first". That is the same idea without the
output.** A query you run and do not write down does not stop the next person —
**or the next session** — repeating it. **The preflight summary is the artefact
that makes the query durable.**

**2. A planned attempt is a blocker.**

> *"Resolve the current planned attempt as `failed`, `inconclusive`,
> `superseded` or `succeeded` before starting a nearby experiment, otherwise the
> ledger becomes a pile of half-memory and Codex starts circling."*

**`DEVICE_QUEUE.md` is 26 open entries with no such rule.** Nothing prevents
adding a 27th beside an unresolved 26.

**3. `fixed` has a definition.**

Outcomes are `fixed`, `improved`, `unchanged`, `worse`,
**`mixed-supports-involvement`**, `contaminated-inconclusive`. And:

> **`fixed` requires the original symptom gone, no obvious neighbouring-scene or
> game regression, no stale debug toggles, and Windows/Android proof when
> Android-affecting.**

**This repo's verdicts — `DEAD`, `FLAT`, `WIN`, `CONFOUNDED` — are about a
NUMBER. These are about a SYMPTOM**, and the two vocabularies do not overlap. **A
project that ships a product needs both.**

**`mixed-supports-involvement` is the entry worth stealing outright**: *"when an
experiment changes visible output but damages adjacent geometry ... The next
hypothesis should explain **both** the improvement and the regression."*

**4. A compatibility ledger keyed on commit.**

`compat add --title-id --platform --commit --status works|regressed|broken|
partial|blocked --scene --summary --artifact`, and the reason:

> **"This ledger is the answer to 'which commit did this game work on?'"**

**This project specified a compatibility sweep and a status classifier and never
said where the results LIVE.** This is the storage, and its key — **title,
platform, commit, scene** — is the right one.

## The instrument-contamination trap, which is the sharpest single item

> *"Treat Android debug prop values `0`, `false` and `off` as **disabled, never
> as hash/address prefixes**. When adding any fhash/vhash/address matcher, check
> the disabled values before prefix matching; otherwise **a stale
> `setprop ... 0` can accidentally match shader hashes beginning with `0`** and
> contaminate later renderer tests."*

**A disable value that is also a valid prefix.** The property says "off", the
matcher reads "match everything starting with 0", and **every subsequent test in
that session is contaminated by a setting somebody thought they had turned
off.** That is `DID_IT_APPLY.md` inverted: **not a setting that failed to apply,
but a disabled setting that applied anyway.**

**And the companion rule:** *"Clear and record debug props before every Android
comparison ... then name the remaining intentional props in the attempt body."*

## The one that challenges this repo's convention

> *"`reports/debug_knowledge.sqlite` is the canonical report and RAG store.
> **Markdown reports are legacy context or human exports only; do not create new
> durable Markdown reports by default.**"*

**This repo produced roughly forty markdown research logs today.** Vita3K's
reasons are specific and they are not about tidiness: **attempt fingerprints,
supersession, recency-versus-long-term search, and compatibility checkpoints
keyed on commit.** **None of those is expressible in a directory of markdown
files**, which is why its rule exists.

**The honest counter-argument, and this repo should keep markdown:** a markdown
log is **diffable, reviewable in a commit, greppable by any tool, and readable
without the tool that wrote it**. Every correction made today was made *in a
diff*, visible to a reader. **A SQLite row updated in place loses that.**

> **The two are not competing. Markdown is the narrative and the audit trail;
> a queryable ledger is the memory that prevents repetition.** This repo has
> the first and, apart from xenia's borrowed experiment ledger, not the
> second — **and today's four self-corrections are what the second one
> prevents.**

## Limits

- **Read from `AGENTS.md`. The tools it names —
  `tools/debug_knowledge.py`, `tools/renderer_experiment.py` — were not read**,
  so how well the mechanisms work in practice is unknown.
- **Its discipline is aimed at RENDERER CORRUPTION debugging**, where the
  symptom is visual and the search space is large. **Some of it may be
  overweight for a measurement question with a number.**
- **No claim is made that Vita3K follows its own rules**; only that it wrote
  them down.

## Sources

- Vita3K `AGENTS.md:27-49`, `68-91`, `92-105`
