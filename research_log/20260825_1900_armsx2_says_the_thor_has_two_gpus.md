# ARMSX2's index says the Thor ships two GPUs, and this project's Foundation assumes one

**Goal: apply the start-at-`AGENTS.md` rule to the seed fork.**

**Its 92 lines contain one claim that touches Foundation point 1, and six
smaller things this repo did not have.**

## The claim, and what this repo has against it

> ARMSX2 `AGENTS.md:69` — *"Gate any new GPU feature on the existing
> `MobileGpuArchitecture` detection in `GSGPUProfile.h`. **Thor ships both an 8
> Gen 2 (Adreno 740) and an 865 (Adreno 650) variant, so never assume 8 Gen
> 2.**"*

**`CLAUDE.md` Foundation point 1 says the opposite is safe:** *"Vulkan on Adreno
740. ARM64 on Snapdragon 8 Gen 2. ... **Every line of code may assume this
hardware.**"*

**What this repo actually has as evidence** is xenia's device baseline, dated
2026-05-17, **measured on the device in this workspace**: board platform
`kalama` — which is Snapdragon 8 Gen 2 — Vulkan device API 1.3.128, vendor ID
`0x5143`. **It also records the target as the *Thor Max*.**

> **Both statements can be true, and probably are.** The device here is an 8 Gen
> 2 Thor, measured. **ARMSX2 is making a claim about the PRODUCT LINE**, and it
> has acted on it: its GPU features are gated on architecture detection rather
> than assumed.

**I cannot settle the SKU question from here** — it needs a source outside the
fleet or a second device, and the device is in use. **What can be settled is
what this project should say.**

## The consequence: a hardware assumption becomes a scope decision

**"Every line of code may assume this hardware" is only a hardware fact if the
Thor is one machine.** If the line has an 865 variant, the sentence is a
**product decision** — *we target the 8 Gen 2 Thor and not the 865 one* — and a
decision has to be written as one, because somebody will otherwise read it as a
fact and ship a740-only code to a 650 owner.

**Foundation point 1 loses nothing by being stated as a decision.** Every
argument built on it — one driver, one pinned Turnip, one memory budget, a
render path that assumes a tiler with these limits — **is unchanged for the
device we target.** What changes is that the boundary is ours and is
acknowledged.

## And it makes today's Turnip finding much more relevant, not less

**The `vk-turnip-attachment-self-read` rule was measured on an Adreno 650.** I
recorded that as *"a different GPU generation"*, which read as a reason to doubt
its applicability.

> **If ARMSX2 is right, the Adreno 650 is the OTHER THOR VARIANT'S GPU.** The
> defect was measured on a device in the same product line, not on an unrelated
> part.

**That does not make it true of the a740** — it is still a different GPU — **but
it removes the "why would this even apply to us" framing.** `DEVICE_QUEUE.md`
entry 26 gets more valuable, and gains a second question: **if the app is ever
to run on both variants, the answer is needed for both.**

## Six smaller things from the same file

- **Cover art for PS2 has a source after all.** *"Cover art defaults to
  xlenore's PS2/PS1 cover repositories."* **Today's cover-art survey put PS2 in
  tier 3, user-supplied, because `GetCoverImagePathForEntry` reads a local
  file. The Android frontend has a tier-2 default and the survey missed it** by
  reading the desktop path.
- **The cheat badge works before first boot**, via
  `assets/cheats/index.tsv` mapping bundled CRC filenames to serials and titles,
  with `CheatPresenceIndex` owning the indexing and an explicit invalidation
  rule on import, install or delete. **`app/SCREENS.md` left "how does the badge
  know" open and this answers it.**
- **The intent distinction again, stated as a rule:** *"Cover `CHEATS` badges
  must come only from real `.pnach` files ... never infer them from widescreen,
  60 FPS, compatibility, or patch folders."* **Third independent appearance of
  cheat-versus-fix today.**
- **Texture upscaling needs three things this repo's specification does not
  name:** a **VRAM budget with batch eviction to a low-water mark**, a **"do not
  retry" mark on evicted hashes**, and a **hash-stability heuristic** — because
  *"animated textures re-hash every frame and will otherwise generate unbounded
  work."* **That last one is a real failure mode for the flagship feature.**
- **The NPU question is answered and closed:** *"Prefer Vulkan compute over the
  Hexagon NPU: the data is already in GPU memory, QNN/SNPE is a per-SoC
  packaging burden, and **NNAPI is deprecated as of Android 15**."*
- **Two operational rules.** *"Android compiles regexes with ICU, which is
  stricter than desktop Java ... treat a green Gradle build as no evidence a
  regex is valid."* And *"**Compiling is not running.**"*

## The shared-device rules, which match the constraint this project runs under

> *"The AYN Thor is **SHARED**. Several Claude sessions do emulator work against
> it at once, so another session's app stealing foreground focus is **normal,
> not a fault to debug**. Never fight for the device ... **the device being busy
> is never a reason to stop working or to end a turn.** Do not force-stop other
> apps to take focus. That is someone else's session in the middle of
> something."*

**That is the same constraint this repo records as "do not use the device", from
the other side**, and it adds an operational rule this repo lacks: **always
`am force-stop` your own package when finished, so the next session gets a clean
device.**

## Limits

- **The two-variant claim is ARMSX2's and is not verified here.** No second
  device, no external source, and the device in this workspace was not touched.
- **The `kalama` baseline is one device on one date.** It proves what that unit
  is, not what the line contains.
- **The xlenore cover default was read from `AGENTS.md`, not from the code.**
- **Six of ARMSX2's own referenced documents** — `docs/texture-upscaling-
  research.md`, `docs/mcp-server.md` — are cited there and were not re-read
  today.

## Sources

- ARMSX2 `AGENTS.md`, in full
- xenia `docs/research/20260517-142224-thor-vulkan-device-baseline.md` (via
  `CLAUDE.md`)
- `research_log/20260825_1710_turnip_breaks_attachment_self_read.md`
- `research_log/20260825_0900_cover_art_has_three_sources_and_all_three_key_on_the_path.md`
