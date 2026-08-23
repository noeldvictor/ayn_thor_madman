# azahar's applet implementation, read

**Goal: read it, as [`CLAUDE.md`](../CLAUDE.md) instructs — "Survey before
designing it. azahar has a working applet implementation and it has not been
read."**

No device. Reading only.

## Result

**It is already the contract, layered exactly the way this project would need.**
Do not design a host-UI interface. **Take this one.**

| Layer | Path | Role |
| --- | --- | --- |
| **the contract** | `src/core/frontend/applets/` | abstract base + config/data structs |
| guest side | `src/core/hle/applets/` | the HLE applet the game calls |
| desktop | `src/citra_qt/applets/` | Qt implementation |
| Android | `src/android/.../jni/applets/` + `.../applets/*.kt` | JNI + Kotlin |

**Two applets are implemented: a software keyboard and a Mii selector.** Both
follow one shape, which is what makes it a contract rather than two dialogs.

## The shape

```
Config struct  →  Setup/Execute  →  ...  →  Finalize  →  DataReady/ReceiveData  →  Data struct
```

**It is asynchronous by construction.** `DataReady()` polls and `ReceiveData()`
consumes and clears. Nothing blocks the guest thread waiting on a person, which
is the property a naive interface gets wrong.

## Five things the draft contract in CLAUDE.md does not have

`CLAUDE.md` sketches three calls: request text entry, request a user selection,
report an error. **Reading azahar shows what that sketch is missing.**

### 1. Validation lives in the shared layer, in three phases

`ValidationError` has **twelve values**, and three separate entry points:

| Call | When | Example error |
| --- | --- | --- |
| `ValidateFilters` | as the person types | `AtSignNotAllowed`, `MaxDigitsExceeded` |
| `ValidateInput` | on submit | `BlankInputNotAllowed`, `FixedLengthRequired` |
| `ValidateButton` | last check before closing | `ButtonOutOfRange` |

**The frontend chooses whether to validate continuously or once**, and the
header says so. **That is the right split**: the rules are guest knowledge and
belong in the shared layer; *when* to apply them is a UI decision.

### 2. The guest can validate, which makes it a round trip

`Filters::enable_callback` means **the guest wants to check the input itself**,
and `ShowError(const std::string&)` is how its rejection reaches the person.

**So a text entry is not request-and-return.** It is: show, collect, hand to the
guest, possibly show the guest's error, let the person edit, repeat. **A
contract with a single "receive a string" call cannot express this.**

### 3. Button labels come from the guest

`ButtonConfig` is `Single` / `Dual` / `Triple` / `None`, and `button_text` is
**supplied by the caller**. The header ships English defaults — `"Ok"`,
`"Cancel"`, `"I Forgot"` — with a comment that frontends must copy them to
internationalise.

**`"I Forgot"` is the tell.** That is a password-recovery button on a 3DS
parental-controls screen. **No generic host dialog would predict it**, which is
precisely why the guest supplies the strings and the host supplies the styling.

### 4. A default implementation is always registered

```cpp
void RegisterDefaultApplets(Core::System& system) {
    system.RegisterSoftwareKeyboard(std::make_shared<DefaultKeyboard>(system));
    system.RegisterMiiSelector(std::make_shared<DefaultMiiSelector>());
}
```

**A guest that requests an applet never faces a missing frontend.** The default
answers, and a real frontend overrides it.

**This matters more in a unified app than it did in azahar**, because seven
backends will request applets and the shell will implement them at different
times. **The default is what makes a partially-implemented shell still boot.**

### 5. The extension policy is written in the header

Both config structs carry the same comment:

> Configuration that's relevant to frontend implementation of applet. Anything
> missing that we later learn is needed can be added here and filled in by the
> backend HLE applet.

**That is the declared-extension rule this project already chose**, arrived at
independently and stated in the file it governs.

## What to change in CLAUDE.md

The applet section says a backend needs "a way to request host UI and receive a
result", and lists three calls. **Replace that with:**

- **A config struct in, a data struct out**, per applet kind.
- **Asynchronous**: `DataReady` / `ReceiveData`, never a blocking call.
- **Validation in the shared layer, in three phases**, with the frontend
  choosing when to run them.
- **A guest-callback path**, because the guest may reject the input and its
  error text must reach the person.
- **Caller-supplied button labels** with host-supplied styling.
- **A default implementation registered for every applet kind.**

## Licence

azahar is **GPL-2.0-or-later**, so it can be used as GPL-3.0. **The code is
usable, not only the design.**

## Limits

- **Two applet kinds read.** azahar also has `src/core/hle/applets/erreula.cpp`
  — the 3DS error dialog — which is guest-side and was not read here.
- **The Android implementations were not read**, only the contract and the
  registration. `MiiSelector.kt` and `SoftwareKeyboard.kt` are the reference for
  how the shell would implement it.
- **No other fork's applets were compared.** rpcsx's
  `overlay_user_list_dialog.h` is already recorded; eden and Cemu are unread.
