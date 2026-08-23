package com.aynthor.shell

/**
 * When the touch overlay is visible.
 *
 * **This is a harvested lesson, not a design.** Vita3K is the only fork in the
 * fleet that solved it — `attachController`, `setAllowVirtualController`,
 * `updateVirtualControllerState`. azahar built the richer overlay and never
 * handled the case.
 *
 * **The Thor has physical controls.** An overlay drawn permanently over a game
 * on a device with real buttons is wrong, and it is wrong in a way nobody
 * notices while developing on a device without them.
 *
 * See shared_layer/UNIFICATION.md: the unit of waste is a lesson, and a test is
 * the cheapest durable way to stop one being re-learned.
 */
enum class OverlayVisibility { SHOWN, HIDDEN }

data class OverlayInputs(
    /** A physical gamepad is connected. */
    val physicalControllerAttached: Boolean,
    /** The person explicitly asked for the overlay, overriding everything. */
    val userForcedOn: Boolean,
    /** The person explicitly turned it off. */
    val userForcedOff: Boolean,
    /** This backend declares at least one overlay element. */
    val backendDeclaresElements: Boolean,
    /** The overlay is being edited, so it must stay on screen. */
    val editing: Boolean,
)

object OverlayPolicy {

    /**
     * Resolve overlay visibility.
     *
     * Order matters and each rule is here because a fork learned it:
     *
     * 1. **Editing wins over everything.** The overlay cannot be repositioned
     *    while hidden. Both azahar and Vita3K keep an explicit edit mode, and
     *    it survived twelve years of independent divergence in both.
     * 2. **An explicit choice beats inference.** If the person turned it on or
     *    off, that is the answer. Never override a stated preference with a
     *    guess about hardware.
     * 3. **A physical controller hides it.** Vita3K's lesson, and the reason
     *    this file exists.
     * 4. **A backend with no elements has no overlay**, however the rest
     *    resolves. Cemu declares 21 elements, xenia declares none.
     */
    fun resolve(inputs: OverlayInputs): OverlayVisibility {
        if (inputs.editing) return OverlayVisibility.SHOWN
        if (!inputs.backendDeclaresElements) return OverlayVisibility.HIDDEN
        if (inputs.userForcedOff) return OverlayVisibility.HIDDEN
        if (inputs.userForcedOn) return OverlayVisibility.SHOWN
        if (inputs.physicalControllerAttached) return OverlayVisibility.HIDDEN
        return OverlayVisibility.SHOWN
    }
}
