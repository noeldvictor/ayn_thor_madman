# A blocking applet needs a canceller, and our `AcceptedInput` is the fourth instance of a shape this repo rejects

**Goal: XenDroid implemented the guest software keyboard — 617 lines across 18
files. This project's contract models applets on azahar. Compare them.**

**Device-free: one commit diff, one header read, one compile check. No device
used.**

## Their contract is 49 lines and three things in it are better than ours

```cpp
struct HostTextInputRequest {
  std::string title, description, default_text;
  uint32_t max_length = 0;  // UTF-16 code units, terminator excluded
  uint32_t flags = 0;       // raw guest flags, uninterpreted
};
```

### 1. The canceller — and this is the second deadlock of this shape today

> *"**Fails every blocked request. The kernel calls this before waiting on the
> dispatch thread, which could otherwise be parked inside a request.**"*

**Our contract already said an applet request BLOCKS THE GUEST and said nothing
about teardown.** The consequence is a deadlock: the shell joins a thread waiting
on a dialog nobody will answer.

**The other instance, found hours earlier and in a different subsystem**, is
XenDroid's audio pause: *"a worker parked on `resume_event_` never sees
`worker_running_` go false."*

> **Any design that parks a thread awaiting an external event owes an answer for
> teardown.** Twice in one day, two subsystems, one fork.

**And azahar does not need one**, because its applets are asynchronous —
`Setup`/`Execute` then `DataReady`/`ReceiveData`, nothing parked. **The canceller
is the price of the blocking model**, and our contract chose blocking. `thor_backend.h`
now carries `CancelApplets()` and the rule that a cancelled request resolves as a
**dismissal**, not an error, so the guest always gets an answer.

### 2. `max_length` had no unit

**Theirs says "UTF-16 code units, terminator excluded". Ours said `uint16_t
max_length` and nothing else.**

> **A backend and a shell that disagree about bytes, code units or code points
> truncate a name or over-run a guest buffer, and neither side errs.** The unit
> is now stated.

### 3. `flags = 0; // raw guest flags, uninterpreted`

**This is the one that exposed a defect in our own design.**

Our `TextEntryConfig` carries `AcceptedInput`, **a fixed enum of five cases taken
from azahar** — `kAnything`, `kNotEmpty`, `kNotEmptyAndNotBlank`, `kNotBlank`,
`kFixedLength` — which `CLAUDE.md` praises because azahar *"learned these cases
from real 3DS titles."*

> **It is a shared enum derived from ONE guest's taxonomy, which is precisely the
> shape this project has rejected three times already.**

| Rejected | Because |
| --- | --- |
| a fixed texture-class enum | *"a fixed enum would impose one emulator's taxonomy on the rest"* |
| a fixed filter-algorithm enum | ARMSX2's `Anime4K` is a neural net, melonDS's is a nine-texel kernel — **same name, unrelated techniques** |
| a fixed cheat memory-region enum | pnach picks `EE`/`IOP`, dmnt has `MainNso`/`Heap`/`Alias`/`Aslr` — **backend-declared** |
| **`AcceptedInput`** | **the fourth, and it is in our own header** |

**The fix is not to delete it.** `AcceptedInput` is what a shell can act on
**without guest knowledge** — it can grey out the OK button knowing only "not
empty". **Deleting it would push guest semantics into the shell**, which is the
opposite error.

**So both**: keep the named cases the shell can act on, **and carry
`guest_flags` uninterpreted** so nothing is lost for a backend that knows more.
**That is the same resolution the cheat design reached** — a shared instruction
set with a backend-declared region namespace.

## What was changed

`shared_layer/thor_backend.h`: `CancelApplets()` with the teardown rule,
`max_length`'s unit, and `guest_flags`. **Compiles under NDK 29 clang++ at C++20
for `aarch64-linux-android33`.**

## Limits

- **Nothing is implemented.** The header is a design that nothing links against,
  and adding a function to it proves only that it parses.
- **The deadlock is reasoned, not observed.** Our applet path does not exist, so
  it cannot deadlock yet. **XenDroid's canceller comment is evidence that the
  hazard is real in a shipped emulator, not that ours would hit it.**
- **The `AcceptedInput` criticism is of our own header**, and it is a design
  judgement rather than a measured defect. **azahar's five cases may well cover
  every guest in this fleet** — nobody has checked, and that is the point:
  the enum was adopted without checking.
- **XenDroid's file is WTFPL**, so its code as well as its design is usable. Only
  the design was taken.

## Sources

- XenDroid, `[Android] Answer guest text prompts with the Android keyboard`,
  `src/xenia/ui/host_text_input.h`
- `shared_layer/thor_backend.h`
- `research_log/20260825_1150_pause_existing_is_not_pause_working.md`
