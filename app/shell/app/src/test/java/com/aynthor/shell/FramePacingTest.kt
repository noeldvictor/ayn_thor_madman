package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The frame-pacing suite.
 *
 * **Cemu is the incumbent and every rule here is one of its four parts.** This
 * repo recorded frame pacing as having no implementation anywhere in the fleet;
 * it was wrong, and the survey's method is why. See
 * `research_log/20260824_0010_frame_pacing_has_an_incumbent.md`.
 */
class FramePacingTest {

    // ------------------------------------------------ queue depth is latency

    @Test
    fun `only FIFO bounds the queue, because only FIFO lets frames queue`() {
        // Cemu sets maxQueued = 1 on FIFO alone and 0 for mailbox, immediate and
        // sync-and-limit.
        assertEquals(1, PresentQueue(PresentMode.FIFO).maxInFlight)
        assertEquals(0, PresentQueue(PresentMode.MAILBOX).maxInFlight)
        assertEquals(0, PresentQueue(PresentMode.IMMEDIATE).maxInFlight)
        assertEquals(0, PresentQueue(PresentMode.HOST_DRIVEN_VSYNC).maxInFlight)
    }

    @Test
    fun `a full FIFO queue must wait before submitting another present`() {
        // This is latency control, not vsync: it bounds how many frames may be
        // in flight, which is what stops render-ahead lag on a device with real
        // buttons.
        assertTrue(PresentQueue(PresentMode.FIFO, inFlight = 1).mustWait())
        assertFalse(PresentQueue(PresentMode.FIFO, inFlight = 0).mustWait())
    }

    @Test
    fun `an unbounded mode never waits`() {
        assertFalse(PresentQueue(PresentMode.MAILBOX, inFlight = 9).mustWait())
    }

    @Test
    fun `the present wait is bounded, so a lost present cannot deadlock the loop`() {
        // Cemu uses 40 ms, about two and a half vsyncs at 60 Hz.
        assertEquals(40_000_000L, FramePacingPolicy.PRESENT_WAIT_TIMEOUT_NS)
    }

    // ---------------------------------------------------- host-driven vsync

    @Test
    fun `host-driven vsync is a distinct mode, not a flavour of FIFO`() {
        // Cemu's SYNC_AND_LIMIT starts a thread that notifies the guest GPU on
        // the host's real vsync. It lives in guest-timing code, not in a
        // renderer, which is why a present-code survey could not see it.
        assertNotEquals(PresentMode.FIFO, PresentMode.HOST_DRIVEN_VSYNC)
        assertEquals(4, PresentMode.entries.size)
    }

    // ------------------------------------------------------- two swapchains

    @Test
    fun `each screen waits on its own marker, never on the other screen's`() {
        // One shared marker across both screens serialises them, so the slower
        // screen paces the faster one. Cemu's comment records fixing exactly
        // this: "keep TV and GamePad swapchains from forcing each other to idle".
        val built = ScreenPresent(HostDisplay.BUILT_IN, previousFrameMarker = 10)
        val second = ScreenPresent(HostDisplay.SCREEN_2, previousFrameMarker = 99)
        val all = listOf(built, second)
        assertEquals(built, FramePacingPolicy.waitsOn(built, all))
        assertEquals(second, FramePacingPolicy.waitsOn(second, all))
    }

    // ----------------------------------------------- Screen-2 costs are real

    @Test
    fun `Screen-2 draws on change, not per frame`() {
        // A second panel drawn every frame costs power and thermal headroom for
        // no benefit. The presenter is where this is enforced.
        assertFalse(FramePacingPolicy.shouldRedraw(HostDisplay.SCREEN_2, contentChanged = false))
        assertTrue(FramePacingPolicy.shouldRedraw(HostDisplay.SCREEN_2, contentChanged = true))
    }

    @Test
    fun `the built-in display always draws`() {
        assertTrue(FramePacingPolicy.shouldRedraw(HostDisplay.BUILT_IN, contentChanged = false))
    }

    // ------------------------------------------------------- stable divisor

    @Test
    fun `a 60 Hz guest on a 120 Hz panel paces at a divisor of two`() {
        assertEquals(2, FramePacingPolicy.divisorFor(panelHz = 120, guestHz = 60))
    }

    @Test
    fun `a 60 Hz guest on a 60 Hz panel paces one to one`() {
        // The device is currently capped to 60 by a user setting even though both
        // panels do 120, so a pacing decision must read the CURRENT cap rather
        // than the panel's capability.
        assertEquals(1, FramePacingPolicy.divisorFor(panelHz = 60, guestHz = 60))
    }

    @Test
    fun `a guest faster than the panel never gets a divisor below one`() {
        assertEquals(1, FramePacingPolicy.divisorFor(panelHz = 60, guestHz = 120))
    }

    // ------------------------------------------ frame generation is gated

    @Test
    fun `a generated frame is refused over a fixed-rate video`() {
        // Extrapolating between two frames of a 30 fps movie invents motion that
        // is not there. The gate is the guest's declared activity, not a guess.
        val movie = ActivityReport(GuestActivity.NON_INTERACTIVE, ActivitySource.GUEST_DECLARED)
        val play = ActivityReport(GuestActivity.INTERACTIVE, ActivitySource.GUEST_DECLARED)
        assertFalse(FramePacingPolicy.mayPresentGeneratedFrame(movie))
        assertTrue(FramePacingPolicy.mayPresentGeneratedFrame(play))
    }

    @Test
    fun `acquire blocks rather than polling`() {
        // rpcsx's present path sets the acquire timeout to zero on a 2017 AMD
        // desktop-driver comment, which on a BufferQueue is a busy-wait against
        // the compositor: 37,000 iterations a second in the flip path.
        assertTrue(FramePacingPolicy.ACQUIRE_MUST_BLOCK)
    }
}
