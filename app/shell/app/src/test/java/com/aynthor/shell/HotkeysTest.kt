package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hotkey suite.
 *
 * **Four of these encode a bug ARMSX2 shipped or a rule it learned**, so this
 * project does not learn them again. See
 * `work_log/20260823_1930_hotkey_extraction.md`.
 */
class HotkeysTest {

    private val KEY_L3 = 102
    private val KEY_R3 = 103
    private val KEY_START = 108

    private fun bindings(vararg pairs: Pair<HotkeyAction, Int>) = pairs.toMap()

    // ------------------------------------------------- the product requirement

    @Test
    fun `the action set is shared, so no backend can define its own`() {
        // The named RetroArch failure: each core does its own thing. The set is
        // an enum in the app, so a backend has nowhere to add one.
        assertTrue(HotkeyAction.entries.size >= 15)
        assertTrue(HotkeyAction.MENU in HotkeyAction.entries)
    }

    @Test
    fun `an unbound key resolves to nothing, and says so`() {
        val (action, denial) = HotkeyPolicy.resolve(KEY_R3, bindings(), HotkeyContext())
        assertNull(action)
        assertEquals(HotkeyDenial.NOT_BOUND, denial)
    }

    @Test
    fun `a bound key resolves to its action`() {
        val b = bindings(HotkeyAction.SCREENSHOT to KEY_L3)
        val (action, denial) = HotkeyPolicy.resolve(KEY_L3, b, HotkeyContext())
        assertEquals(HotkeyAction.SCREENSHOT, action)
        assertEquals(HotkeyDenial.ALLOWED, denial)
    }

    // ------------------------------------------------ the four learned lessons

    @Test
    fun `binding capture may not use a modal dialog`() {
        // ARMSX2's 2.6.0 "can't remap buttons" bug. A focus-stealing AlertDialog
        // has its own window, which swallowed controller keys before the Compose
        // handler could see them. Capture belongs in the already-focused screen.
        assertFalse(HotkeyPolicy.CAPTURE_MAY_USE_MODAL_DIALOG)
    }

    @Test
    fun `while capturing a binding, the next press is a binding and not an action`() {
        // Otherwise binding "menu" opens the menu, and the binding never lands.
        val b = bindings(HotkeyAction.MENU to KEY_START)
        val (action, denial) = HotkeyPolicy.resolve(
            KEY_START, b, HotkeyContext(capturingBinding = true),
        )
        assertNull(action)
        assertEquals(HotkeyDenial.CAPTURING_BINDING, denial)
    }

    @Test
    fun `hardcore achievements block slow motion and rewind, with a reason`() {
        // ARMSX2 disables slowdown under hardcore because it is a banned
        // advantage, and shows a notice rather than silently ignoring the press.
        // A hotkey that does nothing and says nothing reads as a broken binding.
        val ctx = HotkeyContext(hardcoreAchievements = true)
        assertEquals(HotkeyDenial.BLOCKED_BY_HARDCORE, HotkeyPolicy.availability(HotkeyAction.SLOW_MOTION, ctx))
        assertEquals(HotkeyDenial.BLOCKED_BY_HARDCORE, HotkeyPolicy.availability(HotkeyAction.REWIND, ctx))
        // Saving a state is not an advantage under hardcore rules here.
        assertEquals(HotkeyDenial.ALLOWED, HotkeyPolicy.availability(HotkeyAction.SAVE_STATE, ctx))
    }

    @Test
    fun `fast forward exists as both a hold and a toggle`() {
        // One action, two binding kinds. A single "on press" model cannot express
        // this, and ARMSX2 ships both because people want both.
        assertEquals(HotkeyKind.HOLD, HotkeyAction.FAST_FORWARD_HOLD.kind)
        assertEquals(HotkeyKind.TOGGLE, HotkeyAction.FAST_FORWARD_TOGGLE.kind)
    }

    @Test
    fun `screenshot is bindable, because the system gesture interrupts play`() {
        // ARMSX2's reason for making it a hotkey at all: the Android screenshot
        // gesture interrupts the game. A spare button does not.
        assertEquals(HotkeyKind.ONE_SHOT, HotkeyAction.SCREENSHOT.kind)
        val b = bindings(HotkeyAction.SCREENSHOT to KEY_L3)
        assertEquals(HotkeyAction.SCREENSHOT, HotkeyPolicy.resolve(KEY_L3, b, HotkeyContext()).first)
    }

    // ----------------------------------------------------------- availability

    @Test
    fun `in-game actions are refused with no game running, and say which reason`() {
        val ctx = HotkeyContext(gameRunning = false)
        assertEquals(HotkeyDenial.NO_GAME_RUNNING, HotkeyPolicy.availability(HotkeyAction.SAVE_STATE, ctx))
        assertEquals(HotkeyDenial.NO_GAME_RUNNING, HotkeyPolicy.availability(HotkeyAction.RESET_GAME, ctx))
    }

    @Test
    fun `the menu still works with no game running`() {
        // The one that must never be refused, or a person can be stranded.
        assertEquals(
            HotkeyDenial.ALLOWED,
            HotkeyPolicy.availability(HotkeyAction.MENU, HotkeyContext(gameRunning = false)),
        )
    }

    @Test
    fun `swap screens is refused on a single-screen title, as not applicable`() {
        // A distinct reason from "not bound". The binding is fine; the title has
        // one screen.
        assertEquals(
            HotkeyDenial.NOT_APPLICABLE_TO_TITLE,
            HotkeyPolicy.availability(HotkeyAction.SWAP_SCREENS, HotkeyContext(dualScreen = false)),
        )
    }

    @Test
    fun `every denial is a distinct reason, never silence`() {
        // A hotkey that does nothing and says nothing is indistinguishable from
        // a broken binding.
        assertEquals(6, HotkeyDenial.entries.size)
        assertTrue(HotkeyDenial.entries.distinct().size == HotkeyDenial.entries.size)
    }

    // -------------------------------------------------------------- conflicts

    @Test
    fun `two actions on one button are reported, not resolved`() {
        // A hold and a one-shot can legitimately share a button. The UI shows
        // the conflict and the person decides.
        val b = bindings(
            HotkeyAction.SCREENSHOT to KEY_L3,
            HotkeyAction.TOGGLE_OVERLAY to KEY_L3,
            HotkeyAction.MENU to KEY_START,
        )
        val c = HotkeyPolicy.conflicts(b)
        assertEquals(setOf(KEY_L3), c.keys)
        assertEquals(2, c.getValue(KEY_L3).size)
    }

    @Test
    fun `a clean binding set reports no conflicts`() {
        val b = bindings(HotkeyAction.SCREENSHOT to KEY_L3, HotkeyAction.MENU to KEY_START)
        assertTrue(HotkeyPolicy.conflicts(b).isEmpty())
    }

    // -------------------------------------------------------------- durability

    @Test
    fun `bindings key on the enum name, so the set may be reordered`() {
        // The opposite of the upscale-algorithm enum, which is persisted as an
        // integer and is therefore append-only forever. Naming the key means a
        // removed action orphans one binding instead of shifting every other.
        val stored = mapOf("SCREENSHOT" to KEY_L3)
        val restored = stored.mapKeys { HotkeyAction.valueOf(it.key) }
        assertEquals(HotkeyAction.SCREENSHOT, restored.keys.first())
    }
}
