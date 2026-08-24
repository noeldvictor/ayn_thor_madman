package com.aynthor.shell

import java.io.File

/**
 * Where a person's content actually is.
 *
 * **Harvested from Vita3K, which is the only fork that solved this.** Its
 * `util/cheat_paths.h` enumerates roots, builds candidate paths and resolves
 * one, searching app-private storage, internal storage, SD cards and three
 * separate community conventions. Every other fork assumes one path.
 *
 * **That is the real Android problem.** A person's content is wherever they put
 * it, or wherever the guide they followed said to put it.
 *
 * **Generalised on the way across.** Vita3K's is cheats-only; the app needs the
 * same resolver for HD packs, mods, translations, saves and ROMs, so [Kind] is
 * a parameter rather than baked into the function name.
 *
 * See shared_layer/PROPAGATION.md item 12.
 */
enum class Kind(val dirName: String, val defaultExtensions: List<String>) {
    CHEAT("cheats", listOf("txt", "pnach", "ncl", "mch", "psv")),
    TEXTURE_PACK("textures", listOf("zip")),
    MOD("mods", listOf("zip")),
    SAVE("saves", listOf("sav")),
    ;

    /**
     * A FALLBACK, not the answer. The extension belongs to the BACKEND.
     *
     * The kind is ours -- the shell decides that cheats, texture packs, mods
     * and saves are the categories it shows and where it keeps them. The file
     * extension is not: this repo's own survey found SIX cheat formats behind
     * five spellings, and records save formats as "irreducibly per-backend".
     *
     * A single hardcoded extension per kind failed the discriminator derived
     * on 2026-08-25: a fixed list is correct when the HOST owns the concept
     * and wrong when the GUEST does. So [ContentResolver.candidates] takes the
     * extensions, and these values serve only a caller that has no backend to
     * ask -- a library scan before a backend is chosen, or a test.
     */
    fun fallbackExtensions(): List<String> = defaultExtensions
}

/**
 * One place content may live, in precedence order.
 *
 * [label] is reported back by [ContentResolver.locate] so the storage view and
 * a diagnostic can say **which** root answered. Vita3K's version does not
 * report this; the storage screen needs it.
 */
data class ContentRoot(val label: String, val path: File)

data class Located(val file: File, val root: ContentRoot)

/**
 * WHERE THE ROOTS COME FROM IS NOT SOLVED HERE, and this object is only one
 * third of the problem. Recorded 2026-08-25.
 *
 * Three complementary mechanisms, one per fork:
 *
 *  - ENUMERATE what is mounted -- XenDroid, via `StorageManager.storageVolumes`
 *    and `volume.directory`, with `?: continue` because a volume can be listed
 *    and have no directory.
 *  - CONVERT what the person picked -- eden's `PathUtil`, turning a SAF
 *    `content://` URI into a real path, removable volumes included.
 *  - SEARCH known conventions inside a root -- Vita3K, nine locations for one
 *    title. THAT IS THIS FILE.
 *
 * They are not alternatives. Enumerate, offer, convert what is chosen, then
 * search within it. [candidates] promises a diagnostic -- "looked in these six
 * places" -- which is only true once something supplies the places.
 *
 * [ContentRoot.label] exists for exactly the volume name an enumeration gives.
 */
object ContentResolver {

    /**
     * Candidate files for one title, in precedence order.
     *
     * Kept separate from [locate] for the same reason Vita3K keeps
     * `get_candidate_files` separate from `find`: **a diagnostic needs the list
     * that was tried, not only the answer.** "No cheats found" is not
     * actionable; "looked in these six places" is.
     */
    fun candidates(
        roots: List<ContentRoot>,
        kind: Kind,
        titleId: String,
        /** Backend-declared, in precedence order. Defaults to a fallback. */
        extensions: List<String> = kind.defaultExtensions,
    ): List<Pair<File, ContentRoot>> =
        roots.flatMap { root ->
            // Root is the outer loop: a root's own file beats a later root's,
            // whatever the extension. Precedence between roots is the thing a
            // person configured; precedence between extensions is not.
            extensions.map { ext ->
                File(File(root.path, kind.dirName), "$titleId.$ext") to root
            }
        }

    /** The first candidate that exists. Order is precedence. */
    fun locate(
        roots: List<ContentRoot>,
        kind: Kind,
        titleId: String,
        extensions: List<String> = kind.defaultExtensions,
    ): Located? =
        candidates(roots, kind, titleId, extensions)
            .firstOrNull { (file, _) -> file.isFile }
            ?.let { (file, root) -> Located(file, root) }

    /**
     * Whether content exists, for the library badge.
     *
     * **Defined in terms of [locate] on purpose.** Vita3K ships `find_*` and
     * `has_*` as separate functions, which is two chances to disagree — and a
     * badge that disagrees with the loader is worse than no badge, because it
     * says a game has cheats and then the game has none.
     */
    fun has(roots: List<ContentRoot>, kind: Kind, titleId: String): Boolean =
        locate(roots, kind, titleId) != null
}
