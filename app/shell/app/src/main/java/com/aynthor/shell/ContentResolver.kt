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
enum class Kind(val dirName: String, val extension: String) {
    CHEAT("cheats", "txt"),
    TEXTURE_PACK("textures", "zip"),
    MOD("mods", "zip"),
    SAVE("saves", "sav"),
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

object ContentResolver {

    /**
     * Candidate files for one title, in precedence order.
     *
     * Kept separate from [locate] for the same reason Vita3K keeps
     * `get_candidate_files` separate from `find`: **a diagnostic needs the list
     * that was tried, not only the answer.** "No cheats found" is not
     * actionable; "looked in these six places" is.
     */
    fun candidates(roots: List<ContentRoot>, kind: Kind, titleId: String): List<Pair<File, ContentRoot>> =
        roots.map { root ->
            File(File(root.path, kind.dirName), "$titleId.${kind.extension}") to root
        }

    /** The first candidate that exists. Order is precedence. */
    fun locate(roots: List<ContentRoot>, kind: Kind, titleId: String): Located? =
        candidates(roots, kind, titleId)
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
