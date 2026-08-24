# A dead branch in a live detector, found by the cheapest possible check

**Goal: I recorded that `bug_class_sweep.py` still had no control. Give it one.
This proposes no lever.**

**No device. One tool, one live defect fixed.**

## It found a real defect on its first run

**`path_as_identity`'s pattern carried a literal backspace byte** — `\x08` at
position 37, **where a word boundary was meant**:

```
key\([^)]*\)[^=]*= *[a-zA-Z_.]*\.path\x08|${path}_|Cache\.(Insert|insert|...
                                     ^^^^
```

> **No source line contains a backspace, so that alternation could never
> match** — and it is the class's HEADLINE shape, the one the class is named
> for. **`key(...) = something.path` has never been detected.**

**The other alternations still worked**, so the class was **half-dead rather than
dead**, which is worse: it returned plausible results and nobody would question
them.

**Same defect, same cause, second tool.** `supervise.py`'s `queue-stale` had a
backspace in the same position for the same reason — **a backslash escape mangled
when the file was written.** A control character in a regex is **invisible in an
editor, legal in a string, and matches nothing.**

## The fix avoids the construction that caused it

**Replaced with an explicit negative lookahead — `(?![A-Za-z0-9_])` — rather than
a word boundary.** Deliberately: **writing a backslash-b through a shell heredoc
is what produced the backspace twice today.** Verified the branch now matches
`std::string key(a, b) = g.path;`.

## What the self-test checks, and what it does not

**`bug_class_sweep.py --self-test`, 16 classes:**

- every `pattern`, `near`, `near_absent` and `exclude` **compiles**
- **no control characters** — the defect above
- every class has `what`, `paid`, `why`, `pattern`

> **It checks the class TABLE, not behaviour**, and the output says so: *"a class
> can compile cleanly and still describe the wrong shape."* **A behavioural test
> needs a synthetic git repository**, because `scan()` runs `git grep` and its
> filtering is inline. **Not built.**

## Three meta-controls, because the first attempt failed silently

**My first meta-control did not inject at all** — the edit did not take, the
self-test still reported `OK`, and **I had proven nothing.** Doing it properly, in
memory:

| Injected | Result |
| --- | --- |
| a control character | **`[FAIL] CONTROL CHARACTER 0x8 -- matches nothing`, rc 1** |
| a malformed regex | **`[FAIL] bad regex: missing )`, rc 1** |
| a missing required field | **`[FAIL] missing why`, rc 1** |

**Baseline rc 0. Verdict: discriminates.**

> **The failed first attempt is the point.** A meta-control that quietly does not
> run looks exactly like one that passes — **which is the whole disease, met one
> level up while testing for it.**

## Limits

- **A schema check.** It cannot tell whether a class describes a real bug shape,
  only that its patterns are well formed.
- **No behavioural coverage.** `scan()`'s `near`/`near_absent`/`exclude` filtering
  — where this tool's OTHER defect lived — **is still untested.**
- **Four tools still have no self-test**: `emitted_flags.py`,
  `fleet_docs_index.py`, `fleet_lint.py`, `target_check.py`.
- **Any claim this repo made from `path_as_identity` was made with a half-dead
  detector**, and I have not gone back to see which claims those were.

## Files

- `tools/bug_class_sweep.py` — `--self-test`, and the fixed pattern
