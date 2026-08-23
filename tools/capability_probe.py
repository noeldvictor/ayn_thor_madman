#!/usr/bin/env python3
"""Ask "which fork has this capability?" with more than one vocabulary.

THE FAILURE THIS EXISTS TO STOP, measured in this repo's own history:

  * "No fork persists translated code"  -- searched SaveCodeCache, AotCache.
    ARMSX2 calls it a PROGRAM CACHE and the payload a .vuprog.  WRONG.
  * "Frame pacing has no incumbent"     -- searched Swappy, GOOGLE_display_timing.
    Cemu spells it present_wait, and its best part is guest-timing code.  WRONG.
  * "Eight Adreno features have no user" -- read one device-layer file per fork.
    Six of the eight were requested elsewhere in the fork.  WRONG.

  Eleven absolute negatives in this repo have been wrong. The cause used to be
  reading file listings. It is now searching for a NAME rather than a MECHANISM.

    > A survey that searches for a named library finds adopters of that
    > library, not implementations of the capability.

THE RULE THIS TOOL ENFORCES: a capability is defined by SEVERAL INDEPENDENT
probes using different vocabularies. Absence is only reported when EVERY probe
misses, and the report always states how many probes ran, so a thin definition
is visible as a weak negative rather than passing as a strong one.

    python tools/capability_probe.py                    # every capability
    python tools/capability_probe.py --cap frame_pacing
    python tools/capability_probe.py --list
    python tools/capability_probe.py --cap frame_pacing --show   # matching lines

It reads. It never modifies a fork.

ADDING A CAPABILITY: give it at least THREE probes that a different engineer
would plausibly have used, drawn from different vocabularies -- the library
name, the underlying API call, the domain word, the file-name convention. If you
can only think of one, you are not ready to claim absence.
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

# Vendored trees are not the fork's own work. A hit here proves nothing about
# what the fork does -- this is the ARMSX2 "482 Vulkan extensions" trap, where
# the answer was 35 and the rest were the vendored headers.
VENDORED = re.compile(
    r"third_party|3rdparty|externals|dependencies|/vendor/|vulkan_core|volk|"
    r"vk_mem_alloc|/imgui/|/glslang/|/boost/|/ffmpeg/|/SDL|toml11|/toml/|"
    r"xbyak|oaknut|vixl|/fmt/|catch2|gtest", re.I)

SOURCE = ["*.cpp", "*.cc", "*.h", "*.hpp", "*.inl", "*.kt", "*.java", "*.s"]

# A capability: several probes, each a DIFFERENT vocabulary for the same idea.
# `why` records what that probe would catch that the others would not.
CAPABILITIES = {
    "persisted_code_cache": {
        "question": "Does the fork keep translated guest code across launches?",
        "probes": [
            ("library-ish name", r"AotCache|SaveCodeCache|LoadCodeCache"),
            ("domain word: program cache", r"ProgCache|ProgramCache|prog_cache"),
            ("the payload on disk", r"\.vuprog|\.blk\b|payload.*hydrate|HydrateProgram"),
            ("the validity key", r"optionsSentinel|OptionsSentinel|CompilerAbiVersion"),
            ("relocation, the hard part", r"fixup.?table|placement.relative|RelocateAndCopy"),
            # ADDED after this tool scored xenia 0/5 while xenia HAS an AOT
            # precompiler. xenia's vocabulary is "aot" and "object cache", which
            # none of the probes above use. The tool failed its own test first.
            ("AOT vocabulary", r"[^a-z]aot[^a-z]|AOT coverage|object_cache|ObjectCache"),
        ],
        "known": "THREE forks. rpcs3/rpcsx ppu-<sha1> LLVM object cache is the mature one; ARMSX2 VU .vuprog; xenia AOT + llvm object cache, default off.",
    },

    "persisted_derived_assets": {
        "question": "Does the fork keep upscaled or filtered TEXTURES across launches?",
        "probes": [
            ("the cache object", r"FilterCache|SetFilterCache|filter.?cache"),
            ("pack vocabulary", r"HDTexPack|TexturePacks/|texture.?pack"),
            ("content-hash file naming", r"tex1_|obj1_|bg1_|texhash|palhash"),
            ("dump-then-reload", r"DumpTexture|dumpDir|Dump/"),
        ],
        "known": "melonDS: a self-populating pack. Playing the game WRITES a shareable pack.",
    },
    "guest_activity_state": {
        "question": "Can the backend say what the guest is doing -- movie, menu, gameplay?",
        "probes": [
            ("video decode path", r"cellVdec|nvdec|SceAvcdec|mvd_|H264|XMA|ipu_|IPUProcess"),
            ("an explicit state word", r"GuestActivity|PlaybackState|is_playing_video|playback_probe"),
            ("cutscene or movie vocabulary", r"cutscene|[Mm]ovie[A-Z_ ]|fmv|FMV"),
            ("the decoder object", r"VideoDecoder|video_decoder|DecoderContext"),
        ],
        "known": "Every console has a decode path; an explicit state word is the part that may not exist.",
    },
    "pass_merging": {
        "question": "Does the fork merge render passes or use input attachments?",
        "probes": [
            ("Vulkan subpass inputs", r"pInputAttachments|inputAttachmentCount|subpassInput"),
            ("subpass vocabulary", r"subpassCount|pSubpasses|vkCmdNextSubpass"),
            ("merge vocabulary", r"merge.*pass|pass.*merge|fuse.*pass|MergeRenderPass"),
            ("dynamic rendering local read", r"local_read|LocalRead"),
        ],
        "known": "Claimed absent in CLAUDE.md after counting pInputAttachments. Re-checking with four probes.",
    },
    "audio_backend": {
        "question": "Which host audio API does the fork use?",
        "probes": [
            ("Oboe", r"[Oo]boe"),
            ("cubeb", r"cubeb"),
            ("AAudio direct", r"AAudio|aaudio"),
            ("OpenSL", r"OpenSLES|SLES/"),
            ("SDL audio", r"SDL_OpenAudio|SDL_AudioSpec|SDL_QueueAudio"),
        ],
        "known": "CLAUDE.md: three forks use Oboe, five vendor cubeb. Re-checking.",
    },
    "compatibility_sweep": {
        "question": "Can the fork launch a list of titles unattended and report how far each got?",
        "probes": [
            ("harness vocabulary", r"compat.*(sweep|matrix|suite)|boot.*test|smoke.*test"),
            ("a title list", r"titles?\.json|games?\.json|regression.?matrix"),
            ("headless run", r"headless|--no-gui|nogui|batch.*run"),
            ("result classification", r"BootResult|boot_status|reached.*(menu|ingame)"),
        ],
        "known": "CLAUDE.md lists this as specified and never built.",
    },
    "rewind": {
        "question": "Does the fork implement rewind, not just save states?",
        "probes": [
            ("the word", r"[Rr]ewind"),
            ("the ring buffer", r"state.?ring|rewind.?buffer|history.?buffer"),
            ("periodic snapshot", r"snapshot.*interval|auto.?save.?state"),
        ],
        "known": "Unknown. Hotkeys.kt declares a REWIND action with no backend behind it.",
    },
    "savestate": {
        "question": "Does the fork serialise and restore full guest state?",
        "probes": [
            ("the noun", r"SaveState|savestate|save_state"),
            ("the serialiser", r"DoState|do_state|serialize.*context|StateSerializer"),
            ("versioning", r"state.?version|STATE_VERSION|SAVESTATE_VERSION"),
            ("the archive format", r"\.sstate|\.p2s|\.state|StateWrapper"),
        ],
        "known": "CLAUDE.md records eden, Vita3K and xenia savestate code as found by NEITHER earlier search.",
    },
    "thermal_and_power": {
        "question": "Does the fork react to thermal headroom or report power?",
        "probes": [
            ("Android ADPF", r"ADPF|PerformanceHint|getThermalHeadroom|PowerManager"),
            ("thermal vocabulary", r"[Tt]hermal|throttl"),
            ("battery and power", r"BatteryManager|current_now|charge_counter|POWER_SUPPLY"),
            ("cpu frequency", r"scaling_cur_freq|cpufreq|setPerformanceMode"),
        ],
        "known": "CLAUDE.md: ADPF is disabled on this device by persisted config. Fork support unsurveyed.",
    },
    "deterministic_input_replay": {
        "question": "Can the fork record and replay an exact input stream?",
        "probes": [
            ("the movie file", r"movie\.cpp|MovieRecord|PlayMovie|RecordMovie"),
            ("input log", r"input.?log|InputRecording|replay.*input|input.*replay"),
            ("frame counter binding", r"frame.?count.*input|rerecord|re-record"),
        ],
        "known": "azahar has core/movie.cpp. xenia wrote a doc arguing AGAINST movies.",
    },
    "frame_pacing": {
        "question": "Does the fork pace presentation, beyond choosing a present mode?",
        "probes": [
            ("Google's library", r"\bSwappy\b|swappy"),
            ("Google's Vulkan extension", r"GOOGLE_display_timing"),
            ("Vulkan core mechanism", r"present_wait|presentId|PresentWait|vkWaitForPresentKHR"),
            ("guest timing, not present code", r"HostDrivenVSync|NotifyHostVSync|VsyncDriver"),
            ("queue depth / latency control", r"m_maxQueued|queueDepth|frames?.in.flight"),
        ],
        "known": "Cemu, four parts. The first two probes miss it entirely.",
    },
    "shipped_cache_pooling": {
        "question": "Can one user's derived artifacts be merged into another's?",
        "probes": [
            ("the merge tool", r"ShaderCacheMerger|MergeShaderCache|MergeCacheFile"),
            ("the directory convention", r"transferable|precompiled"),
            ("content addressing", r"content.?hash|contentMap|hash32hex"),
            ("import or download path", r"ImportShaderCache|download.*cache|cache.*download"),
        ],
        "known": "Cemu. No fork has an import/download path.",
    },
    "native_guest_execution": {
        "question": "Does the fork run guest code natively instead of translating it?",
        "probes": [
            ("the yuzu/eden name", r"\bNCE\b|arm_nce|ArmNce"),
            ("the mechanism", r"PatchText|patch.*text.*segment|EntryTrampolines"),
            ("the module key", r"ModuleID|build.?id|BuildID"),
            ("the escape hatch", r"interpreter_visitor|InterpreterVisitor"),
        ],
        "known": "eden. Switch guest is ARM64, same as host.",
    },
    "async_pipeline_compile": {
        "question": "Does the fork compile pipelines off the draw thread?",
        "probes": [
            ("the setting", r"AsyncShaderCompile|asyncCompile|async_shader"),
            ("the worker", r"compile.?queue|CompileThread|compileWorker"),
            ("the placeholder", r"placeholder.*pipeline|pipeline.*placeholder|ubershader"),
        ],
        "known": "Cemu, with a per-game profile override. Interacts with any cache experiment.",
    },
    "dual_screen_present": {
        "question": "Does the fork present two guest screens without one stalling the other?",
        "probes": [
            ("two swapchains", r"mainWindow \? 0 : 1|m_commandBufferIDOfPrevFrame|GetChainInfo"),
            ("the layout words", r"SecondaryDisplay|secondary_display|INTERNAL_TOP_EXTERNAL"),
            ("Android presentation", r"Presentation\(|DisplayManager|FLAG_PRESENTATION"),
        ],
        "known": "Cemu serialises presents; melonDS and azahar route layouts.",
    },
}


def probe(fork_path, pattern):
    """Return matching lines from the fork's own source, vendored trees removed."""
    root = os.path.join(FLEET, fork_path)
    if not os.path.isdir(root):
        return None
    cmd = ["git", "-C", root, "grep", "-nIE", pattern, "--"] + SOURCE
    try:
        r = subprocess.run(cmd, capture_output=True, timeout=180)
    except (OSError, subprocess.SubprocessError):
        return []
    out = r.stdout.decode("utf-8", errors="replace").splitlines()
    return [ln for ln in out if not VENDORED.search(ln.split(":", 1)[0])]


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--cap", help="one capability (default: all)")
    ap.add_argument("--list", action="store_true", help="list capabilities and exit")
    ap.add_argument("--show", action="store_true", help="print matching lines")
    ap.add_argument("--lines", type=int, default=3, help="lines per probe with --show")
    args = ap.parse_args()

    if args.list:
        for name, c in CAPABILITIES.items():
            print("%-24s %d probes  %s" % (name, len(c["probes"]), c["question"]))
        return 0

    names = [args.cap] if args.cap else list(CAPABILITIES)
    for name in names:
        cap = CAPABILITIES.get(name)
        if not cap:
            print("unknown capability %r. --list shows them." % name)
            return 1

        print("\n=== %s ===" % name)
        print("%s" % cap["question"])
        print("%d probes. Absence is only reportable when all %d miss."
              % (len(cap["probes"]), len(cap["probes"])))

        for fork, path in FORKS.items():
            hits = {}
            missing = False
            for label, pattern in cap["probes"]:
                found = probe(path, pattern)
                if found is None:
                    missing = True
                    break
                if found:
                    hits[label] = found
            if missing:
                print("  %-10s [not present beside this repo]" % fork)
                continue
            if not hits:
                print("  %-10s no hit on any of %d probes" % (fork, len(cap["probes"])))
                continue
            print("  %-10s %d/%d probes hit: %s"
                  % (fork, len(hits), len(cap["probes"]), ", ".join(hits)))
            if args.show:
                for label, found in hits.items():
                    for ln in found[:args.lines]:
                        print("      [%s] %s" % (label, ln[:150]))

        print("  known: %s" % cap["known"])

    print("\nA fork with 0 hits is a candidate for absence, NOT a proof of it.")
    print("Before recording a negative, add a probe with a vocabulary not used above.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
