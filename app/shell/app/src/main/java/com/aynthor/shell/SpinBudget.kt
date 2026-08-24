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

    // ---------------------------------------------------- when to PARK instead

    /**
     * The measured `WFE` park wake floor on this SoC, in nanoseconds.
     *
     * `FEAT_WFxT` is absent here, so a `WFE` cannot carry its own timeout and a
     * park waits for the architected event stream. Three numbers agree on the
     * scale: the stream fires about every 100 us on Linux and Android against
     * about 1 us on Apple; an armed `WFE` measured 72,024 ns; and the break-even
     * calculation uses 95,060 ns.
     *
     * All three are rpcsx's measurements on this SoC, not reproduced here.
     */
    const val WFE_WAKE_FLOOR_NS = 95_060L

    /**
     * Is parking worth it, given how long this wait is expected to last?
     *
     * PARKING PAYS ONLY WHEN THE EXPECTED REMAINING WAIT EXCEEDS THE WAKE FLOOR.
     * rpcsx's GETLLAR site: one backoff is 15.62 us, so break-even is depth 6.1
     * and its threshold is 8 -- and 95.5% of its spins are shallower than that,
     * which is an argument FOR the threshold, because parking would make exactly
     * those worse.
     *
     * Another fork learned the same thing the expensive way: it parked bare in
     * an RSX FIFO idle wait and had to revert when the wake latency cost
     * frame-time smoothness. The call site now pre-spins eight times. A pre-spin
     * is not a tuning detail; it is that reverted experiment, encoded.
     */
    fun worthParking(expectedRemainingSpinNs: Long): Boolean =
        expectedRemainingSpinNs > WFE_WAKE_FLOOR_NS

    /**
     * A park must not cost a syscall.
     *
     * "Every park measured here that traded a spin for a syscall lost."
     * `SEVL`/`WFE` is in-band; a futex is not.
     */
    const val PARK_MUST_NOT_COST_A_SYSCALL = true

    /**
     * THE STAMPEDE, and it is the reason this is a policy and not a helper.
     *
     * A `WFE` park wakes EVERY waiting core at once. Arm's own wording: the
     * periodic event stream "wakes up all processors waiting in WFE at the same
     * time which would amplify contention."
     *
     * One thread parking is a weak case for that. Every guest in this fleet is
     * multi-core -- PS2 has EE, IOP and two VUs, Wii U three, Switch four, the
     * 360 six hardware threads -- so a shared policy that parks every guest
     * thread on one event stream is the amplification case rather than the win.
     *
     * The number is deliberately conservative and deliberately not measured:
     * nobody has swept it. It exists so a shared spin policy has to state a
     * bound rather than park everything by default.
     */
    const val MAX_THREADS_PARKED_ON_EVENT_STREAM = 1

    /** May this thread park, given how many already are? */
    fun mayPark(expectedRemainingSpinNs: Long, threadsAlreadyParked: Int): Boolean =
        worthParking(expectedRemainingSpinNs) &&
            threadsAlreadyParked < MAX_THREADS_PARKED_ON_EVENT_STREAM
}
