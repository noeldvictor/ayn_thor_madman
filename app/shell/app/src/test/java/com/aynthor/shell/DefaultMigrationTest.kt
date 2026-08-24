package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hazard: "a persisted value overrides a compiled default forever". xenia
 * found three validated fastpaths sitting false on a device whose code said
 * true, worth +2.88%.
 *
 * The rule that makes the cure safe: move only the people who never chose.
 */
class DefaultMigrationTest {

    private fun spec(key: String, default: String) = SettingSpec(
        key = key,
        label = key,
        type = SettingType.BOOL,
        group = "GPU",
        default = default,
    )

    /** The xenia case: a validated fastpath shipped default-on, users stuck off. */
    @Test
    fun a_user_still_on_the_old_default_is_moved() {
        val r = DefaultMigration.migrate(
            stored = mapOf("cpu|rlwinm_fastpath" to "false"),
            specs = listOf(spec("cpu|rlwinm_fastpath", "true")),
            changes = listOf(DefaultChange("cpu|rlwinm_fastpath", "2026-08-25", "false")),
            watermark = null,
        )
        assertEquals("true", r.values["cpu|rlwinm_fastpath"])
        assertEquals(listOf("cpu|rlwinm_fastpath"), r.moved)
        assertEquals("2026-08-25", r.watermark)
    }

    /** The half that makes it safe. A deliberate choice is not a stale default. */
    @Test
    fun a_user_who_chose_is_left_alone() {
        val r = DefaultMigration.migrate(
            stored = mapOf("gpu|scale" to "3"),
            specs = listOf(spec("gpu|scale", "2")),
            changes = listOf(DefaultChange("gpu|scale", "2026-08-25", "1")),
            watermark = null,
        )
        assertEquals("3", r.values["gpu|scale"])
        assertTrue(r.moved.isEmpty())
        assertEquals(listOf("gpu|scale"), r.kept)
    }

    @Test
    fun a_change_at_or_below_the_watermark_is_not_reapplied() {
        val r = DefaultMigration.migrate(
            stored = mapOf("a" to "old"),
            specs = listOf(spec("a", "new")),
            changes = listOf(DefaultChange("a", "2026-08-01", "old")),
            watermark = "2026-08-01",
        )
        assertEquals("old", r.values["a"])
        assertTrue(r.moved.isEmpty())
    }

    /**
     * Two changes to one key. A user on the ORIGINAL default must walk both
     * steps, which is why changes are applied oldest first rather than in the
     * order they were declared.
     */
    @Test
    fun a_key_whose_default_changed_twice_walks_both_steps() {
        val r = DefaultMigration.migrate(
            stored = mapOf("a" to "v1"),
            specs = listOf(spec("a", "v3")),
            changes = listOf(
                // Declared newest-first on purpose.
                DefaultChange("a", "2026-08-20", "v2"),
                DefaultChange("a", "2026-08-10", "v1"),
            ),
            watermark = null,
        )
        assertEquals("v3", r.values["a"])
        assertEquals("2026-08-20", r.watermark)
    }

    /**
     * A user who took the intermediate default deliberately is still moved by
     * the later change, because their stored value equals that step's previous
     * default. This is a real limit of the mechanism, pinned so it is a known
     * behaviour rather than a surprise.
     */
    @Test
    fun the_mechanism_cannot_tell_a_chosen_value_from_a_matching_old_default() {
        val r = DefaultMigration.migrate(
            stored = mapOf("a" to "v2"),
            specs = listOf(spec("a", "v3")),
            changes = listOf(DefaultChange("a", "2026-08-20", "v2")),
            watermark = null,
        )
        assertEquals("v3", r.values["a"])
    }

    /** Never stored means already resolving to the current default. */
    @Test
    fun an_absent_key_is_not_written() {
        val r = DefaultMigration.migrate(
            stored = emptyMap(),
            specs = listOf(spec("a", "new")),
            changes = listOf(DefaultChange("a", "2026-08-25", "old")),
            watermark = null,
        )
        assertTrue(r.values.isEmpty())
        assertTrue(r.moved.isEmpty())
    }

    /** A change for a key the schema no longer declares must not resurrect it. */
    @Test
    fun a_change_for_a_removed_key_is_ignored() {
        val r = DefaultMigration.migrate(
            stored = mapOf("gone" to "old"),
            specs = emptyList(),
            changes = listOf(DefaultChange("gone", "2026-08-25", "old")),
            watermark = null,
        )
        assertEquals("old", r.values["gone"])
        assertTrue(r.moved.isEmpty())
    }

    @Test
    fun a_fresh_install_with_no_changes_reports_no_watermark() {
        val r = DefaultMigration.migrate(emptyMap(), emptyList(), emptyList(), null)
        assertNull(r.watermark)
        assertTrue(r.moved.isEmpty())
        assertTrue(r.kept.isEmpty())
    }

    /**
     * The watermark advances past changes that moved nothing, so a later run
     * does not re-examine them.
     */
    @Test
    fun the_watermark_advances_even_when_nothing_moved() {
        val r = DefaultMigration.migrate(
            stored = mapOf("a" to "chosen"),
            specs = listOf(spec("a", "new")),
            changes = listOf(DefaultChange("a", "2026-08-25", "old")),
            watermark = null,
        )
        assertEquals("2026-08-25", r.watermark)
        assertTrue(r.moved.isEmpty())
    }
}
