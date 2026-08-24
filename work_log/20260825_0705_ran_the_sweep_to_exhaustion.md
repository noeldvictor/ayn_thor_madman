# Ran the bug-class sweep against its own limit: two tool defects fixed, still no new instance

**Goal: `shared_layer/DID_IT_APPLY.md` records its own limit — every instance was
found by reading, the tools were written afterwards, and none has caught a new
one. Try to make one.**

**Result: the limit stands, and it now stands after an attempt rather than by
default. Two real instrument defects were found and fixed on the way.**

## Defect 1: the class matched xenia's vocabulary, so it could only find xenia's bug

`wrong_launch_path` was written from xenia's AOT-object-cache bug and its pattern
was `getBundleExtra`, `getExtras() == null`, `EXTRA_[A-Z_]+ == null`. **Swept the
fleet: one fork hit, and it was xenia.**

**A detector that can only match the case it was written from is not a
detector.** Broadened it to the *shape* — a guard on **how the process was
started** — adding `savedInstanceState == null`, `hasExtra(`, `getAction()`,
`isTaskRoot`.

**14 new hits across four forks. Read every one. Zero were bugs.** All were
`savedInstanceState == null` guarding fragment creation, or `hasExtra` reading an
optional command parameter — **the correct Android idiom in both cases.**

> **If the shape you are matching is also how correct code looks, you are
> counting the idiom, not the defect.**

**So the class needed two patterns and a distance, not one broader pattern.**
What made xenia's case a bug was never the guard — it was that the guard
**wrapped a default**. `scan()` gained a `near` parameter: a second pattern that
must appear within N lines of the first.

| | hits outside xenia |
| --- | --- |
| original pattern | **0** — could only find its own case |
| broadened | **14**, all correct code |
| **broadened + `near`** | **1**, and reading it showed a false positive |

The survivor was melonDS's `ShortcutSetupActivity:75`, matched because
`RomEnableCriteria` contains `Enable` followed by a capital. **Read, dismissed,
and left in — a class with a visible false-positive rate is more honest than one
tuned until it reports nothing.**

## Defect 2: the vendored filter is path-based, and one fork does not use paths

**`x86_only_fastpath` showed melonDS with 10 hits in 2 files.** melonDS is one of
the four forks that has contributed no instance, so this looked like the new one.

**Every hit was vendored code**: `melonDS-android-lib/src/stb/stb_image.h` and
`melonDS-android-lib/src/xxhash/xxhash.h`.

**The filter matched `third_party|3rdparty|externals?/|dependencies|/vendor/`.**
melonDS vendors into a directory named after itself, so **the filter saw none of
it and every hit read as the fork's own work.**

> **A vendored library is not always in a directory named like one. Match the
> LIBRARY, not the convention.**

Added `stb`, `xxhash`, `zlib`, `libpng`, `lodepng`, `zstd`, `lz4`, `miniz`,
`tinyxml`, `rapidjson`, `nlohmann`, `cubeb`, `oboe`, `openssl`, `tracy`,
`dynarmic`, `teakra`, `discord`, `cpp-httplib`, `inih`, `spdlog`, `cryptopp` —
to **`bug_class_sweep.py` and `capability_probe.py`**, which had the same
path-shaped filter.

**Verified it did not over-filter:** `capability_probe.py --self-test` still
passes all nine positive controls, and **Vita3K's fork-own `spin_wait.h` hit
survives** while melonDS's vendored hits vanish. **melonDS 10 to 0, and the class
is unchanged everywhere else.**

**This affects earlier counts.** Any fleet-wide number taken from these two tools
before today may have counted melonDS's vendored `stb` and `xxhash` as melonDS's
own code. **The SVE survey is not affected** — it used a raw `git grep` and every
hit was read and identified as xxHash at the time.

## The class that must NOT be filtered, and is not

`absent_feature_selected` sets `include_vendored`, deliberately: **a vendored
library selecting a feature this device does not have is the entire xxHash-SVE
trap.** Its counts went up rather than down, which is correct. Checked ARMSX2's
43 lines: `3rdparty/include/xxhash.h` and `third_party/xbyak_aarch64/.../
util_impl_linux.h` — **the known SVE case, and a legitimate CPU-feature read.**

## The honest result

**No new instance of any class was found, in any fork.** Four classes were run to
exhaustion — `wrong_launch_path`, `x86_only_fastpath`, `declared_and_unused`,
`absent_feature_selected` — and every non-zero hit outside the known cases was
read and dismissed.

**`DID_IT_APPLY.md`'s limit is unchanged and is now evidence rather than
modesty.** The two defects fixed here are the reason to keep trying: **both would
have made a future sweep lie**, one by matching only its own case and one by
attributing a dependency's code to a fork.

## Files

- `tools/bug_class_sweep.py` — `scan(near=, window=)`, broadened
  `wrong_launch_path` with a `near` clause, extended `VENDORED`
- `tools/capability_probe.py` — extended `VENDORED`
