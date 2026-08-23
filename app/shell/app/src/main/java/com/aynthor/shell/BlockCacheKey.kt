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

// ---------------------------------------------------------------------------
// Persisting a block cache, taken from ARMSX2. See
// research_log/20260823_2205_translate_once_ship_it.md.
// ---------------------------------------------------------------------------

/**
 * The options snapshot a persisted artifact is keyed on.
 *
 * **Taken from ARMSX2's `mVUbuildOptionsSentinel`**, which is a 64-byte
 * fixed-layout snapshot of every option that changes emitted code: eleven
 * codegen switches, eight clamp modes, eight speedhacks, three FPCR masks, and
 * the recording flag itself — because recording changes the emitted forms.
 *
 * **Three rules travel with it and all three are here.**
 *
 * 1. **The layout is fixed and its size is asserted**, so drift is caught at the
 *    point of change rather than by a cache that silently mismatches.
 * 2. **A reserved tail**, so adding an option does not shift the fields below it
 *    and invalidate every existing artifact.
 * 3. **A reserved byte is reclaimed only where zero means "feature off"**, so the
 *    off-state sentinel stays bit-identical and **enabling a feature does not
 *    evict the cache of every user who never turns it on.**
 *
 * **Rule 3 is the one this project did not have.** It is the difference between
 * shipping a feature and shipping a feature that costs every user a cold cache.
 */
data class OptionsSentinel(
    /** Bumped when the layout itself changes. A mismatch evicts, it never merges. */
    val abiVersion: Int,
    /** Which independent engine this belongs to. Guards against sharing across units. */
    val unitIndex: Int,
    /** Every option that changes emitted code, in a fixed order. */
    val options: List<Byte>,
) {
    init {
        require(options.size == SLOTS) {
            "sentinel layout drifted: expected $SLOTS slots, got ${options.size}. " +
                "Bump abiVersion in the same change."
        }
    }

    /** The bytes an artifact is keyed on. Order is load-bearing. */
    fun bytes(): List<Byte> =
        listOf(abiVersion.toByte(), unitIndex.toByte()) + options

    companion object {
        /**
         * The fixed slot count.
         *
         * **ARMSX2 asserts `sizeof(Snapshot) == 64` for the same reason.** A
         * slot that is not yet used holds zero, which is what makes rule 3 work.
         */
        const val SLOTS = 62

        /**
         * Build a sentinel from the options that are set, leaving the rest zero.
         *
         * **A slot absent from [set] is zero, and zero must mean "off, old
         * behaviour".** An option whose off state is not emission-identical to
         * not having the option at all cannot use a reserved slot; it needs an
         * [abiVersion] bump instead.
         */
        fun of(abiVersion: Int, unitIndex: Int, set: Map<Int, Byte>): OptionsSentinel {
            require(set.keys.all { it in 0 until SLOTS }) { "slot out of range" }
            val slots = MutableList<Byte>(SLOTS) { 0 }
            set.forEach { (slot, value) -> slots[slot] = value }
            return OptionsSentinel(abiVersion, unitIndex, slots)
        }
    }
}

/**
 * The key a **persisted** block is stored under.
 *
 * **[BlockCacheKey] cannot be persisted, and that is the point of this type.**
 * It keys on `guestAddress`, which is where the block happened to live in this
 * process. A cache that outlives the process must key on **what the block is**,
 * not where it was — ARMSX2 keys its `.vuprog` payloads on a content hash for
 * exactly this reason, and eden keys its NCE patches on the 32-byte NSO build
 * ID.
 *
 * **The address does not disappear; it moves.** It becomes a relocation input
 * rather than part of the identity, which is what ARMSX2's placement-relative
 * fixup table exists to apply.
 */
data class PersistentBlockKey(
    /** Hash of the guest bytes this block was compiled from. */
    val contentHash: String,
    /** Every option that changed the emitted code. */
    val sentinel: OptionsSentinel,
) {
    /**
     * The on-disk path for this artifact, sharded by the first byte of the hash.
     *
     * **Sharding is not decoration.** ARMSX2 writes `<root>/<hh>/<hash>.vuprog`
     * because a flat directory of tens of thousands of files is slow to open on
     * a phone filesystem.
     */
    fun path(root: String, extension: String): String {
        require(contentHash.length >= 2) { "a content hash needs at least one shard byte" }
        return "$root/${contentHash.take(2)}/$contentHash$extension"
    }
}
