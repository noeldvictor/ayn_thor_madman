package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Admission is the machine-checkable half of BACKEND_STANDARD.md.
 *
 * The three fake backends exist to keep the contract honest, so they are the
 * first thing that must pass it.
 */
class BackendAdmissionTest {

    @Test
    fun the_three_fakes_are_admissible() {
        listOf(FakeMelonDs, FakeCemu, FakeArmsx2).forEach { backend ->
            assertEquals(
                "${backend.info.id} must pass admission",
                emptyList<AdmissionFault>(),
                BackendAdmission.admit(backend),
            )
        }
    }

    private fun backendWith(
        specs: List<SettingSpec>,
        defaults: Map<String, String>,
    ): Backend = object : Backend by FakeMelonDs {
        override fun settings(): List<SettingSpec> = specs
        override fun defaults(): Map<String, String> = defaults
    }

    private fun spec(key: String, default: String) = SettingSpec(
        key = key,
        label = key,
        type = SettingType.BOOL,
        group = "GPU",
        default = default,
    )

    @Test
    fun a_default_for_an_undeclared_key_is_a_fault() {
        val faults = BackendAdmission.admit(
            backendWith(
                specs = listOf(spec("gpu|vsync", "true")),
                defaults = mapOf("gpu|vsync" to "true", "gpu|ghost" to "1"),
            ),
        )
        assertEquals(1, faults.size)
        assertEquals("gpu|ghost", faults[0].key)
        assertTrue(faults[0].problem.contains("no setting declares"))
    }

    @Test
    fun a_declared_setting_with_no_default_is_a_fault() {
        val faults = BackendAdmission.admit(
            backendWith(
                specs = listOf(spec("gpu|vsync", "true"), spec("gpu|scale", "2")),
                defaults = mapOf("gpu|vsync" to "true"),
            ),
        )
        assertEquals(1, faults.size)
        assertEquals("gpu|scale", faults[0].key)
        assertTrue(faults[0].problem.contains("no default"))
    }

    /**
     * The second-writer bug. The settings screen reads SettingSpec.default and
     * the resolver falls through to defaults(); if they differ, both layers
     * behave correctly and the user sees one value while the guest runs
     * another.
     */
    @Test
    fun two_defaults_that_disagree_are_a_fault() {
        val faults = BackendAdmission.admit(
            backendWith(
                specs = listOf(spec("gpu|vsync", "true")),
                defaults = mapOf("gpu|vsync" to "false"),
            ),
        )
        assertEquals(1, faults.size)
        assertTrue(faults[0].problem.contains("the screen and the resolver would disagree"))
        assertTrue(faults[0].problem.contains("'false'"))
        assertTrue(faults[0].problem.contains("'true'"))
    }

    /** Schema defects reach admission rather than being checked separately. */
    @Test
    fun a_schema_defect_is_an_admission_fault() {
        val bad = SettingSpec(
            key = "gpu|cache",
            label = "Cache",
            type = SettingType.INT,
            group = "GPU",
            default = "384",
            min = 512.0,
            max = 4096.0,
        )
        val faults = BackendAdmission.admit(
            backendWith(listOf(bad), mapOf("gpu|cache" to "384")),
        )
        assertTrue(faults.any { it.problem.contains("coerced") })
    }

    @Test
    fun every_fault_is_reported_not_just_the_first() {
        val faults = BackendAdmission.admit(
            backendWith(
                specs = listOf(spec("a", "1"), spec("b", "2")),
                defaults = mapOf("a" to "9", "c" to "3"),
            ),
        )
        // a disagrees, c is undeclared, b has no default.
        assertEquals(3, faults.size)
        assertEquals(setOf("a", "b", "c"), faults.map { it.key }.toSet())
    }

    @Test
    fun isAdmissible_agrees_with_admit() {
        assertTrue(BackendAdmission.isAdmissible(FakeMelonDs))
        assertTrue(
            !BackendAdmission.isAdmissible(
                backendWith(listOf(spec("a", "1")), mapOf("a" to "2")),
            ),
        )
    }
}
