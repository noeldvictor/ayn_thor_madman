package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The game-data suite.
 *
 * Pins the decisions in app/GAME_DATA.md so they cannot be quietly undone. Each
 * test names the choice it defends rather than the method it calls.
 */
class GameDataTest {

    private fun key(id: String = "SLUS-20948") = GameKey(System.PS2, id)

    // ------------------------------------------------------------- identity

    @Test
    fun `identity ignores the title, so renaming a dump loses nothing`() {
        // The whole reason the ES data model is rejected. Two entries with
        // different display names and the same title id are one game.
        val a = key()
        val b = key()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `the same title id on two systems is two different games`() {
        assertNotEquals(GameKey(System.PS2, "ABC-1"), GameKey(System.VITA, "ABC-1"))
    }

    @Test
    fun `storageId is safe to use as a filename on any platform`() {
        // Title ids carry characters that are legal on one platform and not
        // another. Normalising once here stops each caller inventing a scheme.
        val id = GameKey(System.N3DS, "0004000/0008C300").storageId
        assertTrue("no separators survive: $id", id.none { it == '/' || it == '\\' })
        assertEquals("N3DS_0004000-0008C300", id)
    }

    @Test
    fun `storageId is case-insensitive, because title ids are quoted both ways`() {
        assertEquals(key("slus-20948").storageId, key("SLUS-20948").storageId)
    }

    @Test
    fun `a blank identity is refused rather than stored`() {
        // A blank key would silently collide every unidentified dump into one
        // entry, which is worse than failing to index them.
        assertThrows(IllegalArgumentException::class.java) { GameKey(System.PS2, "  ") }
        assertThrows(IllegalArgumentException::class.java) { DumpId("") }
    }

    @Test
    fun `a dump id is not a game key`() {
        // They answer different questions: a cheat targets a title, a code
        // patch targets a build. Conflating them is the bug three forks fixed.
        val dump: Any = DumpId("9f2c")
        assertTrue(dump != key())
    }

    // ------------------------------------------------------------- metadata

    private val bundled = MetaLayer(
        MetaSource.BUNDLED,
        mapOf(MetaField.TITLE to "SHADOW OF THE COLOSSUS", MetaField.PLAYERS to "1"),
    )
    private val scraped = MetaLayer(
        MetaSource.SCRAPED,
        mapOf(
            MetaField.TITLE to "Shadow of the Colossus",
            MetaField.RELEASE_YEAR to "2005",
            MetaField.DEVELOPER to "Team Ico",
        ),
    )
    private val user = MetaLayer(MetaSource.USER, mapOf(MetaField.TITLE to "SotC"))

    @Test
    fun `the edit made by the person wins over everything`() {
        val r = MetadataResolver.resolve(MetaField.TITLE, listOf(bundled, scraped, user))!!
        assertEquals("SotC", r.value)
        assertEquals(MetaSource.USER, r.source)
    }

    @Test
    fun `layer order in the list does not decide precedence`() {
        // Precedence comes from MetaSource, not from how the caller happened to
        // assemble the list. Otherwise every call site can get it wrong.
        val forward = MetadataResolver.resolve(MetaField.TITLE, listOf(bundled, scraped, user))
        val reversed = MetadataResolver.resolve(MetaField.TITLE, listOf(user, scraped, bundled))
        assertEquals(forward, reversed)
    }

    @Test
    fun `fields resolve independently, so fixing a title keeps the scraped date`() {
        // The sparse-override rule, inherited from the settings resolver. This
        // is the single most important property here.
        val layers = listOf(bundled, scraped, user)
        assertEquals("SotC", MetadataResolver.resolve(MetaField.TITLE, layers)!!.value)
        assertEquals("2005", MetadataResolver.resolve(MetaField.RELEASE_YEAR, layers)!!.value)
        assertEquals("Team Ico", MetadataResolver.resolve(MetaField.DEVELOPER, layers)!!.value)
        assertEquals("1", MetadataResolver.resolve(MetaField.PLAYERS, layers)!!.value)
    }

    @Test
    fun `a blank value does not shadow a lower layer`() {
        // A scraper that returns an empty string must not blank out good
        // bundled data. Present-but-empty is absent.
        val empty = MetaLayer(MetaSource.SCRAPED, mapOf(MetaField.TITLE to "   "))
        val r = MetadataResolver.resolve(MetaField.TITLE, listOf(empty, bundled))!!
        assertEquals(MetaSource.BUNDLED, r.source)
    }

    @Test
    fun `an unknown field resolves to null rather than a placeholder`() {
        assertNull(MetadataResolver.resolve(MetaField.GENRE, listOf(bundled, scraped)))
    }

    @Test
    fun `resolveAll omits fields nobody supplies`() {
        val all = MetadataResolver.resolveAll(listOf(bundled, scraped))
        assertTrue(MetaField.GENRE !in all)
        assertTrue(MetaField.TITLE in all)
    }

    @Test
    fun `a row never renders blank, even with no metadata at all`() {
        assertEquals("SLUS-20948", MetadataResolver.displayTitle(key(), emptyList()))
    }

    @Test
    fun `sorting falls back to the title when no sort name is given`() {
        assertEquals("shadow of the colossus", MetadataResolver.sortKey(key(), listOf(scraped)))
    }

    @Test
    fun `a sort name overrides the title for ordering only`() {
        val layers = listOf(
            scraped,
            MetaLayer(MetaSource.USER, mapOf(MetaField.SORT_TITLE to "Colossus, Shadow of the")),
        )
        assertEquals("colossus, shadow of the", MetadataResolver.sortKey(key(), layers))
        assertEquals("Shadow of the Colossus", MetadataResolver.displayTitle(key(), layers))
    }

    // ---------------------------------------------------------------- media

    @Test
    fun `art is cached per role, so a logo never satisfies a cover request`() {
        val cover = MediaRequest(key(), MediaRole.COVER, 96, 128)
        val logo = MediaRequest(key(), MediaRole.LOGO, 96, 128)
        assertNotEquals(cover.cacheKey, logo.cacheKey)
    }

    @Test
    fun `near-identical sizes share one decode`() {
        // A row that measures 100 px on one pass and 97 on the next must not
        // decode the same cover twice. This is the whole point of bucketing.
        val a = MediaRequest(key(), MediaRole.COVER, 97, 128).cacheKey
        val b = MediaRequest(key(), MediaRole.COVER, 100, 128).cacheKey
        assertEquals(a, b)
    }

    @Test
    fun `bucketing reduces decodes but does not eliminate them at a boundary`() {
        // Honest about the limit: 96 is exactly a bucket edge, so 96 and 97
        // still decode twice. Bucketing narrows the window, it does not close
        // it. Anything stronger would need to snap sizes at layout instead.
        assertNotEquals(
            MediaRequest(key(), MediaRole.COVER, 96, 128).cacheKey,
            MediaRequest(key(), MediaRole.COVER, 97, 128).cacheKey,
        )
    }

    @Test
    fun `bucketing rounds up, so art is downscaled at draw time and never upscaled`() {
        // Rounding down would decode below the drawn size and upscale, which
        // looks worse than the cost it saves.
        assertEquals(128, MediaRequest.bucket(97))
        assertEquals(32, MediaRequest.bucket(1))
        assertEquals(96, MediaRequest.bucket(96))
    }

    @Test
    fun `a genuinely different size gets its own decode`() {
        val small = MediaRequest(key(), MediaRole.COVER, 96, 128).cacheKey
        val large = MediaRequest(key(), MediaRole.COVER, 320, 440).cacheKey
        assertNotEquals(small, large)
    }

    @Test
    fun `a zero-sized request is refused rather than decoded`() {
        // Compose reports a zero size on the first measure pass. Decoding for
        // it would cache a useless entry under a real key.
        assertThrows(IllegalArgumentException::class.java) {
            MediaRequest(key(), MediaRole.COVER, 0, 128)
        }
    }

    @Test
    fun `there is no video role`() {
        // Pinned deliberately. A video snap is a per-frame decode behind a menu
        // and is excluded by the cheap-UI rule, not by oversight.
        assertTrue(MediaRole.entries.none { it.name.contains("VIDEO") })
    }
}
