# 241 header basenames collide across the fleet

**Goal: measure the ISOLATE problem concretely, rather than as a percentage of
un-namespaced headers.**

Session 2026-08-23 02:30. `UNIFICATION.md` section 7 names ISOLATE as the third
operation and measures it as *"headers at global scope"*, which is a proxy.
**This is the direct measurement.**

---

## The number

**3,898 distinct header basenames across seven forks. 241 of them appear in two
or more.**

| Appears in | Basename |
| --- | --- |
| **5 forks** | `util.h` |
| **4 forks** | `types.h`, `shared_memory.h`, `ring_buffer.h`, `input.h`, `hash.h`, `file.h`, `config.h`, `atomic.h` |
| 3 forks | `window.h`, `vector_math.h`, `thread.h`, `texture_cache.h`, `system.h`, `string_util.h`, `stream.h`, `state.h`, `socket.h`, `shader.h`, `settings.h`, and more |

**These are the names a codebase gives its own foundations.** Every emulator
needs a `types.h`, a `config.h` and a `util.h`, and each wrote its own.

## What it does and does not mean

**It is not automatically a build failure.** With per-target include
directories — `target_include_directories(armsx2 PRIVATE pcsx2/)` — each fork
resolves its own `types.h` and nothing collides. That is normal practice and
most of these forks do it.

**It becomes a problem the moment anything is shared, and it does so silently.**

**A shared-layer header that writes `#include "types.h"` is ambiguous by
construction.** Which one it gets depends on the include order of whichever
target is compiling it, so **the same shared header can compile against seven
different `types.h` and produce seven different translation units** — and it
will not necessarily error. It may just quietly pick up the wrong `Config`.

**That is the worst class of build problem**: not a failure, a divergence.

## The rule this produces, and it is cheap

**Every shared-layer header must be included by a unique prefixed path, and must
itself use prefixed includes only.**

```cpp
#include "thor/device.h"      // yes
#include "types.h"            // never, in shared code
```

**And the shared layer must never add its own directory to a backend's include
path unprefixed.** A `thor/` prefix directory costs nothing and removes the
entire class.

**Do this before the first extraction, not after.** It is a naming decision, and
naming decisions are cheap before there are callers and expensive afterwards.

## Why the proxy measurement was not enough

`UNIFICATION.md` measures ISOLATE as the share of headers declaring nothing
inside a `namespace` — Cemu 66%, ARMSX2 59%, Vita3K 53%.

**That measures symbol collisions at link time. This measures include collisions
at compile time.** They are different failures with different fixes:

| | Symptom | Fix |
| --- | --- | --- |
| **Symbol collision** | duplicate definition at **link** | namespaces, `-fvisibility=hidden` |
| **Include collision** | the **wrong header** is compiled, silently | a unique include prefix |

**Both are ISOLATE, and neither is unification.** Two forks' `types.h` do not
want to become one `types.h`.

## Method and its limits

```sh
find <fork native root> -name '*.h' -o -name '*.hpp' \
  | grep -v <vendored> | xargs -n1 basename | sort -u
```

**Vendored trees excluded**, including `.cache/cpm` and `_deps` after the false
positive that taught that lesson earlier tonight.

**Limits.** This counts names, not include statements — **it does not prove any
fork actually writes `#include "types.h"` unqualified**, only that the name is
ambiguous if paths are merged. **Confirming the hazard needs a grep for
unqualified includes of the colliding names**, and that is not done.

**And a basename collision between two forks that never share a translation unit
costs nothing.** The number is an upper bound on the hazard, not a defect count.
