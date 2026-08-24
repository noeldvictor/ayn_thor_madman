#!/usr/bin/env python3
"""Find preprocessor guards on macros that nothing in the fork ever defines.

WHY THIS EXISTS. rpcsx guarded its LSE2 fast paths for `atomic_t<u128>` on
`ARM_FEATURE_LSE2`. There is no ACLE macro for FEAT_LSE2, so it inferred one
from `__ARM_ARCH_8_4__` and friends -- which clang on AArch64 defines at no
`-march` at all. The macro was never set, every LSE2 path was dead code, and
every 16-byte atomic ran an `ldaxp`/`stlxp` loop instead. `STLXP` takes the
cache line EXCLUSIVE, so a `try_read` peek at an SPU mailbox invalidated every
other core's copy.

    > A feature probe that cannot fire is indistinguishable from a feature the
    > hardware lacks.

It is the twelfth mechanism in shared_layer/DID_IT_APPLY.md and the hardest to
see: the guard is syntactically valid, compiles cleanly, warns about nothing,
and the fallback is CORRECT -- only slow.

    python tools/dead_guard.py                 # every fork
    python tools/dead_guard.py --fork Cemu
    python tools/dead_guard.py --min-uses 3    # only macros guarded 3+ times

THE HEURISTIC, and its one good discriminator. Real compiler and platform
predefines almost always begin with an underscore -- `__ARM_FEATURE_SVE`,
`_MSC_VER`, `__linux__`. The rpcsx macro did NOT: `ARM_FEATURE_LSE2` looks like
a compiler macro and is a project macro. So this reports guards on identifiers
that

  * do not begin with `_`,
  * are never `#define`d anywhere in the fork (including headers), and
  * are never passed with `-D` in any build file.

It reads. It never modifies a fork, and it compiles nothing.

LIMITS -- read them before believing a hit:
  * A macro defined by a BUILD SYSTEM this tool cannot parse (a generated
    config header, a vendored CMake package, an `add_definitions` behind a
    variable) reads as dead and is not. Check the build before concluding.
  * A macro deliberately left for a user to pass on the command line is a
    legitimate opt-in, not a defect.
  * A guard inside vendored code is that dependency's business; vendored trees
    are excluded, which also means a macro defined only in a vendored header
    will read as undefined. That is the main false-positive source.
  * This finds the SHAPE. Whether the dead branch mattered needs reading.
"""

import argparse
import collections
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FLEET = os.path.dirname(REPO)

FORKS = {
    "ARMSX2": "armsx2-thor/ARMSX2",
    "xenia": "xenia-thor-workspace/xenia-thor",
    "Cemu": "cemu-thor-experiment",
    "azahar": "azahar-thor/azahar",
    "melonDS": "melonds_HD/melonDS-android",
    "Vita3K": "psvita/Vita3K-Thor",
    "eden": "eden-thor",
    "GameThor": "gamethor",
    "rpcsx": "ps3-thor/rpcsx-ui-android",
}

VENDORED = re.compile(
    r"third_party|3rdparty|externals?/|dependencies|/vendor/|node_modules|"
    r"/stb/|stb_image|/xxhash/|xxhash\.h|/zlib/|/libpng/|/zstd/|/lz4/|/miniz|"
    r"/imgui/|/glslang/|/boost/|/ffmpeg/|/SDL|/fmt/|/tracy/|/dynarmic/|"
    r"/openssl/|/cubeb/|/oboe/|/teakra/|catch2|gtest|/vulkan|/spirv|"
    # Non-standard vendoring, found the same way as in bug_class_sweep.py:
    # melonDS keeps faad2, toml11, libslirp and rcheevos outside any
    # directory named like a dependency.
    r"/faad2/|/toml11/|/libslirp/|/rcheevos/|/slirp|/libui|/teaklite|/miniaudio|"
    r"/soundtouch/|/lz4|/discord|/curl/|/mbedtls/|/wxgui/|glxext|wglext", re.I)

SOURCE = ["*.cpp", "*.cc", "*.h", "*.hpp", "*.inl", "*.c"]
BUILD = ["*.txt", "*.cmake", "*.gradle", "*.gradle.kts", "*.lua", "*.mk", "*.bp"]

# Guards worth ignoring even without a leading underscore: include guards,
# well-known third-party feature macros, and the C/C++ standard's own.
IGNORE = re.compile(
    r"^(INCLUDE|GUARD|H|HPP|HH)$|_H$|_HPP$|_INCLUDED$|_GUARD$|"
    r"^(NDEBUG|DEBUG|NOMINMAX|WIN32|WIN64|UNICODE|CONSOLE|TRUE|FALSE)$|"
    r"^(GLM|VK|VMA|IMGUI|SDL|ZLIB|PNG|FMT|CATCH|DOCTEST|GTEST|BOOST|QT|Q)_",
)

# The vocabulary a CPU/GPU feature guard is written in. rpcsx's dead macro was
# ARM_FEATURE_LSE2; the same shape covers NEON, SVE, dot product, FP16, crypto
# and the SIMD width selectors every hashing library uses.
FEATUREY = re.compile(
    r"ARM_FEATURE|AARCH64_|^FEAT_|_FEAT_|NEON|ASIMD|_SVE|SVE2|_LSE|LSE2|"
    r"DOTPROD|_FP16|FP16_|_CRC32|CRC32_|_SHA[0-9]|_AES|_PMULL|I8MM|BF16|"
    r"SIMD|SSE[0-9]?|AVX|VECTOR_|_VFP|HWCAP|CPU_FEATURE|HAS_[A-Z0-9]*(NEON|SIMD|SVE|LSE|CRC|AES|SHA)",
    re.I)

GUARD = re.compile(r"#\s*(?:if|elif)\s+(?:!\s*)?defined\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*\)"
                   r"|#\s*ifdef\s+([A-Za-z_][A-Za-z0-9_]*)"
                   r"|#\s*ifndef\s+([A-Za-z_][A-Za-z0-9_]*)")
DEFINE = re.compile(r"#\s*define\s+([A-Za-z_][A-Za-z0-9_]*)")
DASH_D = re.compile(r"[-/]D\s*([A-Za-z_][A-Za-z0-9_]*)|"
                    r"add_definitions\s*\(\s*-D([A-Za-z_][A-Za-z0-9_]*)|"
                    r"add_compile_definitions\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)|"
                    r"target_compile_definitions[^)]*?([A-Z][A-Z0-9_]{3,})")


def _grep(root, pattern, globs, whole_line=False):
    """Matches for `pattern`.

    `whole_line` matters more than it looks. `-o` prints only the MATCHED
    SUBSTRING, so scanning build files for `add_compile_definitions` returned
    that literal and never the macro name inside the parentheses -- which made
    azahar's CITRA_HAS_SSE42 read as never defined when `CMakeLists.txt:213`
    defines it. Found 2026-08-25 by reading a hit.
    """
    flags = "-hIE" if whole_line else "-hoIE"
    cmd = ["git", "-C", root, "grep", "--recurse-submodules", flags, pattern,
           "--"] + globs
    try:
        r = subprocess.run(cmd, capture_output=True, timeout=300)
    except (OSError, subprocess.SubprocessError):
        return []
    return r.stdout.decode("utf-8", errors="replace").splitlines()


def _grep_with_paths(root, pattern, globs):
    cmd = ["git", "-C", root, "grep", "--recurse-submodules", "-nIE", pattern,
           "--"] + globs
    try:
        r = subprocess.run(cmd, capture_output=True, timeout=300)
    except (OSError, subprocess.SubprocessError):
        return []
    out = r.stdout.decode("utf-8", errors="replace").splitlines()
    return [ln for ln in out if not VENDORED.search(ln.split(":", 1)[0])]


def _defines_from_build_output(root):
    """Every -D that actually reached the compiler, from compile_commands.json."""
    found = set()
    seen = 0
    for base, dirs, files in os.walk(root):
        if re.search(r"vcpkg|buildtrees|node_modules", base, re.I):
            dirs[:] = []
            continue
        if "compile_commands.json" not in files:
            continue
        seen += 1
        if seen > 6:            # a handful of configurations is plenty
            break
        try:
            import json
            with open(os.path.join(base, "compile_commands.json"),
                      encoding="utf-8", errors="replace") as fh:
                entries = json.load(fh)
        except (OSError, ValueError, ImportError):
            continue
        for e in entries:
            cmd = e.get("command") or " ".join(e.get("arguments", []))
            for m in re.finditer(r"[-/]D\s*([A-Za-z_][A-Za-z0-9_]*)", cmd):
                found.add(m.group(1))
    return found


def analyse(fork, features_only=False):
    root = os.path.join(FLEET, FORKS[fork])
    if not os.path.isdir(root):
        return None

    # Everything the fork defines, ANYWHERE -- vendored included, because a
    # vendored header defining a macro makes a guard on it live.
    defined = set()
    for line in _grep(root, r"#\s*define\s+[A-Za-z_][A-Za-z0-9_]*", SOURCE):
        m = DEFINE.search(line)
        if m:
            defined.add(m.group(1))
    for line in _grep(root, r"[-/]D[A-Za-z_]|add_definitions|add_compile_definitions|"
                            r"target_compile_definitions", BUILD, whole_line=True):
        for m in DASH_D.finditer(line):
            for g in m.groups():
                if g:
                    defined.add(g)

    # GROUND TRUTH, where it exists. A build file this tool cannot parse is its
    # main false-positive source, and on 2026-08-25 it produced one immediately:
    # ARMSX2_HAS_LIBRASHADER read as dead and reaches 360 of 1967 translation
    # units. compile_commands.json is what the compiler was ACTUALLY told, which
    # is the same instrument emitted_flags.py uses and the same rule this repo
    # applies everywhere else: verify from the emitted artefact.
    defined |= _defines_from_build_output(root)

    # Guards in the fork's OWN source.
    uses = collections.defaultdict(list)
    for line in _grep_with_paths(root, r"#\s*(if|elif|ifdef|ifndef)", SOURCE):
        try:
            path, _lineno, text = line.split(":", 2)
        except ValueError:
            continue
        for m in GUARD.finditer(text):
            name = m.group(1) or m.group(2) or m.group(3)
            if not name or name.startswith("_"):
                continue          # a leading underscore means a predefine
            if IGNORE.search(name):
                continue
            uses[name].append(path)

    dead = {n: v for n, v in uses.items() if n not in defined}
    if features_only:
        dead = {n: v for n, v in dead.items() if FEATUREY.search(n)}
        uses = {n: v for n, v in uses.items() if FEATUREY.search(n)}
    return defined, uses, dead


def self_test():
    """Prove both halves work, on the real case that produced this tool.

    An instrument that can return zero must be proved able to return non-zero.
    rpcsx is the two-sided control:

      * ARM_FEATURE_LSE2 must appear as a GUARD  -> the guard scanner works
      * and must NOT be reported dead            -> the definition scanner works,
        because rpcsx fixed it with add_compile_definitions in CMakeLists

    A tool that reported "no dead guards anywhere" while unable to see a guard at
    all would look exactly like a clean fleet.
    """
    failures = 0
    got = analyse("rpcsx")
    if got is None:
        print("SKIP: rpcsx is not present beside this repo. This is not a pass.")
        return 0
    _defined, uses, dead = got

    if "ARM_FEATURE_LSE2" in uses:
        print("ok    guard scanner sees ARM_FEATURE_LSE2 (%d use(s))"
              % len(uses["ARM_FEATURE_LSE2"]))
    else:
        print("FAIL  guard scanner did NOT see ARM_FEATURE_LSE2.")
        print("      Every zero this tool reports is unsupported until this passes.")
        failures += 1

    if "ARM_FEATURE_LSE2" in dead:
        print("FAIL  ARM_FEATURE_LSE2 reported dead, but rpcsx defines it in")
        print("      app/src/main/cpp/CMakeLists.txt. The definition scanner is blind.")
        failures += 1
    else:
        print("ok    definition scanner sees the add_compile_definitions that fixed it")

    total = sum(len(analyse(f)[1]) for f in FORKS if analyse(f) is not None)
    print("ok    %d guard macro(s) seen across the fleet" % total if total
          else "FAIL  no guards seen anywhere, which cannot be true")
    if not total:
        failures += 1

    print("")
    print("%d failure(s)." % failures)
    return failures


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--fork")
    ap.add_argument("--min-uses", type=int, default=1,
                    help="only report macros guarded at least this many times")
    ap.add_argument("--limit", type=int, default=12)
    ap.add_argument("--self-test", action="store_true",
                    help="prove the scanner can see a guard and can see a "
                         "definition, on the real case that produced this tool")
    ap.add_argument("--features", action="store_true",
                    help="only HARDWARE-FEATURE guards. This is the high-value "
                         "subset: a dead ENABLE_DISCORD_RPC is a feature toggle "
                         "and harmless, while a dead ARM_FEATURE_LSE2 silently "
                         "selects a slow path nobody can see.")
    args = ap.parse_args()

    if args.self_test:
        return 1 if self_test() else 0

    forks = [args.fork] if args.fork else list(FORKS)
    print("A guard on a macro nothing defines is a branch that can never run.")
    print("READ EVERY HIT: a generated config header or an unparsed build rule")
    print("looks exactly like a dead guard from here.\n")

    for fork in forks:
        got = analyse(fork, args.features)
        if got is None:
            print("%-10s [not present beside this repo]" % fork)
            continue
        _defined, uses, dead = got
        ranked = sorted(((n, v) for n, v in dead.items() if len(v) >= args.min_uses),
                        key=lambda kv: -len(kv[1]))
        print("%-10s %d guard macro(s), %d never defined" % (fork, len(uses), len(dead)))
        for name, paths in ranked[:args.limit]:
            where = sorted(set(paths))
            print("    %-38s %2d use(s)  %s" % (name, len(paths), where[0]))
            for p in where[1:3]:
                print("    %-38s            %s" % ("", p))
        if len(ranked) > args.limit:
            print("    ... %d more, raise --limit" % (len(ranked) - args.limit))
    return 0


if __name__ == "__main__":
    sys.exit(main())
