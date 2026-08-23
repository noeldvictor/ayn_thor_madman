package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the per-game override rules.
 *
 * Each test names the ARMSX2 bug it pins. The obvious implementation passes
 * none of the first three; that is the whole reason they are here. See
 * research_log/20260822_2203_armsx2_frontend_is_the_shell.md.
 */
class SettingResolverTest {

    private fun spec(key: String, scope: SettingScope = SettingScope.PER_GAME) =
        SettingSpec(
            key = key,
            label = key,
            type = SettingType.BOOL,
            group = "test",
            default = "off",
            scope = scope,
        )

    private val cheats = spec("cheats")
    private val frameCap = spec("frameCap")
    private val pine = spec("pine", SettingScope.PROMOTED)
    private val specs = listOf(cheats, frameCap, pine)

    // ---------------------------------------------------------------- resolve

    @Test
    fun `resolution order is per-game, global, profile, default`() {
        val r = SettingResolver.resolve(
            cheats,
            perGame = mapOf("cheats" to "a"),
            global = mapOf("cheats" to "b"),
            thorProfile = mapOf("cheats" to "c"),
            backendDefault = mapOf("cheats" to "d"),
        )
        assertEquals("a", r?.value)
        assertEquals(SettingSource.PER_GAME, r?.source)

        assertEquals(
            SettingSource.BACKEND_DEFAULT,
            SettingResolver.resolve(
                cheats, emptyMap(), emptyMap(), emptyMap(), mapOf("cheats" to "d"),
            )?.source,
        )

        assertNull(
            SettingResolver.resolve(cheats, emptyMap(), emptyMap(), emptyMap(), emptyMap()),
        )
    }

    @Test
    fun `a promoted setting is never read from the per-game tier`() {
        // Even with a stale per-game value present, global wins for PROMOTED.
        val r = SettingResolver.resolve(
            pine,
            perGame = mapOf("pine" to "stale"),
            global = mapOf("pine" to "real"),
            thorProfile = emptyMap(),
            backendDefault = emptyMap(),
        )
        assertEquals("real", r?.value)
        assertEquals(SettingSource.GLOBAL, r?.source)
    }

    // ------------------------------------------------------------------ rule 1

    @Test
    fun `sparse - an untouched field is not stored, so global still reaches it`() {
        val out = SettingResolver.writeOverride(
            specs,
            existingPerGame = emptyMap(),
            global = mapOf("cheats" to "off", "frameCap" to "60"),
            updated = mapOf("cheats" to "on", "frameCap" to "60"),
            previous = mapOf("cheats" to "off", "frameCap" to "60"),
        )
        assertEquals("on", out.perGame["cheats"])
        assertFalse("frameCap was never touched, so it must not be stored",
            out.perGame.containsKey("frameCap"))
    }

    // ------------------------------------------------------------------ rule 2

    @Test
    fun `sticky - ARMSX2 bug 1, an override equal to global survives a global change`() {
        // The user turns cheats on for this game while global is ALSO on.
        // The obvious implementation stores nothing here, because nothing differs.
        val first = SettingResolver.writeOverride(
            specs,
            existingPerGame = emptyMap(),
            global = mapOf("cheats" to "on"),
            updated = mapOf("cheats" to "on"),
            previous = mapOf("cheats" to "off"),
        )
        assertEquals(
            "the user explicitly set this for this game; it must be pinned even though it equals global",
            "on", first.perGame["cheats"],
        )

        // Global later goes off. The game must keep its own value.
        assertEquals(
            "on",
            SettingResolver.resolve(
                cheats,
                perGame = first.perGame,
                global = mapOf("cheats" to "off"),
                thorProfile = emptyMap(),
                backendDefault = emptyMap(),
            )?.value,
        )
    }

    // ------------------------------------------------------------------ rule 3

    @Test
    fun `change-tracked - ARMSX2 bug 3, a stale whole-object write cannot clobber a pin`() {
        // frameCap is pinned to 30 for this game.
        val existing = mapOf("frameCap" to "30")

        // A screen saves the whole settings object from a STALE snapshot that
        // still holds 0, while only changing cheats.
        val out = SettingResolver.writeOverride(
            specs,
            existingPerGame = existing,
            global = mapOf("cheats" to "off", "frameCap" to "0"),
            updated = mapOf("cheats" to "on", "frameCap" to "0"),
            previous = mapOf("cheats" to "off", "frameCap" to "0"),
        )

        assertEquals("the pinned value must survive a write that did not touch it",
            "30", out.perGame["frameCap"])
        assertEquals("on", out.perGame["cheats"])
    }

    @Test
    fun `change-tracked - the caller can still change a pinned field deliberately`() {
        val out = SettingResolver.writeOverride(
            specs,
            existingPerGame = mapOf("frameCap" to "30"),
            global = mapOf("frameCap" to "0"),
            updated = mapOf("frameCap" to "45"),
            previous = mapOf("frameCap" to "30"),
        )
        assertEquals("45", out.perGame["frameCap"])
    }

    // --------------------------------------------------------- promotion, bug 2

    @Test
    fun `promoted - ARMSX2 bug 2, a process-wide setting is written to global, not nowhere`() {
        val out = SettingResolver.writeOverride(
            specs,
            existingPerGame = emptyMap(),
            global = mapOf("pine" to "off"),
            updated = mapOf("pine" to "on"),
            previous = mapOf("pine" to "off"),
        )
        assertEquals("a promoted field must land in the global patch", "on", out.globalPatch["pine"])
        assertFalse("and must never be stored per game", out.perGame.containsKey("pine"))
    }

    @Test
    fun `promotion carries only the changed field, never the resolved object`() {
        val out = SettingResolver.writeOverride(
            specs,
            existingPerGame = emptyMap(),
            global = mapOf("pine" to "off", "cheats" to "off"),
            updated = mapOf("pine" to "on", "cheats" to "on"),
            previous = mapOf("pine" to "off", "cheats" to "off"),
        )
        assertEquals(setOf("pine"), out.globalPatch.keys)
        assertTrue("the per-game change must not leak into global",
            !out.globalPatch.containsKey("cheats"))
        assertEquals("on", out.perGame["cheats"])
    }
}

/**
 * Tests for settings migration.
 *
 * The shape is melonDS-android's, which is the only real migration framework in
 * the fleet. See research_log/20260822_2233_fleet_frontend_census.md.
 */
class SettingsMigratorTest {

    private class Rename(
        override val from: Int,
        override val to: Int,
        private val oldKey: String,
        private val newKey: String,
    ) : SettingsMigration {
        override fun migrate(store: MutableMap<String, String>) {
            store.remove(oldKey)?.let { store[newKey] = it }
        }
    }

    @Test
    fun `runs only the migrations in range, in order`() {
        val store = mutableMapOf("a" to "1")
        val migrations = listOf(
            Rename(1, 2, "a", "b"),
            Rename(2, 3, "b", "c"),
            // Out of range: the app has not reached version 4 yet.
            Rename(3, 4, "c", "d"),
        )
        val now = SettingsMigrator.migrate(migrations, store, lastVersion = 1, currentVersion = 3)
        assertEquals(3, now)
        assertEquals(mapOf("c" to "1"), store)
    }

    @Test
    fun `does nothing when the store is already current`() {
        val store = mutableMapOf("a" to "1")
        val now = SettingsMigrator.migrate(
            listOf(Rename(1, 2, "a", "b")), store, lastVersion = 5, currentVersion = 5,
        )
        assertEquals(5, now)
        assertEquals(mapOf("a" to "1"), store)
    }

    @Test
    fun `refuses two migrations from the same version`() {
        // Their order would be undefined, so this is a programming error and
        // must fail loudly rather than pick one.
        var threw = false
        try {
            SettingsMigrator.migrate(
                listOf(Rename(1, 2, "a", "b"), Rename(1, 2, "a", "c")),
                mutableMapOf(), lastVersion = 1, currentVersion = 2,
            )
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("a duplicate `from` must be rejected", threw)
    }
}
