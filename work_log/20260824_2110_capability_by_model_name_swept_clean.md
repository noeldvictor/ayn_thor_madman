# A capability flag decided by CPU model name: swept, and the packed binary is clean

**Goal: take rpcsx's `capability_by_model_name` defect and make it sweepable
here, because `CLAUDE.md` records the sibling bug and never checked for this
one.**

## The class

**A host-capability flag answered by comparing a CPU MODEL NAME string**, rather
than by the architecture or a runtime probe.

**rpcsx paid for it twice.** `m_use_fma` was gated on
`cpu == "cyclone" || cpu.contains("cortex")` — and **FMA is mandatory on
AArch64**, so the allowlist could only ever fail to enable it. `m_use_ssse3` was
gated on an allowlist of old x86 parts **including `"generic"`**, and it gates
the `x86_pshufb` lowering whose fallback is a **16-iteration scalar loop** of
`extractelement`/`insertelement`. That lowering backs `VPERM`, `LVLX`, `LVRX`,
`STVLX`, `STVRX`, `ROTQBY` and `SHUFB`.

> **One unlucky CPU string would degrade every byte permute in both recompilers
> at once.**

**The rule, in rpcsx's words:** *a host-capability flag should be answered by the
architecture or a runtime probe, never by a model-name allowlist, and any
allowlist that predates the port should be assumed to be x86-only.*

## The sweep

`tools/bug_class_sweep.py --class capability_by_model_name`. Pattern: a string
comparison against a CPU name, within three lines of a capability-flag word.
**Shape-scoped, so a zero is a fact about the pattern rather than about a path
list.**

| Fork | lines | files | verdict |
| --- | --- | --- | --- |
| ARMSX2, Cemu, azahar, melonDS, Vita3K, eden, GameThor | 0 | 0 | — |
| **xenia** | 1 | 1 | **dismissed on reading** |
| **rpcsx** | 10 | 2 | **the known sites** |

**xenia's is `cvars::cpu == "arm64"` in `emulator.cc` and its PPC test main** — a
**backend selector** the user sets, choosing which recompiler to instantiate. It
compares an architecture name, not a part name, and it decides which code to
build rather than whether an instruction exists. **Not the class.**

**rpcsx's ten are all `Emu/CPU/CPUTranslator.cpp`** — the `m_use_ssse3` and
`m_use_fma` allowlists above, which that fork has already closed. **rpcsx is out
of the packed binary.**

## What this is worth

**A negative, and the class is now permanent.** This project has almost no tests,
and `CLAUDE.md` says the cheapest durable form of a lesson is a test. **The next
LLVM-backend or feature-detection change gets swept without anybody remembering
why.**

**Distinguishing the legitimate case is the part that needed reading.** Matching
a name to pick a **scheduling model**, a **workaround** or a **backend** is
correct — ARMSX2's driver-bug database and melonDS's `-mtune=cortex-x3` are both
name-matching and both right. **Matching one to decide whether an INSTRUCTION
EXISTS is the defect.** The class text says so, so the next reader does not
re-derive it.

## Limits

- **Nine forks searched, two had hits, both read.** The other seven were not
  read; a zero from this pattern means the shape is absent, not that no
  capability is mis-detected by some other mechanism.
- **The pattern keys on `cpu` as an identifier name.** An allowlist over a
  differently-named variable would be missed.
- **No claim that any fork's feature detection is correct** — only that this
  shape is absent.

## Files

- `tools/bug_class_sweep.py` — class `capability_by_model_name`
- rpcsx `docs/arm64/codegen.md:318-355`
