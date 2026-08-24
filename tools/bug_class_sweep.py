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
    r"third_party|3rdparty|externals?/|dependencies|/vendor/|node_modules|"
    r"vulkan_core|volk|vk_mem_alloc|/imgui/|/glslang/|/boost/|/ffmpeg/|/SDL|"
    r"toml11|xbyak|oaknut|vixl|catch2|gtest|/proot/|virglrenderer|"
    r"/gallium/|sysnums-|/wine/|/box64/|/fex/|/stb/|stb_image|stb_vorbis|/xxhash/|xxhash\.h|/zlib/|zlib\.h|/libpng/|/lodepng|/zstd/|/lz4/|/miniz|/tinyxml|/rapidjson/|/nlohmann/|/json\.hpp|/cubeb/|/oboe/|/openssl/|/tracy/|/dynarmic/|/teakra/|/discord|/cpp-httplib|/inih/|/fmt/|/spdlog/|/cryptopp/", re.I)


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
        # An x86 guard counts only when NO ARM token follows within the window.
        # Added 2026-08-25 after rpcsx recorded getting exactly this wrong: it
        # read an `#if defined(ARCH_X64)` guard, stopped before the `#endif`,
        # and concluded ARM had no arm -- the AArch64 branch was twenty lines
        # below inside the same conditional. A line-based grep cannot see that.
        # Any of these nearby means the hit is not a defect: an ARM sibling in
        # or under the guard, or -- Cemu's precompiled.h:29 -- a guard whose
        # body DEFINES the architecture token rather than gating a fast path.
        "near_absent": r"__aarch64__|_M_ARM64|ARCH_ARM64|__ARM_NEON|arm_neon\.h|"
                       r"defined\(__arm|AArch64|aarch64|#\s*define\s+ARCH_",
        "window": 30,
        "pattern": r"defined\(__SSE2__\)|defined\(__AES__\)|defined\(_M_X64\)|"
                   r"defined\(__AVX2__\)|defined\(__BMI2__\)",
    },
    "x86_correction_on_arm": {
        "what": "A hand-written correction for an x86 quirk, applied on a target that "
                "does not have the quirk.",
        "paid": "rpcsx: SPU CFLTS was INCORRECT on ARM64. x86 CVTTPS2DQ returns 0x80000000 "
                "on overflow so the shared code XORs it to 0x7fffffff; FCVTZS already returns "
                "0x7fffffff and the same XOR turns it into 0x80000000. Every value at or above "
                "2^31 was wrong, upstream included. CFLTU was correct but redundant.",
        "why": "The most dangerous x86-detour form: the others cost speed, this one changes "
               "RESULTS, on values a test may never reach. CORRECT code states which parts of "
               "the guest semantics the host already provides -- ARMSX2's iCOP2-arm64.cpp says "
               "'finite overflow and +/-Inf already saturate correctly in Fcvtzs; only NaN lanes "
               "need the fixup'.",
        # TIGHTENED after the first version matched any 0x7FFFFFFF constant and
        # returned 2,498 lines in one fork. A bare saturation constant is
        # everywhere; only the x86 CONVERSION intrinsics identify the shape.
        # Even so this class cannot be settled by a regex -- every hit needs
        # reading, because most are genuine guest semantics.
        # rpcsx names the mechanical tell for this class, and it is sharper than
        # a list of conversion intrinsics: AN XOR AGAINST A SIGN-EXTENDED
        # COMPARISON, which is the shape a saturation fix-up takes. It swept that
        # across both its translators and found three sites, all correct -- two
        # being the fix working as intended, one being the GUEST's own semantics.
        # The lens is exhausted for that shape and NOT for a correction spelled as
        # a select, a clamp before a conversion, or a literal limit.
        "pattern": r"cvttps|CVTTPS|_mm_cvtt|fptosi|fptoui|CVTTSS|CVTTSD|"
                   r"eor.*sext|sext.*eor|_mm_xor.*cmp|xor.*cmpgt",
    },
    "setting_written_by_multiple_writers": {
        "what": "One setting written from more than one place, where a later writer "
                "silently overwrites the file the operator edited.",
        "paid": "rpcsx: 'Max LLVM Compile Threads' lived in config.yml, AND was set by "
                "ThorPerformanceProfile on EVERY boot, AND was carried in the game's "
                "GameSettingsDatabase profile. Editing the config alone was undone on "
                "the next launch, so freeing the CPU affinity mask looked like it did "
                "nothing -- the concurrency cap was still 2.",
        "why": "This is emitted_flags.py's rule for RUNTIME settings. A setting that "
               "exists is not a setting that applies, and the mechanism here is not a "
               "wrong default but a SECOND WRITER that runs later. It is this project's "
               "own risk: the per-game override design has three tiers, and ARMSX2 "
               "already shipped two bugs in that area. A profile applier that writes on "
               "every boot bypasses ConfigStore's change-tracking. Read the hits: a "
               "setter is normal, and the finding is a setter that runs unconditionally "
               "at startup over a value a person can edit.",
        "pattern": r"setSetting|SetSetting|applyProfile|ApplyProfile|"
                   r"PerformanceProfile|GameSettingsDatabase|overrideSetting|"
                   r"forceSetting|writeDefaults|applyDefaults",
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
        # A launch-shape guard ALONE is the correct Android idiom, so it is
        # paired with a config-write shape that must appear within 8 lines.
        # Without the pairing this class found 14 correct fragment-creation
        # guards and nothing else. See scan()'s docstring.
        "near": r"setDefault|putBoolean|putString|putInt|Default\(|"
                r"cvar|Cvar|CVar|setSetting|SetSetting|config\.|Config\.|"
                r"enable[A-Z]|Enable[A-Z]|\.path *=|Path *=",
        # BROADENED 2026-08-25. The first version matched xenia's own vocabulary
        # and therefore only ever found xenia's own bug -- which is a detector
        # that cannot catch a new instance. The SHAPE is: a default or an
        # initialisation applied inside a guard on HOW the process was started.
        # On Android that guard is usually a null intent extra, a first-creation
        # check, or a launch-source test.
        "pattern": r"getBundleExtra|getExtras\(\) *== *null|EXTRA_[A-Z_]+ *\) *== *null|"
                   r"intent\.[a-zA-Z]+ *== *null|"
                   r"savedInstanceState *== *null|"
                   r"getStringExtra\([^)]*\) *[?=!]= *null|"
                   r"hasExtra\(|"
                   r"getAction\(\) *[!=]= *null|"
                   r"callingActivity *== *null|isTaskRoot",
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
    "path_as_identity": {
        "what": "Durable per-game data keyed on a FILE PATH rather than on the "
                "game's own identity.",
        "paid": "Three instances found 2026-08-25, all by reading. azahar and eden "
                "both key their Coil icon cache on data.path; ARMSX2 keys its cover "
                "pixmap cache and its invalidation on the path; and eden's Game class "
                "keys keyAddedToLibraryTime and keyLastPlayedTime on the path while "
                "keying settings on programId four lines above.",
        "why": "A path is not identity. Move, rename or re-dump and the data is "
               "orphaned; worse for a CACHE, where two different dumps at one path "
               "COLLIDE and the previous game's icon is served. The first two "
               "instances are caches and recover. The third is play history and does "
               "not. THE LINE THIS CLASS EXISTS TO DRAW, from reading all 19 hits "
               "across eight forks: A PATH IS THE CORRECT KEY FOR A PROPERTY OF THE "
               "FILE and the wrong key for A PROPERTY OF THE GAME. Size, mtime, an "
               "in-session metadata cache and an is-this-still-the-same-selection "
               "check are properties of the file -- Vita3K m_size_cache, eden "
               "m_rom_metadata_cache and eden AddonViewModel are all CORRECT. "
               "Settings, play history, art, cheats and saves are properties of the "
               "GAME and must key on GameKey or DumpId.",
        "exclude": r"Timber|Log\.[dveiw]\(|LOG_|printf|fmt::|spdlog|std::cout|DEBUG_|"
                   r"Exception\(|throw |FileNotFound|Result\.failure",
        "pattern": r"key\([^)]*\)[^=]*= *[a-zA-Z_.]*\.path(?![A-Za-z0-9_])|"
                   r"\$\{path\}_|\$\{[a-z]+\.path\}|"
                   r"Cache\.(Insert|insert|put|get)\( *[a-zA-Z_.]*path|"
                   r"invalidate[A-Za-z]*ForPath|"
                   r"[Mm]ap<std::string,[^>]*> *[a-z_]*(cache|Cache)",
    },
    "capability_by_model_name": {
        "what": "A host-capability flag decided by comparing a CPU MODEL NAME string, "
                "rather than by the architecture or a runtime probe.",
        "paid": "rpcsx, two instances. m_use_fma was gated on "
                "`cpu == \"cyclone\" || cpu.contains(\"cortex\")` -- and FMA is MANDATORY "
                "on AArch64, so the allowlist could only ever fail to enable it. "
                "m_use_ssse3 was gated on an allowlist of old x86 parts including "
                "\"generic\", and it gates the x86_pshufb lowering whose fallback is a "
                "16-ITERATION SCALAR LOOP of extractelement/insertelement -- backing "
                "VPERM, LVLX, LVRX, STVLX, STVRX, ROTQBY and SHUFB. One unlucky CPU "
                "string degrades every byte permute in both recompilers at once.",
        "why": "A model-name allowlist written before an ARM port cannot know an ARM "
               "name, so it silently selects the fallback. THE RULE: a host-capability "
               "flag should be answered by the architecture or a runtime probe, never "
               "by a model name, and any allowlist predating the port is x86-only until "
               "proven otherwise. Read the hits: matching a model name to pick a "
               "SCHEDULING model or a workaround is legitimate; matching one to decide "
               "whether an instruction EXISTS is not.",
        "near": r"m_use|has_|support|enable|feature|capab|fast|avx|sse|fma|neon|use_",
        "window": 3,
        "pattern": r"cpu *== *\"|cpu\.contains\(|cpu_name *== *\"|"
                   r"strstr\( *cpu|model_name.*==|brand.*contains\(",
    },
    "publish_then_flag": {
        "what": "Two adjacent relaxed stores to different atomics -- the flag "
                "published before, or without ordering against, the data it guards.",
        "paid": "rpcsx swept all three weak-memory shapes across its tree and found four "
                "defects: an RSX auditor inversion where the flag was stored before the "
                "data (wrong on x86 too), a semaphore fast cache, an SPU reservation "
                "seqlock, and an MFC DMA read. Two of the four are this shape.",
        "why": "x86 TSO gives store-store ordering for free, so a publish-then-flag "
               "written on x86 is correct there and racy on AArch64. Read the hits: a "
               "descriptive field consumed by a stats dump is benign last-writer-wins; "
               "what matters is whether a reader keys on one store to decide the other "
               "is visible.",
        # The signature is TWO ADJACENT relaxed stores, not one. Matching a
        # single relaxed store gave 58 hits in one fork and is useless; the
        # pair is what makes it a publish-then-flag.
        "near": r"\.store\([^)]*relaxed|atomic_store_explicit\([^)]*relaxed",
        "window": 3,
        "pattern": r"\.store\([^)]*relaxed[^)]*\)|atomic_store_explicit\([^)]*relaxed",
    },
    "validated_reread": {
        "what": "The same accessor loaded twice with work between, then compared -- a "
                "seqlock shape.",
        "paid": "rpcsx: the MFC DMA read at SPUThread.cpp:3767 needed an acquire fence "
                "before the validating read.",
        "why": "THE DECIDING FACTOR IS WHICH READ COMES FIRST, and two sites that "
               "pattern-match identically need opposite treatment. Version-then-data is "
               "safe when the version load is an acquire, because the data read cannot "
               "move above it. Data-then-version is the trap: the data read can sink "
               "below the validating counter read. A hit is not a diagnosis and here it "
               "is not even a direction -- read the order.",
        "pattern": r"seqlock|seq_?cst_?counter|"
                   r"(version|seq|generation|stamp|rtime)[a-z_]* *!= *[a-z_]*(acquire|load)",
    },
    "double_check": {
        "what": "Hand-rolled double-checked locking: test, take a lock, test again.",
        "paid": "rpcsx swept for it and found ZERO -- and the zero is STRUCTURAL, not "
                "luck: lazy initialisation there uses function-local statics, `static "
                "const auto x = []{...}()`, 413 of them across 25 files, which C++11 "
                "requires to be thread-safe and for which the compiler emits a guard "
                "with correct acquire/release.",
        "why": "A ZERO THAT IS EXPLAINED IS WORTH MORE THAN A ZERO THAT IS REPORTED. If "
               "this class returns nothing for a fork, check whether that fork uses "
               "function-local statics for lazy init before believing it.",
        # A null/initialized test only counts when a LOCK appears just below;
        # without that clause `if (!initialized)` matches ordinary lazy setup
        # and gave 37 hits in one fork.
        "near": r"lock\(\)|lock_guard|unique_lock|scoped_lock|mutex|EnterCritical",
        "window": 4,
        "pattern": r"if *\([^)]*== *nullptr\)|double.?check|"
                   r"if *\(![a-z_]*[Ii]nitialized[a-z_]*\)",
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


def scan(fork, pattern, include_vendored=False, near=None, window=8, exclude=None,
         near_absent=None):
    """Lines matching `pattern`, optionally only where `near` is close by.

    WHY `near` EXISTS. Broadening wrong_launch_path beyond xenia's own
    vocabulary produced 14 new hits across four forks and ZERO new instances:
    every one was `savedInstanceState == null` guarding fragment creation, or
    `hasExtra` reading an optional command parameter. Both are the CORRECT
    Android idiom.

    A shape that is usually correct is not a detector. What made xenia's case a
    bug was not the guard -- it was that the guard WRAPPED A DEFAULT. So the
    class needs two patterns and a distance, not one broader pattern.

        > If the shape you are matching is also how correct code looks,
        > you are counting the idiom, not the defect.
    """
    root = os.path.join(FLEET, FORKS[fork])
    if not os.path.isdir(root):
        return None
    cmd = ["git", "-C", root, "grep", "--recurse-submodules",
           "-nIE", pattern, "--"] + SOURCE
    if near or near_absent:
        # -A gives the following lines; the co-occurrence has to be found in a
        # second pass so the line numbers stay honest.
        cmd = ["git", "-C", root, "grep", "--recurse-submodules",
               "-nIE", "-A", str(window), pattern, "--"] + SOURCE
    try:
        r = subprocess.run(cmd, capture_output=True, timeout=240)
    except (OSError, subprocess.SubprocessError):
        return []
    out = r.stdout.decode("utf-8", errors="replace").splitlines()

    if near:
        rx = re.compile(near)
        kept, block, head = [], [], None
        def flush():
            if head and any(rx.search(b) for b in block):
                kept.append(head)
        for ln in out:
            if ln == "--":
                flush()
                block, head = [], None
                continue
            # A match line uses ':' after the line number; context uses '-'.
            if head is None:
                head = ln
                block = []
            else:
                block.append(ln)
        flush()
        out = kept

    if near_absent:
        # THE OPPOSITE OF `near`, and it fixes a real blind spot. rpcsx recorded
        # concluding "ARM has no arm" from reading an `#if defined(ARCH_X64)`
        # guard and stopping before its `#endif` -- the AArch64 branch was
        # twenty lines below, inside the same conditional. A line-based grep
        # cannot see that, so an x86 guard only counts here when NO ARM token
        # appears within the window.
        rx = re.compile(near_absent)
        kept, block, head = [], [], None
        def flush_abs():
            # The MATCH LINE counts too. Cemu's fast_float.h reads
            # `#if defined(_M_X64) || defined(_M_ARM64)` -- correct code that
            # names ARM64 in the guard itself, and checking only the FOLLOWING
            # lines reported it as x86-only. Found by reading a hit, 2026-08-25.
            if head and not rx.search(head) and not any(rx.search(b) for b in block):
                kept.append(head)
        for ln in out:
            if ln == "--":
                flush_abs(); block, head = [], None; continue
            if head is None:
                head = ln; block = []
            else:
                block.append(ln)
        flush_abs()
        out = kept

    if exclude:
        # A path INSIDE A LOG MESSAGE is not identity. path_as_identity produced
        # 16 GameThor hits and every one was `Timber.tag(..).d("... ${file.path}")`.
        # Interpolating a path into a diagnostic string is correct and common.
        ex = re.compile(exclude)
        out = [ln for ln in out if not ex.search(ln)]

    if include_vendored:
        return out
    return [ln for ln in out if not VENDORED.search(ln.split(":", 1)[0])]



def self_test():
    """Prove every class CAN match something, before trusting a zero.

    FOUND ON ITS FIRST RUN: path_as_identity carried a literal backspace byte
    (0x08) where a word boundary was meant, so its headline alternation --
    `key(...) = ....path` -- could never match any source line. The class had
    been silently half-dead since it was added.

    That is the same defect as supervise.py's queue-stale check, in a second
    tool, from the same cause: a backslash escape mangled when the file was
    written. A control character in a regex is invisible in an editor, legal in
    a string, and matches nothing.
    """
    problems = []
    for name, c in sorted(CLASSES.items()):
        for key in ("what", "paid", "why", "pattern"):
            if not c.get(key):
                problems.append("%s: missing %s" % (name, key))
        for key in ("pattern", "near", "near_absent", "exclude"):
            v = c.get(key)
            if not v:
                continue
            try:
                re.compile(v)
            except re.error as exc:
                problems.append("%s.%s: bad regex: %s" % (name, key, exc))
                continue
            ctrl = [hex(ord(ch)) for ch in v if ord(ch) < 32 and ch != "	"]
            if ctrl:
                problems.append(
                    "%s.%s: CONTROL CHARACTER %s -- matches nothing"
                    % (name, key, ", ".join(ctrl)))
    print("bug_class_sweep --self-test: %d classes" % len(CLASSES))
    print()
    if not problems:
        print("  [ OK ] every pattern compiles and is printable")
        print()
        print("This checks the class TABLE, not behaviour. A class can compile")
        print("cleanly and still describe the wrong shape.")
        return 0
    for pr in problems:
        print("  [FAIL] %s" % pr)
    return 1


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--class", dest="cls", help="one class (default: all)")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--self-test", action="store_true",
                    help="prove every class pattern compiles and can match")
    ap.add_argument("--show", action="store_true", help="print matching lines")
    ap.add_argument("--lines", type=int, default=4)
    args = ap.parse_args()

    if args.list:
        for name, c in CLASSES.items():
            print("%-24s %s" % (name, c["what"]))
        return 0

    if args.self_test:
        return self_test()
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
            hits = scan(fork, c["pattern"], c.get("include_vendored", False),
                        c.get("near"), window=c.get("window", 8),
                        exclude=c.get("exclude"),
                        near_absent=c.get("near_absent"))
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
    print("")
    print("AND A ZERO MAY BE THE WRONG QUESTION, NOT AN ABSENCE.")
    print("Every class here searches a SHAPE. A zero means that shape is absent,")
    print("which equals the CAPABILITY being absent only if the shape is the only")
    print("way to express it. Three times in one session a zero was a vocabulary")
    print("failure, not a finding:")
    print("  melonDS saves no D8/Q8 -- it calls ABI_PushRegisters(BitSet32(...))")
    print("  no Oboe latency in AGENTS.md -- that file is 92 lines on project shape")
    print("  four forks have no gamefix -- they compare a title id inline instead")
    print("Before reporting a zero: name a SECOND spelling, and run a positive")
    print("control proving the search space is non-empty.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
