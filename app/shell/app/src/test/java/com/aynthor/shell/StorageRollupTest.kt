package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the library-wide storage rollups.
 *
 * This is the one screen in SCREENS.md with no prior art in the fleet, so
 * nothing here was harvested and nothing has been proven by anyone else's
 * users. That makes the tests the only check on the rules.
 */
class StorageRollupTest {

    private fun game(title: String, vararg items: StorageItem) =
        Game(
            title = title,
            system = System.PS2,
            guestScreens = emptyList(),
            hasCheats = false,
            hasOverride = false,
            hasPack = false,
            hasPatch = false,
            storage = items.toList(),
        )

    private val small = game(
        "Small",
        StorageItem("Game data", 100, false),
        StorageItem("Shader cache", 50, true),
    )
    private val big = game(
        "Big",
        StorageItem("Game data", 4000, false),
        StorageItem("Shader cache", 200, true),
        StorageItem("Saves and states", 10, false),
    )

    @Test
    fun `games are sorted biggest first`() {
        // The question is "which game should I delete", so the answer must be
        // at the top.
        val out = StorageRollup.byGame(listOf(small, big))
        assertEquals(listOf("Big", "Small"), out.map { it.first.title })
        assertEquals(4210, out.first().second)
    }

    @Test
    fun `categories are summed across the library and sorted biggest first`() {
        val out = StorageRollup.byCategory(listOf(small, big))
        assertEquals("Game data", out.first().label)
        assertEquals(4100, out.first().megabytes)
        assertEquals(250, out.first { it.label == "Shader cache" }.megabytes)
    }

    @Test
    fun `a category is clearable only if EVERY instance is`() {
        // One game keeps its screenshots deliberately; another does not. The
        // rolled-up row must take the unsafe answer, because a bulk action on
        // it would hit both.
        val keeps = game("Keeps", StorageItem("Screenshots", 10, false))
        val clears = game("Clears", StorageItem("Screenshots", 90, true))

        val row = StorageRollup.byCategory(listOf(keeps, clears))
            .first { it.label == "Screenshots" }

        assertEquals(100, row.megabytes)
        assertFalse(
            "one non-rebuildable member must make the whole row unsafe",
            row.rebuildable,
        )
    }

    @Test
    fun `reclaimable never counts saves`() {
        // Saves and states are the only irreplaceable category. They must not
        // appear in any total that a button acts on.
        val out = StorageRollup.reclaimableMb(listOf(small, big))
        assertEquals(250, out)
        assertTrue("reclaimable must be less than the total", out < StorageRollup.totalMb(listOf(small, big)))
    }

    @Test
    fun `an empty library does not crash or divide by anything`() {
        assertEquals(0, StorageRollup.totalMb(emptyList()))
        assertEquals(0, StorageRollup.reclaimableMb(emptyList()))
        assertTrue(StorageRollup.byGame(emptyList()).isEmpty())
        assertTrue(StorageRollup.byCategory(emptyList()).isEmpty())
    }
}
