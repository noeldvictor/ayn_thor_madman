package com.aynthor.shell

/**
 * What the guest says it is doing.
 *
 * **The hardest part of the paused agent loop is knowing when NOT to look at the
 * frame.** A vision model asked "what button?" during a cutscene will guess.
 *
 * **`AGENT_LOOP.md` originally proposed inferring this from the guest's video
 * decoder. That is the fallback, not the signal.** Every console has an API
 * meaning *"do not sleep, the user is watching something they did not
 * trigger"*, and games call it — because that is exactly when a person watches
 * without touching the controls.
 *
 * | Console | The call | Fork state |
 * | --- | --- | --- |
 * | Switch | `SetMediaPlaybackState`, `ReportUserIsActive`, four more | eden discards five of six |
 * | Vita | `sceKernelPowerTick(DISABLE_OLED_OFF)` | Vita3K drops the type |
 * | 3DS | `ReplySleepQuery` — a two-way protocol | azahar stubs it |
 * | Wii U | `OSEnableHomeButtonMenu` | Cemu keeps it, for its own HOME menu |
 *
 * **Three reasons the declaration beats the decoder**: it is a statement rather
 * than an inference; a running decoder may be video on a screen inside the
 * world; and **most in-engine cutscenes touch no decoder at all** yet still
 * disable sleep.
 *
 * See `research_log/20260824_0140_the_guest_declares_its_activity.md`.
 */

/** How a backend learned the state. Kept, because it decides how much to trust it. */
enum class ActivitySource {
    /** The guest called a system API saying so. The strongest form. */
    GUEST_DECLARED,

    /** Inferred from the guest's video decoder running. Weaker; misses in-engine scenes. */
    DECODER_INFERRED,

    /** The backend does not report activity. Assume interactive, and say so. */
    UNKNOWN,
}

/** What the guest is doing right now. */
enum class GuestActivity {
    /** Normal play. Safe to capture a frame and act on it. */
    INTERACTIVE,

    /** The user is watching. **Do not act on the frame.** */
    NON_INTERACTIVE,

    /** Loading or a transition. Acting is pointless, not harmful. */
    BUSY,

    /** Nothing is known. Treated as INTERACTIVE, with the source recorded. */
    UNKNOWN,
}

/**
 * A backend's report.
 *
 * **The backend declares this; the app never derives it.** How a backend knows
 * is guest knowledge, exactly like the texture key and the filter list.
 */
data class ActivityReport(
    val activity: GuestActivity,
    val source: ActivitySource,
    /**
     * The guest's own statement that an input registered.
     *
     * Switch has `ReportUserIsActive`. **For an agent that injects input, this is
     * the guest confirming the injection landed** — which nothing else can tell
     * it. Null where the backend has no such signal.
     */
    val userActivityAcknowledged: Boolean? = null,
)

/** Why the loop refused to act on a frame. Never silence. */
enum class ActivityDenial {
    ALLOWED,
    /** The guest declared it is playing media or has disabled sleep. */
    GUEST_IS_NON_INTERACTIVE,
    /** Loading or transitioning. */
    GUEST_IS_BUSY,
    /** The backend reports nothing and the caller demanded a declared state. */
    NO_DECLARED_STATE,
}

object ActivityPolicy {

    /**
     * Whether an agent may capture a frame and choose a button.
     *
     * **`requireDeclared` exists because an inferred state is not good enough for
     * every caller.** A measurement harness refusing to run in a noisy state
     * should demand a declaration; a convenience feature may accept an
     * inference.
     */
    fun mayAct(report: ActivityReport, requireDeclared: Boolean = false): ActivityDenial = when {
        report.activity == GuestActivity.NON_INTERACTIVE -> ActivityDenial.GUEST_IS_NON_INTERACTIVE
        report.activity == GuestActivity.BUSY -> ActivityDenial.GUEST_IS_BUSY
        requireDeclared && report.source != ActivitySource.GUEST_DECLARED ->
            ActivityDenial.NO_DECLARED_STATE
        else -> ActivityDenial.ALLOWED
    }

    /**
     * Whether a measurement may be trusted in this state.
     *
     * **Measured noise floors: a gated title screen is +/-0.2%, a restored
     * savestate +/-5%, and pressing through cutscenes about +/-50% and
     * unusable.** A backend that can say it is in a cutscene can refuse to be
     * measured there.
     */
    fun mayMeasure(report: ActivityReport): Boolean =
        report.activity == GuestActivity.INTERACTIVE

    /**
     * Whether frame generation should run.
     *
     * **A fixed-rate video must not be extrapolated**: synthesising motion
     * between two frames of a 30 fps movie invents motion that is not there.
     * The same applies to upscaling decoded video and to drawing the overlay
     * over a cutscene.
     */
    fun mayGenerateFrames(report: ActivityReport): Boolean =
        report.activity != GuestActivity.NON_INTERACTIVE

    /**
     * The default when a backend declares nothing.
     *
     * **Assume the guest is interactive.** The failure of assuming
     * NON_INTERACTIVE is that the agent never acts at all; the failure of
     * assuming INTERACTIVE is one wasted button press. **Prefer the recoverable
     * failure.**
     */
    val WHEN_UNKNOWN = GuestActivity.INTERACTIVE

    /**
     * Whether this signal has been calibrated for a title.
     *
     * **A game may never call these APIs, or call them wrongly.** The stub
     * fraction in `hle_coverage.json` needs the same treatment: a marker is a
     * starting weight, not a score, until it is checked against titles whose
     * behaviour is known.
     */
    const val REQUIRES_CALIBRATION_PER_TITLE = true
}
