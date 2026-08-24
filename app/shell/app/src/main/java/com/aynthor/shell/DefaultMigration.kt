package com.aynthor.shell

/**
 * Moving users who never chose a value onto a changed default.
 *
 * THE HAZARD, which this project records with a measured cost. `CLAUDE.md`:
 * "a persisted value overrides a compiled default forever -- across the
 * process, the install and the app update". xenia found three validated
 * `rlwinm` fastpaths sitting false on a device whose code said true, worth
 * +2.88%, and concluded that "every device number taken this session was on a
 * handicapped baseline".
 *
 * Shipping a better default reaches only NEW installs unless something moves
 * the people already holding the old one.
 *
 * THE MECHANISM is XenDroid's, which spells it
 * `UPDATE_from_string(turnip_debug, 2026, 7, 24, 12, "")`: at this stamp the
 * default changed, and the OLD default was this value.
 *
 * The rule that makes it safe is the second half. A stored value that still
 * equals the old default means the person never chose; anything else means
 * they did, and their choice is left alone.
 *
 * This is NOT the schema migration this project took from melonDS. That one
 * migrates a SHAPE -- fields appearing, moving, changing type. This migrates a
 * VALUE. They are complementary and a settings system needs both.
 */
data class DefaultChange(
    val key: String,
    /**
     * Sortable stamp, e.g. "2026-08-25T14:00". Compared as a string, so any
     * fixed-width sortable format works and a mixed set does not.
     */
    val at: String,
    /** The default this key had BEFORE [at]. */
    val previousDefault: String,
)

data class MigrationResult(
    val values: Map<String, String>,
    /** Highest stamp applied, to persist as the new watermark. */
    val watermark: String?,
    /** Keys actually moved, for a log line and for a test. */
    val moved: List<String>,
    /** Keys skipped because the person had chosen. Not a fault -- the point. */
    val kept: List<String>,
)

object DefaultMigration {

    /**
     * @param stored what the person's config holds today, sparse.
     * @param specs the current schema, whose [SettingSpec.default] is the NEW
     *   default.
     * @param changes every default change ever shipped, in any order.
     * @param watermark the highest stamp already applied, or null for a fresh
     *   install.
     */
    fun migrate(
        stored: Map<String, String>,
        specs: List<SettingSpec>,
        changes: List<DefaultChange>,
        watermark: String?,
    ): MigrationResult {
        val byKey = specs.associateBy { it.key }
        val values = stored.toMutableMap()
        val moved = mutableListOf<String>()
        val kept = mutableListOf<String>()
        var highest = watermark

        // Oldest first: a key whose default changed twice must walk both steps,
        // or a user on the ORIGINAL default is stranded by the second change.
        for (change in changes.sortedBy { it.at }) {
            if (highest != null && change.at <= highest) continue
            if (change.at > (highest ?: "")) highest = change.at

            val spec = byKey[change.key] ?: continue
            // Absent means the person never stored anything, so they already
            // resolve to the current default. Nothing to move.
            val current = values[change.key] ?: continue

            if (current == change.previousDefault) {
                if (spec.default != current) {
                    values[change.key] = spec.default
                    moved.add(change.key)
                }
            } else {
                kept.add(change.key)
            }
        }
        return MigrationResult(values, highest, moved, kept)
    }
}
