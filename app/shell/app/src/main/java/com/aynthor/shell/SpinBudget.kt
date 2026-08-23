package com.aynthor.shell

/**
 * How long a busy-wait should spin before it hands the core back.
 *
 * **This is ARMSX2's design, extracted as the shared rule.** See
 * `research_log/20260823_1454_spin_wait_audit.md`.
 *
 * **The problem it solves.** On this SoC `YIELD` costs 0.36 ns and `ISB` costs
 * 11.42 ns, measured. That is **32x**. A spin loop written as "do the backoff
 * 80 times" therefore changes its total backoff by 32x the moment somebody
 * swaps the instruction, and three forks measured that regression separately:
 * rpcsx **+23%**, xenia **CONFOUNDED**, and Cemu's attempt to make the same
 * loop time-based came out **worse on the Thor**.
 *
 * **The fix is to budget in time and to measure the step cost on the host that
 * booted**, rather than to assume either one. ARMSX2 does exactly this: it
 * times its own backoff at startup, takes the minimum of several samples, and
 * derives the iteration count from a nanosecond target.
 *
 * **What this deliberately does not do.** It does not pick a target for a
 * caller. Cemu's Latte command processor measured *shorter* as better, because
 * falling through to `sched_yield` sooner beat burning the core. **The right
 * budget is per loop and must be measured**, which is why [budgetNs] is an
 * argument and not a constant.
 */
object SpinBudget {

    /**
     * How many backoff steps fit in [budgetNs], given a measured step cost.
     *
     * Returns at least 1: a caller that asked to spin should spin once, even
     * when the step is more expensive than the whole budget. Returning 0 would
     * turn a spin into a bare retry loop with no backoff at all, which is the
     * worst of both designs.
     */
    fun steps(budgetNs: Int, measuredStepNs: Double): Int {
        require(budgetNs >= 0) { "budget must not be negative" }
        require(measuredStepNs > 0.0) { "step cost must be measured, not assumed" }
        if (budgetNs == 0) return 0
        return maxOf(1, (budgetNs / measuredStepNs).toInt())
    }

    /**
     * Picks the lowest of several timing samples.
     *
     * **The minimum, not the mean.** A calibration run competes with everything
     * else on the device, so a high sample is contamination rather than signal.
     * ARMSX2 takes the min of five for this reason.
     */
    fun calibrate(samplesNs: List<Double>): Double {
        require(samplesNs.isNotEmpty()) { "no calibration samples" }
        val best = samplesNs.min()
        require(best > 0.0) { "a backoff step cannot measure as free" }
        return best
    }
}
