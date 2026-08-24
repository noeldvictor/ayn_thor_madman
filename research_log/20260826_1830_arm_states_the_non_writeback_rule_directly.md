# Arm states the non-writeback rule directly, and neither mid-core has a loop buffer

**Goal: finish `microarchitecture.md`'s memory-routines section. This proposes no
lever.**

**Device-free: one section. No device used.**

## Arm says it outright, which closes this morning's finding

**§4.3, quoted:**

> *"Unroll the loop to include multiple load and store operations per iteration,
> minimizing the overheads of looping. **Align stores on 32B boundary** wherever
> possible. **Use non-writeback forms of `LDP` and `STP` instructions**
> interleaving them."*

**This morning I recorded rpcsx REDISCOVERING the third point** — its
`vld1q_u8_x2` compiled to the writeback form, which adds the `I` pipe, and its
conclusion was *"read the addressing mode, not the instruction name."*

> **Arm's guide states it as direct advice.** The rediscovery was correct and
> unnecessary — **the rule was in §4.3 all along**, in the same document the fork
> had vendored.

**That is the same shape as everything else this week**: the qualification exists
in a document nobody read to the end. **Here the document is Arm's.**

**And rpcsx's `thor_copy_nontemporal` already follows it** — non-writeback
`LDNP`/`STNP` with a separate `add` rather than post-increment. **It does 64 bytes
per iteration and does NOT align stores**, so one of the three points is
unimplemented.

## The discipline note beside it is the better half

> *"**Whether it is worth doing is a different question**, because the eviction
> the non-temporal path was built for measured as nothing across cores, and the
> copy itself gained only 3.1%. **Improving a kernel whose benefit is
> unestablished is not the next thing to do.**"*

**A concrete refusal to optimise**, with the reason: the kernel exists to prevent
an eviction that **measured as nothing**, so making the kernel faster improves
something whose value is unproven.

> **That is the ceiling rule with a second axis.** The usual form asks whether the
> SITE is big enough to matter. **This asks whether the site should exist.**

## A well-specified negative worth keeping

> **"Neither the A715 nor the A710 document mentions a LOOP BUFFER or a micro-op
> cache."**

**So there is no fetch-side argument against unrolling on the mid cluster** — and
the mid cluster is where rpcsx measured the work to be.

**This matters because "will it blow the loop buffer" is the standard objection to
unrolling**, and on these cores the structure it names is not documented to
exist. **The negative is specific**: two named documents, one named structure.

## Limits

- **The §4.3 quotation is from rpcsx's document**, not read from Arm's PDF here,
  though this repo holds those PDFs.
- **"Not mentioned" is not "not present."** A guide omitting a structure is weak
  evidence it is absent — **but it does mean the guide offers no fetch-side
  argument, which is the operative point.**
- **The 3.1% and the null eviction result are rpcsx's**, on its workload.
- **No claim about the X3**, which was not part of that statement.

## Sources

- rpcsx `docs/arm64/microarchitecture.md:735-756`
- `research_log/20260826_0905_read_the_addressing_mode_not_the_instruction_name.md`
