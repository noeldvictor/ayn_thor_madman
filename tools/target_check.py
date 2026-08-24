#!/usr/bin/env python3
"""Assert the project's compile target emits the instructions it is meant to.

WHY THIS EXISTS. `hardware_ref/thor/THOR_TARGET.md` names one compile line for
the whole fleet. On 2026-08-24 three separate traps were found in it or beside
it, all invisible without compiling something:

  * `+rcpc` was MISSING. With it an acquire load is `ldapr`; without it `ldar`.
    `-mtune` adds no features -- only `-march`/`-mcpu` do -- so the line looked
    complete and was not.
  * `-mno-outline-atomics` ALONE IS WORSE THAN THE DEFAULT. Without `+lse` the
    compiler cannot emit an LSE atomic, so removing the outline helper only
    removes the runtime upgrade path and leaves an `ldaxr`/`stlxr` loop.
  * `-mcpu=cortex-x3` and `-march=armv9-a` DEFINE `__ARM_FEATURE_SVE`. Clang
    models the core; these cores implement SVE2 and Qualcomm did not expose it.
    A compile-time dispatcher such as xxHash then selects SVE over NEON with no
    runtime fallback.

    > A flag list is a claim about emitted code. This checks the claim.

    python tools/target_check.py            # check the recorded target
    python tools/target_check.py --verbose  # show the emitted instructions
    python tools/target_check.py "--flags=-mcpu=cortex-x3"   # note the = form

Use `--flags=...` with an equals sign: a value beginning with `-` is otherwise
parsed as another option.

PROVEN AGAINST THE FOUR REAL TRAPS, 2026-08-24:
  * the pre-2026-08-24 target line          -> FAIL "acquire load uses LDAPR"
  * `-mcpu=cortex-x3`                       -> FAIL "no SVE is selected"
  * `-mcpu=cortex-x3+nosve+nosve2`          -> FAIL "no SVE is selected", because
    NO negative attribute clears __ARM_FEATURE_SVE2 from the preprocessor. The
    surviving state -- SVE2 defined, SVE not -- describes no real machine, and
    acting on it is a hard compile error rather than a SIGILL.
  * `-mno-outline-atomics` on its own       -> FAIL "atomic RMW is a single LSE instruction"

Exit status is non-zero if any assertion fails, so this is usable as a gate.

It compiles tiny probes into assembly. It never links, never runs anything on a
device, and never touches a fork.

LIMITS:
  * Needs an Android NDK clang. Without one this reports SKIP, not PASS.
  * Assembly is checked, not behaviour. `ldapr` being emitted does not prove it
    is faster -- that is DEVICE_QUEUE entry 23.
  * The NDK found here may differ from the one the standard row pins.
"""

import argparse
import glob
import os
import re
import subprocess
import sys
import tempfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Kept in one place so a change to THOR_TARGET.md and a change here are one edit.
TARGET_FLAGS = (
    "-march=armv8.2-a+crc+lse+fp16+dotprod+sha3+i8mm+bf16+rcpc "
    "-mtune=cortex-x3"
)
TRIPLE = "aarch64-linux-android33"

PROBES = [
    {
        "name": "acquire load uses LDAPR",
        "why": "+rcpc. Without it clang emits the stronger, costlier LDAR.",
        "source": """#include <atomic>
int f(std::atomic<int>& a){ return a.load(std::memory_order_acquire); }
""",
        "want": r"\bldapr\b",
        "reject": r"\bldar\b",
    },
    {
        "name": "seq_cst load still uses LDAR",
        "why": "LDAPR provides RCpc and seq_cst needs RCsc. A seq_cst load that "
               "became LDAPR would be a correctness bug, not an optimisation.",
        "source": """#include <atomic>
int f(std::atomic<int>& a){ return a.load(std::memory_order_seq_cst); }
""",
        "want": r"\bldar\b",
        "reject": r"\bldapr\b",
    },
    {
        "name": "atomic RMW is a single LSE instruction",
        "why": "+lse. The NDK's arm64-v8a ABI is Armv8.0-A, so clang's default is "
               "-moutline-atomics: a call to a stub that loads a feature flag and "
               "branches. Vita3K found every atomic in its shipped .so going through it.",
        "source": """#include <atomic>
long f(std::atomic<long>& a){ return a.fetch_add(-1, std::memory_order_acq_rel); }
""",
        "want": r"\bldaddal\b",
        "reject": r"__aarch64_ldadd|\bldaxr\b",
    },
    {
        "name": "no SVE is selected",
        "why": "The Thor reports no sve in /proc/cpuinfo. -march=armv9-a and "
               "-mcpu=cortex-x3 both define __ARM_FEATURE_SVE, and a compile-time "
               "dispatcher like xxHash then picks SVE over NEON with no fallback -- "
               "confirmed emitted: uqadd z0.b. AND NO NEGATIVE ATTRIBUTE FIXES IT: "
               "+nosve, +nosve2 and both together all leave __ARM_FEATURE_SVE2 "
               "DEFINED, so this probe must test BOTH macros. rpcsx measured that "
               "-sve/-sve2 clears SVE, and that is true of the LLVM BACKEND it calls "
               "through setMAttrs; the clang FRONTEND is the half we compile on.",
        "source": """#if defined(__ARM_FEATURE_SVE) || defined(__ARM_FEATURE_SVE2)
#error "SVE selected on a device that has none"
#endif
int f(void){ return 0; }
""",
        "want": None,          # compiling at all is the assertion
        "reject": None,
    },
]


def find_clang():
    roots = [
        os.path.expanduser("~/AppData/Local/Android/Sdk/ndk"),
        os.path.expanduser("~/Android/Sdk/ndk"),
        "/opt/android-sdk/ndk",
    ]
    for root in roots:
        for ndk in sorted(glob.glob(os.path.join(root, "*")), reverse=True):
            for exe in ("clang++.exe", "clang++"):
                for host in ("windows-x86_64", "linux-x86_64", "darwin-x86_64"):
                    p = os.path.join(ndk, "toolchains", "llvm", "prebuilt", host, "bin", exe)
                    if os.path.isfile(p):
                        return p, os.path.basename(ndk)
    return None, None


def compile_to_asm(clang, flags, source):
    with tempfile.TemporaryDirectory() as d:
        src = os.path.join(d, "probe.cpp")
        with open(src, "w", encoding="utf-8") as fh:
            fh.write(source)
        cmd = [clang, "--target=" + TRIPLE] + flags.split() + ["-O2", "-S", "-o", "-", src]
        try:
            r = subprocess.run(cmd, capture_output=True, timeout=120)
        except (OSError, subprocess.SubprocessError) as e:
            return None, str(e)
        if r.returncode != 0:
            return None, r.stderr.decode("utf-8", errors="replace").strip()[:300]
        return r.stdout.decode("utf-8", errors="replace"), None


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--flags", default=TARGET_FLAGS)
    ap.add_argument("--verbose", action="store_true")
    args = ap.parse_args()

    clang, ndk = find_clang()
    if not clang:
        print("SKIP: no Android NDK clang found. This is not a pass.")
        return 0

    print("ndk    %s" % ndk)
    print("target %s" % TRIPLE)
    print("flags  %s\n" % args.flags)

    failures = 0
    for probe in PROBES:
        asm, err = compile_to_asm(clang, args.flags, probe["source"])
        if asm is None:
            print("FAIL  %s" % probe["name"])
            print("        %s" % probe["why"])
            print("        compile failed: %s" % err)
            failures += 1
            continue

        body = "\n".join(l for l in asm.splitlines() if l.startswith(("\t", "    ")))
        ok = True
        detail = ""
        if probe["want"] and not re.search(probe["want"], body):
            ok, detail = False, "expected %s, not emitted" % probe["want"]
        if ok and probe["reject"] and re.search(probe["reject"], body):
            ok, detail = False, "found %s, which this target should avoid" % probe["reject"]

        print("%s  %s" % ("PASS " if ok else "FAIL ", probe["name"]))
        if not ok:
            print("        %s" % probe["why"])
            print("        %s" % detail)
            failures += 1
        if args.verbose:
            for line in body.splitlines():
                if re.search(r"\b(ldapr|ldar|ldaddal|ldaxr|stlxr|bl)\b", line):
                    print("        %s" % line.strip())

    print("\n%d probe(s), %d failure(s)" % (len(PROBES), failures))
    if failures:
        print("A failing probe means the emitted code no longer matches what")
        print("hardware_ref/thor/THOR_TARGET.md claims. Fix the flags or the claim.")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
