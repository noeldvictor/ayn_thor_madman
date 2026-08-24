#!/usr/bin/env python3
"""Report the flags that ACTUALLY reached the compiler, from real build output.

WHY THIS EXISTS. Three times in three forks, a lever was configured correctly and
never applied:

  * xenia's AOT object cache was enabled only when no cvar bundle was supplied,
    and the launcher always supplies one -- so every real launch recompiled
    ~10,000 functions while a 111 MB cache sat unused.
  * xenia's three rlwinm fastpaths were defaultEnabled=true in code and false on
    the device, from a persisted config. Cost: 2.88%.
  * Vita3K's USE_LTO defaulted to RELEASE_ONLY, which covers the Release
    configuration -- and neither shipped build is Release. Confirmed from the
    emitted flags: all 973 translation units, -O2 -DNDEBUG, no -flto.

    > The rule they all produce: VERIFY FROM THE EMITTED FLAGS, NOT FROM THE
    > BUILD FILES. A setting that exists is not a setting that applies.

Every CMake-based Android fork writes `app/.cxx/<config>/<hash>/<abi>/
compile_commands.json` when it builds. That file is ground truth: it is the
command line each translation unit was actually compiled with.

    python tools/emitted_flags.py                 # every fork with build output
    python tools/emitted_flags.py --fork ARMSX2
    python tools/emitted_flags.py --show-tus      # per-file counts

It reads. It never modifies a fork, and it never triggers a build.

LIMITS:
  * Only forks that have been built on this machine appear. A missing fork means
    "not built here", never "no flags".
  * A stale `compile_commands.json` describes the last build, not the current
    source. Check the config name against what the shipping variant uses.
  * Flags reaching the compiler are not flags reaching the LINKER. `-flto` at
    compile time still needs the link step to cooperate.
"""

import argparse
import collections
import json
import os
import re
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

# Dependency build trees are not the fork's own compilation.
SKIP_DB = re.compile(r"vcpkg|buildtrees|node_modules", re.I)

# What we care about, in the order a reader wants it.
INTERESTING = [
    ("-march=",            "march"),
    ("-mcpu=",             "mcpu"),
    ("-mtune=",            "mtune"),
    ("-flto",              "lto"),
    ("-moutline-atomics",  "outline-atomics"),
    ("-mno-outline-atomics", "no-outline-atomics"),
    ("-O",                 "opt"),
]


def databases(fork):
    """Every compile_commands.json under the fork, with its config and ABI."""
    root = os.path.join(FLEET, FORKS[fork])
    if not os.path.isdir(root):
        return []
    found = []
    for base, dirs, files in os.walk(root):
        if SKIP_DB.search(base):
            dirs[:] = []
            continue
        if "compile_commands.json" in files:
            path = os.path.join(base, "compile_commands.json")
            parts = base.replace("\\", "/").split("/")
            abi = parts[-1] if parts else "?"
            cfg = "?"
            for i, seg in enumerate(parts):
                if seg == ".cxx" and i + 1 < len(parts):
                    cfg = parts[i + 1]
                    break
            found.append((cfg, abi, path))
    return sorted(found)


def summarise(path):
    try:
        with open(path, encoding="utf-8", errors="replace") as fh:
            entries = json.load(fh)
    except (OSError, ValueError):
        return None
    tally = collections.defaultdict(collections.Counter)
    for e in entries:
        cmd = e.get("command") or " ".join(e.get("arguments", []))
        for token in cmd.split():
            for prefix, name in INTERESTING:
                if name == "outline-atomics" and token.startswith("-mno-"):
                    continue
                if token.startswith(prefix):
                    tally[name][token] += 1
    return len(entries), tally


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--fork")
    ap.add_argument("--show-tus", action="store_true",
                    help="print how many translation units carry each flag")
    args = ap.parse_args()

    forks = [args.fork] if args.fork else list(FORKS)
    any_found = False
    for fork in forks:
        dbs = databases(fork)
        if not dbs:
            print("%-10s [no build output on this machine]" % fork)
            continue
        any_found = True
        print("\n=== %s ===" % fork)
        for cfg, abi, path in dbs:
            got = summarise(path)
            if not got:
                continue
            n, tally = got
            print("  config=%-14s abi=%-12s %d translation units" % (cfg, abi, n))
            for _prefix, name in INTERESTING:
                counter = tally.get(name)
                if not counter:
                    # Absence is the finding for lto and the atomics flags.
                    if name in ("lto", "no-outline-atomics"):
                        print("      %-20s ABSENT" % name)
                    continue
                for tok, count in counter.most_common(3):
                    suffix = "  (%d/%d TUs)" % (count, n) if args.show_tus else ""
                    print("      %-20s %s%s" % (name, tok, suffix))

    if any_found:
        print("\nThese are the flags that REACHED THE COMPILER, not what the build")
        print("files ask for. A fork missing above was not built on this machine.")
        print("Compile-time -flto still needs the LINK step to cooperate.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
