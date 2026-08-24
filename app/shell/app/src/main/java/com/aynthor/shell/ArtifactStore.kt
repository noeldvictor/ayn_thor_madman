package com.aynthor.shell

/**
 * The artifact store: one place for everything the app derives from a game.
 *
 * **The rule:** if an artifact's output is a pure function of the guest content
 * and the host configuration, **it should be computed once for everybody**, not
 * once per launch per user. See `shared_layer/ARTIFACT_STORE.md`.
 *
 * **This is policy, and every rule below was paid for by a fork.** Four forks
 * already persist derived artifacts and none cites another: rpcs3's
 * `ppu-<sha1>` LLVM object cache, ARMSX2's `.vuprog` VU programs, melonDS's
 * self-populating texture pack, and Cemu's shader-cache merger.
 */

/**
 * What an artifact is derived from, which decides what invalidates it.
 *
 * **The split is not tidiness. It is the whole layout.** Cemu ships exactly
 * this as `shaderCache/transferable` against `shaderCache/precompiled`, and
 * `PipelineCache.kt` reached it independently.
 */
enum class ArtifactTier {
    /**
     * Survives a driver swap. Guest content plus this app's own options.
     *
     * Translated code, guest-shader-to-SPIR-V, AOT module patches, upscaled
     * textures.
     */
    PORTABLE,

    /**
     * Dies when the driver changes, because the driver compiled it.
     *
     * Only the Vulkan pipeline blob. **It is also the tier that costs the
     * compile stall**, and the one a pinned driver makes shareable between
     * users — which nothing else in emulation can do.
     */
    DRIVER_BOUND,
}

/** Why the store refused or dropped an artifact. Never silence. */
enum class StoreVerdict {
    HIT,
    MISS,
    /** The options that produced it differ from the options now in force. */
    STALE_OPTIONS,
    /** The driver changed under a [ArtifactTier.DRIVER_BOUND] artifact. */
    DRIVER_CHANGED,
    /** The payload failed its own integrity check. */
    CORRUPT,
    /** The store is over its cap and this entry was discarded whole. */
    EVICTED,
}

/**
 * One artifact's identity.
 *
 * **Content plus options, never a path and never a runtime address.**
 * EmulationStation keys metadata on the file path and this project rejected
 * that; the same reasoning applies here, and more sharply, because an artifact
 * outlives the process that made it.
 */
data class ArtifactKey(
    val contentHash: String,
    val sentinel: OptionsSentinel,
    val tier: ArtifactTier,
    /**
     * Identifies the driver, for [ArtifactTier.DRIVER_BOUND] only.
     *
     * **Use `pipelineCacheUUID`, not a driver package name.** A driver can
     * change its compiled format without changing its version string; the
     * Vulkan specification says the UUID is what changes.
     */
    val driverUuid: String? = null,
) {
    init {
        require(contentHash.length >= 2) { "a content hash needs at least one shard byte" }
        if (tier == ArtifactTier.DRIVER_BOUND) {
            require(driverUuid != null) { "a driver-bound artifact must name its driver" }
        }
    }

    /** Sharded, because a flat directory of tens of thousands of files opens slowly. */
    fun path(root: String, extension: String): String =
        "$root/${tier.name.lowercase()}/${contentHash.take(2)}/$contentHash$extension"
}

object ArtifactStorePolicy {

    /** Whether a stored artifact may be used now. */
    fun lookup(
        stored: ArtifactKey?,
        wantSentinel: OptionsSentinel,
        currentDriverUuid: String?,
    ): StoreVerdict = when {
        stored == null -> StoreVerdict.MISS
        stored.sentinel.bytes() != wantSentinel.bytes() -> StoreVerdict.STALE_OPTIONS
        stored.tier == ArtifactTier.DRIVER_BOUND && stored.driverUuid != currentDriverUuid ->
            StoreVerdict.DRIVER_CHANGED
        else -> StoreVerdict.HIT
    }

    /**
     * A driver change drops only the driver-bound tier.
     *
     * **The portable tier survives**, because SPIR-V and translated guest code do
     * not care which driver is loaded. Dropping both is the bug this rule exists
     * to prevent: it would discard hours of work to swap one driver.
     */
    fun tiersInvalidatedByDriverChange(): Set<ArtifactTier> = setOf(ArtifactTier.DRIVER_BOUND)

    /**
     * Whether the store may warm on the thread that paints.
     *
     * **It may not, and this is a shipped bug.** xenia's AOT precompile ran at
     * 85 functions/sec while **the UI thread blocked in the paint path**, so its
     * progress overlay could not draw *even though its logic was correct*.
     * **Android fired an ANR at 18 seconds and the user force-closed the app
     * mid-compile**, believing it had hung.
     */
    const val MAY_WARM_ON_UI_THREAD = false

    /**
     * Whether progress may be reported per unit rather than cumulatively.
     *
     * **It may not.** xenia's native estimate **grew from 6,665 to 10,540
     * mid-module** and both counters **reset per module**, so a raw done-over-total
     * bar jumped backwards — **which reads as a hang even when nothing is wrong.**
     */
    const val PROGRESS_MUST_BE_CUMULATIVE = true

    /**
     * Whether the user must be warned that the platform may accuse the app of hanging.
     *
     * **Yes, and before the dialog appears.** Android offers "Close app"
     * unprompted, and a person who has not been told will take it.
     */
    const val MUST_WARN_ABOUT_PLATFORM_ANR = true

    /**
     * Whether a non-empty store counts as evidence the store is working.
     *
     * **It does not.** xenia's object cache held **111 MB** and was **off for
     * every launch a person performs**, because its enabling block was guarded on
     * *no cvar bundle supplied* and the launcher always supplies one. Headless
     * runs filled the cache; real launches recompiled ~10,000 functions.
     *
     * **The acceptance test is a counted cold-build of zero**, on the launch path
     * people actually use.
     */
    const val NON_EMPTY_DIRECTORY_PROVES_NOTHING = true

    /**
     * What to do when the store exceeds its cap.
     *
     * **Discard whole entries; never truncate one.** A payload is opaque — a
     * driver blob especially — so a partially written entry is indistinguishable
     * from a valid one until it is used.
     */
    fun evict(
        entries: List<Pair<ArtifactKey, Long>>,
        capBytes: Long,
    ): List<ArtifactKey> {
        require(capBytes >= 0) { "a cap is not negative" }
        var total = entries.sumOf { it.second }
        val dropped = mutableListOf<ArtifactKey>()
        // Oldest-listed first; the caller orders by its own recency.
        for ((key, size) in entries) {
            if (total <= capBytes) break
            dropped += key
            total -= size
        }
        return dropped
    }

    /**
     * Whether a payload must be written to a temporary name and renamed.
     *
     * **Yes.** A process killed mid-write must not leave a half payload that
     * looks whole. ARMSX2 writes its `.vuprog` files tmp-then-rename.
     */
    const val WRITE_TMP_THEN_RENAME = true
}
