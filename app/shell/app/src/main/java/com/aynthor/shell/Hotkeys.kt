package com.aynthor.shell

/**
 * The hotkey layer. Owned by the app, never by a backend.
 *
 * **This is a stated requirement, not a convenience.** One hotkey set works on
 * every system, always. Save state, load state, fast forward, screenshot,
 * overlay and menu use the same input on every backend — **the named RetroArch
 * failure this project exists to fix is that each core does its own thing.**
 *
 * **A backend never defines a hotkey.** The app owns the layer and tells the
 * backend what happened.
 *
 * **The action set and the interaction come from ARMSX2**, which split hotkeys
 * out of its Pad tab so they are easy to find — this project's own usability
 * complaint, fixed independently inside the fleet before this repo existed.
 * Four learned lessons travel with it; see the tests.
 *
 * **And this matters more here than a touch overlay does.** The Thor has real
 * buttons, so the input feature worth owning is the one driven by them.
 */

/**
 * How a binding behaves.
 *
 * **Not every hotkey is a press.** ARMSX2 ships fast-forward twice, once as a
 * hold and once as a toggle, and its pressure modifier is a hold that is
 * explicitly *not* handled as a one-shot. **A single "on press" model cannot
 * express either.**
 */
enum class HotkeyKind {
    /** Fires once on press. */
    ONE_SHOT,

    /** Active only while held. Fast forward, the pressure modifier. */
    HOLD,

    /** Flips on each press. */
    TOGGLE,
}

/**
 * The shared action set.
 *
 * **Ordering is not persisted** — bindings key on the enum name, so entries may
 * be reordered or removed without corrupting a saved binding. That is the
 * opposite of the upscale-algorithm enum, which is persisted as an integer and
 * is therefore append-only forever.
 */
enum class HotkeyAction(val kind: HotkeyKind, val label: String) {
    MENU(HotkeyKind.ONE_SHOT, "Menu / Pause"),
    SAVE_STATE(HotkeyKind.ONE_SHOT, "Quick save state"),
    LOAD_STATE(HotkeyKind.ONE_SHOT, "Quick load state"),
    CYCLE_SLOT(HotkeyKind.ONE_SHOT, "Cycle save slot"),

    /**
     * Bindable on purpose.
     *
     * ARMSX2's note: a bindable screenshot can live on a spare button — L3 is
     * the usual pick — **instead of the Android system gesture, which
     * interrupts play.**
     */
    SCREENSHOT(HotkeyKind.ONE_SHOT, "Screenshot"),

    TOGGLE_OVERLAY(HotkeyKind.ONE_SHOT, "Cycle performance overlay"),
    FAST_FORWARD_HOLD(HotkeyKind.HOLD, "Fast forward (hold)"),
    FAST_FORWARD_TOGGLE(HotkeyKind.TOGGLE, "Fast forward (toggle)"),
    SLOW_MOTION(HotkeyKind.TOGGLE, "Slow motion"),
    REWIND(HotkeyKind.HOLD, "Rewind"),
    RES_UP(HotkeyKind.ONE_SHOT, "Increase resolution"),
    RES_DOWN(HotkeyKind.ONE_SHOT, "Decrease resolution"),
    SWAP_SCREENS(HotkeyKind.ONE_SHOT, "Swap screens"),
    CLOSE_GAME(HotkeyKind.ONE_SHOT, "Close game"),
    SAVE_AND_EXIT(HotkeyKind.ONE_SHOT, "Save state and exit"),
    RESET_GAME(HotkeyKind.ONE_SHOT, "Reset game"),
}

/** What the app knows when a key arrives. */
data class HotkeyContext(
    val gameRunning: Boolean = true,
    /**
     * Achievements hardcore mode.
     *
     * **ARMSX2 disables slow motion under it**, because slowdown is a banned
     * advantage in hardcore, and shows a notice rather than silently ignoring
     * the press.
     */
    val hardcoreAchievements: Boolean = false,
    /** Whether the running title has more than one guest screen. */
    val dualScreen: Boolean = true,
    /** True while the binding screen is waiting for a button. */
    val capturingBinding: Boolean = false,
)

/**
 * Why an action did not run.
 *
 * **Every reason is separate, and none of them is silence.** A hotkey that does
 * nothing and says nothing is indistinguishable from a broken binding — which
 * is exactly the bug ARMSX2's PINE setting had in a different subsystem.
 */
enum class HotkeyDenial {
    ALLOWED,
    NOT_BOUND,
    NO_GAME_RUNNING,
    /** Banned advantage under hardcore. Must be reported, not ignored. */
    BLOCKED_BY_HARDCORE,
    /** The title has one guest screen, so there is nothing to swap. */
    NOT_APPLICABLE_TO_TITLE,
    /** The binding screen owns the next press. */
    CAPTURING_BINDING,
}

object HotkeyPolicy {

    /** Actions that make no sense with no game running. */
    private val needsGame = setOf(
        HotkeyAction.SAVE_STATE, HotkeyAction.LOAD_STATE, HotkeyAction.CYCLE_SLOT,
        HotkeyAction.SCREENSHOT, HotkeyAction.FAST_FORWARD_HOLD,
        HotkeyAction.FAST_FORWARD_TOGGLE, HotkeyAction.SLOW_MOTION,
        HotkeyAction.REWIND, HotkeyAction.RES_UP, HotkeyAction.RES_DOWN,
        HotkeyAction.SWAP_SCREENS, HotkeyAction.CLOSE_GAME,
        HotkeyAction.SAVE_AND_EXIT, HotkeyAction.RESET_GAME,
    )

    /** Actions a hardcore achievements session forbids. */
    private val bannedInHardcore = setOf(HotkeyAction.SLOW_MOTION, HotkeyAction.REWIND)

    /**
     * Which action a physical key should run, and whether it may.
     *
     * **Capture wins over everything.** While the binding screen is waiting, the
     * next press is a binding, not an action — otherwise binding "menu" would
     * open the menu.
     */
    fun resolve(
        keyCode: Int,
        bindings: Map<HotkeyAction, Int>,
        context: HotkeyContext,
    ): Pair<HotkeyAction?, HotkeyDenial> {
        if (context.capturingBinding) return null to HotkeyDenial.CAPTURING_BINDING
        val action = bindings.entries.firstOrNull { it.value == keyCode }?.key
            ?: return null to HotkeyDenial.NOT_BOUND
        return action to availability(action, context)
    }

    /** Whether an action may run right now. */
    fun availability(action: HotkeyAction, context: HotkeyContext): HotkeyDenial = when {
        !context.gameRunning && action in needsGame -> HotkeyDenial.NO_GAME_RUNNING
        context.hardcoreAchievements && action in bannedInHardcore -> HotkeyDenial.BLOCKED_BY_HARDCORE
        action == HotkeyAction.SWAP_SCREENS && !context.dualScreen ->
            HotkeyDenial.NOT_APPLICABLE_TO_TITLE
        else -> HotkeyDenial.ALLOWED
    }

    /**
     * Bindings that share a key.
     *
     * **A conflict is reported, not resolved.** Two actions on one button is a
     * choice a person may want — a hold and a one-shot can coexist — so the UI
     * shows it and the person decides.
     */
    fun conflicts(bindings: Map<HotkeyAction, Int>): Map<Int, List<HotkeyAction>> =
        bindings.entries
            .groupBy({ it.value }, { it.key })
            .filterValues { it.size > 1 }

    /**
     * Whether binding capture may use a modal dialog.
     *
     * **It may not, and this is a shipped bug.** ARMSX2 records that a
     * focus-stealing `AlertDialog` has its own window, which **swallowed
     * controller keys before the Compose handler could see them** — the 2.6.0
     * "can't remap buttons" bug. **Capture must be handled in the screen that is
     * already focused.**
     */
    const val CAPTURE_MAY_USE_MODAL_DIALOG = false
}
