# Auditing the device queue for work that never needed the device

**Goal: I wrote that "the same re-read is owed to the other 26 entries" after one
turned out not to need the Thor. Do the pass.**

**Device-free: reading this repo's own queue. No device used.**

## The queue already carries the lesson and under-applies it

**`DEVICE_QUEUE.md` line 204, from entry 4:**

> **"The lesson for this queue: ask whether a question needs the device at all."**

**Three entries are already marked as needing none** — 4 (the compile target), 14
(carryless multiply), 20 (xenia's save-state deadlock, which *gates* device work
without needing it). **The rule exists. It has not been re-run over the entries
added since.**

## The finding: 12 and 13 are ONE experiment, and it is now unblocked

**Entry 12, "What does an IR cost on this device", already says:**

> *"**Do the cheap half first, and it needs no device.** Count host instructions
> emitted per guest instruction for the same guest block, in a fork with an IR
> and one without. **That is a disassembly count, exactly like the
> target-features work.**"*

**Entry 13, "Measure instruction inflation", supplies the instrument for exactly
that count** — xenia's `--disassemble_functions` with a function filter, and a
five-line script over `DumpMachineCode`'s output.

> **12 names the measurement and says it needs no device. 13 has the instrument
> and claimed it did. Neither cites the other, and the claim in 13 was wrong.**

**So the two are one experiment**, and with entry 13's blocker corrected —
`aarch64-linux-gnu-g++` and `qemu-aarch64` are both present in WSL on this
machine — **the whole thing is device-free.**

**That matters because 12 calls itself "the only device experiment in this queue
with a published mechanism behind it, rather than a hunch."** The best-founded
item in the queue has been waiting for hardware it does not need.

## What the pass does NOT change

**Most of the queue genuinely needs the Thor**, and saying so is the point of the
audit rather than a disappointment:

| Entry | Why the device is genuinely required |
| --- | --- |
| **23** `LDAPR` against `LDAR` | a **timing** difference on real silicon; **qemu cannot answer it** |
| **24** `textureCompressionBC` | a **driver-reported boolean** — only the driver knows |
| **18** AArch32 at EL0 | a **property of the shipped SoC** |
| **26** Turnip attachment self-read | **driver behaviour**, and the whole question is what THIS driver does |
| **5** the `targetSdk` raise | *"a build passing does not test it"* — the failures are **display-cutout and two-panel** problems |
| **2, 6, 7, 9, 10, 11, 15, 17, 19, 22, 25, 27** | timing, thermals, or driver behaviour |

> **The discriminator that fell out: a question about EMITTED CODE is
> device-free. A question about TIMING, THERMALS or a DRIVER'S ANSWER is not.**
>
> **qemu-user gives correct instructions and meaningless cycles.** That is why it
> serves 12 and 13 and nothing else in this list.

## Ledger queried before calling it runnable

**`exp_ledger.py check "inflation"` returns ZERO matches**, so the measurement is
genuinely untaken rather than dead or flat.

**And `check "disassem"` confirms the instrument works in practice.** xenia
already used it on-device — *"Dumped on-device with
`--es disassemble_function_filter 8238CD28`"* — to identify a guest function
called **~21M times per second**, reading its PowerPC line by line.

> **So the mechanism is proven; only the host it ran on is in question.** That
> narrows the remaining risk to the cross build and qemu-user, which is where I
> already put it.

## Two smaller observations

**Entry 5 is the one whose blocker is most easily misread.** Its own title —
*"a build passing does not test it"* — is a warning that the cheap check is not
the real one. **The opposite error from entry 13's**, and worth keeping beside it:
**one entry overstated its need for the device and another exists to stop
somebody understating theirs.**

**Entry 20 is listed *"because it gates device work, not because it needs the
device"***, which is a third category the queue already models and does not name:
**device-free work that unblocks device work.** Entries 12 and 13 now join it.

## Limits

- **This is a read of the queue's own text, not a re-derivation of each
  blocker.** Only entry 13's premise was actually tested, and only because it
  named a checkable fact.
- **The table above takes each entry's stated reason at face value** — which is
  precisely what failed for entry 13. **Twelve entries are marked "genuinely
  needs the device" on their own say-so.**
- **Nothing was measured.** The device-free route for 12 and 13 is identified and
  untried: the cross build and the qemu run are both unexercised.

## Files

- `DEVICE_QUEUE.md` — entries 12 and 13 joined and marked device-free
