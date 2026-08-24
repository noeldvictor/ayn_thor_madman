package com.aynthor.shell

/**
 * The backend contract.
 *
 * Derived from app/SCREENS.md. Every entry here exists because a screen needed
 * a fact only the emulator knows. Nothing here was designed in advance.
 *
 * Two rules govern this file:
 *
 *  1. **Thin.** The cores are widely different. A PS2 recompiler, a Wii U
 *     graphic pack system and a DS plane compositor do not fit one shape.
 *     Forcing them into one is what makes libretro slow and limiting.
 *  2. **Not a module boundary.** This is an internal interface inside one
 *     binary. It exists for clarity and it must stay inlinable. See
 *     CLAUDE.md, The backend model.
 *
 * Anything a backend can do beyond this is an [Extension] it declares, and the
 * app renders the UI only when the extension is present.
 */

// ---------------------------------------------------------------- identity

data class BackendInfo(
    val id: String,
    val name: String,
    val version: String,
    /** The systems this backend handles. Usually one. */
    val systems: List<System>,
)

/** A title the backend recognised in a file. */
data class TitleId(
    val id: String,
    val title: String,
    val region: String?,
    val version: String?,
)

// --------------------------------------------------------------- lifecycle

/**
 * What a backend can do while a title is loaded.
 *
 * A backend declares which of these it supports. The overlay greys out the
 * rest rather than failing at the press.
 */
enum class LifecycleOp { LOAD, RUN, PAUSE, STOP, SAVE_STATE, LOAD_STATE, REWIND }

// ----------------------------------------------------------------- screens

/**
 * A guest screen. The backend declares what exists; the app decides where it
 * goes.
 *
 * [requiredByTitle] matters: a Wii U game may never draw the GamePad screen,
 * and the layout picker should not offer a panel for it.
 */
data class GuestScreenSpec(
    val name: String,
    val width: Int,
    val height: Int,
    val takesTouch: Boolean,
    val requiredByTitle: Boolean,
)

// ---------------------------------------------------------------- settings

enum class SettingType { BOOL, INT, FLOAT, ENUM, STRING }

/**
 * Which tier a setting may be stored in.
 *
 * Taken from ARMSX2's ConfigStore, which found the exception the hard way. Its
 * PINE server is one instance for the whole process, so the per-game file
 * structurally cannot hold it: toggling it from the in-game menu wrote it
 * NOWHERE, and it read as enabled until the process restarted.
 *
 * See research_log/20260822_2203_armsx2_frontend_is_the_shell.md.
 */
enum class SettingScope {
    /** The normal case. Sparse, and sticky once set. */
    PER_GAME,

    /**
     * Offered per game, but written to GLOBAL, because the per-game tier
     * cannot hold it. Promote by copying this one field onto global, never by
     * saving the resolved object, which would leak every per-game value upward.
     */
    PROMOTED,

    /** Never offered per game at all. */
    GLOBAL_ONLY,
}

/**
 * One setting.
 *
 * [key] must be stable forever. A setting with no stable key cannot carry a
 * per-game override.
 *
 * [scope] exists because "every setting is overridable per game" is nearly
 * true and the exception is real. See [SettingScope].
 *
 * [liveChangeable] drives a rule from SCREENS.md: a setting that needs a
 * restart says so **before** it is changed, not after.
 */
data class SettingSpec(
    val key: String,
    val label: String,
    val type: SettingType,
    val group: String,
    val default: String,
    val options: List<String> = emptyList(),
    val liveChangeable: Boolean = false,
    val scope: SettingScope = SettingScope.PER_GAME,
)

/**
 * Where a resolved setting value came from.
 *
 * The order is fixed and lives in one place. A backend never invents its own.
 */
enum class SettingSource { PER_GAME, GLOBAL, THOR_PROFILE, BACKEND_DEFAULT }

data class ResolvedSetting(val value: String, val source: SettingSource)

/**
 * The one resolver and the one override writer.
 *
 * Kept here rather than in a screen so that no caller can invent an order.
 *
 * The write half is not obvious and is not ours. ARMSX2 shipped the obvious
 * version and reported three bugs from it; [writeOverride] carries all three
 * fixes. Do not simplify it without reading the log named above.
 */
object SettingResolver {

    /** Per-game, then global, then Thor profile, then backend default. */
    fun resolve(
        spec: SettingSpec,
        perGame: Map<String, String>,
        global: Map<String, String>,
        thorProfile: Map<String, String>,
        backendDefault: Map<String, String>,
    ): ResolvedSetting? {
        // A setting the per-game tier cannot hold must not be read from it, or
        // a stale override outlives the rule that put it there.
        if (spec.scope == SettingScope.PER_GAME) {
            perGame[spec.key]?.let { return ResolvedSetting(it, SettingSource.PER_GAME) }
        }
        global[spec.key]?.let { return ResolvedSetting(it, SettingSource.GLOBAL) }
        thorProfile[spec.key]?.let { return ResolvedSetting(it, SettingSource.THOR_PROFILE) }
        backendDefault[spec.key]?.let { return ResolvedSetting(it, SettingSource.BACKEND_DEFAULT) }
        return null
    }

    /** What a game-scope save produces: a new override map and a global patch. */
    data class WriteResult(
        val perGame: Map<String, String>,
        val globalPatch: Map<String, String>,
    )

    /**
     * Write a game-scope change.
     *
     * Three rules, each paid for by a reported ARMSX2 bug:
     *
     * 1. **Sparse.** A field the user never touched is absent, so a later
     *    global change still reaches it. That inheritance is the point.
     * 2. **Sticky.** A field is PINNED once overridden, and stays pinned even
     *    when its value happens to equal global. Without this, setting a value
     *    per game while global agrees stores nothing, and a later global change
     *    silently takes the game's setting with it.
     * 3. **Change-tracked.** Rule 2 makes a wrong value permanent, so a pinned
     *    key the caller did not just touch keeps its STORED value rather than
     *    whatever [updated] holds. Screens write the whole settings object, so
     *    [updated] can be a stale snapshot; without this a per-game frame cap
     *    was overwritten and then stuck, surviving the fix to the writers.
     *
     * [previous] is what the caller last saw. Only keys it proves changed are
     * taken from [updated]. Pass null only when the caller genuinely cannot
     * know, and accept that rule 3 does not apply.
     */
    fun writeOverride(
        specs: List<SettingSpec>,
        existingPerGame: Map<String, String>,
        global: Map<String, String>,
        updated: Map<String, String>,
        previous: Map<String, String>?,
    ): WriteResult {
        val byKey = specs.associateBy { it.key }
        val perGame = LinkedHashMap<String, String>()
        val globalPatch = LinkedHashMap<String, String>()

        val changedNow: Set<String> =
            previous?.let { p -> updated.filter { (k, v) -> p[k] != v }.keys } ?: emptySet()

        // Keys already pinned to this game, plus whatever the caller just changed.
        val pinned = LinkedHashSet<String>(existingPerGame.keys)
        pinned.addAll(changedNow)

        for (key in pinned) {
            val spec = byKey[key] ?: continue
            when (spec.scope) {
                // Cannot live per game. Promote this ONE field, do not write the
                // resolved object, and never keep a per-game copy of it.
                SettingScope.PROMOTED ->
                    if (key in changedNow) updated[key]?.let { globalPatch[key] = it }

                SettingScope.GLOBAL_ONLY -> Unit

                SettingScope.PER_GAME -> {
                    // Trust `updated` only where `previous` proves a change, or
                    // where the caller could not tell us.
                    val trustUpdated = key in changedNow || previous == null
                    val value =
                        if (trustUpdated) updated[key] ?: existingPerGame[key]
                        else existingPerGame[key] ?: updated[key]
                    if (value != null) perGame[key] = value
                }
            }
        }

        // A field that differs from global is an override even if it was never
        // pinned before. This is the entry point into rule 2.
        for ((key, value) in updated) {
            val spec = byKey[key] ?: continue
            if (spec.scope != SettingScope.PER_GAME) continue
            if (key in perGame) continue
            if (global[key] != value) perGame[key] = value
        }

        return WriteResult(perGame, globalPatch)
    }

    /**
     * What a settings save must DO, as opposed to what it must store.
     *
     * A FOURTH ARMSX2 bug, and a different mechanism from the three above.
     * Its in-game "Load Texture Packs" switch wrote the persisted value and
     * never fired the live GS reconfigure, so an imported pack did not appear
     * until the next game boot. The switch moved and nothing happened, which is
     * the PINE symptom reached a third way.
     *
     * The cause is worth more than the bug. ARMSX2 decides what needs a live
     * reconfigure with a hand-written chain of `!=` comparisons, one line per
     * field. **A setting added without touching that chain silently gets no
     * live apply**, and the failure is invisible until somebody flips the
     * switch during a game.
     *
     * So this is DERIVED from the specs and never enumerated. Adding a
     * [SettingSpec] is sufficient; there is no second list to forget.
     */
    data class ApplyPlan(
        /** Changed, and the backend can take it now. */
        val liveApply: Set<String>,
        /** Changed, and it will not take effect until the process restarts. */
        val needsRestart: Set<String>,
    ) {
        val isEmpty: Boolean get() = liveApply.isEmpty() && needsRestart.isEmpty()
    }

    /**
     * Split the changed keys by whether the backend can take them now.
     *
     * A key with no spec is NOT silently dropped into either bucket -- it is
     * ignored, because the app does not own it. A key whose value did not
     * change is not in the plan at all, so a screen that writes its whole
     * settings object does not trigger a reconfigure per keystroke.
     */
    fun applyPlan(
        specs: List<SettingSpec>,
        previous: Map<String, String>,
        updated: Map<String, String>,
    ): ApplyPlan {
        val live = LinkedHashSet<String>()
        val restart = LinkedHashSet<String>()
        for (spec in specs) {
            val before = previous[spec.key]
            val after = updated[spec.key] ?: continue
            if (before == after) continue
            if (spec.liveChangeable) live.add(spec.key) else restart.add(spec.key)
        }
        return ApplyPlan(live, restart)
    }
}

/**
 * Settings migration.
 *
 * Taken from melonDS-android, which has the only real framework in the fleet:
 * 37 files, 16 concrete migrations. ARMSX2 does it with ad-hoc one-time keys
 * instead, and needed seven of them.
 *
 * Two rules that are not obvious:
 *
 * 1. **The schema version is the app's own version code**, not a separate
 *    constant. A separate constant is a number somebody has to remember to
 *    bump; a version code is already bumped for other reasons. [firstVersion]
 *    is the floor for installs that predate migrations.
 * 2. **A migration must never deserialize with the CURRENT data class.** Freeze
 *    a DTO per version and read that. Otherwise the migration silently breaks
 *    when the current class changes, and only for users upgrading from an old
 *    version, which is the hardest case to test.
 *
 * **And a third rule, which is why rule 2 costs nothing here.**
 *
 * **Store settings as a flat map of stable key to string, never as a serialised
 * object graph.** melonDS needs frozen DTOs — `Rom21`, `RomConfigDto25`,
 * `RomDto31` — because it deserialises into typed objects, so the shape of the
 * current class is part of the read path. **A string map has no shape to
 * break.** A migration renames a key or rewrites a value, and a key it does not
 * know about passes through untouched.
 *
 * That is the reason [SettingsMigration.migrate] takes a `MutableMap` rather
 * than a settings object, and it is a deliberate divergence from the fork the
 * rest of this design was taken from. **The typed view is built from the map at
 * read time, after migration, so a stale type can never be applied to old
 * data.**
 */
interface SettingsMigration {
    val from: Int
    val to: Int

    /** State the reason in a comment. The reader is a future upgrade bug. */
    fun migrate(store: MutableMap<String, String>)
}

object SettingsMigrator {
    /** Version at which migrations began. Installs older than this start here. */
    const val firstVersion: Int = 1

    /**
     * Run every migration in (lastVersion, currentVersion]. Refuses duplicate
     * [SettingsMigration.from] values, because two migrations from one version
     * have no defined order.
     */
    fun migrate(
        migrations: List<SettingsMigration>,
        store: MutableMap<String, String>,
        lastVersion: Int,
        currentVersion: Int,
    ): Int {
        require(migrations.map { it.from }.toSet().size == migrations.size) {
            "two migrations share a `from` version; their order would be undefined"
        }
        if (lastVersion >= currentVersion) return lastVersion
        migrations.sortedBy { it.from }
            .filter { it.from >= lastVersion && it.to <= currentVersion }
            .forEach { it.migrate(store) }
        return currentVersion
    }
}

// ----------------------------------------------------------------- storage

/**
 * A storage category the backend owns.
 *
 * [rebuildable] decides whether the storage screen may offer to clear it. A
 * cache is an asset, not junk: clearing it frees space and brings the stutter
 * back. Saves and states are never rebuildable and never sit near a bulk
 * action.
 */
data class StorageCategory(
    val label: String,
    val path: String,
    val rebuildable: Boolean,
)

// ---------------------------------------------------------------- counters

/**
 * What the backend can report for the diagnostics readout.
 *
 * Deliberately a declaration rather than a fixed list. A backend that cannot
 * report GPU busy time should not be made to lie about it.
 */
enum class Counter { FPS, FRAME_TIME_US, GPU_BUSY_PCT, DRAW_CALLS, GUEST_CPU_PCT }

// --------------------------------------------------------- cheats, patches

data class CheatSpec(
    val formats: List<String>,
    val liveToggle: Boolean,
)

/**
 * Why a patch exists.
 *
 * This was justified as a UI grouping, so that somebody choosing a cheat and
 * somebody chasing frames see different lists. ARMSX2's shipped bugs show it
 * decides three things, and the grouping is the least of them:
 *
 *   1. whether integrity mode blocks it   -- IntegrityPolicy.allowsPatch
 *   2. whether it binds to GameKey or DumpId -- IntegrityPolicy.bindsTo
 *   3. whether it may auto-apply at boot  -- IntegrityPolicy.mayAutoApply
 *
 * CHEAT was added 2026-08-25. Without it the gate cannot tell a cheat from a
 * widescreen fix, which is precisely ARMSX2's bug: hardcore mode dropped every
 * on-disk patch and silently killed everything its Patch Manager wrote.
 */
enum class PatchIntent {
    /** Faster. Not blocked by integrity mode; binds to the dump. */
    SPEED,

    /** A bug fix, widescreen, de-interlace. Not blocked; binds to the dump. */
    FIX,

    /** Gameplay change, restored content, translation. Blocked; binds to the title. */
    CHANGE,

    /** A cheat. Blocked; binds to the title, so a wrong-CRC name still matches. */
    CHEAT,
}

data class PatchSpec(
    val format: String,
    val applyAtLoadOnly: Boolean,
)

// --------------------------------------------------------------- extensions

/**
 * Capability a backend has that others do not.
 *
 * The app renders the matching UI only when the extension is declared. Cemu
 * declares graphic packs. melonDS declares three filter planes. ARMSX2
 * declares two texture classes. None pretends to be the others.
 */
sealed interface Extension {
    /** Texture classes this backend can distinguish at upload. */
    data class TextureClasses(val classes: List<String>) : Extension

    /** Named pack format the backend loads. */
    data class TexturePacks(val format: String) : Extension

    /** A combined pack format, such as Cemu graphic packs. */
    data class GraphicPacks(val format: String) : Extension

    /** Settings that are worth surfacing in the in-game overlay. */
    data class HotSettings(val keys: List<String>) : Extension
}

// ------------------------------------------------------------ the contract

/**
 * What every backend implements. Nothing more is required.
 *
 * Kept free of Android types on purpose, so the contract can be tested off
 * the device.
 */
interface Backend {

    val info: BackendInfo

    /** Identify a title from a file, or null if this backend does not know it. */
    fun identify(path: String): TitleId?

    /** Which lifecycle operations this backend supports. */
    fun supportedOps(): Set<LifecycleOp>

    /** The guest screens for a loaded title. */
    fun guestScreens(title: TitleId): List<GuestScreenSpec>

    /** Every setting this backend exposes, with stable keys. */
    fun settings(): List<SettingSpec>

    /** Default values, by key. */
    fun defaults(): Map<String, String>

    /** Storage categories this backend owns for a title. */
    fun storage(title: TitleId): List<StorageCategory>

    /** Counters this backend can report. */
    fun counters(): Set<Counter>

    fun cheats(): CheatSpec?

    fun patches(): PatchSpec?

    /** Extensions this backend declares. Empty is valid. */
    fun extensions(): List<Extension> = emptyList()
}

/**
 * Open questions this contract does not answer yet. Recorded here so they are
 * not lost between SCREENS.md and the code.
 *
 * 1. Cover art. Does a backend supply it, does the app scrape it, or is it a
 *    bundled database? No fork was surveyed for this.
 * 2. Game identification. Every fork does it differently and none was
 *    surveyed. [identify] assumes a path is enough, which may be wrong for
 *    multi-file titles.
 * 3. Rewind. Listed as a [LifecycleOp] but its cost is unknown, and only some
 *    backends can do it.
 * 4. The counters list is a guess. It should follow what the forks already
 *    report, which is unsurveyed except in xenia.
 */
private object ContractOpenQuestions
