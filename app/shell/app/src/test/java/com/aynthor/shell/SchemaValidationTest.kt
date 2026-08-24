package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Schema integrity, with no device, no backend and no running game.
 *
 * Every case here is a bug somebody shipped. The headline one is XenDroid's:
 * its `texture_cache_memory_limit_soft` slider had a floor of 512 while the
 * real default was 384, so the widget SILENTLY COERCED every user's default
 * upward. Its own regression guard says a default outside [min, max] means
 * "the slider silently coerces the persisted default to a different value".
 *
 * That is DID_IT_APPLY mechanism 14 -- a substituted value -- arriving through
 * the UI instead of through an API, which is why the check belongs in the
 * schema rather than in a screen.
 */
class SchemaValidationTest {

    private fun intSpec(
        key: String,
        default: String,
        min: Double? = null,
        max: Double? = null,
    ) = SettingSpec(
        key = key,
        label = key,
        type = SettingType.INT,
        group = "GPU",
        default = default,
        min = min,
        max = max,
    )

    private fun enumSpec(key: String, default: String, options: List<String>) = SettingSpec(
        key = key,
        label = key,
        type = SettingType.ENUM,
        group = "GPU",
        default = default,
        options = options,
    )

    @Test
    fun a_clean_schema_has_no_defects() {
        val specs = listOf(
            intSpec("gpu|texture_cache_soft", "384", min = 384.0, max = 4096.0),
            enumSpec("gpu|filter", "linear", listOf("nearest", "linear")),
            SettingSpec(
                key = "gpu|vsync",
                label = "VSync",
                type = SettingType.BOOL,
                group = "GPU",
                default = "true",
            ),
        )
        assertEquals(emptyList<SchemaDefect>(), validateSchema(specs))
    }

    /** The XenDroid bug, reproduced against our own schema. */
    @Test
    fun a_default_below_its_own_floor_is_a_defect() {
        val defects = validateSchema(
            listOf(intSpec("gpu|texture_cache_soft", "384", min = 512.0, max = 4096.0)),
        )
        assertEquals(1, defects.size)
        assertEquals("gpu|texture_cache_soft", defects[0].key)
        assertTrue(defects[0].problem.contains("below min"))
        assertTrue(
            "the message must say what actually happens, not just that it is invalid",
            defects[0].problem.contains("coerced"),
        )
    }

    @Test
    fun a_default_above_its_own_ceiling_is_a_defect() {
        val defects = validateSchema(
            listOf(intSpec("cpu|threads", "16", min = 1.0, max = 8.0)),
        )
        assertEquals(1, defects.size)
        assertTrue(defects[0].problem.contains("above max"))
    }

    @Test
    fun inverted_bounds_are_a_defect() {
        val defects = validateSchema(
            listOf(intSpec("gpu|scale", "2", min = 8.0, max = 1.0)),
        )
        assertTrue(defects.any { it.problem.contains("min 8.0 > max 1.0") })
    }

    /** An unbounded numeric setting is legal: the backend declares no bound. */
    @Test
    fun a_numeric_setting_with_no_bounds_is_legal() {
        assertEquals(
            emptyList<SchemaDefect>(),
            validateSchema(listOf(intSpec("gpu|anything", "1234"))),
        )
    }

    @Test
    fun a_non_numeric_default_on_a_numeric_setting_is_a_defect() {
        val defects = validateSchema(listOf(intSpec("gpu|scale", "auto")))
        assertTrue(defects.any { it.problem.contains("not numeric") })
    }

    /**
     * XenDroid's `list_defaults_are_empty_or_a_member_of_options`. A default the
     * picker cannot display means the picker chooses something else.
     */
    @Test
    fun an_enum_default_outside_its_options_is_a_defect() {
        val defects = validateSchema(
            listOf(enumSpec("gpu|filter", "bilinear", listOf("nearest", "linear"))),
        )
        assertEquals(1, defects.size)
        assertTrue(defects[0].problem.contains("not one of its options"))
    }

    @Test
    fun an_enum_with_no_options_is_a_defect() {
        val defects = validateSchema(listOf(enumSpec("gpu|filter", "linear", emptyList())))
        assertTrue(defects.any { it.problem.contains("no options") })
    }

    /**
     * Resolution is by key, per-game over global. Two specs sharing a key make
     * that order undefined.
     */
    @Test
    fun a_duplicate_key_is_a_defect() {
        val defects = validateSchema(
            listOf(
                intSpec("gpu|scale", "2", min = 1.0, max = 4.0),
                intSpec("gpu|scale", "3", min = 1.0, max = 4.0),
            ),
        )
        assertTrue(defects.any { it.problem == "duplicate key" })
    }

    /** Bounds on a BOOL or ENUM mean somebody misread the type. */
    @Test
    fun bounds_on_a_non_numeric_setting_are_a_defect() {
        val defects = validateSchema(
            listOf(
                SettingSpec(
                    key = "gpu|vsync",
                    label = "VSync",
                    type = SettingType.BOOL,
                    group = "GPU",
                    default = "true",
                    min = 0.0,
                    max = 1.0,
                ),
            ),
        )
        assertTrue(defects.any { it.problem.contains("bounds on a BOOL") })
    }

    /**
     * The validator reports EVERY defect, so a backend author fixes one list
     * rather than iterating one failure at a time.
     */
    @Test
    fun every_defect_is_reported_not_just_the_first() {
        val defects = validateSchema(
            listOf(
                intSpec("a", "1", min = 5.0, max = 10.0),
                enumSpec("b", "x", listOf("y", "z")),
                intSpec("c", "99", min = 1.0, max = 8.0),
            ),
        )
        assertEquals(3, defects.size)
        assertEquals(listOf("a", "b", "c"), defects.map { it.key })
    }
}
