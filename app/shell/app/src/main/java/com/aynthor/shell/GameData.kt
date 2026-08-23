package com.aynthor.shell

/**
 * Game identity, metadata and media.
 *
 * Implements [`app/GAME_DATA.md`](../../../../../../../GAME_DATA.md). The short
 * version: **take EmulationStation's media-role vocabulary, reject its
 * path-based identity and its startup-XML model.**
 */

// ---------------------------------------------------------------- identity

/**
 * What a game *is*.
 *
 * **Never a path.** Move, rename or re-dump a game and this does not change, so
 * overrides, cheats, patches, saves, art and playtime all survive.
 *
 * The fleet reached this independently three times: Vita3K keys cheats by
 * `TITLEID`, ARMSX2 by disc serial, xenia by an 8-hex-digit title id.
 * EmulationStation is the outlier, not this.
 */
data class GameKey(val system: System, val titleId: String) {
    init {
        require(titleId.isNotBlank()) { "titleId must not be blank" }
    }

    /**
     * A filesystem- and database-safe form.
     *
     * Title ids carry characters that are legal on one platform and not
     * another — the PS2 uses `SLUS-20948`, the Vita `PCSE00001`, the 360 a bare
     * hex string. Normalising once here means no caller invents its own scheme.
     */
    val storageId: String
        get() = system.name + "_" + titleId.uppercase().map {
            if (it.isLetterOrDigit()) it else '-'
        }.joinToString("")
}

/**
 * *Which copy* of a game this is.
 *
 * Deliberately separate from [GameKey], because they answer different
 * questions. **A cheat targets a title. A code patch usually targets a build.**
 * That distinction is the problem Cemu, rpcsx and eden each solved alone.
 */
data class DumpId(val contentHash: String) {
    init {
        require(contentHash.isNotBlank()) { "contentHash must not be blank" }
    }
}

// ---------------------------------------------------------------- metadata

/**
 * Where a metadata value came from, highest precedence first.
 *
 * **Declaration order is the precedence order** and [MetadataResolver] depends
 * on it, so do not reorder these to group them differently.
 */
enum class MetaSource { USER, SCRAPED, BUNDLED, DERIVED }

/**
 * The metadata vocabulary. Deliberately short.
 *
 * **No rating, no play count, no favourite.** Rating is opinion, and the other
 * two are the person's library state rather than a description of the game.
 */
enum class MetaField {
    TITLE, SORT_TITLE, REGION, RELEASE_YEAR,
    DEVELOPER, PUBLISHER, GENRE, PLAYERS, DESCRIPTION,
}

/** One layer's contribution. Sparse: a layer states only what it knows. */
data class MetaLayer(val source: MetaSource, val fields: Map<MetaField, String>)

/** A resolved value, carrying where it won from so the UI can show provenance. */
data class ResolvedMeta(val value: String, val source: MetaSource)

/**
 * Resolves metadata across layers.
 *
 * **This is the settings resolver again, on purpose.** Same sparse-override
 * rule, same per-field independence: correcting a wrong title must not discard
 * the scraped release date.
 */
object MetadataResolver {

    /** The winning value for one field, or null if no layer supplies it. */
    fun resolve(field: MetaField, layers: List<MetaLayer>): ResolvedMeta? =
        MetaSource.entries.firstNotNullOfOrNull { source ->
            layers.firstOrNull { it.source == source }
                ?.fields?.get(field)
                ?.takeIf { it.isNotBlank() }
                ?.let { ResolvedMeta(it, source) }
        }

    fun resolveAll(layers: List<MetaLayer>): Map<MetaField, ResolvedMeta> =
        MetaField.entries.mapNotNull { f -> resolve(f, layers)?.let { f to it } }.toMap()

    /**
     * The title to display, with a fallback that never returns blank.
     *
     * A library row with no text is worse than a wrong one.
     */
    fun displayTitle(key: GameKey, layers: List<MetaLayer>): String =
        resolve(MetaField.TITLE, layers)?.value ?: key.titleId

    /** What the list sorts by. Falls back to the title, as ES does. */
    fun sortKey(key: GameKey, layers: List<MetaLayer>): String =
        (resolve(MetaField.SORT_TITLE, layers)?.value
            ?: displayTitle(key, layers)).lowercase()
}

// ------------------------------------------------------------------- media

/**
 * The art roles, taken from EmulationStation and trimmed.
 *
 * **A role is a request, not a file.** Nothing in the UI holds a path.
 *
 * **There is no video role.** A video snap is a per-frame decode and a
 * full-screen fill behind a menu, which is the most expensive thing an ES theme
 * does and is excluded by the cheap-UI rule.
 */
enum class MediaRole { COVER, LOGO, SCREENSHOT, BANNER }

/**
 * A request for art at the size it will be drawn.
 *
 * **The size is part of the request** because decoding a 1200 px cover to draw
 * it at 96 px is exactly the waste the cheap-UI rule exists to prevent. A
 * path-based API cannot express this; it hands back a file and lets the caller
 * scale per frame.
 */
data class MediaRequest(
    val key: GameKey,
    val role: MediaRole,
    val widthPx: Int,
    val heightPx: Int,
) {
    init {
        require(widthPx > 0 && heightPx > 0) { "media size must be positive" }
    }

    /**
     * The cache key.
     *
     * **Sizes are bucketed to [BUCKET] pixels.** Without this, a list whose row
     * height changes by one pixel during layout decodes the same cover twice,
     * and a resize animation would decode it every frame. Bucketing up rather
     * than down keeps the decode at or above the drawn size, so art is
     * downscaled at draw time and never upscaled.
     */
    val cacheKey: String
        get() = "${key.storageId}/${role.name}@${bucket(widthPx)}x${bucket(heightPx)}"

    companion object {
        const val BUCKET = 32
        fun bucket(px: Int): Int = ((px + BUCKET - 1) / BUCKET) * BUCKET
    }
}

/**
 * Art lookup.
 *
 * Two calls, split for a reason: [available] answers "is there a cover" for a
 * badge without paying to decode one. That is the same `candidates` and
 * `locate` split the content resolver has, for the same reason.
 */
interface MediaStore {
    /**
     * Art, or null while it is not resident.
     *
     * **Null is normal, not an error.** The caller draws a placeholder and is
     * recomposed when the art arrives. This never blocks.
     */
    fun get(request: MediaRequest): CoverArt?

    /** Which roles have art, without loading any of it. */
    fun available(key: GameKey): Set<MediaRole>
}
