package com.aynthor.shell

/**
 * The check a backend passes before the app will use it.
 *
 * `shared_layer/BACKEND_STANDARD.md` calls itself the acceptance test for "is
 * this backend finished". This is the machine-checkable part of it, and it runs
 * with no device, no guest and no game.
 *
 * Every fault here is a bug somebody shipped, and all of them present the same
 * way to a person: a control moves and nothing happens, or a value is quietly
 * something other than what was configured. See `shared_layer/DID_IT_APPLY.md`.
 */
data class AdmissionFault(val key: String, val problem: String)

object BackendAdmission {

    /**
     * Every fault, not the first.
     *
     * A backend author fixes one list rather than iterating one failure at a
     * time, which is the same reason [validateSchema] returns a list.
     */
    fun admit(backend: Backend): List<AdmissionFault> {
        val specs = backend.settings()
        val faults = validateSchema(specs)
            .map { AdmissionFault(it.key, it.problem) }
            .toMutableList()

        val byKey = specs.associateBy { it.key }
        val declared = byKey.keys
        val supplied = backend.defaults()

        // A default for a key no setting declares can never be shown, never be
        // overridden, and never be resolved. It is dead configuration.
        for (key in supplied.keys.sorted()) {
            if (key !in declared) {
                faults.add(AdmissionFault(key, "default supplied for a key no setting declares"))
            }
        }

        // THE CONTRACT HAS TWO DEFAULTS, and nothing made them agree.
        //
        // SettingSpec.default is what the settings screen shows. defaults() is
        // what SettingResolver falls through to. A backend can set them to
        // different values and every layer behaves "correctly": the screen
        // displays one number and the emulator runs another.
        //
        // That is the second-writer bug this project already records from
        // rpcsx, where Max LLVM Compile Threads was written by a config file, a
        // performance profile and a per-game profile, and editing the config
        // was silently undone. Two writers, no owner.
        //
        // The right long-term fix is one source of truth. Until the contract is
        // changed, admission refuses the disagreement.
        for (key in supplied.keys.sorted()) {
            val spec = byKey[key] ?: continue
            val value = supplied.getValue(key)
            if (value != spec.default) {
                faults.add(
                    AdmissionFault(
                        key,
                        "defaults() says '$value' and SettingSpec.default says " +
                            "'${spec.default}' -- the screen and the resolver would disagree",
                    ),
                )
            }
        }

        // A declared setting with no supplied default resolves to nothing once
        // the per-game and global layers miss.
        for (spec in specs) {
            if (spec.key !in supplied) {
                faults.add(AdmissionFault(spec.key, "declared setting has no default"))
            }
        }

        return faults
    }

    /** Convenience for a caller that only wants the verdict. */
    fun isAdmissible(backend: Backend): Boolean = admit(backend).isEmpty()
}
