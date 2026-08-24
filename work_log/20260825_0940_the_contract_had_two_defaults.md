# The contract had two defaults, and nothing made them agree

**Goal: close the gap I recorded an hour ago — the schema validator was checked
only by a test, which makes it advisory. Wire it into an admission check.**

**No device. A change to this repo's own app shell, plus a test run.**

## What wiring it revealed

**`shared_layer/BACKEND_STANDARD.md` calls itself the acceptance test for "is
this backend finished". There was no code for it**, so `BackendAdmission` is the
machine-checkable half: no device, no guest, no game.

**Reading the contract to find the wiring point found a defect in the contract.**

```kotlin
data class SettingSpec(..., val default: String, ...)
interface Backend { fun settings(): List<SettingSpec>; fun defaults(): Map<String, String> }
```

> **`SettingSpec.default` is what the settings screen shows.
> `defaults()` is what `SettingResolver` falls through to. A backend can set them
> to different values, and every layer behaves correctly: the screen displays one
> number and the emulator runs another.**

**That is the second-writer bug this project already records from rpcsx**, where
`Max LLVM Compile Threads` was written by a config file, a performance profile
**and** a per-game profile, so editing the config was silently undone at the next
launch. **Two writers, no owner.** Here it is in our own contract, unbuilt and
therefore free to fix.

## The four faults

| Fault | Why it is a bug and not untidiness |
| --- | --- |
| **the two defaults disagree** | the screen and the resolver show different values, both "correctly" |
| **a default for a key no setting declares** | dead configuration — never shown, never overridable, never resolved |
| **a declared setting with no default** | resolution falls through to nothing once per-game and global miss |
| **any schema defect** | the coercion and option bugs from the previous entry |

**All four present to a person the same way**: a control moves and nothing
happens, or a value is quietly not what was configured. **That is
`DID_IT_APPLY.md`, and the check is where it belongs — at admission, not in a
screen.**

## The interim choice, stated

**The right long-term fix is ONE source of truth for a default.** Admission
**refuses the disagreement** rather than removing a field, because this phase
prefers a reversible decision to a complete one and no backend exists yet to
break. **The code comment says so, so the next reader does not mistake the guard
for the design.**

## Result

**`BackendAdmissionTest`, 7 cases. Suite is 233 tests, 0 failures**, up from 226.

**The three fake backends pass admission unchanged** — including the agreement
check — so the fixtures were already consistent. **That is a real result: the
check could have failed on our own fixtures and did not.**

## Limits

- **Nothing calls `admit()` at runtime yet**, because nothing loads a backend.
  **It is enforced by a test, which is one step better than the previous entry
  and still not structural.** The structural version is a registration path that
  refuses a faulty backend, and that path does not exist.
- **`isAdmissible` is a convenience** and hides which fault fired; every real
  caller should use `admit`.
- **Admission checks the schema and the defaults only.** The other contract
  obligations — lifecycle ops, guest screens, storage categories, counters — are
  unchecked.
- **No claim that a passing backend is correct.** It means the declarations do
  not contradict each other.

## Files

- `app/shell/.../BackendAdmission.kt` — new
- `app/shell/.../BackendAdmissionTest.kt` — 7 cases
