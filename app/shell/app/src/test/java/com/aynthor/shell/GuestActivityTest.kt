package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The guest-activity suite.
 *
 * **Every test here encodes something four forks already receive from the guest
 * and throw away.** See
 * `research_log/20260824_0140_the_guest_declares_its_activity.md`.
 */
class GuestActivityTest {

    private fun declared(a: GuestActivity, ack: Boolean? = null) =
        ActivityReport(a, ActivitySource.GUEST_DECLARED, ack)

    private fun inferred(a: GuestActivity) =
        ActivityReport(a, ActivitySource.DECODER_INFERRED)

    // ------------------------------------------------ the loop's central gate

    @Test
    fun `an agent may not act on a frame the guest calls non-interactive`() {
        // The whole reason this type exists. A vision model asked "what button?"
        // during a cutscene will guess.
        assertEquals(
            ActivityDenial.GUEST_IS_NON_INTERACTIVE,
            ActivityPolicy.mayAct(declared(GuestActivity.NON_INTERACTIVE)),
        )
    }

    @Test
    fun `an agent may act during normal play`() {
        assertEquals(ActivityDenial.ALLOWED, ActivityPolicy.mayAct(declared(GuestActivity.INTERACTIVE)))
    }

    @Test
    fun `busy is a distinct refusal from non-interactive`() {
        // Loading is pointless to act on; a cutscene is harmful to act on. A
        // caller may reasonably treat them differently.
        assertEquals(ActivityDenial.GUEST_IS_BUSY, ActivityPolicy.mayAct(declared(GuestActivity.BUSY)))
    }

    // --------------------------------------- declared beats inferred, and why

    @Test
    fun `a caller may demand a declared state and refuse an inferred one`() {
        // A measurement harness should demand a declaration. A convenience
        // feature may accept an inference. The policy does not decide for them.
        val report = inferred(GuestActivity.INTERACTIVE)
        assertEquals(ActivityDenial.ALLOWED, ActivityPolicy.mayAct(report, requireDeclared = false))
        assertEquals(
            ActivityDenial.NO_DECLARED_STATE,
            ActivityPolicy.mayAct(report, requireDeclared = true),
        )
    }

    @Test
    fun `the source is kept, because it decides how much to trust the state`() {
        // Most in-engine cutscenes touch no video decoder and still disable
        // sleep, so a DECODER_INFERRED "interactive" can simply be wrong.
        assertEquals(ActivitySource.DECODER_INFERRED, inferred(GuestActivity.INTERACTIVE).source)
        assertEquals(3, ActivitySource.entries.size)
    }

    // ------------------------------------------------------- measurement gate

    @Test
    fun `a backend can refuse to be measured in a noisy state`() {
        // Measured noise floors: gated title screen +/-0.2%, restored savestate
        // +/-5%, pressing through cutscenes ~+/-50% and unusable.
        assertTrue(ActivityPolicy.mayMeasure(declared(GuestActivity.INTERACTIVE)))
        assertFalse(ActivityPolicy.mayMeasure(declared(GuestActivity.NON_INTERACTIVE)))
        assertFalse(ActivityPolicy.mayMeasure(declared(GuestActivity.BUSY)))
    }

    // -------------------------------------------------------- frame synthesis

    @Test
    fun `frame generation is refused over a fixed-rate video`() {
        // Extrapolating between two frames of a 30 fps movie invents motion that
        // is not there. Same for upscaling decoded video, and for drawing the
        // overlay over a cutscene.
        assertFalse(ActivityPolicy.mayGenerateFrames(declared(GuestActivity.NON_INTERACTIVE)))
        assertTrue(ActivityPolicy.mayGenerateFrames(declared(GuestActivity.INTERACTIVE)))
    }

    // ------------------------------------------------- the injected-input loop

    @Test
    fun `the guest can confirm an injected input registered`() {
        // Switch has ReportUserIsActive. For an agent that injects input this is
        // the guest's own statement that the injection landed, which nothing
        // else can supply.
        val r = declared(GuestActivity.INTERACTIVE, ack = true)
        assertNotNull(r.userActivityAcknowledged)
        assertTrue(r.userActivityAcknowledged!!)
    }

    @Test
    fun `a backend with no such signal reports null, not false`() {
        // Null means "cannot answer". False would mean "the input did not
        // register", which is a different and much stronger claim.
        assertEquals(null, declared(GuestActivity.INTERACTIVE).userActivityAcknowledged)
    }

    // -------------------------------------------------------------- defaults

    @Test
    fun `an unknown state defaults to interactive, the recoverable failure`() {
        // Assuming NON_INTERACTIVE means the agent never acts at all. Assuming
        // INTERACTIVE costs one wasted button press. Prefer the recoverable one.
        assertEquals(GuestActivity.INTERACTIVE, ActivityPolicy.WHEN_UNKNOWN)
    }

    @Test
    fun `a backend that reports nothing is still allowed to act`() {
        val silent = ActivityReport(GuestActivity.UNKNOWN, ActivitySource.UNKNOWN)
        assertEquals(ActivityDenial.ALLOWED, ActivityPolicy.mayAct(silent))
        // but a harness that demands a declaration still refuses it
        assertEquals(
            ActivityDenial.NO_DECLARED_STATE,
            ActivityPolicy.mayAct(silent, requireDeclared = true),
        )
    }

    @Test
    fun `the signal is declared to need per-title calibration`() {
        // A game may never call these APIs, or call them wrongly. The same rule
        // as the HLE stub fraction: a marker is a starting weight, not a score.
        assertTrue(ActivityPolicy.REQUIRES_CALIBRATION_PER_TITLE)
    }

    @Test
    fun `every denial is a distinct reason, never silence`() {
        assertEquals(4, ActivityDenial.entries.size)
        assertEquals(ActivityDenial.entries.size, ActivityDenial.entries.distinct().size)
    }
}
