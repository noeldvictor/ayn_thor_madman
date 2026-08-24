#!/usr/bin/env python3
"""Sweep the fleet for the bug CLASSES that have already paid somewhere.

WHY THIS EXISTS. Reading the fleet's own measurement record produced one result
that dominates the rest:

    > This emulator's ARM64 and GPU paths are already well matched to the
    > hardware. The wins have come from code that was BROKEN, not code that was
    > SLOW.   -- rpcsx, docs/arm64/adreno-tiler.md

Every optimisation this fleet reasoned its way to was refuted: the native render
rearch, bindless, EOR3 fusion, TBL2-for-TBX2, the LOAD_OP_CLEAR conversion, the
A510 shared VPU, ISB-for-yield. Every win was something broken: a stale config, a
guard on the wrong launch path, a guest index used as a host index, a timing
constant correct on x86, an extension the device layer never asked for.

AND EVERY ONE OF THOSE IS A CLASS, NOT AN INSTANCE. A guest index used as a host
index was found in xenia and then in eden. So the useful move is not to find one
more instance by hand -- it is to sweep every fork for the shape.

    python tools/bug_class_sweep.py                  # every class
    python tools/bug_class_sweep.py --class affinity
    python tools/bug_class_sweep.py --list
    python tools/bug_class_sweep.py --class timing --show

It reads. It never modifies a fork.

WHAT A HIT IS AND IS NOT. Every pattern here is a SHAPE, not a diagnosis. A hit
means "this line has the shape of a bug that cost another fork real performance";
it does not mean the bug is present. Every one must be read. The two confirmed
instances of the affinity class were both found by reading, and one of them --
Cemu's -- would have been a false positive if counted rather than read.
"""

import argparse
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
    r"third_party|3rdparty|externals|dependencies|/vendor/|node_modules|"
    r"vulkan_core|volk|vk_mem_alloc|/imgui/|/glslang/|/boost/|/ffmpeg/|/SDL|"
    r"toml11|xbyak|oaknut|vixl|catch2|gtest|/proot/|virglrenderer|"
    r"/gallium/|sysnums-|/wine/|/box64/|/fex/", re.I)


# SUBMODULES. `git grep` in a parent repository DOES NOT SEE SUBMODULE CONTENTS.
# That blind spot produced three wrong results in this project: dynarmic in
# Vita3K, xxHash in Vita3K, and a fleet SVE search that reported Vita3K as clean
# when its vendored xxHash carries five SVE branches. Every scan below therefore
# passes --recurse-submodules.
#
# NOTE THE TENSION WITH THE VENDORED FILTER: excluding vendored trees is right
# for "which fork IMPLEMENTS this", and wrong for "what will COMPILE INTO the
# binary". A dependency's code is not the fork's work, but it is in the product.

SOURCE = ["*.cpp", "*.cc", "*.h", "*.hpp", "*.inl", "*.kt", "*.java"]

CLASSES = {
    "affinity": {
        "what": "A GUEST core index used as a HOST core index.",
        "paid": "xenia: guest CPUs 0-2 pinned to the three A510 little cores, and "
                "the X3 never got guest work. eden: PinCurrentThreadToPerformanceCore "
                "pins to host 0-3, which are little cores here, tuned for a SoC with none.",
        "why": "Correct on homogeneous hardware, silently wrong on big.LITTLE, and "
               "wrong in the worst direction because guest core 0 is usually the main thread.",
        "pattern": r"set_affinity|sched_setaffinity|SetThreadAffinity|CPU_SET|"
                   r"PinCurrentThread|affinity_mask",
    },
    "timing": {
        "what": "A spin or delay whose constant assumes a fast free-running counter.",
        "paid": "rpcsx: busy_wait(500) x 50 is ~1 microsecond on a 3 GHz TSC and "
                "1.3 ms on this chip's 19.2 MHz generic timer -- 74% of all cycles.",
        "why": "CNTFRQ_EL0 is 19.2 MHz here. A tick count tuned on x86 is ~156x longer "
               "per tick. Vita3K has the cure: derive a wall-clock budget from CNTFRQ_EL0.",
        # BROADENED after the first run returned zero for eight forks, which is
        # the all-zeros signature this project's own rule says to suspect.
        # busy_wait(N) is an rpcs3 idiom; other forks spell a spin differently.
        # The diagnostic is "spins on a literal count AND does not derive a budget
        # from CNTFRQ_EL0" -- run the cntfrq class beside this one.
        "pattern": r"busy_wait|spin_wait|SpinWait|spin_count|kSpinCount|"
                   r"MAX_SPIN|spin_limit|SPIN_ITER|retry_count *= *[0-9]{2,}|"
                   r"[0-9]{2,} *\* *1000 *\* *1000|nanosleep|usleep *\( *[0-9]+",
    },
    "cntfrq_aware": {
        "what": "Reads CNTFRQ_EL0 / CNTVCT_EL0 -- i.e. derives a real time budget.",
        "paid": "This is the CURE, not the disease. Vita3K's spin_wait.h derives its "
                "budget from CNTFRQ_EL0 'rather than from an iteration count tuned on "
                "x86', and records 19.2 MHz on the Qualcomm parts.",
        "why": "Run this beside the timing class. A fork that spins and does NOT appear "
               "here is the candidate; a fork in both may already be correct.",
        "pattern": r"cntfrq|CNTFRQ|cntvct|CNTVCT|counter_frequency|"
                   r"QueryPerformanceFrequency",
    },
    "absent_feature_selected": {
        "what": "Code selected at COMPILE TIME by a feature this device does not have.",
        "paid": "Not yet paid here -- demonstrated, not observed. -march=armv9-a and "
                "-mcpu=cortex-x3 both define __ARM_FEATURE_SVE on this box's clang, and "
                "xxhash.h tests it BEFORE NEON with no runtime fallback. The Thor has no "
                "SVE, so the result would be SIGILL at the first hash, not a slower hash.",
        "why": "Clang models the CORE; these cores implement SVE2 and Qualcomm did not "
               "expose it. Use -mtune, never -mcpu, and never -march=armv9-a. eden ships "
               "a YUZU_BUILD_PRESET=armv9 that sets it.",
        # Vendored code counts here: it compiles into the binary even though it is
        # not the fork's own work. This is the class where the VENDORED filter is
        # WRONG, so read the hits with that in mind.
        "pattern": r"__ARM_FEATURE_SVE|arm_sve\.h|svbool_t|svfloat|"
                   r"mcpu=cortex-|march=armv9",
        # THE ONE CLASS WHERE VENDORED CODE COUNTS. A dependency is not the fork's
        # own work, but it compiles into the product and will execute. Excluding
        # 3rdparty/ here hid ARMSX2's seven files entirely.
        "include_vendored": True,
    },
    "x86_only_fastpath": {
        "what": "A fast path guarded on an x86 macro, with no ARM sibling in the guard.",
        "paid": "rpcsx: Crypto/aesni.cpp is entirely behind #if defined(__SSE2__), so every "
                "AES operation runs the four-table software path on a chip reporting "
                "aes/pmull/sha1/sha2/sha3 -- on the boot path, 1187 modules for one title. "
                "ARMSX2: BC7DECOMP_USE_SSE2 with zero ARM/NEON in the file, so BC7 texture "
                "blocks decode scalar.",
        "why": "The fallback is correct, so nothing crashes, nothing fails a test and nothing "
               "warns. Only the reference implementation runs. CORRECT code names ARM64 beside "
               "x86 in the same guard -- Cemu's fast_float, Vita3K's spin_wait and eden's "
               "uint128 all do. Look for an x86 macro with NO ARM sibling.",
        "pattern": r"defined\(__SSE2__\)|defined\(__AES__\)|defined\(_M_X64\)|"
                   r"defined\(__AVX2__\)|defined\(__BMI2__\)",
    },
    "stale_default": {
        "what": "A persisted setting that can outlive and override a compiled default.",
        "paid": "xenia: three rlwinm fastpaths were defaultEnabled=true in code and "
                "false on the device, costing 2.88%. Every number that session was on "
                "a handicapped baseline.",
        "why": "A persisted value survives the process, the install and the app update. "
               "A validated optimisation can stay silently off forever.",
        "pattern": r"defaultEnabled|default_enabled|LoadConfig|SaveConfig|"
                   r"config\.toml|persisted|getPersisted|SettingsWrap",
    },
    "wrong_launch_path": {
        "what": "A default applied only on a code path the real launch does not take.",
        "paid": "xenia: the AOT object cache was enabled only when NO cvar bundle was "
                "supplied, and the launcher always supplies one -- so headless runs "
                "filled a 111 MB cache while every real launch recompiled ~10,000 functions.",
        "why": "The cache directory being full made it look like it worked. Verify a "
               "hit; never infer one from a non-empty cache.",
        "pattern": r"getBundleExtra|getExtras\(\) *== *null|EXTRA_[A-Z_]+ *\) *== *null|"
                   r"intent\.[a-zA-Z]+ *== *null",
    },
    "unrequested_capability": {
        "what": "Code that wants a device feature its own device layer never requests.",
        "paid": "ARMSX2: frame generation runs fp32 because PCSX2's Vulkan backend "
                "never asks for VK_KHR_shader_float16_int8, so a Float16 shader module "
                "would be invalid usage regardless of what the device reports.",
        "why": "A backend's ceiling is an accident of its fork's portability history. "
               "tools/vk_capability_census.py measures the spread.",
        "pattern": r"never asks for|not enabled by|would be invalid usage|"
                   r"device layer never|is not requested|unsupported by our device",
    },
    "declared_and_unused": {
        "what": "A type or key declared for a feature that was never built.",
        "paid": "eden: PatchCacheKey has a std::hash specialisation and zero uses, so "
                "NCE re-patches the whole text segment every launch. ErrorSavestate is "
                "an enum value the core never raises.",
        "why": "Dead scaffolding reads as a feature. Both of eden's cost real work per "
               "launch or hide a missing capability.",
        "pattern": r"struct [A-Za-z]*CacheKey|class [A-Za-z]*CacheKey|"
                   r"[A-Za-z]*CacheKey *\{",
    },
}


def scan(fork, pattern, include_vendored=False):
    root = os.path.join(FLEET, FORKS[fork])
    if not os.path.isdir(root):
        return None
    cmd = ["git", "-C", root, "grep", "--recurse-submodules",
           "-nIE", pattern, "--"] + SOURCE
    try:
        r = subprocess.run(cmd, capture_output=True, timeout=240)
    except (OSError, subprocess.SubprocessError):
        return []
    out = r.stdout.decode("utf-8", errors="replace").splitlines()
    if include_vendored:
        return out
    return [ln for ln in out if not VENDORED.search(ln.split(":", 1)[0])]


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--class", dest="cls", help="one class (default: all)")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--show", action="store_true", help="print matching lines")
    ap.add_argument("--lines", type=int, default=4)
    args = ap.parse_args()

    if args.list:
        for name, c in CLASSES.items():
            print("%-24s %s" % (name, c["what"]))
        return 0

    names = [args.cls] if args.cls else list(CLASSES)
    for name in names:
        c = CLASSES.get(name)
        if not c:
            print("unknown class %r. --list shows them." % name)
            return 1
        print("\n=== %s ===" % name)
        print("SHAPE   %s" % c["what"])
        print("PAID    %s" % c["paid"])
        print("WHY     %s" % c["why"])
        for fork in FORKS:
            hits = scan(fork, c["pattern"], c.get("include_vendored", False))
            if hits is None:
                print("  %-10s [not present beside this repo]" % fork)
                continue
            files = sorted({h.split(":", 1)[0] for h in hits})
            print("  %-10s %3d line(s) in %d file(s)" % (fork, len(hits), len(files)))
            if args.show:
                for h in hits[:args.lines]:
                    print("      %s" % h[:150])

    print("\nA HIT IS A SHAPE, NOT A DIAGNOSIS. Every one must be read.")
    print("Two instances of the affinity class were confirmed by reading; counting")
    print("them would have produced at least one false positive.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
