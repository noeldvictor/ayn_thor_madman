#!/usr/bin/env python3
"""Supervisor: catch this repo's repeated unproductive cycles.

This is the one part of NVIDIA's AVO architecture the project does not already
have. AVO runs a supervision layer beside the main agent loop that "monitors the
broader trajectory for stagnation or repeated unproductive cycles and can
redirect the main agent toward alternative strategies". The other three AVO
components -- the agent loop, persistent memory and domain tools -- already
exist here as research_log/, work_log/, capability_inventory.md, exp_ledger.py,
DEVICE_QUEUE.md and the builds.

It exists in executable form rather than as a rule because CLAUDE.md says so:
"A rule in a document does not stop this. An agent skips what it does not read.
The fix must be structural."

Every check below is a loop this repo has actually run, more than once, with the
evidence named in the check.

Usage:
    python tools/supervise.py                  # report on the working tree
    python tools/supervise.py --strict         # exit 1 on any FAIL
    python tools/supervise.py --check negatives
    python tools/supervise.py --paths a.md b.md

It reports. It does not modify anything, and it never touches a fork.
"""

import argparse
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

FORKS = [
    ("ARMSX2", "armsx2-thor/ARMSX2"),
    ("xenia-thor", "xenia-thor-workspace/xenia-thor"),
    ("Cemu-thor", "cemu-thor-experiment"),
    ("azahar-thor", "azahar-thor/azahar"),
    ("melonDS-android", "melonds_HD/melonDS-android"),
    ("Vita3K-Thor", "psvita/Vita3K-Thor"),
    ("eden-thor", "eden-thor"),
    ("rpcsx-ui-android", "ps3-thor/rpcsx-ui-android"),
    ("GameThor", "gamethor"),
]

OK, WARN, FAIL, SKIP = "OK", "WARN", "FAIL", "SKIP"


def _run(cmd, cwd=None):
    try:
        p = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, timeout=60)
        return p.returncode, p.stdout
    except Exception:
        return 1, ""


def _tracked_markdown(paths=None):
    """Markdown this session is adding or changing, not the whole repo."""
    if paths:
        return [p for p in paths if p.endswith(".md")]
    rc, out = _run(["git", "status", "--porcelain"], cwd=REPO)
    if rc != 0:
        return []
    files = []
    for line in out.splitlines():
        name = line[3:].strip().strip('"')
        if name.endswith(".md"):
            files.append(name)
    return files


def _lines(path):
    full = os.path.join(REPO, path)
    if not os.path.isfile(full):
        return []
    with open(full, encoding="utf-8", errors="replace") as fh:
        return fh.read().split(chr(10))


def _added_lines(path):
    """Only the lines this session ADDS.

    Scope matters more than the pattern here. CLAUDE.md is over 4,000 lines of
    accumulated history, and most negatives in it are already-corrected records
    sitting in the "Read before you claim" table. Scanning whole files reports
    the cure as the disease.

    Returns (line_number, text) pairs.
    """
    rc, out = _run(["git", "diff", "-U0", "--", path], cwd=REPO)
    tracked = rc == 0 and out.strip()
    if not tracked:
        rc2, _ = _run(["git", "ls-files", "--error-unmatch", path], cwd=REPO)
        if rc2 != 0:
            # untracked: every line is new
            return list(enumerate(_lines(path), start=1))
        return []
    added, lineno = [], 0
    for line in out.split(chr(10)):
        m = re.match(r"^@@ -\d+(?:,\d+)? \+(\d+)", line)
        if m:
            lineno = int(m.group(1))
            continue
        if line.startswith("+++") or line.startswith("---"):
            continue
        if line.startswith("+"):
            added.append((lineno, line[1:]))
            lineno += 1
        elif not line.startswith("-"):
            lineno += 1
    return added


# ---------------------------------------------------------------- 1. negatives

# CLAUDE.md: "Every claim of the form 'no fork has this' made in this repo has
# been wrong." Nine rows in that table when it was written; three more were
# added on 2026-08-23. This is the most reliable failure mode the repo has.
NEGATIVE = re.compile(
    r"\b("
    r"no fork\b|nothing (?:else )?(?:in the fleet )?(?:does|has|uses|implements)\b"
    r"|no (?:other )?(?:fork|backend|emulator) (?:does|has|uses|implements)\b"
    r"|none of the forks\b|nobody (?:does|has|else)\b"
    r"|only \w[\w-]* (?:has|does|implements|uses)\b"
    r"|the only fork\b|first in the fleet\b"
    r")",
    re.IGNORECASE,
)

# A negative is allowed once it names how it was searched, twice, with different
# words. CLAUDE.md: "A negative is only worth recording after a second search
# with different words."
QUALIFIED = re.compile(
    r"\b(searched (?:for|twice|again)|second search|verified with a second"
    r"|grep(?:ped)? for|measured|read (?:in full|all|every)|checked (?:twice|by)"
    r"|\bmethod\b|two searches|different words|census)",
    re.IGNORECASE,
)


# A line is exempt when it is recording a correction rather than making a claim,
# when it is a table row in one of the correction tables, or when it is about
# this session's own discipline rather than about the fleet.
EXEMPT = re.compile(
    r"(was wrong|has been wrong|CORRECT(?:ED|ION)|withdrawn|refut|turned out"
    r"|claim(?:ed)? .*(?:wrong|reversed)|no fork (?:modified|was touched|has been)"
    r"|no device|not committed|do not modify a fork|stays in this repo"
    r"|nobody is tracking|was committed|no fork converted|no fork is converted"
    # A DISCLAIMER OF WORK NOT DONE is a scope statement, not a capability
    # claim. "No fork has X" asserts a property of the fleet and must name its
    # search; "no fork was audited" asserts only that I did not look, which is
    # the opposite -- it withholds a claim. Four false positives of this exact
    # shape in one session, all in Limits sections, before this was added.
    r"|no (?:fork|backend|other fork)s? (?:was|were|has been|have been) "
    r"(?:audited|checked|read|opened|examined|surveyed|built|run|measured"
    r"|traced|tested|profiled|reproduced)"
    r"|^\s*\||->|→)",
    re.IGNORECASE,
)


def check_negatives(paths):
    """An absolute negative must name its search method nearby.

    CLAUDE.md: "Every claim of the form 'no fork has this' made in this repo has
    been wrong." Nine rows in that table when it was written; three more were
    added on 2026-08-23. This is the most reliable failure mode the repo has.

    Only ADDED lines are checked. See _added_lines.
    """
    findings = []
    for path in paths:
        whole = _lines(path)
        for lineno, line in _added_lines(path):
            stripped = line.lstrip()
            if stripped.startswith(">"):
                continue  # a quotation is somebody else's claim
            # A claim in quotation marks inside a list item, a table row or a
            # heading is being DISCUSSED, not asserted -- that is how the audit
            # backlog, the verdict tables and their headings are written.
            #
            # Checked on the LINE. The EXEMPT pattern below runs against a
            # multi-line window, where an anchored ^ cannot match a row that
            # starts partway through.
            if re.match(r'^([-*]\s|\||#{1,6}\s)', stripped) and re.search(r'["“]', stripped):
                continue
            if not NEGATIVE.search(line):
                continue
            # The window, not just the line. A method note or a correction
            # marker often sits a paragraph away from the claim it qualifies.
            lo = max(0, lineno - 11)
            window = chr(10).join(whole[lo:lineno + 10])
            if EXEMPT.search(window):
                continue
            if QUALIFIED.search(window):
                continue
            findings.append((path, lineno, stripped[:88]))
    if not findings:
        return OK, "every new absolute negative names how it was searched", []
    return (
        FAIL,
        "%d unqualified negative claim(s) added. This repo has been wrong every "
        "time it made one." % len(findings),
        ["%s:%d  %s" % f for f in findings],
    )


# -------------------------------------------------------------- 2. fork writes

def check_fork_writes(_paths):
    """Working rule 1: all work stays in this repo. Do not modify a fork."""
    dirty = []
    missing = 0
    for name, rel in FORKS:
        root = os.path.join(os.path.dirname(REPO), rel)
        if not os.path.isdir(os.path.join(root, ".git")) and not os.path.isdir(root):
            missing += 1
            continue
        rc, out = _run(["git", "status", "--porcelain"], cwd=root)
        if rc != 0:
            missing += 1
            continue
        n = len([x for x in out.splitlines() if x.startswith(" M")])
        if n:
            dirty.append("%s: %d modified tracked file(s)" % (name, n))
    if missing == len(FORKS):
        return SKIP, "no forks found beside this repo", []
    if not dirty:
        return OK, "no fork has modified tracked files", []
    return (
        WARN,
        "%d fork(s) have modified tracked files. Rule 1 says work stays here. "
        "Confirm these are the user's own, not this session's." % len(dirty),
        dirty,
    )


# ------------------------------------------------------------ 3. dead levers

def check_dead_levers(paths):
    """Do not re-run a lever the experiment ledger already recorded DEAD."""
    ledger = os.path.join(
        os.path.dirname(REPO), "xenia-thor-workspace/xenia-thor/tools/exp_ledger.py"
    )
    if not os.path.isfile(ledger):
        return SKIP, "xenia's experiment ledger not found", []
    # Only fire when an experiment is actually being proposed. A check that
    # fires on every change is noise, and noise is how a check gets ignored.
    proposing = re.compile(
        r"\b(A/B|experiment|lever|try (?:enabling|setting|turning)"
        r"|measure on the (?:device|Thor)|queue(?:d|s)? (?:an?|this) (?:run|experiment))\b",
        re.I,
    )
    # A file that RECORDS the query has already done what this check asks for.
    # Fired on hardware_ref/thor/THOR_TARGET.md on 2026-08-24, on a passage whose
    # subject was the ledger query and its zero result. Same class as the
    # DEVICE_QUEUE false positive: the document was right and the tool was wrong.
    # This is not a magic word -- it demands the query be named, which is an act.
    # REJECTED.md is the fleet-wide half of the same job. The ledger holds
    # xenia's experiments; azahar's rejections -- the densest set found, and the
    # only ones covering audio, the Android build and the guest scheduler --
    # are in a fork's AGENTS.md and in no queryable store. Consulting either
    # store satisfies this check, and naming it is the act being demanded.
    satisfied = re.compile(
        r"exp_ledger|ledger was queried|REJECTED\.md|"
        r"(?:query|queried|querying) the (?:experiment )?ledger|"
        r"(?:checked|searched|consulted) (?:the )?rejection(?:s| index)", re.I)
    hits = [p for p in paths if proposing.search(chr(10).join(_lines(p)))
            and not satisfied.search(chr(10).join(_lines(p)))
            and not p.startswith("research_log/")]
    if not hits:
        return OK, "no new experiment proposed, or the ledger query is recorded", []
    return (
        WARN,
        "%d file(s) propose an experiment. Query the ledger AND "
        "shared_layer/REJECTED.md before running one."
        % len(hits),
        hits[:6] + [
            "  python <xenia>/tools/exp_ledger.py check \"<keyword>\"",
            "the ledger holds DEAD and FLAT verdicts precisely to stop re-runs",
        ],
    )


# --------------------------------------------------- 4. device-queue discipline

def check_device_queue(_paths):
    """Every queued device experiment states its expected signature first."""
    path = "DEVICE_QUEUE.md"
    lines = _lines(path)
    if not lines:
        return SKIP, "DEVICE_QUEUE.md not found", []
    entries, unpredicted = [], []
    cur = None
    body = []
    for line in lines:
        if re.match(r"^## \d+\.", line):
            if cur:
                entries.append((cur, "\n".join(body)))
            cur, body = line.strip(), []
        elif cur:
            body.append(line)
    if cur:
        entries.append((cur, "\n".join(body)))
    for title, text in entries:
        # A prediction may be a bold sentence, a table column, or an "expect"
        # clause. Requiring one shape produced a false positive on entry 3,
        # which states its predictions in a table headed "Prediction".
        if not re.search(r"\bPrediction\b|\bExpect(?:s|ed)?\b|expected signature",
                         text, re.I):
            unpredicted.append(title[:90])
    if not entries:
        return SKIP, "no numbered entries in DEVICE_QUEUE.md", []
    if not unpredicted:
        return OK, "all %d queued experiments state a prediction" % len(entries), []
    return (
        FAIL,
        "%d of %d queued experiments have no prediction. "
        "A run with no prediction cannot fail." % (len(unpredicted), len(entries)),
        unpredicted,
    )


# ------------------------------------------------------------- 5. session log

def check_session_log(paths):
    """Write the log as you go, not at the end from memory."""
    if not paths:
        return SKIP, "nothing changed", []
    non_log = [p for p in paths
               if not p.startswith(("research_log/", "work_log/"))]
    logs = [p for p in paths if p.startswith(("research_log/", "work_log/"))]
    if not non_log:
        return OK, "only logs changed", []
    if logs:
        return OK, "%d log(s) written alongside %d other file(s)" % (
            len(logs), len(non_log)), []
    return (
        WARN,
        "%d file(s) changed and no research or work log was written" % len(non_log),
        non_log[:8],
    )


# ------------------------------------------------------- 6. capability-shaped search

# 2026-08-23: five audits found the fleet had already done the work. The cause
# each time was searching for a capability NAME rather than a MECHANISM -- a
# DEFINE_bool with reasoning attached, a comment carrying a measurement, a
# header that is already a contract, a CPM cache that is a dependency manifest.
def check_instrument(paths):
    """A claim about the fleet should name the mechanism it searched for."""
    hits = []
    for path in paths:
        if not path.startswith(("research_log/", "work_log/")):
            continue
        text = "\n".join(_lines(path))
        if not text.strip():
            continue
        claims_fleet = re.search(
            r"\b(the fleet|across the (?:fleet|forks)|every fork|all \w+ forks)\b",
            text, re.I)
        names_method = re.search(
            r"\b(searched for|grep|`[^`]*grep[^`]*`|nm |llvm-nm|Method|read in full"
            r"|opcode|symbol|census|manifest|build file|CMake)\b", text, re.I)
        if claims_fleet and not names_method:
            hits.append(path)
    if not hits:
        return OK, "fleet-wide claims name their instrument", []
    return (
        WARN,
        "%d log(s) claim something fleet-wide without naming the search" % len(hits),
        hits,
    )


def check_queue_staleness(paths):
    """A log that cites a queue entry should say whether the entry still holds.

    A queue entry is a hypothesis with a date. Three times in two days a finding
    landed that bore on an entry and the entry was not updated: XenDroid's
    in-pass resolve series and xenia's local_read probe both bore on entry 26,
    and entries 12 and 13 turned out to be one experiment without citing each
    other.

    This does NOT assert a fault. A log may cite an entry for context. It asks
    the question at the moment the citation is written, which is the only moment
    somebody can cheaply answer it.
    """
    cites = re.compile(
        r"(?:DEVICE_QUEUE(?:\.md)?\s*)?(?:queue\s+)?entr(?:y|ies)\s+([0-9]+)"
        r"|(?<![A-Za-z])queue\s+([0-9]+)", re.I)
    # A log that RECORDS the answer has done what this check asks. Same shape
    # as dead-levers accepting a recorded ledger query: not a magic word, but a
    # statement that somebody looked. Without this, a log ABOUT queue staleness
    # is a permanent WARN, and a permanent WARN is ignored.
    answered = re.compile(
        r"still holds|was updated (?:earlier )?today|entry (?:was )?re-read"
        r"|answer(?:ed)?[: ]|not staleness", re.I)
    queue_touched = any(p.endswith("DEVICE_QUEUE.md") for p in paths)
    if queue_touched:
        return OK, "a log cites a queue entry and the queue was updated too", []
    hits = []
    for path in paths:
        if not path.startswith(("research_log/", "work_log/")):
            continue
        if answered.search(chr(10).join(_lines(path))):
            continue
        for lineno, line in _added_lines(path):
            for m in cites.finditer(line):
                n = m.group(1) or m.group(2)
                hits.append("%s:%d  cites entry %s" % (path, lineno, n))
                break
    if not hits:
        return OK, "no log cites a queue entry", []
    return (
        WARN,
        "%d log line(s) cite a queue entry while DEVICE_QUEUE.md is unchanged. "
        "Does that entry still hold?" % len(hits),
        hits[:6],
    )


CHECKS = [
    ("negatives", check_negatives),
    ("fork-writes", check_fork_writes),
    ("device-queue", check_device_queue),
    ("session-log", check_session_log),
    ("instrument", check_instrument),
    ("dead-levers", check_dead_levers),
    ("queue-stale", check_queue_staleness),
]


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--strict", action="store_true", help="exit 1 on any FAIL")
    ap.add_argument("--check", help="run one check by name")
    ap.add_argument("--paths", nargs="*", help="files to inspect instead of git status")
    args = ap.parse_args()

    paths = _tracked_markdown(args.paths)
    worst = OK
    print("supervise: %d changed markdown file(s)\n" % len(paths))

    for name, fn in CHECKS:
        if args.check and args.check != name:
            continue
        status, summary, detail = fn(paths)
        print("[%-4s] %-13s %s" % (status, name, summary))
        for d in detail:
            print("         %s" % d)
        if status == FAIL:
            worst = FAIL
        elif status == WARN and worst != FAIL:
            worst = WARN
        print()

    print("worst: %s" % worst)
    if args.strict and worst == FAIL:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
