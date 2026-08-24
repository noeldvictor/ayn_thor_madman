# Four fork indexes read, and the rule paid every time

**Goal: apply the rule established today — start at a fork's `AGENTS.md`, not at
whichever document you found first — to the forks it had not been applied to.**

**Four read: xenia, ARMSX2, eden, GameThor. Every one produced something this
repo did not have, and two changed existing positions.**

## What each index gave

| Fork | Lines | What it produced |
| --- | --- | --- |
| **xenia** | 128 | **Its product priority is Android usability, and Blue Dragon speed work is PAUSED** — this repo builds heavily on Blue Dragon material. Plus the **game status classifier**, the missing half of the specified compatibility sweep, and the **proof-packet** definition |
| **ARMSX2** | 92 | **"Thor ships both an 8 Gen 2 (Adreno 740) and an 865 (Adreno 650) variant"** — Foundation point 1 becomes a scope decision. Plus the PS2 cover source, the cheat-badge index, three texture-upscaling requirements, the NPU decision, and the shared-device etiquette |
| **eden** | 65 | **The fullest proof-packet field list in the fleet**, including timezone, upstream commit, host toolchain versions, firmware build number and **settings changed from defaults** |
| **GameThor** | 123 | **The only complete harvest policy**: no wholesale merges, an allowlist, **a denylist AND a post-transplant scan** |

## The two that changed positions here

**Foundation point 1.** *"Every line of code may assume this hardware"* is a
hardware fact only if the Thor is one machine. **ARMSX2 says it is not**, and
gates its GPU features on architecture detection accordingly. **This repo's own
evidence is one device, measured** — board `kalama`, Adreno 740 — **which proves
what that unit is, not what the line contains.** Recorded as a decision with both
sources named, not settled.

**And it upgraded a finding from earlier today rather than contradicting it.**
The Turnip attachment-self-read defect was measured on an **Adreno 650**, which I
had framed as "a different GPU generation". **If ARMSX2 is right, that is the
other Thor variant's GPU.**

## The pattern

**The rule was established because reading `ledger.md` instead of `AGENTS.md`
cost a day** — a thermal conclusion published in a weaker form than the fork's
own document supported.

**Four applications, four payoffs, and two of them touched foundational
positions.** The cost of each was under ten minutes, because **an index is short
by construction.**

> **A fork's `AGENTS.md` is the cheapest document in the fleet and the highest
> yield per line. Read it before anything else in that fork — including its
> ledger, its research directory and its code.**

**Four remain unread**: Cemu (390 lines), Vita3K (346), azahar (**1,458**), and
melonDS, which has no `AGENTS.md` at all and keeps its conventions in
`.claude/skills/`.

## Files

- `CLAUDE.md` — Foundation point 1 qualification; the harvest denylist and scan;
  texture-upscaling requirements; shared-device etiquette; two Android gotchas
- `shared_layer/MEASUREMENT.md` — the proof packet, four rows
- `shared_layer/STATIC_TRIAGE.md` — the classifier half
- `app/SCREENS.md` — cover-art tier correction, cheat-badge answer
- Research logs `20260825_1810`, `20260825_1900`
