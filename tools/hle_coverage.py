#!/usr/bin/env python3
"""Extract each backend's HLE coverage into one machine-readable table.

This is half of the static triage described in shared_layer/STATIC_TRIAGE.md.

Every console executable carries an import table naming the functions it calls.
Every emulator's HLE layer is a list of what it provides. The difference is a
compatibility score computable without booting anything. Both halves exist in
the fleet; nothing computed the difference, and the emulator half was never
written down at all.

This produces the emulator half: for each backend, every HLE function it
declares and whether that function is stubbed.

    python tools/hle_coverage.py                  # summary
    python tools/hle_coverage.py --json out.json  # the full table
    python tools/hle_coverage.py --fork Vita3K

It reads. It never modifies a fork.

LIMITS, stated because the number is easy to misread:
  * A STUBBED marker anywhere in a function body marks the whole function, so a
    partially implemented function that logs one unimplemented branch counts as
    stubbed. The figure OVERSTATES.
  * A stub is not automatically a problem. Many correctly return success -- a
    logging call, a power query, a feature the title tolerates missing. The
    fraction is a starting weight, not a score. Calibrate against titles known
    to work; the stubs they import are proven harmless.
"""

import argparse
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FLEET = os.path.dirname(REPO)

STUB = re.compile(r"\bSTUBBED\b|\bUNIMPLEMENTED\b|\bUNIMPLEMENTED_MSG\b")


def _walk(root, suffix=".cpp"):
    for base, _dirs, files in os.walk(root):
        if any(x in base for x in ("third_party", "externals", "3rdparty", "/.git")):
            continue
        for f in files:
            if f.endswith(suffix):
                yield os.path.join(base, f)


def _scan_blocks(path, opener):
    """Yield (name, body) for each brace-balanced block whose first line matches."""
    try:
        lines = open(path, encoding="utf-8", errors="replace").readlines()
    except OSError:
        return
    cur, depth, body = None, 0, []
    for line in lines:
        m = opener.match(line)
        if m and cur is None:
            cur, depth, body = m.group(1), 0, []
        if cur is not None:
            body.append(line)
            depth += line.count("{") - line.count("}")
            if depth == 0 and len(body) > 1:
                yield cur, "".join(body)
                cur = None


# Vita3K: EXPORT(ret, name, args...) { ... }, one directory per guest module.
def collect_vita3k(root):
    opener = re.compile(r"^EXPORT\((?:[^,]+),\s*(\w+)")
    out = {}
    base = os.path.join(root, "vita3k", "modules")
    for path in _walk(base):
        module = os.path.basename(os.path.dirname(path))
        for name, body in _scan_blocks(path, opener):
            out.setdefault(module, {})[name] = bool(STUB.search(body))
    return out


# eden: the Switch service layer declares coverage EXPLICITLY, which makes it the
# best signal in the fleet. Its FunctionInfo tables read:
#
#     {0, D<&IManagerForSystemService::CheckAvailability>, "CheckAvailability"},
#     {2, nullptr, "EnsureIdTokenCacheAsync"},
#
# A nullptr handler IS the declaration that the function is unimplemented. No
# marker-grepping and no guessing -- the emulator states it.
EDEN_ENTRY = re.compile(r"\{\s*-?\d+\s*,\s*(nullptr|[^,]+?)\s*,\s*\"([A-Za-z_]\w*)\"")


def collect_eden(root):
    out = {}
    base = os.path.join(root, "src", "core", "hle", "service")
    for path in _walk(base):
        module = os.path.basename(os.path.dirname(path))
        try:
            text = open(path, encoding="utf-8", errors="replace").read()
        except OSError:
            continue
        for handler, name in EDEN_ENTRY.findall(text):
            # last write wins; a name can appear in more than one interface
            out.setdefault(module, {})[name] = handler.strip() == "nullptr"
    return out


# Cemu: export(...) registrations plus cafeExportRegister.
def collect_cemu(root):
    opener = re.compile(r"^\s*(?:void|sint32|uint32|int)\s+(?:export_)?(\w+)\(PPCInterpreter_t")
    # Cemu does not use UNIMPLEMENTED. Its idioms are assert_dbg() and
    # debugBreakpoint() for "should not happen", plus a logged unsupported note.
    cemu_stub = re.compile(r"assert_dbg\(\)|debugBreakpoint\(\)|[Uu]nsupported|"
                           r"[Nn]ot implemented|[Nn]ot supported")
    out = {}
    base = os.path.join(root, "src", "Cafe", "OS", "libs")
    for path in _walk(base):
        module = os.path.basename(os.path.dirname(path))
        for name, body in _scan_blocks(path, opener):
            out.setdefault(module, {})[name] = bool(cemu_stub.search(body))
    return out


BACKENDS = [
    ("Vita3K", "psvita/Vita3K-Thor", collect_vita3k),
    ("eden", "eden-thor", collect_eden),
    ("Cemu", "cemu-thor-experiment", collect_cemu),
]


# POSITIVE CONTROLS. This tool is PATH-SCOPED -- each collector walks a named
# directory in one fork. A moved or renamed tree returns zero functions, which
# looks exactly like a backend with no HLE layer.
#
#     A path-scoped search that covers nothing looks identical to one that
#     found nothing.
#
# Floors from the 2026-08-23 census, deliberately below the measured values so
# that a fork implementing more does not fail the control.
CONTROLS = {"Vita3K": 5000, "eden": 200, "Cemu": 200}


def self_test():
    """Prove each collector can see its fork's HLE layer at all."""
    failures = 0
    print("positive controls -- a miss means the TREE moved, not that the")
    print("backend stopped implementing HLE functions.")
    print("")
    for name, rel, collector in BACKENDS:
        floor = CONTROLS.get(name)
        if floor is None:
            print("  %-8s [no control defined -- absence here is unproven]" % name)
            failures += 1
            continue
        fork = name
        root = os.path.join(FLEET, rel)
        if not os.path.isdir(root):
            print("  %-8s SKIP  not present beside this repo" % fork)
            continue
        try:
            rows = collector(root)
        except Exception as e:                     # noqa: BLE001
            print("  %-8s FAIL  collector raised %s" % (fork, e))
            failures += 1
            continue
        n = sum(len(v) for v in rows.values()) if isinstance(rows, dict) else len(rows)
        if n < floor:
            print("  %-8s FAIL  %d functions, expected at least %d" % (fork, n, floor))
            failures += 1
        else:
            print("  %-8s ok    %d functions" % (fork, n))
    print("")
    print("%d failure(s)." % failures)
    if failures:
        print("DO NOT REPORT A FUNCTION AS UNIMPLEMENTED until these pass.")
    return failures


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--self-test", action="store_true",
                    help="prove each collector can see its fork's HLE tree. This tool "
                         "is PATH-scoped, so a moved tree reads as a backend with no "
                         "HLE layer at all.")
    ap.add_argument("--json", help="write the full table here")
    ap.add_argument("--fork", help="only this backend")
    ap.add_argument("--modules", type=int, default=10, help="modules to list per backend")
    args = ap.parse_args()
    
    if args.self_test:
        return 1 if self_test() else 0

    table = {}
    for name, rel, fn in BACKENDS:
        if args.fork and args.fork.lower() != name.lower():
            continue
        root = os.path.join(FLEET, rel)
        if not os.path.isdir(root):
            print("[SKIP] %s not found beside this repo" % name)
            continue
        table[name] = fn(root)

    for name, mods in table.items():
        total = sum(len(v) for v in mods.values())
        stubbed = sum(sum(v.values()) for v in mods.values())
        if not total:
            print("\n%s: no HLE functions matched. The probe needs updating for "
                  "this backend's shape." % name)
            continue
        print("\n%s: %d HLE functions, %d stubbed (%.0f%%), %d modules"
              % (name, total, stubbed, 100.0 * stubbed / total, len(mods)))
        rows = sorted(mods.items(), key=lambda kv: -len(kv[1]))[:args.modules]
        print("  %-26s %6s %8s %6s" % ("module", "funcs", "stubbed", "%"))
        for mod, fns in rows:
            t, s = len(fns), sum(fns.values())
            print("  %-26s %6d %8d %5.0f%%" % (mod, t, s, 100.0 * s / t if t else 0))

    if args.json:
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump(table, fh, indent=1, sort_keys=True)
        print("\nwrote %s" % args.json)
    return 0


if __name__ == "__main__":
    sys.exit(main())
