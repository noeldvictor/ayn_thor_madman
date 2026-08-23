package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * rpcsx's driver-advisor idea, propagated.
 *
 * **The names in these tests are real files sitting on the device**, recorded
 * in CLAUDE.md. Two of them are wrong-GPU drivers.
 *
 * Part of the lesson suite. See shared_layer/PROPAGATION.md item 13.
 */
class DriverAdvisorTest {

    @Test
    fun `the a8xx drivers actually on this device are rejected`() {
        // CLAUDE.md: these are in the Thor's storage right now. They target
        // a8xx, the Snapdragon 8 Elite generation. The Thor is a740 = a7xx.
        for (name in listOf("Turnip_Gen8_V33.zip", "a8xx-gen8-V24.zip")) {
            val a = DriverAdvisor.assess(name)
            assertEquals("$name must be rejected", Verdict.INCOMPATIBLE, a.verdict)
            assertTrue(
                "the reason must name the family: ${a.reason}",
                a.reason.contains("a8xx"),
            )
        }
    }

    @Test
    fun `Qualcomm marketing names are decoded`() {
        // The whole point. "Gen 8" never says a8xx, and a picker matching only
        // "a8xx" would install it.
        assertTrue("Gen8 must resolve to a8xx",
            "a8xx" in DriverAdvisor.claimedFamilies("Turnip_Gen8_V33"))
        assertTrue("an explicit family still works",
            "a7xx" in DriverAdvisor.claimedFamilies("turnip_a7xx_build"))
        assertTrue("an Adreno model resolves to its family",
            "a7xx" in DriverAdvisor.claimedFamilies("Adreno 740 driver"))
    }

    @Test
    fun `the provisional pin is not rejected`() {
        // turnip_mrpurple_T30-toasted is CLAUDE.md's provisional pin. It names
        // no family and no Mesa version, so the honest verdict is RISKY —
        // not COMPATIBLE, because nothing in the name proves it.
        val a = DriverAdvisor.assess("turnip_mrpurple_T30-toasted.adpkg.zip")
        assertTrue("must not be rejected outright", a.verdict != Verdict.INCOMPATIBLE)
        assertEquals(Verdict.RISKY, a.verdict)
        assertTrue("the reason must admit what it cannot verify: ${a.reason}",
            a.reason.contains("no GPU family"))
    }

    @Test
    fun `a recent Mesa turnip build for this family is compatible`() {
        val a = DriverAdvisor.assess("mesa-turnip-a7xx-v26.3.0-20260803-r7.zip")
        assertEquals(a.reason, Verdict.COMPATIBLE, a.verdict)
    }

    @Test
    fun `a Mesa too old for a7xx is rejected`() {
        val a = DriverAdvisor.assess("mesa-turnip-a7xx-v21.2.0.zip")
        assertEquals(Verdict.INCOMPATIBLE, a.verdict)
        assertTrue(a.reason.contains("Mesa"))
    }

    @Test
    fun `something that is not a driver at all is not called compatible`() {
        assertTrue(
            DriverAdvisor.assess("my_holiday_photos.zip").verdict != Verdict.COMPATIBLE,
        )
    }

    @Test
    fun `every verdict carries a reason`() {
        // A verdict with no reason is a list with extra steps, which is the
        // thing rpcsx's design exists to replace.
        for (name in listOf("Turnip_Gen8_V33.zip", "turnip_T30.zip",
                            "mesa-turnip-a7xx-v26.3.0.zip", "random.bin")) {
            assertTrue("$name produced an empty reason",
                DriverAdvisor.assess(name).reason.isNotBlank())
        }
    }
}
