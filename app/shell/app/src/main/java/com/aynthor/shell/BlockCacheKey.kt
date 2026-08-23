package com.aynthor.shell

/**
 * The key a recompiled block is cached under.
 *
 * **This exists because of a hazard, not because a key is hard.** See
 * `research_log/20260823_1520_cpu_leads_already_done.md`.
 *
 * A backend that bakes the guest floating-point environment into generated code
 * — which ARMSX2 does for EE `DIV`/`SQRT`, and which is the right way to avoid
 * reading `FPCR` in the hot path — has made that code **correct only for the
 * environment it was compiled under**. Reuse it after the guest changes
 * rounding mode or denormal handling and it silently computes the wrong
 * numbers.
 *
 * **ARMSX2 guards this with a sentinel that hashes all four FP environments
 * into mVU's block cache.** This is that rule, stated once for any backend that
 * bakes anything.
 *
 * **The general form: whatever a block bakes in must appear in its key.**
 */

/**
 * One guest floating-point environment.
 *
 * A guest may have several independent ones — the PS2 has three (the EE FPU and
 * two VUs), and the Xbox 360 has two (`FPSCR` for scalar, `VSCR.NJ` for VMX)
 * against ARM64's single `FPCR`.
 */
data class FpEnv(
    val roundToZero: Boolean,
    val flushDenormals: Boolean,
)

/**
 * A block cache key.
 *
 * [guestAddress] alone is what a naive cache uses, and it is the bug.
 * [bakedEnvs] carries every environment the generated code assumed.
 */
data class BlockCacheKey(
    val guestAddress: Long,
    val bakedEnvs: List<FpEnv>,
    /**
     * How many guest bytes this block was compiled from.
     *
     * Needed so a write into the middle of a block can be detected. A block
     * identified only by its start address cannot answer "did this write land
     * inside me".
     */
    val guestLength: Int = 1,
) {
    init {
        require(guestLength > 0) { "a block covers at least one byte" }
    }

    fun overlaps(address: Long, length: Int): Boolean =
        address < guestAddress + guestLength && guestAddress < address + length
}

object BlockCache {

    /**
     * Whether a cached block may be reused under the current environments.
     *
     * **Refuses on any difference.** A partial match is still wrong: the block
     * baked all of them, so all of them must agree.
     */
    fun canReuse(cached: BlockCacheKey, guestAddress: Long, envs: List<FpEnv>): Boolean =
        cached.guestAddress == guestAddress && cached.bakedEnvs == envs

    /**
     * Which cached blocks a write to guest memory invalidates.
     *
     * **This exists because of cheats, and it was found by reading them.**
     * VitaCheat has `$A000`/`$A100`/`$A200` — writes to ARM code, documented as
     * needing "JIT cache invalidation when bytes change". No other cheat format
     * in the fleet has that opcode, because Atmosphere's `dmnt` VM runs on real
     * Switch hardware where there is no recompiler to invalidate.
     *
     * **A cheat engine that only writes data does nothing on a recompiled
     * guest** — or worse, appears to work until the block is next compiled and
     * then starts working, which reads as a random failure.
     *
     * See `research_log/20260823_1620_cheat_formats_all_six.md`.
     */
    fun invalidatedBy(
        cached: Collection<BlockCacheKey>,
        address: Long,
        length: Int,
    ): List<BlockCacheKey> {
        require(length > 0) { "a write covers at least one byte" }
        return cached.filter { it.overlaps(address, length) }
    }
}

/**
 * What a cheat write targets.
 *
 * **The distinction is emulator knowledge, not guest knowledge.** On real
 * hardware a write is a write; on a recompiling emulator, a write to executable
 * memory has a second consequence.
 */
enum class WriteTarget { DATA, CODE }
