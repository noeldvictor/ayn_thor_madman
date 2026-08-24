package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the guest time scale.
 *
 * Each test names the fork evidence behind it. The headline one is
 * `a HLE backend that does not scale host time is refused`: Vita3K shipped that
 * bug twice, and a `fastForward: Boolean` contract would ship it a third time.
 *
 * See research_log/20260825_0245_fast_forward_is_a_time_scale_not_a_toggle.md.
 */
class TimeScaleTest {

    /** melonDS: cycle-driven, so the loop speeding up is enough. */
    private val lowLevel = TimeScaleSupport(
        scaled = setOf(ClockDomain.GUEST_CPU, ClockDomain.DISPLAY_PACING),
        anchored = true,
    )

    /** Vita3K after its second pass: every clock, anchored. */
    private val highLevelFixed = TimeScaleSupport(
        scaled = setOf(
            ClockDomain.GUEST_CPU,
            ClockDomain.DISPLAY_PACING,
            ClockDomain.AUDIO,
            ClockDomain.HOST_DERIVED_TIME,
            ClockDomain.GUEST_TIMERS,
        ),
        anchored = true,
    )

    /** Vita3K after its FIRST pass: timers scaled, host time not. */
    private val highLevelBroken = TimeScaleSupport(
        scaled = setOf(
            ClockDomain.GUEST_CPU,
            ClockDomain.DISPLAY_PACING,
            ClockDomain.GUEST_TIMERS,
        ),
        anchored = true,
    )

    @Test
    fun `one value covers pause, slow motion, normal and fast forward`() {
        assertTrue(TimeScale.PAUSED.isPaused)
        assertTrue(TimeScale.REAL_TIME.isNormal)
        assertTrue(TimeScale.of(200).isFastForward)
        assertTrue(TimeScale.of(50).isSlowMotion)
        // Pause is not slow motion. The agent loop depends on the distinction:
        // it freezes the guest so model latency costs nothing.
        assertFalse(TimeScale.PAUSED.isSlowMotion)
    }

    @Test
    fun `a low-level backend needs nothing else`() {
        assertTrue(
            TimeScalePolicy.check(
                TimeScale.of(200), lowLevel, hleGuestTimeFromHostClock = false,
            ).isEmpty()
        )
    }

    @Test
    fun `a HLE backend that does not scale host time is refused`() {
        // THE BUG VITA3K SHIPPED. The frame limiter moves, the guest keeps
        // reading real time, and the user reports that nothing happened.
        val denials = TimeScalePolicy.check(
            TimeScale.of(200), highLevelBroken, hleGuestTimeFromHostClock = true,
        )
        assertEquals(listOf(ScaleDenial.HOST_TIME_NOT_SCALED), denials)
    }

    @Test
    fun `the same backend is fine once it declares host time`() {
        assertTrue(
            TimeScalePolicy.check(
                TimeScale.of(200), highLevelFixed, hleGuestTimeFromHostClock = true,
            ).isEmpty()
        )
    }

    @Test
    fun `an unanchored backend is flagged even when every clock is scaled`() {
        // Vita3K's word. A backend that multiplies rather than anchors makes
        // guest time jump at the toggle.
        val denials = TimeScalePolicy.check(
            TimeScale.of(200), highLevelFixed.copy(anchored = false),
            hleGuestTimeFromHostClock = true,
        )
        assertEquals(listOf(ScaleDenial.NOT_ANCHORED), denials)
    }

    @Test
    fun `pause and real time are always honourable`() {
        val nothing = TimeScaleSupport(scaled = emptySet(), anchored = false)
        assertTrue(TimeScalePolicy.check(TimeScale.PAUSED, nothing, true).isEmpty())
        assertTrue(TimeScalePolicy.check(TimeScale.REAL_TIME, nothing, true).isEmpty())
    }

    @Test
    fun `a backend declaring nothing is refused, not silently accepted`() {
        val nothing = TimeScaleSupport(scaled = emptySet(), anchored = true)
        assertTrue(
            ScaleDenial.NOTHING_DECLARED in
                TimeScalePolicy.check(TimeScale.of(200), nothing, false)
        )
    }

    @Test
    fun `a scale above the declared maximum is refused`() {
        val capped = lowLevel.copy(maxPercent = 400)
        assertEquals(
            listOf(ScaleDenial.ABOVE_BACKEND_MAXIMUM),
            TimeScalePolicy.check(TimeScale.of(800), capped, false),
        )
    }

    @Test
    fun `only a real-time run is measurable`() {
        assertTrue(TimeScalePolicy.measurable(TimeScale.REAL_TIME))
        assertFalse(TimeScalePolicy.measurable(TimeScale.of(200)))
        // Pause is not a measurement either, however tempting a still frame is.
        assertFalse(TimeScalePolicy.measurable(TimeScale.PAUSED))
    }

    @Test
    fun `audio stretches when the emulator falls behind, not when it runs ahead`() {
        // azahar's direction, and the common case on a thermal-limited handheld.
        assertTrue(TimeScalePolicy.shouldStretchAudio(90))
        assertTrue(TimeScalePolicy.shouldStretchAudio(95))
        assertFalse(TimeScalePolicy.shouldStretchAudio(96))
        assertFalse(TimeScalePolicy.shouldStretchAudio(200))
    }

    @Test
    fun `fast forward is refused while the user is watching a cutscene`() {
        assertFalse(
            TimeScalePolicy.safeDuringActivity(
                TimeScale.of(200), GuestActivity.NON_INTERACTIVE,
            )
        )
        assertTrue(
            TimeScalePolicy.safeDuringActivity(
                TimeScale.of(200), GuestActivity.INTERACTIVE,
            )
        )
        // Pausing a cutscene is fine. Only moving the clock under a fixed-rate
        // decode is the hazard.
        assertTrue(
            TimeScalePolicy.safeDuringActivity(
                TimeScale.PAUSED, GuestActivity.NON_INTERACTIVE,
            )
        )
    }

    @Test
    fun `fast forward caps host presentation without touching guest timing`() {
        // azahar's Eco Turbo. 400% guest speed does not mean 240 presents a
        // second; on a 120 Hz panel that is GPU work nobody sees.
        val ns = TimeScalePolicy.hostPresentIntervalNs(TimeScale.of(400), panelHz = 120)
        assertEquals(1_000_000_000L / 60, ns)
        // And the guest scale is untouched -- the cap is a host-side budget.
        assertTrue(TimeScale.of(400).isFastForward)
    }

    @Test
    fun `the cap is wall-clock, not a divisor of the requested speed`() {
        // The rejected design divides presentation by the REQUESTED scale, so a
        // scene that only reaches 200% of a requested 400% is undersampled by
        // half. A wall-clock interval does not care what was requested.
        val at200 = TimeScalePolicy.hostPresentIntervalNs(TimeScale.of(200), 120)
        val at800 = TimeScalePolicy.hostPresentIntervalNs(TimeScale.of(800), 120)
        assertEquals(at200, at800)
    }

    @Test
    fun `real time and a 60 Hz panel present every frame`() {
        assertNull(TimeScalePolicy.hostPresentIntervalNs(TimeScale.REAL_TIME, 120))
        assertNull(TimeScalePolicy.hostPresentIntervalNs(TimeScale.of(400), 60))
    }

    @Test
    fun `an out-of-range scale is rejected at the edge`() {
        var threw = false
        try {
            TimeScale.of(-1)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("a negative scale must be rejected", threw)
    }
}
