package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pipeline cache suite.
 *
 * **Each test names a fork's learned bug or a rule from the Vulkan
 * specification, not a method.** See
 * `research_log/20260823_1905_pipeline_cache_extraction.md`.
 */
class PipelineCacheTest {

    private val uuidA = List(16) { it.toByte() }
    private val uuidB = List(16) { (it + 1).toByte() }
    private val device = DeviceIdentity(vendorId = 0x5143, deviceId = 0x43051401, uuid = uuidA)

    private fun header(
        length: Int = PipelineCacheHeader.MIN_LENGTH,
        version: Int = PipelineCacheHeader.VERSION_ONE,
        vendor: Int = 0x5143,
        dev: Int = 0x43051401,
        uuid: List<Byte> = uuidA,
    ) = PipelineCacheHeader(length, version, vendor, dev, uuid)

    // ------------------------------------------------------------- validation

    @Test
    fun `a matching blob is accepted`() {
        assertEquals(
            CacheVerdict.ACCEPT,
            PipelineCachePolicy.validate(header(), device, fileEpoch = 7, appEpoch = 7),
        )
    }

    @Test
    fun `every rejection names which field disagreed`() {
        // ARMSX2 logs the disagreeing field, and that is the difference between
        // "the cache did not load" and a diagnosis. A single boolean would make
        // a driver swap and an app update look identical.
        assertEquals(
            CacheVerdict.HEADER_TOO_SHORT,
            PipelineCachePolicy.validate(header(length = 31), device, 7, 7),
        )
        assertEquals(
            CacheVerdict.BAD_HEADER_VERSION,
            PipelineCachePolicy.validate(header(version = 2), device, 7, 7),
        )
        assertEquals(
            CacheVerdict.VENDOR_MISMATCH,
            PipelineCachePolicy.validate(header(vendor = 0x10DE), device, 7, 7),
        )
        assertEquals(
            CacheVerdict.DEVICE_MISMATCH,
            PipelineCachePolicy.validate(header(dev = 1), device, 7, 7),
        )
        assertEquals(
            CacheVerdict.UUID_MISMATCH,
            PipelineCachePolicy.validate(header(uuid = uuidB), device, 7, 7),
        )
    }

    @Test
    fun `a truncated file is refused before its fields are read`() {
        // Order matters. A short file must not have its vendor id trusted.
        assertEquals(
            CacheVerdict.HEADER_TOO_SHORT,
            PipelineCachePolicy.validate(header(length = 4, vendor = 0xBAD), device, 7, 7),
        )
    }

    // -------------------------------------------- the bug ARMSX2 shipped

    @Test
    fun `an app update invalidates the blob even though the device is identical`() {
        // THE learned bug. vendorID, deviceID and pipelineCacheUUID are all
        // identical across an app update on the same phone, so the device header
        // alone cannot see that the shaders changed. ARMSX2 shipped this: a
        // shader-cache version bump wiped the SPIR-V and kept every pipeline
        // built from the previous build's shaders.
        val verdict = PipelineCachePolicy.validate(header(), device, fileEpoch = 7, appEpoch = 8)
        assertEquals(CacheVerdict.EPOCH_MISMATCH, verdict)
    }

    @Test
    fun `an app update drops the translation cache too, a driver swap does not`() {
        // The two caches have different lifetimes and this is the rule that
        // separates them. SPIR-V is portable, so it survives a driver change.
        // It does not survive the app that generated it changing.
        assertTrue(PipelineCachePolicy.alsoDropTranslationCache(CacheVerdict.EPOCH_MISMATCH))
        assertFalse(PipelineCachePolicy.alsoDropTranslationCache(CacheVerdict.UUID_MISMATCH))
        assertFalse(PipelineCachePolicy.alsoDropTranslationCache(CacheVerdict.ACCEPT))
    }

    // ----------------------------------------------------------------- naming

    @Test
    fun `the file is named by the UUID, not by a driver version string`() {
        // A driver can change its compiled format without changing its package
        // name or version. The specification says pipelineCacheUUID is what
        // moves, so anything else silently feeds stale blobs to a new compiler.
        val a = PipelineCachePolicy.fileName(uuidA, 7)
        val b = PipelineCachePolicy.fileName(uuidB, 7)
        assertNotEquals(a, b)
        assertTrue(a.startsWith("pipeline-000102"))
    }

    @Test
    fun `two epochs of one driver are separate files`() {
        assertNotEquals(
            PipelineCachePolicy.fileName(uuidA, 7),
            PipelineCachePolicy.fileName(uuidA, 8),
        )
    }

    @Test
    fun `a malformed UUID is refused rather than truncated into a filename`() {
        assertThrows(IllegalArgumentException::class.java) {
            PipelineCachePolicy.fileName(List(15) { 0 }, 7)
        }
    }

    // -------------------------------------------------------------- retention

    @Test
    fun `switching driver and back does not cost a cold cache both ways`() {
        // The whole reason for keeping more than one file. Trying a per-game
        // driver override is normal, and with a single shared cache file it
        // would discard the warm cache for EVERY backend at once.
        val files = listOf("pipeline-B-7.bin", "pipeline-A-7.bin")
        val removed = PipelineCachePolicy.evict(files, current = "pipeline-B-7.bin", keep = 2)
        assertTrue("the previous driver's cache must survive", removed.isEmpty())
    }

    @Test
    fun `older caches beyond the retained few are removed`() {
        val files = listOf("cur", "n1", "n2", "n3", "n4")
        assertEquals(listOf("n3", "n4"), PipelineCachePolicy.evict(files, current = "cur", keep = 2))
    }

    @Test
    fun `the current cache is never evicted, wherever it sits in the list`() {
        val files = listOf("n1", "n2", "n3", "cur")
        assertFalse("cur" in PipelineCachePolicy.evict(files, current = "cur", keep = 1))
    }

    @Test
    fun `keep zero still spares the current cache`() {
        val files = listOf("cur", "n1")
        assertEquals(listOf("n1"), PipelineCachePolicy.evict(files, current = "cur", keep = 0))
    }

    // ------------------------------------------------------------------- cap

    @Test
    fun `an unbounded blob is the ARMSX2 stall, so there is a cap`() {
        // Nothing prunes a pipeline cache blob and it is re-serialised in full,
        // so an uncapped one becomes a mid-gameplay stall that scales with how
        // long the game has been played.
        assertTrue(PipelineCachePolicy.overCap(sizeBytes = 200, capBytes = 100))
        assertFalse(PipelineCachePolicy.overCap(sizeBytes = 100, capBytes = 100))
    }

    @Test
    fun `a cap of zero is refused rather than discarding every time`() {
        assertThrows(IllegalArgumentException::class.java) {
            PipelineCachePolicy.overCap(1, 0)
        }
    }
}
