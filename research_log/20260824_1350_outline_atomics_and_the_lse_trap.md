# Every atomic on Android is a function call by default, and `-mno-outline-atomics` alone makes it worse

**Goal: read Vita3K's ARM64 review of its own shipped binary, then verify its
central finding on this box.**

**Verified, and testing it produced a trap that neither that document nor this
repo had recorded.**

## What Vita3K found, by disassembling its own `.so`

`docs/research/20260821-arm64-review-of-the-vita3k-tree.md`, **7.68 M lines of
`llvm-objdump` output** from the shipped `libVita3K.so` — **read from the binary,
not from source.**

> **The NDK's `arm64-v8a` ABI is Armv8.0-A, which predates LSE atomics.** Clang's
> default response is `-moutline-atomics`: instead of emitting an atomic
> instruction, **it emits a call to a stub that decides at runtime.**

The stub, disassembled there:

```
__aarch64_ldadd8_acq_rel:
  bti     c
  adrp    x16, ...            ; address of the feature flag
  ldrb    w16, [x16, #0x7f8]  ; load it
  cbz     w16, .Lllsc         ; branch on it
  ldaddal x0, x0, [x1]        ;   <- the entire point
  ret
```

**One atomic instruction becomes a call, a BTI landing pad, an ADRP, a byte load,
a conditional branch, the instruction, and a return** — plus the return at the
call site. **On every atomic in the binary.**

**Five findings in that review**, and the other four are worth naming: **once-only
log guards are an atomic RMW on the hot path**; **LTO is configured but never
actually on**; **dynarmic leaves exclusive accesses on the slow path**; and
**nothing sets thread affinity**, which this repo already records.

**Two of the five are "a lever that is configured and never applies"** — the same
class as xenia's AOT object cache being off on the launch path people use.

## Verified here, and the third row is the trap

Compiled `std::atomic<long>::fetch_add(-1, acq_rel)` with this box's NDK clang,
`aarch64-linux-android33`:

| Flags | Emitted |
| --- | --- |
| **NDK default** | **`bl __aarch64_ldadd8_acq_rel`** — the outline call |
| **`THOR_TARGET.md`'s line, which carries `+lse`** | **`ldaddal x8, x0, [x0]`** — one instruction |
| **`-mno-outline-atomics` alone** | **`ldaxr` / `stlxr` retry loop** |

> **`-mno-outline-atomics` by itself is worse than the default.** Without `+lse`
> the compiler cannot emit an LSE atomic, so disabling the outline helper only
> removes the runtime upgrade path and leaves the LL/SC loop.

**And LL/SC is not merely more instructions.** `STLXR` takes the cache line in
**exclusive** state, so **a contended read-modify-write invalidates every other
core's copy** — the same mechanism rpcsx measured on its reservation path.

## What this settles

- **`THOR_TARGET.md` is already correct**, and now for a checked reason: **`+lse`
  alone is sufficient**, and `-mno-outline-atomics` is not required to get
  `ldaddal`.
- **xenia's `-march=armv8.2-a+lse+... -mno-outline-atomics` is correct**, because
  it carries both halves.
- **A fork that sets `-mno-outline-atomics` without `+lse` would pessimise
  itself.** None currently does — searched the fleet's own build files for both
  spellings on 2026-08-24.
- **Cemu's choice is defensible and inverted here.** It uses `-moutline-atomics`
  deliberately so the binary runs on pre-ARMv8.1 devices. **This project has one
  device and it reports `atomics`**, so the dispatch is a tax for hardware that
  does not exist here.

## Limits

- **No timing.** The instruction-count difference is disassembled; **the cost of
  the call and the flag load is not measured**, and this repo's record says
  instruction count is a poor objective on its own.
- **Vita3K's finding is marked "Fixed" in its own document**, so its shipped
  binary may no longer show it. **The disassembly quoted is theirs, not a fresh
  capture.**
- **The other four findings in that review were read but not verified.**
- **NDK 30's clang was used; the standard row pins NDK 29.**

## Sources

- Vita3K `docs/research/20260821-arm64-review-of-the-vita3k-tree.md`
- NDK 30 clang, `aarch64-linux-android33`, compiled on this box
