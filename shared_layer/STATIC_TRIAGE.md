# Static triage: what a dump tells you before you run it

**Read the game's import table, look each function up in the backend's HLE
table, and score the result. No boot, no device, no scene.**

**Written 2026-08-23**, from a measurement of one fork's HLE layer.

---

## The idea

**Every console executable declares what it needs.** XEX, RPL, SELF, NSO, NCCH
and PRX all carry an **import table** naming the modules and functions the title
calls.

**Every emulator declares what it provides.** The HLE layer *is* that list, and
in at least three forks it is machine-readable.

> **risk(title) = the fraction of its imports that land on a function the
> backend does not really implement.**

**Both halves already exist in the fleet. Nothing computes the difference.**

## The measurement that makes it concrete

**Vita3K, counted 2026-08-23 by parsing `EXPORT(` blocks and looking for
`STUBBED` or `UNIMPLEMENTED` inside each:**

**7,377 HLE functions. 6,285 stubbed or unimplemented — 85%.**

**The aggregate is not the useful number. The distribution is:**

| Module | Functions | Stubbed |
| --- | --- | --- |
| **`SceGxm`** — graphics | 292 | **23%** |
| `SceLibKernel` | 371 | 61% |
| `SceKernelThreadMgr` | 264 | 59% |
| `SceDriverUser` | 229 | 73% |
| `SceSysmem` | 296 | 89% |
| `SceAppMgr` | 203 | 97% |
| `SceLibc` | 1,071 | 98% |
| **`SceLibMonoBridge`** | 296 | **100%** |
| **`SceLibXml`** | 186 | **100%** |

**A title that uses `SceGxm` and the thread manager is in the best-covered part
of the emulator. A title that uses `SceLibMonoBridge` is asking for 296
functions of which none is implemented.**

**That difference is knowable from the dump alone.**

## BUILT 2026-08-23: the emulator half exists now

**`tools/hle_coverage.py` extracts it, and
`shared_layer/data/hle_coverage.json` is the artifact.**

**12,833 HLE functions indexed across three backends, 9,784 stubbed — 76%.**

| Backend | Functions | Stubbed | Modules | How coverage is read |
| --- | --- | --- | --- | --- |
| **Vita3K** | **7,377** | 85% | 149 | `STUBBED` / `UNIMPLEMENTED` inside each `EXPORT(` block |
| **eden** | **4,986** | 68% | 59 | **the emulator declares it** — see below |
| **Cemu** | 470 | 18% | 32 | `assert_dbg()`, `debugBreakpoint()`, logged "unsupported" |

**eden's signal is the best in the fleet, because it is explicit.** Its service
tables read:

```cpp
{0, D<&IManagerForSystemService::CheckAvailability>, "CheckAvailability"},
{2, nullptr, "EnsureIdTokenCacheAsync"},
```

**A `nullptr` handler *is* the declaration that the function is unimplemented.**
No marker-grepping, no inference — **the emulator states it, function by
function, with the guest's own name attached.** That is exactly the shape static
triage needs, and eden produced it without anybody asking.

**The first probes were wrong and the numbers moved a lot.** eden first read as
**222 functions** because the probe looked for a C++ method signature rather than
the service tables; Cemu read as **0% stubbed** because it does not use
`UNIMPLEMENTED` at all. **Both were under-detection, not coverage.** The rule
from earlier today applies: **when a tool and the code disagree, read the actual
line.**

## What it buys

**1. A compatibility signal with no run.** The library badge in
[`../app/GAME_DATA.md`](../app/GAME_DATA.md) currently shows what content exists.
**It could also show whether the backend implements what this title asks for.**

**2. A development priority list, computed rather than argued.** Invert the
query: **which stubbed function is imported by the most titles in the library?**
That is the highest-value thing to implement, and it is a database query rather
than an opinion.

**3. Per-game configuration before first boot.** A title that never imports the
video decoder does not need the movie path; one that imports `SceLibMonoBridge`
needs a warning, not a resolution multiplier.

**4. A compatibility sweep that costs nothing.** `CLAUDE.md` specifies a boot
sweep — launch every title, record how far it reaches — and it has never been
built because it needs the device. **A static sweep needs neither the device nor
the game to run.**

## Which backends can do this today

| Backend | Import side | HLE side | Machine-readable? |
| --- | --- | --- | --- |
| **Vita3K** | SELF/PRX | **7,377 `EXPORT(` with `STUBBED` markers** | **yes, measured** |
| **eden** | NSO/NCA | 1,522 stub markers | **likely** |
| **Cemu** | RPL imports, resolved by name | 371 stub markers | **likely** |
| xenia | **XEX, parsed in depth** | different mechanism — 3 marker hits | **needs a different probe** |
| ARMSX2, melonDS | no import table — the guest is bare metal | — | **not applicable** |

**The split is the same one found for API translation earlier today**, and for
the same reason: **a console that shipped its system software as separate
modules leaves an import table; one that linked everything into the game does
not.**

**PS2 and DS titles are bare metal.** There is no import table to read, so
static triage of this kind does not apply — **their equivalent is a signature
scan, which is what xenia's `SetupExtern` does.**

## The calibration problem, stated up front

**A `STUBBED` marker does not mean broken.** Many stubs return success and that
is correct — a logging call, a power-state query, a feature the title tolerates
missing. **`SceLibc` at 98% is not 98% broken**, because most of libc is
forwarded to the host anyway.

> **The raw fraction is a starting weight, not a score.**

**It has to be calibrated against titles known to work**: run the count over
games that already play well, and the stubs they import are proven harmless.
**What remains — stubs imported only by titles that fail — is the real signal.**

**That calibration needs no device either.** It needs a list of titles that work,
which is exactly what a compatibility ledger is.

## What is not claimed

- **The emulator half is built. The game half is not.** No import table has been
  read, so the score cannot yet be computed for any title.
- **No import table has been read.** The game side is unmeasured; only the
  emulator side is.
- **The 85% figure counts functions containing a marker anywhere in their
  body**, so a partially implemented function that logs one unimplemented branch
  is counted as stubbed. **It overstates.**
- **eden and Cemu were counted by marker frequency**, not by the per-function
  parse used for Vita3K.
