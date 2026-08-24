package com.aynthor.shell

/**
 * One mode that governs every feature which would make a run not count.
 *
 * WHY THIS IS ONE TYPE AND NOT FIVE CHECKS.
 *
 * melonDS disables rewind under RetroAchievements hardcore mode. ARMSX2 states
 * the full set in its own UI text: hardcore "prevents the usage of save states,
 * cheats and slowdown functionality". Between them that is FIVE features this
 * project specified separately -- save states, cheats, rewind, a time scale
 * below 100%, and patches whose intent is a cheat -- and one mode reaching all
 * of them.
 *
 * ARMSX2 shipped two bugs getting this right, and both are pinned by tests.
 *
 *   1. THE GATE WAS TOO WIDE AND ASYMMETRIC. Hardcore dropped every on-disk
 *      pnach, while the bundled patches.zip stayed enabled -- so a widescreen
 *      or bug-fix patch worked when it shipped in the zip and silently did
 *      nothing when the same patch sat on disk. The in-app Patch Manager only
 *      ever writes to disk, so turning hardcore on killed everything it
 *      produced, "with no message explaining why".
 *
 *   2. CHEATS AND FIXES NEED DIFFERENT IDENTITY SCOPING. A cheat named for the
 *      wrong CRC must still be found; a fix named for the wrong revision must
 *      NOT auto-apply. See PatchIntent below.
 *
 * See research_log/20260825_0410_rewind_and_the_integrity_mode_nobody_designed.md.
 */

// PatchIntent lives in Backend.kt, beside the rest of the contract. CHEAT was
// added there for this file: without it the gate cannot tell a cheat from a
// widescreen fix, which is ARMSX2's bug one.

/** A capability the app may want to use while a game runs. */
enum class GuardedFeature {
    SAVE_STATE,
    LOAD_STATE,
    REWIND,
    CHEATS,
    /** Any guest time scale below 100%. Fast forward is a separate question. */
    SLOW_MOTION,
    /** Fast forward. Listed separately because no fork was found blocking it. */
    FAST_FORWARD,
    PATCHES_THAT_CHANGE_PLAY,
}

/**
 * Whether this session's results are claimed to count for anything.
 *
 * Named for what it protects rather than for RetroAchievements, because the
 * same gate serves any such claim -- a leaderboard, a speedrun timer, or a
 * measurement run that must not be contaminated.
 */
enum class IntegrityMode {
    /** Nothing is claimed. Everything is available. The default. */
    OFF,

    /** Results are claimed. The guarded features are refused. */
    ENFORCED,
}

object IntegrityPolicy {

    /**
     * Turning the mode on must not silently disable things.
     *
     * ARMSX2's bug one was exactly this: the Patch Manager's output stopped
     * working with no message. The app must be able to LIST what a mode change
     * will cost before the user confirms it.
     */
    const val MUST_LIST_LOSSES_BEFORE_ENABLING = true

    /** Features refused while results are claimed. */
    private val GUARDED = setOf(
        GuardedFeature.SAVE_STATE,
        GuardedFeature.LOAD_STATE,
        GuardedFeature.REWIND,
        GuardedFeature.CHEATS,
        GuardedFeature.SLOW_MOTION,
        GuardedFeature.PATCHES_THAT_CHANGE_PLAY,
    )

    fun allows(mode: IntegrityMode, feature: GuardedFeature): Boolean =
        mode == IntegrityMode.OFF || feature !in GUARDED

    /**
     * Is this patch blocked?
     *
     * NOT "is this a patch" -- that was ARMSX2's bug. A fix, a widescreen hack
     * and a performance patch all survive integrity mode; only a patch that
     * changes play does not.
     */
    fun allowsPatch(mode: IntegrityMode, intent: PatchIntent): Boolean =
        when (mode) {
            IntegrityMode.OFF -> true
            IntegrityMode.ENFORCED -> intent == PatchIntent.FIX || intent == PatchIntent.SPEED
        }

    /**
     * Which identity a patch binds to.
     *
     * ARMSX2's reasoning, kept because it is the justification GAME_DATA.md did
     * not have: a cheat written before the CRC was known, or imported under
     * another revision's name, must still be found -- a wrong-revision cheat is
     * harmless. A wrong-revision graphics patch is not, so a fix stays bound to
     * the exact dump and cannot auto-apply across revisions.
     */
    fun bindsTo(intent: PatchIntent): PatchBinding =
        when (intent) {
            PatchIntent.CHEAT, PatchIntent.CHANGE -> PatchBinding.GAME_KEY
            PatchIntent.FIX, PatchIntent.SPEED -> PatchBinding.DUMP_ID
        }

    /** May this patch apply automatically at boot, with nobody asking? */
    fun mayAutoApply(intent: PatchIntent): Boolean =
        bindsTo(intent) == PatchBinding.DUMP_ID

    /**
     * Everything a mode change would take away, so the user is told first.
     *
     * Returns empty when nothing is lost.
     */
    fun lossesFromEnabling(): Set<GuardedFeature> = GUARDED
}

/** What a patch is matched against. */
enum class PatchBinding {
    /** The title. Survives a different revision or an unknown CRC. */
    GAME_KEY,

    /** This exact copy. A wrong-revision match would be harmful. */
    DUMP_ID,
}
