# A guard that guarded nothing, caught by the control I ran out of habit

**Goal: I named the queue-staleness gap three times in two days and said it was
"enough to name, not yet enough to automate". It is narrow enough. Build it.**

**No device. One check, three controls, one bug in the check itself.**

## What the check does

**A log that cites a queue entry — "entry 26", "queue 13" — while
`DEVICE_QUEUE.md` is unchanged raises a WARN asking whether the entry still
holds.**

**It does not assert a fault.** A log may cite an entry for context. **It asks the
question at the moment the citation is written**, which is the only moment
somebody can cheaply answer it.

**The exemption is the important half**: if the same change touches
`DEVICE_QUEUE.md`, the check is satisfied — the author already considered it.

## The bug, and it is the one this whole project is about

**The check reported `OK` on a file that plainly cited entry 26.** The regex
matched the line when tested standalone. The path was iterated. `_added_lines`
returned the line. **And `finditer` found nothing.**

**Printing the compiled pattern:**

```
'\x08(?:DEVICE_QUEUE(?:\.md)?...|\x08queue\s+(\d+)\x08'
```

> **`\x08` is a literal BACKSPACE byte.** The `\b` word boundaries I wrote had
> been mangled into control characters when the file was written, and **a regex
> beginning with a backspace matches nothing in ordinary text.**

**So the check ran, iterated every file, examined every line, and could never
fire.** It reported `OK` on every input.

> **That is a `DID_IT_APPLY` instance in a tool built to detect staleness** — the
> guard was installed, configured, and structurally incapable of firing. **The
> same shape as rpcsx's LSE2 fast path guarded on a macro nothing defined**, and
> as the census that produced zero because it sat behind an unrelated cvar.

**It was caught by a positive control I ran from habit**, not from suspicion. The
rule this repo already carries — *an instrument that can return zero must be
proved able to return non-zero* — **is the only reason this is a work log rather
than a silently useless check in the tree.**

## Three controls, because one is not enough

| Control | Expected | Got |
| --- | --- | --- |
| a log citing entry 26, queue untouched | **WARN, naming 26** | **WARN, naming 26** |
| a log citing nothing | OK | OK |
| a log citing entry 26 **with `DEVICE_QUEUE.md` also changed** | OK | OK |

**The third is the one that would have been skipped.** A check that fires
correctly and never stops firing is as useless as one that never fires — **the
exemption needs proving too.**

## An honest note on the escape problem

**This is at least the fifth time this session that a backslash escape has been
mangled writing a file through a shell heredoc.** Every previous instance was
visible immediately — a syntax error, a broken string. **This one was invisible**,
because a backspace byte is a legal character in a string literal and prints as
nothing.

**The fix was to remove the word boundaries entirely** and use a negative
lookbehind for the one place it mattered. **The pattern is slightly looser and
verifiably correct**, which is the better trade.

## It fired on this log, and the answer is recorded

**On the commit that added it, `queue-stale` warned on four lines of this very
log**, which cites entries 26 and 13. **The answer: both were updated earlier
today** — 26 gained a third probe arm, 13 had its device blocker corrected. **The
citations are context, not staleness.**

> **A log ABOUT queue staleness will always cite entries, so this file would be a
> permanent WARN — and a permanent WARN is ignored.**

**So the check gained an exemption, the same shape as `dead-levers` accepting a
recorded ledger query**: a log that RECORDS the answer satisfies it. **Not a
magic word — a statement that somebody looked.** Controls re-run: an unanswered
citation still WARNs, an answered one is OK.

> **The check asks, a person answers, and the answer lives beside the
> citation.** That is the workflow, and now the tool recognises it.

## Limits

- **A WARN, not a failure.** It cannot block anything and will produce false
  positives whenever a log legitimately cites an entry it does not change.
- **It only sees ADDED lines in logs.** A citation in an unchanged part of an old
  log is invisible, which is correct but worth knowing.
- **It does not know whether the entry ACTUALLY needed updating** — nothing could,
  short of understanding the finding. **It asks; a person answers.**
- **The three instances that motivated it were all mine**, from two days.

## Files

- `tools/supervise.py` — `check_queue_staleness`, registered as `queue-stale`
