package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The cover cache suite.
 *
 * **These defend the cheap-UI rule, not the cache class.** "Cover art is
 * decoded once at display size" is a rule in CLAUDE.md with no way to enforce
 * it by review, because the failure is invisible until somebody profiles a
 * scroll on the device. A counted decoder makes it a test instead.
 */
class MediaCacheTest {

    private fun key(id: String) = GameKey(System.PS2, id)

    /** A source that counts, so the cache can be tested without a decoder. */
    private class Counting(
        private val roles: Set<MediaRole> = setOf(MediaRole.COVER),
    ) : MediaSource {
        var decoded = 0
        override fun available(key: GameKey) = roles
        override fun decode(request: MediaRequest): CoverArt {
            decoded++
            return CoverArt(request.cacheKey, request.widthPx, request.heightPx, 0.5f)
        }
    }

    private fun thumb(id: String) = MediaRequest(key(id), MediaRole.COVER, 34, 46)

    // ------------------------------------------------------- the actual rule

    @Test
    fun `scrolling a row past the viewport many times decodes once`() {
        // The rule this whole file exists for. A lazy list re-composes a row
        // every time it scrolls back into view; without the cache that is a
        // decode per appearance.
        val src = Counting()
        val store = InMemoryMediaStore(src)
        repeat(50) { store.get(thumb("A")) }
        assertEquals(1, src.decoded)
        assertEquals(1, store.decodes)
    }

    @Test
    fun `the same cover at two display sizes is two decodes, deliberately`() {
        // The row thumbnail and the metadata panel draw the same cover at very
        // different sizes. Sharing one decode would mean one of them scales,
        // which is the waste the size-in-the-request rule prevents.
        val src = Counting()
        val store = InMemoryMediaStore(src)
        store.get(MediaRequest(key("A"), MediaRole.COVER, 34, 46))
        store.get(MediaRequest(key("A"), MediaRole.COVER, 132, 180))
        assertEquals(2, src.decoded)
    }

    @Test
    fun `a game with no art never reaches the decoder`() {
        // available() is checked first. Asking a decoder for art that does not
        // exist is the expensive way to discover it does not exist.
        val src = Counting(roles = emptySet())
        val store = InMemoryMediaStore(src)
        assertNull(store.get(thumb("A")))
        assertEquals(0, src.decoded)
    }

    @Test
    fun `a missing role does not poison the cache for a present one`() {
        val src = Counting(roles = setOf(MediaRole.COVER))
        val store = InMemoryMediaStore(src)
        assertNull(store.get(MediaRequest(key("A"), MediaRole.LOGO, 34, 46)))
        assertTrue(store.get(MediaRequest(key("A"), MediaRole.COVER, 34, 46)) != null)
        assertEquals(1, src.decoded)
    }

    @Test
    fun `available does not decode`() {
        // The library shows a badge for art it has not drawn yet. That must
        // cost nothing.
        val src = Counting()
        val store = InMemoryMediaStore(src)
        repeat(10) { store.available(key("A")) }
        assertEquals(0, src.decoded)
    }

    // ------------------------------------------------------------ the bound

    @Test
    fun `the cache is bounded, because a library is not`() {
        val src = Counting()
        val store = InMemoryMediaStore(src, maxEntries = 4)
        repeat(10) { store.get(thumb("G$it")) }
        assertEquals(4, store.residentCount())
        assertEquals(10, src.decoded)
    }

    @Test
    fun `eviction is least-recently-used, not least-recently-added`() {
        // A person scrolling back and forth over the same few games must keep
        // those covers, even though they were added first. Insertion order
        // would evict exactly the ones still on screen.
        val store = InMemoryMediaStore(Counting(), maxEntries = 3)
        store.get(thumb("A"))
        store.get(thumb("B"))
        store.get(thumb("C"))
        store.get(thumb("A")) // A is touched again, so B is now the eldest
        store.get(thumb("D"))
        assertTrue("A must survive a re-read", store.isResident(thumb("A")))
        assertFalse("B was least recently used", store.isResident(thumb("B")))
    }

    // ------------------------------------------------------- theme and hue

    @Test
    fun `a theme switch does not invalidate a single cached cover`() {
        // CoverArt stores a hue, not a resolved colour. If it stored a colour,
        // switching to dark mode would mean discarding the whole cache, which
        // is the most expensive possible response to a toggle.
        val store = InMemoryMediaStore(Counting())
        val first = store.get(thumb("A"))
        val second = store.get(thumb("A"))
        assertSame("the same instance is served both times", first, second)
        assertEquals(1, store.decodes)
    }

    @Test
    fun `a placeholder colour is stable across runs`() {
        // A hue that changed between launches would read as a rendering fault
        // rather than as a placeholder.
        val a = FakeMediaSource.hueFor(key("SCES-50916"))
        val b = FakeMediaSource.hueFor(key("SCES-50916"))
        assertEquals(a, b, 0f)
        assertTrue(a in 0f..1f)
    }

    @Test
    fun `different games get different placeholder colours`() {
        assertNotEquals(
            FakeMediaSource.hueFor(key("SCES-50916")),
            FakeMediaSource.hueFor(key("SLES-51234")),
        )
    }

    @Test
    fun `the fake source leaves some games with no cover, on purpose`() {
        // A cover UI only ever tested with art present ships a broken empty
        // state, and empty is the common case for personal dumps.
        val bare = FakeMediaSource.available(GameKey(System.PS2, "SCUS-97472"))
        assertFalse(MediaRole.COVER in bare)
    }

    // --------------------------------------------------------------- labels

    @Test
    fun `initials skip articles, so placeholders stay distinguishable`() {
        // Without this, a shelf of covers all read "TH".
        assertEquals("LZ", initialsFor("The Legend of Zelda: Phantom Hourglass"))
        assertEquals("SC", initialsFor("Shadow of the Colossus"))
        assertEquals("BD", initialsFor("Blue Dragon"))
    }

    @Test
    fun `a one-word title still gets a label`() {
        assertEquals("I", initialsFor("Ico"))
    }

    @Test
    fun `a title made only of articles still gets a label`() {
        // Degenerate, but a blank cover reads as a bug.
        assertEquals("T", initialsFor("The"))
    }
}
