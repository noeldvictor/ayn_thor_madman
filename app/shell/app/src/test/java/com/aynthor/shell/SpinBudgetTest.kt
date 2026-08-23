package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The spin-budget suite.
 *
 * **Encodes the fleet-wide spin-wait audit** so the 32x hazard cannot be
 * reintroduced by somebody doing the obvious thing. See
 * `research_log/20260823_1454_spin_wait_audit.md`.
 *
 * The measured constants are rpcsx's, from `tools/bench/thor_bench.cpp` run on
 * this SoC. They are used here as fixtures, not re-derived.
 */
class SpinBudgetTest {

    /** Measured on the Thor's Snapdragon 8 Gen 2. */
    private val yieldNs = 0.36
    private val isbNs = 11.42

    @Test
    fun `yield and nop measure the same, which is the whole finding`() {
        // Pinned as a fixture so the number stays attached to the reasoning.
        // This project asserted it from the manual for months; rpcsx measured
        // it on the silicon.
        val nopNs = 0.36
        assertEquals(yieldNs, nopNs, 0.0)
    }

    @Test
    fun `ISB costs about 32 times a yield`() {
        // The single number that explains three separate regressions.
        val ratio = isbNs / yieldNs
        assertTrue("ratio was $ratio", ratio > 30 && ratio < 34)
    }

    // ------------------------------------------------------- the actual rule

    @Test
    fun `a time budget holds the backoff constant when the instruction changes`() {
        // The point of the whole design. Swap yield for ISB and the wait still
        // lasts the same wall-clock time, because the count is derived from the
        // measured cost rather than fixed.
        val budget = 2800 // ns, Cemu's x86 intent: 80 pauses at ~140 cycles
        val onYield = SpinBudget.steps(budget, yieldNs)
        val onIsb = SpinBudget.steps(budget, isbNs)

        val timeOnYield = onYield * yieldNs
        val timeOnIsb = onIsb * isbNs
        assertTrue(
            "expected similar total backoff, got $timeOnYield vs $timeOnIsb",
            abs(timeOnYield - timeOnIsb) < budget * 0.05,
        )
    }

    @Test
    fun `a fixed iteration count does not, and that is the regression`() {
        // The counter-example, kept as a test so the hazard is visible rather
        // than described. Holding the count and changing the instruction
        // multiplies the wait by 32 -- rpcsx measured +23% from exactly this.
        val fixedCount = 80
        val before = fixedCount * yieldNs
        val after = fixedCount * isbNs
        assertTrue("the wait grew ${after / before}x", after / before > 30)
    }

    // ------------------------------------------------------------ the edges

    @Test
    fun `a step costlier than the whole budget still spins once`() {
        // Returning zero would turn a spin into a bare retry loop with no
        // backoff at all, which is worse than either design.
        assertEquals(1, SpinBudget.steps(budgetNs = 5, measuredStepNs = isbNs))
    }

    @Test
    fun `a zero budget means do not spin`() {
        // Distinct from the case above: the caller asked for no spin, which is
        // a legitimate request and must not be rounded up to one.
        assertEquals(0, SpinBudget.steps(budgetNs = 0, measuredStepNs = yieldNs))
    }

    @Test
    fun `the step cost must be measured, never assumed`() {
        // A zero or negative cost means calibration did not run. Dividing by it
        // would produce an unbounded spin, so it fails loudly instead.
        assertThrows(IllegalArgumentException::class.java) {
            SpinBudget.steps(1000, 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpinBudget.steps(1000, -1.0)
        }
    }

    // ------------------------------------------------------- calibration

    @Test
    fun `calibration takes the minimum sample, not the mean`() {
        // A calibration run competes with everything else on the device, so a
        // high sample is contamination. ARMSX2 takes the min of five.
        val samples = listOf(11.4, 11.5, 84.0, 11.42, 12.0)
        assertEquals(11.4, SpinBudget.calibrate(samples), 1e-9)
    }

    @Test
    fun `one outlier cannot inflate the budget`() {
        // Without the min rule, an OS interrupt during calibration would make
        // every later spin far too short.
        val clean = SpinBudget.steps(2800, SpinBudget.calibrate(listOf(11.42)))
        val noisy = SpinBudget.steps(2800, SpinBudget.calibrate(listOf(11.42, 900.0)))
        assertEquals(clean, noisy)
    }

    @Test
    fun `a backoff step that measures as free is refused`() {
        // A zero would mean the timer is too coarse to see the step, not that
        // the step is free. Deriving a count from it gives an infinite spin.
        assertThrows(IllegalArgumentException::class.java) {
            SpinBudget.calibrate(listOf(0.0, 11.42))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpinBudget.calibrate(emptyList())
        }
    }
}
