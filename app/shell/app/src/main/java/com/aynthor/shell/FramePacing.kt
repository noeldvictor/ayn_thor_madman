package com.aynthor.shell

/**
 * Frame pacing, and the presenter's contract with two displays.
 *
 * **This file exists because the fleet already has an incumbent and this repo
 * did not know it.** `CLAUDE.md` recorded frame pacing as the one host subsystem
 * with no implementation anywhere. **Cemu has four parts of one, in production,
 * on Android** — and the survey missed it because it searched for two library
 * names while Cemu spells its mechanism in Vulkan core terms and keeps its most
 * important part in guest-timing code.
 *
 * See `research_log/20260824_0010_frame_pacing_has_an_incumbent.md`.
 *
 * **No fork uses Swappy or `VK_GOOGLE_display_timing`**, re-checked twice.
 * **The Thor's Turnip does expose `VK_GOOGLE_display_timing`.**
 */

/**
 * How presentation is paced.
 *
 * **FIFO is vsync, not pacing.** A 20 ms frame at 60 Hz misses its vsync and
 * alternates 16.6, 33.3, 16.6 — and that judder is more visible than a stable
 * 30.
 */
enum class PresentMode {
    /** Vsync. The only mode where frames can queue, so the only one that needs a depth limit. */
    FIFO,
    /** Latest frame wins. No queue to bound. */
    MAILBOX,
    /** No sync. Tearing, lowest latency. */
    IMMEDIATE,
    /**
     * Vsync with the guest's own timing driven from the host display.
     *
     * **Cemu's `SYNC_AND_LIMIT`**, which starts a thread that notifies the guest
     * GPU on the host's real vsync. **This is what frame pacing means for an
     * emulator**, and it lives in guest-timing code rather than in a renderer,
     * which is why a present-code survey could not see it.
     */
    HOST_DRIVEN_VSYNC,
}

/**
 * How many presents may be in flight.
 *
 * **This is latency control, not vsync.** Cemu tags each present with a
 * `presentId` and, when the queue is full, waits for the present `queueDepth`
 * frames back to have actually reached the display — `vkWaitForPresentKHR` with
 * a 40 ms timeout.
 */
data class PresentQueue(
    val mode: PresentMode,
    val inFlight: Int = 0,
) {
    /**
     * The bound, and it is mode-dependent.
     *
     * **Cemu sets `maxQueued = 1` on `FIFO` alone and 0 everywhere else**,
     * because FIFO is the only mode where frames queue. One frame in flight is
     * the minimum-latency choice on a device with real buttons.
     */
    val maxInFlight: Int get() = if (mode == PresentMode.FIFO) 1 else 0

    /** Whether the caller must wait for an older present before submitting. */
    fun mustWait(): Boolean = maxInFlight > 0 && inFlight >= maxInFlight
}

/** Which physical display a guest screen is routed to. */
enum class HostDisplay { BUILT_IN, SCREEN_2 }

/**
 * One guest screen's present state.
 *
 * **Two screens need two swapchains and a marker each.** Cemu's comment, on
 * Android: *"Keep TV and GamePad swapchains from forcing each other to idle. A
 * single shared previous-frame marker serializes dual-screen presents."*
 */
data class ScreenPresent(
    val display: HostDisplay,
    val previousFrameMarker: Long?,
)

object FramePacingPolicy {

    /**
     * Whether a present must wait on the *other* screen's work.
     *
     * **It must not.** One shared marker across both screens serialises them, so
     * the slower screen paces the faster one. **Each screen keeps its own
     * marker** — which is the bug Cemu's comment records fixing.
     */
    fun waitsOn(screen: ScreenPresent, screens: List<ScreenPresent>): ScreenPresent? =
        screens.firstOrNull { it.display == screen.display }

    /**
     * Whether Screen-2 should be redrawn when nothing on it changed.
     *
     * **No.** A second panel drawn every frame costs power and thermal headroom
     * for no benefit, and `CLAUDE.md` already makes "draws on change, not per
     * frame" a rule. **Restated here because the presenter is where it is
     * enforced.**
     */
    fun shouldRedraw(display: HostDisplay, contentChanged: Boolean): Boolean =
        display == HostDisplay.BUILT_IN || contentChanged

    /**
     * The refresh rate a guest locked to 60 Hz should be paced against.
     *
     * **Pick a stable divisor of the panel rate and hold it.** Both Thor panels
     * do 120 Hz and the device is currently capped to 60 by a user setting, so a
     * pacing decision must read the *current* cap rather than the panel's
     * capability.
     */
    fun divisorFor(panelHz: Int, guestHz: Int): Int {
        require(panelHz > 0 && guestHz > 0) { "a refresh rate is positive" }
        val d = panelHz / guestHz
        return if (d < 1) 1 else d
    }

    /**
     * Whether a generated frame may be presented in this state.
     *
     * **Delegates to the guest's declared activity**, because extrapolating
     * between two frames of a fixed-rate video invents motion that is not there.
     */
    fun mayPresentGeneratedFrame(activity: ActivityReport): Boolean =
        ActivityPolicy.mayGenerateFrames(activity)

    /**
     * How long to wait for a present that should already have happened.
     *
     * **Bounded, because a lost present must not deadlock the frame loop.**
     * Cemu uses 40 ms, which is about two and a half vsyncs at 60 Hz.
     */
    /**
     * ACQUIRE BLOCKS. IT DOES NOT POLL.
     *
     * rpcsx's present path sets its swapchain-acquire timeout to zero and
     * continues, on a comment naming "AMD Crimson 17.7.2" -- a 2017 desktop
     * driver -- applied unconditionally. On Android the acquire goes through a
     * BufferQueue, so a zero timeout is a busy-wait against the compositor:
     * 37,000 iterations per second, burning a core in the flip path, and a log
     * storm that evicted the fault it was meant to help diagnose.
     *
     * A driver workaround with no driver condition is a timing constant tuned
     * for hardware you are not running on. Four forks that will be in the packed
     * binary pass UINT64_MAX and block correctly; this rule exists so the shared
     * present path does not acquire the habit from anywhere else.
     */
    const val ACQUIRE_MUST_BLOCK = true

    const val PRESENT_WAIT_TIMEOUT_NS: Long = 40_000_000
}
