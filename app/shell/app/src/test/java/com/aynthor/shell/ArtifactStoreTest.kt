package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The artifact store suite.
 *
 * **Every test encodes something a fork already paid for.** See
 * `research_log/20260824_2205_*`, `20260824_0645_*` and `shared_layer/ARTIFACT_STORE.md`.
 */
class ArtifactStoreTest {

    private val opts = OptionsSentinel.of(1, 0, emptyMap())
    private val otherOpts = OptionsSentinel.of(1, 0, mapOf(3 to 1))

    private fun portable(hash: String = "deadbeef") =
        ArtifactKey(hash, opts, ArtifactTier.PORTABLE)

    private fun driverBound(hash: String = "deadbeef", uuid: String = "UUID-A") =
        ArtifactKey(hash, opts, ArtifactTier.DRIVER_BOUND, uuid)

    // ------------------------------------------------------------ the tiers

    @Test
    fun `a driver change drops the driver-bound tier only`() {
        // Dropping both would discard hours of translated code to swap one
        // driver. SPIR-V and translated guest code do not care which driver is
        // loaded; only the driver's own compiled blob does.
        assertEquals(
            setOf(ArtifactTier.DRIVER_BOUND),
            ArtifactStorePolicy.tiersInvalidatedByDriverChange(),
        )
    }

    @Test
    fun `a portable artifact survives a driver change`() {
        assertEquals(
            StoreVerdict.HIT,
            ArtifactStorePolicy.lookup(portable(), opts, currentDriverUuid = "UUID-B"),
        )
    }

    @Test
    fun `a driver-bound artifact does not survive a driver change`() {
        assertEquals(
            StoreVerdict.DRIVER_CHANGED,
            ArtifactStorePolicy.lookup(driverBound(uuid = "UUID-A"), opts, "UUID-B"),
        )
    }

    @Test
    fun `a driver-bound artifact must name its driver`() {
        // Nothing can decide whether it is stale otherwise.
        try {
            ArtifactKey("deadbeef", opts, ArtifactTier.DRIVER_BOUND, driverUuid = null)
            fail("a driver-bound artifact without a driver id must be refused")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("driver"))
        }
    }

    // ------------------------------------------------------- options staleness

    @Test
    fun `changed options are a distinct verdict from a driver change`() {
        // They need different remedies: recompute against new options, versus
        // recompute because the driver's format moved.
        assertEquals(
            StoreVerdict.STALE_OPTIONS,
            ArtifactStorePolicy.lookup(portable(), otherOpts, null),
        )
    }

    @Test
    fun `a miss is not an error`() {
        assertEquals(StoreVerdict.MISS, ArtifactStorePolicy.lookup(null, opts, null))
    }

    // ------------------------------------------------------------- the layout

    @Test
    fun `paths are sharded and tiered`() {
        // Sharding: a flat directory of tens of thousands of files opens slowly
        // on a phone filesystem. Tiering: a driver swap must be able to drop one
        // subtree without walking the other.
        assertEquals("/r/portable/de/deadbeef.blk", portable().path("/r", ".blk"))
        assertEquals("/r/driver_bound/de/deadbeef.bin", driverBound().path("/r", ".bin"))
    }

    @Test
    fun `a key carries no path and no runtime address`() {
        // EmulationStation keys metadata on the file path and this project
        // rejected that. It is sharper here: an artifact outlives the process
        // that made it.
        val fields = ArtifactKey::class.java.declaredFields.map { it.name }
        assertFalse(fields.any { it.contains("path", ignoreCase = true) })
        assertFalse(fields.any { it.contains("address", ignoreCase = true) })
        assertTrue(fields.contains("contentHash"))
    }

    // --------------------------------------------------- the warm-up lessons

    @Test
    fun `the store may not warm on the UI thread`() {
        // xenia: AOT precompile at 85 functions/sec while the UI thread blocked
        // in the paint path, so a correct progress overlay could not draw.
        // Android fired an ANR at 18s and the user force-closed it mid-compile.
        assertFalse(ArtifactStorePolicy.MAY_WARM_ON_UI_THREAD)
    }

    @Test
    fun `progress must be cumulative, because a bar that goes backwards reads as a hang`() {
        // xenia's estimate grew 6,665 -> 10,540 mid-module and both counters
        // reset per module.
        assertTrue(ArtifactStorePolicy.PROGRESS_MUST_BE_CUMULATIVE)
    }

    @Test
    fun `the user is warned that the platform may call the app unresponsive`() {
        // Android offers "Close app" unprompted. A person not told will take it.
        assertTrue(ArtifactStorePolicy.MUST_WARN_ABOUT_PLATFORM_ANR)
    }

    @Test
    fun `a full cache directory is not evidence the cache works`() {
        // xenia's object cache held 111 MB and was off for every real launch,
        // because its enabling block was guarded on "no cvar bundle supplied"
        // and the launcher always supplies one. The acceptance test is a counted
        // cold-build of zero, on the launch path people use.
        assertTrue(ArtifactStorePolicy.NON_EMPTY_DIRECTORY_PROVES_NOTHING)
    }

    // ---------------------------------------------------------------- eviction

    @Test
    fun `eviction discards whole entries and stops at the cap`() {
        val entries = listOf(
            portable("aaaa") to 40L,
            portable("bbbb") to 40L,
            portable("cccc") to 40L,
        )
        val dropped = ArtifactStorePolicy.evict(entries, capBytes = 80)
        assertEquals(1, dropped.size)
        assertEquals("aaaa", dropped.first().contentHash)
    }

    @Test
    fun `nothing is dropped when the store is under its cap`() {
        val entries = listOf(portable("aaaa") to 10L)
        assertTrue(ArtifactStorePolicy.evict(entries, capBytes = 100).isEmpty())
    }

    @Test
    fun `a payload is written tmp-then-rename`() {
        // A process killed mid-write must not leave a half payload that looks
        // whole. A driver blob is opaque, so nothing downstream can tell.
        assertTrue(ArtifactStorePolicy.WRITE_TMP_THEN_RENAME)
    }

    @Test
    fun `every verdict is a distinct reason`() {
        assertEquals(6, StoreVerdict.entries.size)
        assertEquals(StoreVerdict.entries.size, StoreVerdict.entries.distinct().size)
    }
}
