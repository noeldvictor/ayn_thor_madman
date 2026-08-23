#!/usr/bin/env python3
"""Fleet lint: check every fork against the Thor standard.

This is the executable form of hardware_ref/thor/THOR_TARGET.md and the
standard row in CLAUDE.md. It exists because of the ranking in
shared_layer/UNIFICATION.md: a document is the weakest form of unification
because it is simply not read, and a check that fails is the strongest cheap
one.

Every rule here was found by reading the fleet on 2026-08-22 and 2026-08-23,
and every one names the evidence.

Usage:
    python tools/fleet_lint.py                 # report
    python tools/fleet_lint.py --fork ARMSX2   # one fork
    python tools/fleet_lint.py --strict        # exit 1 on any FAIL

It reports. It does not modify a fork.
"""

import argparse
import os
import re
import sys

# Fork name -> (path relative to Documents/, android app build file, native root)
FLEET = {
    "ARMSX2":   ("armsx2-thor/ARMSX2",
                 "platforms/android/app/build.gradle.kts", "pcsx2"),
    "azahar":   ("azahar-thor/azahar",
                 "src/android/app/build.gradle.kts", "src"),
    "Cemu":     ("cemu-thor-experiment",
                 "src/android/app/build.gradle.kts", "src/Cafe"),
    "eden":     ("eden-thor",
                 "src/android/app/build.gradle.kts", "src"),
    "Vita3K":   ("psvita/Vita3K-Thor",
                 "android/app/build.gradle", "vita3k"),
    "xenia":    ("xenia-thor-workspace/xenia-thor",
                 "android/android_studio_project/app/build.gradle", "src/xenia"),
    "melonDS":  ("melonds_HD/melonDS-android",
                 "app/build.gradle.kts", "melonDS-android-lib/src"),
    "GameThor": ("gamethor", "app/build.gradle.kts", None),
    "rpcsx":    ("ps3-thor/rpcsx-ui-android",
                 "app/build.gradle.kts", "app/src/main/cpp/rpcsx"),
}

VENDOR_RE = re.compile(
    r"3rdparty|third_party|externals|/deps/|dependencies|vendor/|\.cxx|vcpkg|/build/",
    re.I,
)

PASS, FAIL, WARN, SKIP = "PASS", "FAIL", "WARN", "SKIP"


def _read(path):
    try:
        with open(path, encoding="utf-8", errors="replace") as fh:
            return fh.read()
    except OSError:
        return None


# --------------------------------------------------------------------- checks

def check_abi(_fork, root, build_file, _native):
    """The Thor is arm64-v8a.

    rpcsx measured that adding x86_64 put 26 MiB compressed of unreachable code
    into a 96 MiB APK and DOUBLED the native compile. Vita3K's build FAILS on
    the ABI the device cannot run.
    See research_log/20260823_0030_abi_waste.md
    """
    if build_file is None:
        return SKIP, "no app build file mapped"
    text = _read(os.path.join(root, build_file))
    if text is None:
        return SKIP, f"{build_file} not found"
    line = None
    for m in re.finditer(r"abiFilters[^\n]*", text):
        line = m.group(0)
        break
    if line is None:
        return WARN, "no abiFilters — inherits every ABI the NDK supports"

    # azahar and rpcsx assign from a variable. Resolving it is the difference
    # between reporting the truth and reporting "I could not tell", and both
    # turned out to be arm64-only.
    if not re.search(r"['\"]", line):
        var = re.search(r"abiFilters\s*(?:\+=|=)\s*([A-Za-z_][\w.]*)", line)
        if var:
            decl = re.search(
                r"(?:val|var|def)\s+" + re.escape(var.group(1)) + r"\s*=\s*([^\n]*)",
                text,
            )
            if decl:
                line = decl.group(1)
            else:
                return WARN, f"resolves from {var.group(1)}, declared elsewhere"
    bad = [a for a in ("x86_64", "armeabi-v7a", '"x86"', "'x86'", "riscv64")
           if a in line]
    if bad:
        return FAIL, f"builds {', '.join(bad)} — the Thor runs none of them"
    if "arm64-v8a" not in line:
        return WARN, f"no arm64-v8a in: {line.strip()[:60]}"
    return PASS, "arm64-v8a only"


# Build files that can carry compiler flags. xenia keeps its in premake5.lua and
# melonDS keeps its in app/CMakeLists.txt, so walking only the native root
# misses both — a false negative found while checking this lint's own output.
BUILD_FILE_RE = re.compile(
    r"(CMakeLists\.txt|\.cmake|\.lua|\.gn|\.gni|\.gradle|\.gradle\.kts|\.mk)$", re.I
)

# x86 flags are not evidence about the arm64 target. ARMSX2's -mtune=haswell is
# in an x86 test suite, and reporting it as an arm64 finding is simply wrong.
X86_FLAG_RE = re.compile(
    r"haswell|sandybridge|nehalem|znver|skylake|atom|x86-64|core-avx|apple-[am]\d", re.I
)


def _arch_flags(root):
    """Every -march/-mtune in the fork's own build files, x86 ones excluded."""
    march, mtune = [], []
    for dirpath, _dirs, files in os.walk(root):
        if VENDOR_RE.search(dirpath.replace("\\", "/")):
            continue
        for name in files:
            if not BUILD_FILE_RE.search(name):
                continue
            text = _read(os.path.join(dirpath, name)) or ""
            for m in re.finditer(r"-march=([\w.+-]+)", text):
                if not X86_FLAG_RE.search(m.group(1)):
                    march.append(m.group(1))
            for m in re.finditer(r"-mtune=([\w.+-]+)", text):
                if not X86_FLAG_RE.search(m.group(1)):
                    mtune.append(m.group(1))
    return march, mtune


def check_target_features(_fork, root, _build_file, _native):
    """Name the device's features explicitly, and never target armv9-a.

    All four cores are ARMv9 and ARMv9.0-A mandates SVE2, but this SoC exposes
    no SVE. A compiler told armv9-a may emit instructions that trap here.
    See hardware_ref/thor/THOR_TARGET.md
    """
    march, mtune = _arch_flags(root)
    if any(a.startswith("armv9") for a in march):
        return FAIL, "targets armv9-a — this device exposes no SVE"
    feats = [a for a in march if "dotprod" in a or "sha3" in a]
    tuned = [t for t in mtune if t.startswith("cortex") or t.startswith("neoverse")]
    if feats and tuned:
        return PASS, f"-march={feats[0]} -mtune={tuned[0]}"
    if feats:
        return WARN, f"-march={feats[0]} but no -mtune"
    if tuned:
        return WARN, f"-mtune={tuned[0]} but baseline -march"
    return WARN, "baseline: names none of the device's features"


def check_namespaces(_fork, root, _build_file, native):
    """Seven emulators in one binary share one global namespace.

    Not a duplication problem — an ISOLATE problem. The risk is the long tail
    of helper types at global scope, not the famous class names, which are the
    ones people remembered to namespace.
    See shared_layer/UNIFICATION.md section 7
    """
    if native is None:
        return SKIP, "no native root mapped"
    total = bare = 0
    for dirpath, _dirs, files in os.walk(os.path.join(root, native)):
        if VENDOR_RE.search(dirpath.replace("\\", "/")):
            continue
        for name in files:
            if not name.endswith((".h", ".hpp")):
                continue
            total += 1
            text = _read(os.path.join(dirpath, name)) or ""
            if not re.search(r"^namespace\s", text, re.M):
                bare += 1
    if total == 0:
        return SKIP, "no headers found"
    pct = bare * 100 // total
    msg = f"{bare}/{total} headers at global scope ({pct}%)"
    if pct >= 50:
        return FAIL, msg + " — will collide in a packed binary"
    if pct >= 20:
        return WARN, msg
    return PASS, msg


def check_libretro(_fork, root, _build_file, native):
    """libretro is rejected by this project.

    Dead weight that survives an extraction because nobody asks what it is for.
    """
    if native is None:
        return SKIP, "no native root mapped"
    hits = []
    for dirpath, _dirs, files in os.walk(os.path.join(root, native)):
        if VENDOR_RE.search(dirpath.replace("\\", "/")):
            continue
        for name in files:
            if "libretro" in name.lower():
                hits.append(name)
    if hits:
        return WARN, f"carries libretro glue: {', '.join(sorted(set(hits))[:3])}"
    return PASS, "no libretro glue"


CHECKS = [
    ("abi", check_abi),
    ("target-features", check_target_features),
    ("namespaces", check_namespaces),
    ("libretro", check_libretro),
]


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--root", default=os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "..", ".."))
    ap.add_argument("--fork")
    ap.add_argument("--strict", action="store_true",
                    help="exit 1 if any check FAILs")
    args = ap.parse_args()

    documents = os.path.abspath(args.root)
    tally = {PASS: 0, FAIL: 0, WARN: 0, SKIP: 0}
    print(f"fleet lint  (forks under {documents})\n")

    for fork, (rel, build_file, native) in sorted(FLEET.items()):
        if args.fork and args.fork.lower() != fork.lower():
            continue
        root = os.path.join(documents, rel)
        if not os.path.isdir(root):
            print(f"{fork:9} -- not on disk at {rel}")
            continue
        print(f"{fork}")
        for name, fn in CHECKS:
            try:
                status, detail = fn(fork, root, build_file, native)
            except Exception as exc:                      # a lint must not crash
                status, detail = SKIP, f"check raised {type(exc).__name__}: {exc}"
            tally[status] += 1
            print(f"   {status:4} {name:16} {detail}")
        print()

    print(f"PASS {tally[PASS]}   FAIL {tally[FAIL]}   "
          f"WARN {tally[WARN]}   SKIP {tally[SKIP]}")
    if args.strict and tally[FAIL]:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
