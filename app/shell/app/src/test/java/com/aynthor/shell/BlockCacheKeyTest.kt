package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The block-cache sentinel suite.
 *
 * **Encodes ARMSX2's mVU sentinel as a rule any backend can be held to.** A
 * backend that bakes the guest FP environment into generated code — the right
 * way to keep `FPCR` out of the hot path — has made that code correct only for
 * the environment it compiled under.
 *
 * See `research_log/20260823_1520_cpu_leads_already_done.md`.
 */
class BlockCacheKeyTest {

    private val production = FpEnv(roundToZero = true, flushDenormals = true)
    private val ieee = FpEnv(roundToZero = false, flushDenormals = false)

    /** The PS2's three: EE FPU, VU0, VU1. */
    private fun three(fpu: FpEnv, vu0: FpEnv, vu1: FpEnv) = listOf(fpu, vu0, vu1)

    @Test
    fun `the same address under the same environment reuses the block`() {
        val envs = three(production, production, production)
        val key = BlockCacheKey(0x100000, envs)
        assertTrue(BlockCache.canReuse(key, 0x100000, envs))
    }

    @Test
    fun `a rounding mode change refuses the cached block`() {
        // The bug this file exists to prevent. Round-toward-zero turns every
        // overflow into a saturation to FLT_MAX rather than Inf, so a block
        // baked under one mode computes different numbers under the other.
        val key = BlockCacheKey(0x100000, three(production, production, production))
        assertFalse(
            BlockCache.canReuse(key, 0x100000, three(ieee, production, production)),
        )
    }

    @Test
    fun `a denormal setting change refuses the cached block`() {
        val key = BlockCacheKey(0x100000, three(production, production, production))
        val denormalsLive = FpEnv(roundToZero = true, flushDenormals = false)
        assertFalse(
            BlockCache.canReuse(key, 0x100000, three(denormalsLive, production, production)),
        )
    }

    @Test
    fun `a change to any one of several environments is enough to refuse`() {
        // The PS2 has three independent FP environments and a block may bake
        // more than one. A partial match is still wrong.
        val key = BlockCacheKey(0x100000, three(production, production, production))
        assertFalse(
            "VU1 changed and the block still claimed to be valid",
            BlockCache.canReuse(key, 0x100000, three(production, production, ieee)),
        )
    }

    @Test
    fun `the key distinguishes environments, not just addresses`() {
        // Stated as a property of the key itself, so a cache built on a plain
        // map cannot get this wrong either.
        assertNotEquals(
            BlockCacheKey(0x100000, three(production, production, production)),
            BlockCacheKey(0x100000, three(ieee, production, production)),
        )
    }

    @Test
    fun `a different address never reuses, whatever the environment`() {
        val envs = three(production, production, production)
        assertFalse(BlockCache.canReuse(BlockCacheKey(0x100000, envs), 0x200000, envs))
    }

    @Test
    fun `order matters, because the environments are not interchangeable`() {
        // VU0 and VU1 are separately configurable. A block baked with VU0 in
        // IEEE mode is not the same as one with VU1 in IEEE mode.
        val key = BlockCacheKey(0x100000, three(production, ieee, production))
        assertFalse(
            BlockCache.canReuse(key, 0x100000, three(production, production, ieee)),
        )
    }

    // ----------------------------------------------- code writes from cheats

    private val envs3 = three(production, production, production)
    private fun block(addr: Long, len: Int) = BlockCacheKey(addr, envs3, len)

    @Test
    fun `a code write invalidates the block it lands in`() {
        // Found by reading VitaCheat, which has code-write opcodes documented as
        // needing "JIT cache invalidation when bytes change". dmnt has no such
        // opcode because Atmosphere runs on real hardware.
        val blocks = listOf(block(0x1000, 64), block(0x2000, 64))
        val hit = BlockCache.invalidatedBy(blocks, 0x1010, 4)
        assertEquals(listOf(block(0x1000, 64)), hit)
    }

    @Test
    fun `a write inside a block counts, not only one at its start`() {
        // A block keyed by start address alone cannot answer this, which is why
        // guestLength exists.
        assertTrue(BlockCache.invalidatedBy(listOf(block(0x1000, 64)), 0x103C, 4).isNotEmpty())
    }

    @Test
    fun `a write that straddles two blocks invalidates both`() {
        val blocks = listOf(block(0x1000, 16), block(0x1010, 16))
        assertEquals(2, BlockCache.invalidatedBy(blocks, 0x100C, 8).size)
    }

    @Test
    fun `a write just past a block does not invalidate it`() {
        // Off-by-one here silently throws away a warm cache on every cheat tick.
        assertTrue(BlockCache.invalidatedBy(listOf(block(0x1000, 16)), 0x1010, 4).isEmpty())
    }

    @Test
    fun `a write just before a block does not invalidate it`() {
        assertTrue(BlockCache.invalidatedBy(listOf(block(0x1000, 16)), 0x0FFC, 4).isEmpty())
    }

    @Test
    fun `a data write to an address no block covers invalidates nothing`() {
        // The common case. Most cheats poke data, and that must stay free.
        assertTrue(BlockCache.invalidatedBy(listOf(block(0x1000, 64)), 0x9000, 4).isEmpty())
    }

    @Test
    fun `a zero-length write is refused rather than treated as a point`() {
        assertThrows(IllegalArgumentException::class.java) {
            BlockCache.invalidatedBy(listOf(block(0x1000, 16)), 0x1000, 0)
        }
    }

    @Test
    fun `a block covers at least one byte`() {
        assertThrows(IllegalArgumentException::class.java) {
            BlockCacheKey(0x1000, envs3, 0)
        }
    }

    @Test
    fun `code and data writes are distinguished, because only one is emulator knowledge`() {
        // On real hardware a write is a write. On a recompiling emulator a write
        // to executable memory has a second consequence, and the cheat format
        // has to say which it is.
        assertEquals(2, WriteTarget.entries.size)
        assertTrue(WriteTarget.CODE != WriteTarget.DATA)
    }
}
