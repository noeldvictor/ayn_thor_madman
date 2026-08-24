package com.aynthor.shell

/**
 * The guest time scale, and which clocks a backend promises to move with it.
 *
 * WHY THIS IS NOT A `fastForward: Boolean`.
 *
 * Vita3K shipped the fast-forward toggle twice and it did nothing both times.
 * The first pass scaled `sceKernelDelayThread`, kernel wait timeouts and timer
 * scheduling; the user reported gameplay still ran at real time. The second
 * pass had to add an ANCHORED speeded process clock and route
 * `sceKernelGetProcessTime`, `GetSystemTimeWide`, `LibcClock`, `LibcTime`,
 * `LibcGettimeofday`, the RTC APIs, thread start ticks and NetCtl peer timing
 * through it.
 *
 *   The switch moved and nothing happened. That is the settings symptom with a
 *   fifth cause: the feature is not a value, it is a cross-cutting property of
 *   every clock in the backend.
 *
 * And the cost is not uniform. A low-level backend derives guest time from
 * emulated cycles, so running the loop faster speeds the guest up for free --
 * melonDS's whole implementation is a bool the frame limiter reads, three
 * references. A high-level backend that implements the guest's `gettimeofday`
 * by asking the HOST what time it is gets real time no matter how fast the loop
 * runs. Most of this fleet is the second kind.
 *
 * See research_log/20260825_0245_fast_forward_is_a_time_scale_not_a_toggle.md.
 */

/**
 * A clock a backend may own. A backend DECLARES the ones it scales.
 *
 * The declaration is the point. An undeclared domain is how fast-forward fails
 * silently, and a declaration turns that into something the app can see at
 * integration instead of in a user report.
 */
enum class ClockDomain {
    /** Guest CPU progress. Free in a cycle-driven backend. */
    GUEST_CPU,

    /** Vblank and frame pacing. */
    DISPLAY_PACING,

    /** Audio retiming. Vita3K, ARMSX2 and azahar each solve this differently. */
    AUDIO,

    /**
     * A guest time API answered from the HOST clock.
     *
     * THE ONE THAT BITES. A backend that HLEs the guest kernel's time APIs must
     * declare this, or the guest keeps reading real time and the scale is
     * invisible to it -- which is exactly the bug Vita3K shipped.
     */
    HOST_DERIVED_TIME,

    /** Guest timers and delays: sleep, wait timeouts, timer events. */
    GUEST_TIMERS,
}

/**
 * Percent of normal guest speed.
 *
 * One value covers pause, slow motion, normal and fast-forward. Pause is not a
 * special case bolted on: the paused agent loop freezes the guest so a vision
 * model's latency costs the guest nothing, and that is this type at zero.
 */
@JvmInline
value class TimeScale(val percent: Int) {

    val isPaused: Boolean get() = percent == 0
    val isNormal: Boolean get() = percent == NORMAL
    val isFastForward: Boolean get() = percent > NORMAL
    val isSlowMotion: Boolean get() = percent in 1 until NORMAL

    companion object {
        const val NORMAL = 100
        const val MAX_PERCENT = 1600

        val PAUSED = TimeScale(0)
        val REAL_TIME = TimeScale(NORMAL)

        /** Reject a nonsensical scale at the edge rather than deep in a backend. */
        fun of(percent: Int): TimeScale {
            require(percent in 0..MAX_PERCENT) { "time scale out of range: $percent" }
            return TimeScale(percent)
        }
    }
}

/** What a backend promises about the time scale. */
data class TimeScaleSupport(
    /** Every clock this backend moves with the scale. */
    val scaled: Set<ClockDomain>,
    /**
     * True when the backend keeps guest time CONTINUOUS across a scale change.
     *
     * Vita3K's word is "anchored". A backend that simply multiplies makes guest
     * time jump at the toggle, which desynchronises anything holding a
     * timestamp across it.
     */
    val anchored: Boolean,
    /** The largest scale this backend will honour. */
    val maxPercent: Int = TimeScale.MAX_PERCENT,
)

/** Why a requested scale was refused, or what the caller must be told. */
enum class ScaleDenial {
    /** Above what the backend declared it will honour. */
    ABOVE_BACKEND_MAXIMUM,

    /**
     * The backend HLEs guest time from the host clock and did not declare
     * HOST_DERIVED_TIME. Honouring the request would move the frame limiter and
     * leave the guest reading real time -- the visible bug.
     */
    HOST_TIME_NOT_SCALED,

    /** The backend declared no clock at all. */
    NOTHING_DECLARED,

    /**
     * Not a refusal of the scale -- a refusal to keep a timestamp across it.
     * An unanchored backend makes guest time discontinuous at the change.
     */
    NOT_ANCHORED,
}

object TimeScalePolicy {

    /**
     * A run at any scale but 100 is not a measurement.
     *
     * MEASUREMENT.md did not say this and it should: every noise floor, frame
     * time and energy-per-frame figure in this project assumes real time.
     */
    const val MEASUREMENT_REQUIRES_REAL_TIME = true

    /**
     * Audio needs a policy of its own, and this fleet has three.
     *
     * ARMSX2 carries a separate fast-forward volume, defaulting to 100. Take
     * it: it is the only answer anywhere in the fleet to "what should
     * fast-forward sound like", and muting is the common preference.
     */
    const val DEFAULT_FAST_FORWARD_VOLUME_PERCENT = 100

    /**
     * Below-100 is not a hypothetical on a thermal-limited handheld.
     *
     * azahar time-stretches when emulation speed drops to 95 or below, to keep
     * audio intact while the emulator cannot keep up. That is the direction
     * that matters most here, and it is the one a fast-forward feature would
     * never have found.
     */
    const val AUDIO_STRETCH_AT_OR_BELOW_PERCENT = 95

    /**
     * Check a requested scale against what a backend declared.
     *
     * An empty list means the request is honourable as asked. Real time and
     * pause are always honourable: a backend that cannot pause cannot save
     * state either, and pause is the agent loop's whole mechanism.
     */
    fun check(
        request: TimeScale,
        support: TimeScaleSupport,
        hleGuestTimeFromHostClock: Boolean,
    ): List<ScaleDenial> {
        if (request.isNormal || request.isPaused) return emptyList()

        val denials = mutableListOf<ScaleDenial>()
        if (support.scaled.isEmpty()) {
            denials.add(ScaleDenial.NOTHING_DECLARED)
        }
        if (request.percent > support.maxPercent) {
            denials.add(ScaleDenial.ABOVE_BACKEND_MAXIMUM)
        }
        if (hleGuestTimeFromHostClock &&
            ClockDomain.HOST_DERIVED_TIME !in support.scaled
        ) {
            denials.add(ScaleDenial.HOST_TIME_NOT_SCALED)
        }
        if (!support.anchored) {
            denials.add(ScaleDenial.NOT_ANCHORED)
        }
        return denials
    }

    /** Is this run reportable as a measurement? */
    fun measurable(scale: TimeScale): Boolean = scale.isNormal

    /** Should the audio path time-stretch at this ACHIEVED speed? */
    fun shouldStretchAudio(achievedPercent: Int): Boolean =
        achievedPercent <= AUDIO_STRETCH_AT_OR_BELOW_PERCENT

    /**
     * A guest the user is watching must not be fast-forwarded blindly.
     *
     * GuestActivity already refuses to act on a frame during a cutscene,
     * because a vision model asked "what button" during a pre-rendered movie
     * will guess. Scaling time under a fixed-rate decode is the same hazard
     * from the other end: the decoder produces frames at its own rate, and
     * moving the clock under it does not make the movie shorter, it makes the
     * playback wrong.
     */
    fun safeDuringActivity(scale: TimeScale, activity: GuestActivity): Boolean =
        when {
            scale.isNormal || scale.isPaused -> true
            activity == GuestActivity.NON_INTERACTIVE -> false
            else -> true
        }
}
