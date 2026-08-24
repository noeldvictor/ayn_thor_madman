package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the integrity mode.
 *
 * The first two pin ARMSX2's shipped bugs. Both are the obvious implementation:
 * block every patch, and match every patch the same way.
 *
 * See research_log/20260825_0410_rewind_and_the_integrity_mode_nobody_designed.md.
 */
class IntegrityModeTest {

    @Test
    fun `a fix survives integrity mode - ARMSX2 bug one`() {
        // "Hardcore blocks CHEATS, not fixes." The old condition dropped every
        // on-disk pnach, so a widescreen or bug-fix patch silently stopped
        // working the moment the user enabled hardcore, with no message.
        assertTrue(
            IntegrityPolicy.allowsPatch(IntegrityMode.ENFORCED, PatchIntent.FIX)
        )
        assertTrue(
            IntegrityPolicy.allowsPatch(IntegrityMode.ENFORCED, PatchIntent.SPEED)
        )
        assertFalse(
            IntegrityPolicy.allowsPatch(IntegrityMode.ENFORCED, PatchIntent.CHEAT)
        )
        // A translation or restored-content patch changes what the game is, so
        // it goes with the cheats.
        assertFalse(
            IntegrityPolicy.allowsPatch(IntegrityMode.ENFORCED, PatchIntent.CHANGE)
        )
    }

    @Test
    fun `a cheat binds to the title and a fix binds to the dump - ARMSX2 bug two`() {
        // A cheat written before the CRC was known, or imported under another
        // revision's name, must still be found: a wrong-revision cheat is
        // harmless. A wrong-revision graphics patch is not.
        assertEquals(PatchBinding.GAME_KEY, IntegrityPolicy.bindsTo(PatchIntent.CHEAT))
        assertEquals(PatchBinding.GAME_KEY, IntegrityPolicy.bindsTo(PatchIntent.CHANGE))
        assertEquals(PatchBinding.DUMP_ID, IntegrityPolicy.bindsTo(PatchIntent.FIX))
        assertEquals(PatchBinding.DUMP_ID, IntegrityPolicy.bindsTo(PatchIntent.SPEED))
    }

    @Test
    fun `only a dump-bound patch may auto-apply at boot`() {
        assertTrue(IntegrityPolicy.mayAutoApply(PatchIntent.FIX))
        assertFalse(IntegrityPolicy.mayAutoApply(PatchIntent.CHEAT))
    }

    @Test
    fun `everything is available when nothing is claimed`() {
        for (f in GuardedFeature.values()) {
            assertTrue(f.name, IntegrityPolicy.allows(IntegrityMode.OFF, f))
        }
    }

    @Test
    fun `the five features this project specified separately are all guarded`() {
        val mode = IntegrityMode.ENFORCED
        assertFalse(IntegrityPolicy.allows(mode, GuardedFeature.SAVE_STATE))
        assertFalse(IntegrityPolicy.allows(mode, GuardedFeature.REWIND))
        assertFalse(IntegrityPolicy.allows(mode, GuardedFeature.CHEATS))
        assertFalse(IntegrityPolicy.allows(mode, GuardedFeature.SLOW_MOTION))
        assertFalse(IntegrityPolicy.allows(mode, GuardedFeature.PATCHES_THAT_CHANGE_PLAY))
    }

    @Test
    fun `fast forward is not guarded, because no fork was found blocking it`() {
        // ARMSX2's own text names save states, cheats and SLOWDOWN. Fast
        // forward is a separate question and nothing here answers it, so it is
        // not blocked by assumption.
        assertTrue(
            IntegrityPolicy.allows(IntegrityMode.ENFORCED, GuardedFeature.FAST_FORWARD)
        )
    }

    @Test
    fun `the losses are enumerable before the switch is thrown`() {
        // ARMSX2's bug one was a silent loss. The app must be able to say what
        // enabling the mode costs, before the user confirms it.
        assertTrue(IntegrityPolicy.MUST_LIST_LOSSES_BEFORE_ENABLING)
        val losses = IntegrityPolicy.lossesFromEnabling()
        assertTrue(GuardedFeature.REWIND in losses)
        assertTrue(GuardedFeature.CHEATS in losses)
        assertFalse("a silent loss is the bug", losses.isEmpty())
    }

    @Test
    fun `slow motion is guarded and the time scale agrees`() {
        // TimeScale owns the mechanism; this owns whether it may be used.
        assertTrue(TimeScale.of(50).isSlowMotion)
        assertFalse(IntegrityPolicy.allows(IntegrityMode.ENFORCED, GuardedFeature.SLOW_MOTION))
    }
}
