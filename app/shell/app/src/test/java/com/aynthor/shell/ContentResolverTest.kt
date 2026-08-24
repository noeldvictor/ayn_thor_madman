package com.aynthor.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Vita3K's content path resolver, propagated and generalised.
 *
 * Part of the lesson suite. See shared_layer/PROPAGATION.md item 12.
 */
class ContentResolverTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun root(label: String): ContentRoot =
        ContentRoot(label, tmp.newFolder(label))

    private fun place(root: ContentRoot, kind: Kind, titleId: String): File {
        val dir = File(root.path, kind.dirName).apply { mkdirs() }
        return File(dir, "$titleId.${kind.defaultExtensions.first()}").apply { writeText("x") }
    }

    @Test
    fun `a missing root does not crash and simply does not match`() {
        // A person's SD card is not always mounted. Vita3K searches nine places
        // precisely because most of them are usually absent.
        val absent = ContentRoot("sdcard", File(tmp.root, "never-created"))
        assertNull(ContentResolver.locate(listOf(absent), Kind.CHEAT, "GAME001"))
        assertFalse(ContentResolver.has(listOf(absent), Kind.CHEAT, "GAME001"))
    }

    @Test
    fun `order is precedence and the first match wins`() {
        val first = root("app-private")
        val second = root("sdcard")
        place(second, Kind.CHEAT, "GAME001")
        val expected = place(first, Kind.CHEAT, "GAME001")

        val found = ContentResolver.locate(listOf(first, second), Kind.CHEAT, "GAME001")
        assertEquals(expected, found?.file)
        assertEquals("app-private", found?.root?.label)
    }

    @Test
    fun `it reports which root answered`() {
        // The storage view needs to say where content actually is, and
        // "no cheats found" is not actionable while "looked here" is.
        // Vita3K's resolver does not report this; ours must.
        val a = root("app-private")
        val b = root("community-path")
        place(b, Kind.MOD, "GAME002")

        assertEquals("community-path",
            ContentResolver.locate(listOf(a, b), Kind.MOD, "GAME002")?.root?.label)
    }

    @Test
    fun `the badge and the loader can never disagree`() {
        // Vita3K ships find_* and has_* as separate functions, which is two
        // chances to diverge. A badge saying a game has cheats when the loader
        // finds none is worse than no badge at all.
        val a = root("app-private")
        val b = root("sdcard")
        val roots = listOf(a, b)

        for (kind in Kind.entries) {
            assertEquals(
                "has() and locate() must agree for $kind before the file exists",
                ContentResolver.locate(roots, kind, "T") != null,
                ContentResolver.has(roots, kind, "T"),
            )
            place(b, kind, "T")
            assertEquals(
                "has() and locate() must agree for $kind after the file exists",
                ContentResolver.locate(roots, kind, "T") != null,
                ContentResolver.has(roots, kind, "T"),
            )
            assertTrue(ContentResolver.has(roots, kind, "T"))
        }
    }

    @Test
    fun `candidates lists every place tried, whether or not it exists`() {
        // The diagnostic value is the list that was tried. Keeping this
        // separate from locate() is Vita3K's design and it is right.
        val roots = listOf(root("a"), root("b"), root("c"))
        val tried = ContentResolver.candidates(roots, Kind.TEXTURE_PACK, "GAME003")

        assertEquals(3, tried.size)
        assertEquals(listOf("a", "b", "c"), tried.map { it.second.label })
        assertTrue(tried.all { it.first.name == "GAME003.zip" })
    }

    @Test
    fun `kinds do not collide with each other`() {
        val only = root("app-private")
        place(only, Kind.CHEAT, "GAME004")

        assertTrue(ContentResolver.has(listOf(only), Kind.CHEAT, "GAME004"))
        assertFalse(ContentResolver.has(listOf(only), Kind.MOD, "GAME004"))
        assertFalse(ContentResolver.has(listOf(only), Kind.SAVE, "GAME004"))
    }

    /**
     * The extension belongs to the BACKEND. This repo's own survey found six
     * cheat formats; a single hardcoded extension could see one of them.
     */
    @Test
    fun candidates_cover_every_declared_extension() {
        val roots = listOf(root("app"))
        val got = ContentResolver.candidates(roots, Kind.CHEAT, "T1", listOf("pnach", "ncl"))
        assertEquals(2, got.size)
        assertTrue(got[0].first.name.endsWith(".pnach"))
        assertTrue(got[1].first.name.endsWith(".ncl"))
    }

    /**
     * Root is the outer loop. Precedence between roots is what a person
     * configured; precedence between extensions is not, so a nearer root's
     * second-choice extension still beats a further root's first choice.
     */
    @Test
    fun a_nearer_root_beats_a_further_one_whatever_the_extension() {
        val roots = listOf(root("near"), root("far"))
        val order = ContentResolver
            .candidates(roots, Kind.CHEAT, "T1", listOf("pnach", "ncl"))
            .map { it.second.label }
        assertEquals(listOf("near", "near", "far", "far"), order)
    }

    @Test
    fun locate_finds_a_non_default_extension() {
        val only = root("only")
        File(only.path, Kind.CHEAT.dirName).apply { mkdirs() }
        File(File(only.path, Kind.CHEAT.dirName), "T1.ncl").writeText("x")
        val found = ContentResolver.locate(
            listOf(only), Kind.CHEAT, "T1", listOf("pnach", "ncl"),
        )
        assertNotNull(found)
        assertTrue(found!!.file.name.endsWith(".ncl"))
    }
}
