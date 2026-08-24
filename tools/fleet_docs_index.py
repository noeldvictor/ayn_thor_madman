#!/usr/bin/env python3
"""Index the fleet's own research documents, and surface the negative results.

WHY THIS EXISTS. CLAUDE.md calls the forks' research directories "the richest
unmined seam in the fleet": roughly 590 documents and 100 skills, of which this
repo had read about a dozen. Nobody reads 553 documents. But the TITLES are a
searchable index of every question somebody already asked, and the VERDICTS
inside them say how it turned out.

    > The first question for any new feature is "which fork already has this?",
    > not "how do I write this?"

AND THE NEGATIVES ARE THE VALUABLE HALF. A document recording that a lever
measured DEAD is worth more than one recording a win, because it stops the lever
being re-run. This repo's experiment ledger exists for the same reason.

    python tools/fleet_docs_index.py --search "spin wait"
    python tools/fleet_docs_index.py --verdicts DEAD FLAT     # do-not-retry
    python tools/fleet_docs_index.py --stats
    python tools/fleet_docs_index.py --search cache --show    # with the title line
    python tools/fleet_docs_index.py --search residency --after 20260626

AND THE SAME TRAP INSIDE ONE DOCUMENT, 2026-08-25. A 1,480-line topic file was
sampled by heading; a claim at line 678 was written up as a live lever and its
refutation sat at line 1268 of the same file, measured and dead. --after guards
this BETWEEN documents and nothing guarded it WITHIN one.

    > Before quoting a section of a long document, grep that document for its own
    > later corrections: refute|corrected|superseded|wrong|dead|does not.

THE SUPERSEDED-CONCLUSION TRAP. On 2026-08-24 three conclusions were taken from
dated fork documents and each had been overturned by later work in the same fork:
a frame anatomy corrected five days later, a residency result measured CONFOUNDED a
month later, and a fragment-overdraw premise disproved two days later. The repo's
own rule -- a newest failure outranks an older success -- applies to MEASUREMENTS,
not only to results. Use --after with the source document's date.

It reads. It never modifies a fork.

LIMITS:
  * A verdict is matched by a word in the text. A document that DISCUSSES the
    word DEAD is indistinguishable here from one that RECORDS a DEAD verdict.
    Treat a hit as "worth opening", never as the verdict itself.
  * Only tracked markdown is indexed. Untracked notes are invisible.
  * A title is a hint. Several xenia titles are opaque -- "82282490 r[1]
    Live-In Availability Report" -- and only the body says what happened.
"""

import argparse
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FLEET = os.path.dirname(REPO)

FORKS = {
    "xenia": "xenia-thor-workspace/xenia-thor",
    "rpcsx": "ps3-thor/rpcsx-ui-android",
    "Vita3K": "psvita/Vita3K-Thor",
    "ARMSX2": "armsx2-thor/ARMSX2",
    "Cemu": "cemu-thor-experiment",
    "azahar": "azahar-thor/azahar",
    "melonDS": "melonds_HD/melonDS-android",
    "eden": "eden-thor",
    "GameThor": "gamethor",
}

SKIP = re.compile(r"third_party|3rdparty|externals|dependencies|node_modules|"
                  r"vcpkg|CHANGELOG|LICENSE", re.I)

# The experiment ledger's vocabulary, plus the words a failed probe uses.
VERDICTS = ["DEAD", "FLAT", "WIN", "CONFOUNDED", "GFX-LOSS", "OPEN",
            "BLOCKED", "REGRESSION", "REFUTED", "NULL RESULT", "NO-OP",
            "stack-regression", "route-miss", "migration-credit"]

DATE = re.compile(r"(20\d{6})")


def tracked_md(fork):
    root = os.path.join(FLEET, FORKS[fork])
    if not os.path.isdir(root):
        return []
    try:
        r = subprocess.run(["git", "-C", root, "ls-files", "*.md"],
                           capture_output=True, timeout=120)
    except (OSError, subprocess.SubprocessError):
        return []
    out = r.stdout.decode("utf-8", errors="replace").splitlines()
    return [p for p in out if not SKIP.search(p)]


def title_of(root, rel):
    path = os.path.join(root, rel)
    try:
        with open(path, encoding="utf-8", errors="replace") as fh:
            for line in fh:
                line = line.strip()
                if line.startswith("#"):
                    return line.lstrip("#").strip()[:120]
                if line:
                    return line[:120]
    except OSError:
        pass
    return os.path.basename(rel)


def verdicts_in(root, rel):
    path = os.path.join(root, rel)
    try:
        with open(path, encoding="utf-8", errors="replace") as fh:
            text = fh.read()
    except OSError:
        return []
    found = []
    for v in VERDICTS:
        # Word-ish boundary without relying on escapes that get mangled.
        if re.search("(^|[^A-Za-z])" + re.escape(v) + "([^A-Za-z]|$)", text):
            found.append(v)
    return found


def build(forks):
    index = []
    for fork in forks:
        root = os.path.join(FLEET, FORKS[fork])
        for rel in tracked_md(fork):
            m = DATE.search(rel)
            index.append({
                "fork": fork,
                "path": rel,
                "date": m.group(1) if m else "",
                "title": title_of(root, rel),
            })
    return index


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--search", nargs="*", help="words to match in title or path")
    ap.add_argument("--verdicts", nargs="*", help="only docs mentioning these verdicts")
    ap.add_argument("--fork", help="one fork only")
    ap.add_argument("--stats", action="store_true", help="corpus shape")
    ap.add_argument("--after", help="only documents dated after this YYYYMMDD. "
                    "THE QUERY THIS TOOL EXISTED WITHOUT: a conclusion taken from a "
                    "dated fork document may have been superseded, and on 2026-08-24 "
                    "three were. Ask what the fork wrote LATER about the same subject.")
    ap.add_argument("--limit", type=int, default=40)
    ap.add_argument("--show", action="store_true", help="print the title")
    args = ap.parse_args()

    forks = [args.fork] if args.fork else list(FORKS)
    index = build(forks)

    if args.stats:
        print("%-10s %6s %6s %s" % ("fork", "docs", "dated", "earliest..latest"))
        for fork in forks:
            rows = [r for r in index if r["fork"] == fork]
            dated = sorted(r["date"] for r in rows if r["date"])
            span = "%s..%s" % (dated[0], dated[-1]) if dated else "-"
            print("%-10s %6d %6d %s" % (fork, len(rows), len(dated), span))
        print("\ntotal %d tracked markdown files" % len(index))
        return 0

    rows = index
    if args.after:
        rows = [r for r in rows if r["date"] and r["date"] > args.after]

    if args.search:
        words = [w.lower() for w in args.search]
        rows = [r for r in rows
                if all(w in (r["title"] + " " + r["path"]).lower() for w in words)]

    if args.verdicts is not None:
        wanted = [v.upper() for v in args.verdicts] if args.verdicts else VERDICTS
        keep = []
        for r in rows:
            root = os.path.join(FLEET, FORKS[r["fork"]])
            got = [v for v in verdicts_in(root, r["path"]) if v.upper() in wanted]
            if got:
                r["verdicts"] = got
                keep.append(r)
        rows = keep

    print("%d matching document(s)%s" % (len(rows),
          "" if len(rows) <= args.limit else ", showing %d" % args.limit))
    for r in sorted(rows, key=lambda r: (r["fork"], r["date"]))[:args.limit]:
        v = " [" + ",".join(r.get("verdicts", [])) + "]" if r.get("verdicts") else ""
        print("  %-8s %-9s %s%s" % (r["fork"], r["date"] or "-", r["path"], v))
        if args.show:
            print("           %s" % r["title"])
    if len(rows) > args.limit:
        print("  ... raise --limit to see the rest")
    return 0


if __name__ == "__main__":
    sys.exit(main())
