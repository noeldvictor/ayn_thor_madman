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
 * One setting.
 *
 * [key] must be stable forever. A setting with no stable key cannot carry a
 * per-game override, and a per-game override for every option is a
 * requirement.
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
)

/**
 * Where a resolved setting value came from.
 *
 * The order is fixed and lives in one place. A backend never invents its own.
 */
enum class SettingSource { PER_GAME, THOR_PROFILE, BACKEND_DEFAULT }

data class ResolvedSetting(val value: String, val source: SettingSource)

/**
 * The one resolver. Per-game value, then Thor profile, then backend default.
 *
 * Kept here rather than in a screen so that no caller can invent an order.
 */
object SettingResolver {
    fun resolve(
        key: String,
        perGame: Map<String, String>,
        thorProfile: Map<String, String>,
        backendDefault: Map<String, String>,
    ): ResolvedSetting? {
        perGame[key]?.let { return ResolvedSetting(it, SettingSource.PER_GAME) }
        thorProfile[key]?.let { return ResolvedSetting(it, SettingSource.THOR_PROFILE) }
        backendDefault[key]?.let { return ResolvedSetting(it, SettingSource.BACKEND_DEFAULT) }
        return null
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

/** Why a patch exists. The patches screen groups by this. */
enum class PatchIntent { SPEED, FIX, CHANGE }

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
