package com.aynthor.shell

/**
 * The cover art store.
 *
 * Implements the media half of `app/GAME_DATA.md`. **The cache is the feature,
 * not an optimisation.** The cheap-UI rule says cover art is decoded once at
 * display size and never scaled per frame, and a store without a cache breaks
 * that rule on the first scroll.
 */

/**
 * One piece of resident art.
 *
 * **This is a placeholder shape, not a bitmap.** Real decoding is not built,
 * so the shell carries what it needs to draw a stand-in: the size it was
 * prepared for, and a hue.
 *
 * **The hue is stored, not the colour.** A resolved colour would be wrong the
 * moment the person switches to dark mode, and the whole cache would have to be
 * thrown away to fix it. Storing the hue means the theme resolves it at draw
 * time and **the cache survives a theme switch.**
 *
 * When real art arrives this gains an `ImageBitmap` and keeps everything else.
 */
data class CoverArt(
    val cacheKey: String,
    val widthPx: Int,
    val heightPx: Int,
    val hue: Float,
)

/**
 * A bounded, least-recently-used art cache.
 *
 * **Bounded because a library is unbounded.** Several hundred games at two
 * sizes each would exhaust the budget, and this device has one memory budget
 * with one owner.
 */
class InMemoryMediaStore(
    private val source: MediaSource,
    private val maxEntries: Int = 64,
) : MediaStore {

    /** Counted so a test can prove the cache works. See MediaCacheTest. */
    var decodes: Int = 0
        private set

    private val cache = object : LinkedHashMap<String, CoverArt>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CoverArt>) =
            size > maxEntries
    }

    override fun get(request: MediaRequest): CoverArt? {
        if (request.role !in source.available(request.key)) return null
        cache[request.cacheKey]?.let { return it }
        val art = source.decode(request)
        decodes++
        cache[request.cacheKey] = art
        return art
    }

    override fun available(key: GameKey): Set<MediaRole> = source.available(key)

    /** Test and diagnostics only. */
    fun residentCount(): Int = cache.size

    fun isResident(request: MediaRequest): Boolean = request.cacheKey in cache
}

/**
 * Where art comes from.
 *
 * Separated from the cache so the cache can be tested without a decoder, and so
 * a real decoder can replace this without touching cache behaviour.
 */
interface MediaSource {
    fun available(key: GameKey): Set<MediaRole>
    fun decode(request: MediaRequest): CoverArt
}

/**
 * Stand-in art for the shell.
 *
 * **Not every game has every role, on purpose.** A cover UI that is only ever
 * tested with art present ships a broken empty state, and the empty state is
 * the common case for a library of personal dumps.
 */
object FakeMediaSource : MediaSource {

    /** Games with no cover at all, so the placeholder path is always visible. */
    private val noCover = setOf("SCUS-97472", "steam-329310")

    override fun available(key: GameKey): Set<MediaRole> = buildSet {
        if (key.titleId !in noCover) add(MediaRole.COVER)
        add(MediaRole.LOGO)
    }

    override fun decode(request: MediaRequest): CoverArt = CoverArt(
        cacheKey = request.cacheKey,
        widthPx = MediaRequest.bucket(request.widthPx),
        heightPx = MediaRequest.bucket(request.heightPx),
        hue = hueFor(request.key),
    )

    /**
     * A stable colour per game.
     *
     * **Stability matters more than the specific colour.** A placeholder that
     * changed hue between launches would read as a rendering fault. `hashCode`
     * on a String is specified by the Java language, so this is stable across
     * runs and across devices.
     */
    fun hueFor(key: GameKey): Float {
        val h = key.storageId.hashCode()
        return ((h % 360) + 360) % 360 / 360f
    }
}

/**
 * Up to two initials for a title.
 *
 * Skips articles, because a shelf of placeholders all reading "TH" tells the
 * person nothing.
 */
fun initialsFor(title: String): String {
    val skip = setOf("the", "a", "an", "of", "and")
    val words = title.split(' ', ':', '-')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.lowercase() !in skip }
    if (words.isEmpty()) return title.take(1).uppercase()
    return words.take(2).joinToString("") { it.first().uppercase() }
}
