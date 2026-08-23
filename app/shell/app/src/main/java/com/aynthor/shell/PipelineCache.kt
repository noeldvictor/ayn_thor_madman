package com.aynthor.shell

/**
 * The driver pipeline cache policy.
 *
 * **Extraction candidate 3, and the first genuine 8-for-8 duplication in the
 * fleet.** Verified 2026-08-23: every fork calls `vkGetPipelineCacheData` in its
 * own file — ARMSX2 `VKShaderCache.cpp`, xenia `vulkan_pipeline_cache.cc`, Cemu
 * `VulkanRenderer.cpp`, azahar `vk_pipeline_cache.cpp`, melonDS
 * `GPU3D_Vulkan.cpp`, Vita3K `pipeline_cache.cpp`, eden `vulkan_wrapper.cpp`,
 * rpcsx `VKPipelineCompiler.cpp`.
 *
 * **This owns the driver blob only.** The guest-shader-to-SPIR-V cache stays
 * with the backend: it is keyed on a guest source hash and is guest knowledge.
 * The two differ usefully — **the driver blob dies on a driver swap and the
 * translation cache survives it**, because SPIR-V is portable.
 *
 * **What is here is policy, not Vulkan.** Validation, naming, retention and
 * pruning are pure decisions, so they are testable without a device or a driver.
 * The native side is declared in `shared_layer/pipeline_cache.h`.
 */

/** The 32-byte header every Vulkan pipeline cache blob begins with. */
data class PipelineCacheHeader(
    val headerLength: Int,
    val headerVersion: Int,
    val vendorId: Int,
    val deviceId: Int,
    val uuid: List<Byte>,
) {
    companion object {
        /** `VK_PIPELINE_CACHE_HEADER_VERSION_ONE`. The only version that exists. */
        const val VERSION_ONE = 1

        /** `VK_UUID_SIZE`. */
        const val UUID_SIZE = 16

        /** Four `uint32` plus the UUID. */
        const val MIN_LENGTH = 32
    }
}

/** What the device reports today. */
data class DeviceIdentity(
    val vendorId: Int,
    val deviceId: Int,
    val uuid: List<Byte>,
)

/**
 * Why a cache blob was refused.
 *
 * **Every reason is separate on purpose.** ARMSX2 logs which field disagreed,
 * and that is the difference between "the cache did not load" and a diagnosis.
 * A single boolean would make a driver swap and an app update look identical.
 */
enum class CacheVerdict {
    ACCEPT,
    HEADER_TOO_SHORT,
    BAD_HEADER_VERSION,
    VENDOR_MISMATCH,
    DEVICE_MISMATCH,
    /** The driver's compiled format changed. The Vulkan specification's own signal. */
    UUID_MISMATCH,
    /**
     * The app changed, and the device header cannot see that.
     *
     * **This is the bug ARMSX2 shipped and fixed**, recorded in its own comment:
     * `vendorID`, `deviceID` and `pipelineCacheUUID` are **identical across an
     * app update on the same phone**, so bumping the shader-cache version wiped
     * the SPIR-V while keeping every pipeline built from the previous build's
     * shaders. Nothing pruned the blob, and it was re-serialised in full every
     * N compiles on the render thread, so the dead entries became **an
     * ever-growing mid-gameplay stall that only a clean reinstall cleared** —
     * users reported "clean install improved performance".
     */
    EPOCH_MISMATCH,
}

object PipelineCachePolicy {

    /**
     * Whether a blob may be handed to `vkCreatePipelineCache`.
     *
     * **Order matters.** The cheapest and most fundamental checks run first, so
     * a truncated file never has its fields read.
     */
    fun validate(
        header: PipelineCacheHeader,
        device: DeviceIdentity,
        fileEpoch: Long,
        appEpoch: Long,
    ): CacheVerdict {
        if (header.headerLength < PipelineCacheHeader.MIN_LENGTH) return CacheVerdict.HEADER_TOO_SHORT
        if (header.headerVersion != PipelineCacheHeader.VERSION_ONE) return CacheVerdict.BAD_HEADER_VERSION
        if (header.vendorId != device.vendorId) return CacheVerdict.VENDOR_MISMATCH
        if (header.deviceId != device.deviceId) return CacheVerdict.DEVICE_MISMATCH
        if (header.uuid != device.uuid) return CacheVerdict.UUID_MISMATCH
        if (fileEpoch != appEpoch) return CacheVerdict.EPOCH_MISMATCH
        return CacheVerdict.ACCEPT
    }

    /**
     * The file a blob lives in.
     *
     * **Named by `pipelineCacheUUID`, never by a driver package name.** A driver
     * can change its compiled format without changing its version string, and
     * the specification says the UUID is what moves. Keying on anything else
     * silently feeds stale blobs to a new compiler.
     *
     * **And naming by UUID is what makes a per-game driver override survivable.**
     * With one shared cache file, switching driver for one game would discard the
     * warm cache for **every backend at once**. With one file per UUID, switching
     * back finds the old file intact.
     */
    fun fileName(uuid: List<Byte>, epoch: Long): String {
        require(uuid.size == PipelineCacheHeader.UUID_SIZE) { "a pipelineCacheUUID is 16 bytes" }
        val hex = uuid.joinToString("") { "%02x".format(it) }
        return "pipeline-$hex-$epoch.bin"
    }

    /**
     * Which cache files to delete, given every file present.
     *
     * **Keeps the current one and the [keep] most recent others.** Switching
     * driver and switching back is a normal thing to do while testing a per-game
     * override, and it should not cost a cold cache both ways.
     *
     * Input is newest-first. Returns the files to remove.
     */
    fun evict(newestFirst: List<String>, current: String, keep: Int = 2): List<String> {
        require(keep >= 0) { "keep must not be negative" }
        val others = newestFirst.filter { it != current }
        return others.drop(keep)
    }

    /**
     * Whether the blob has grown past its cap.
     *
     * **A pipeline cache with no cap is the ARMSX2 bug waiting to happen.** The
     * blob only ever grows, and it is re-serialised in full, so an unbounded one
     * turns into a stall that scales with how long the game has been played.
     *
     * **Over the cap the answer is to discard, not to trim.** A pipeline cache
     * blob is opaque: there is no supported way to remove one entry, so the only
     * available action is to start again.
     */
    fun overCap(sizeBytes: Long, capBytes: Long): Boolean {
        require(capBytes > 0) { "a cap must be positive" }
        return sizeBytes > capBytes
    }

    /**
     * Whether this verdict means the *translation* cache should also be dropped.
     *
     * **Only a UUID change does.** The driver blob dies when the driver's
     * compiled format changes; the guest-shader-to-SPIR-V cache survives it,
     * because SPIR-V is portable. **Discarding both on a driver swap throws away
     * work that is still valid.**
     *
     * An epoch mismatch is the opposite case: the app changed, so the SPIR-V may
     * be stale and **both** must go — which is exactly the direction ARMSX2 got
     * wrong before it was fixed.
     */
    fun alsoDropTranslationCache(verdict: CacheVerdict): Boolean = when (verdict) {
        CacheVerdict.EPOCH_MISMATCH -> true
        else -> false
    }
}
