package com.aynthor.shell

/**
 * Fake data for the shell.
 *
 * Everything here is invented. The point is to pin the shape of the data the
 * app needs, so the backend contract can be written from real screens instead
 * of from an argument. See app/SCREENS.md.
 */

/** A system the app can run. Maps to one backend. */
enum class System(val label: String, val backend: String) {
    PS2("PlayStation 2", "ARMSX2"),
    XBOX360("Xbox 360", "xenia-thor"),
    WIIU("Wii U", "Cemu-thor"),
    N3DS("Nintendo 3DS", "azahar-thor"),
    NDS("Nintendo DS", "melonDS-thor"),
    VITA("PlayStation Vita", "Vita3K-thor"),
    SWITCH("Nintendo Switch", "eden-thor"),
    PC("PC, Proton", "GameThor"),
}

/**
 * A guest screen a backend declares.
 *
 * The app owns routing. The backend only says what exists. This is the
 * contract entry that dual-screen routing needs.
 */
data class GuestScreen(
    val name: String,
    val width: Int,
    val height: Int,
    val takesTouch: Boolean,
    val requiredByTitle: Boolean,
)

/** How guest screens are placed on the two panels. Per game. */
enum class ScreenLayout(val label: String, val detail: String) {
    ONE_EACH("One per display", "Top on the main panel, bottom on Screen-2"),
    BOTH_MAIN("Both on main", "Screen-2 free for the companion view"),
    MAIN_ONLY("Main only", "Second guest screen hidden"),
    SWAPPED("Swapped", "Bottom on the main panel, top on Screen-2"),
}

/** What Screen-2 shows when the game does not need it. */
enum class Companion(val label: String) {
    CHEATS("Cheat list"),
    PERFORMANCE("Performance"),
    STORAGE("Storage"),
    NOTES("Notes"),
    NOTHING("Nothing"),
}

/** One storage category for a game. Rebuildable ones are safe to clear. */
data class StorageItem(
    val label: String,
    val megabytes: Int,
    val rebuildable: Boolean,
)

/**
 * Storage rollups for the whole library.
 *
 * This is the one screen in SCREENS.md with NO prior art anywhere in the fleet.
 * Several forks measure a directory; none aggregates per game and per category,
 * and none states the cost of clearing before offering it.
 *
 * Kept here as plain functions rather than in the screen, so the rules can be
 * tested without Compose.
 */
object StorageRollup {

    /** Games biggest first, because the real question is which one to delete. */
    fun byGame(games: List<Game>): List<Pair<Game, Int>> =
        games.map { it to it.totalMb }.sortedByDescending { it.second }

    /** One row per category, summed across the library, biggest first. */
    fun byCategory(games: List<Game>): List<StorageItem> =
        games.flatMap { it.storage }
            .groupBy { it.label }
            .map { (label, items) ->
                StorageItem(
                    label = label,
                    megabytes = items.sumOf { it.megabytes },
                    // A category is only clearable if EVERY instance is. One
                    // non-rebuildable member makes the whole row unsafe, which
                    // is the conservative direction and the only safe one.
                    rebuildable = items.all { it.rebuildable },
                )
            }
            .sortedByDescending { it.megabytes }

    /** What a bulk clear would actually free. Rebuildable rows only. */
    fun reclaimableMb(games: List<Game>): Int =
        games.flatMap { it.storage }.filter { it.rebuildable }.sumOf { it.megabytes }

    fun totalMb(games: List<Game>): Int = games.sumOf { it.totalMb }
}

/**
 * One game in the library.
 *
 * **Identity is [key], never the title.** The title is a resolved metadata
 * value, so correcting it moves nothing and breaks no override. See
 * app/GAME_DATA.md.
 */
data class Game(
    val key: GameKey,
    val meta: List<MetaLayer>,
    val guestScreens: List<GuestScreen>,
    val hasCheats: Boolean,
    val hasOverride: Boolean,
    val hasPack: Boolean,
    val hasPatch: Boolean,
    val storage: List<StorageItem>,
) {
    val system: System get() = key.system
    val title: String get() = MetadataResolver.displayTitle(key, meta)
    val sortKey: String get() = MetadataResolver.sortKey(key, meta)
    val totalMb: Int get() = storage.sumOf { it.megabytes }
    val isDualScreen: Boolean get() = guestScreens.size > 1
}

/**
 * Builds a fake entry.
 *
 * **The title ids below are illustrative placeholders in the right format, not
 * verified serials.** Nothing reads them yet; they exist so the shell exercises
 * identity-by-title rather than identity-by-name.
 */
private fun game(
    system: System,
    titleId: String,
    title: String,
    guestScreens: List<GuestScreen>,
    hasCheats: Boolean,
    hasOverride: Boolean,
    hasPack: Boolean,
    hasPatch: Boolean,
    storage: List<StorageItem>,
) = Game(
    key = GameKey(system, titleId),
    meta = listOf(MetaLayer(MetaSource.BUNDLED, mapOf(MetaField.TITLE to title))),
    guestScreens = guestScreens,
    hasCheats = hasCheats,
    hasOverride = hasOverride,
    hasPack = hasPack,
    hasPatch = hasPatch,
    storage = storage,
)

private fun single(w: Int, h: Int) =
    listOf(GuestScreen("screen", w, h, takesTouch = false, requiredByTitle = true))

private fun dual(tw: Int, th: Int, bw: Int, bh: Int) = listOf(
    GuestScreen("top", tw, th, takesTouch = false, requiredByTitle = true),
    GuestScreen("bottom", bw, bh, takesTouch = true, requiredByTitle = true),
)

/** The fake library. Systems match the real fleet. */
object Fake {

    val games: List<Game> = listOf(
        game(
            System.PS2, "SCUS-97472", "Shadow of the Colossus", single(640, 448),
            hasCheats = true, hasOverride = true, hasPack = true, hasPatch = false,
            storage = listOf(
                StorageItem("Game data", 4200, false),
                StorageItem("Saves and states", 38, false),
                StorageItem("HD texture pack", 1900, false),
                StorageItem("Texture cache", 420, true),
                StorageItem("Shader cache", 260, true),
                StorageItem("Screenshots", 74, true),
            ),
        ),
        game(
            System.XBOX360, "4D5307E6", "Blue Dragon", single(1280, 720),
            hasCheats = false, hasOverride = true, hasPack = false, hasPatch = true,
            storage = listOf(
                StorageItem("Game data", 7100, false),
                StorageItem("Saves and states", 22, false),
                StorageItem("Shader cache", 880, true),
                StorageItem("Recompiled code cache", 640, true),
                StorageItem("Screenshots", 31, true),
            ),
        ),
        game(
            System.WIIU, "0005000010180600", "Star Fox Zero", dual(854, 480, 854, 480),
            hasCheats = true, hasOverride = true, hasPack = false, hasPatch = true,
            storage = listOf(
                StorageItem("Game data", 3300, false),
                StorageItem("Saves and states", 12, false),
                StorageItem("Shader cache", 1400, true),
                StorageItem("Graphic packs", 6, false),
            ),
        ),
        game(
            System.N3DS, "00040000001A2F00", "Ever Oasis", dual(400, 240, 320, 240),
            hasCheats = true, hasOverride = false, hasPack = true, hasPatch = false,
            storage = listOf(
                StorageItem("Game data", 1700, false),
                StorageItem("Saves and states", 9, false),
                StorageItem("Custom textures", 740, false),
                StorageItem("Shader cache", 130, true),
            ),
        ),
        game(
            System.NDS, "NTR-AZHE", "The Legend of Zelda: Phantom Hourglass", dual(256, 192, 256, 192),
            hasCheats = true, hasOverride = true, hasPack = true, hasPatch = false,
            storage = listOf(
                StorageItem("Game data", 62, false),
                StorageItem("Saves and states", 6, false),
                StorageItem("HD texture pack", 310, false),
                StorageItem("Shader cache", 44, true),
            ),
        ),
        game(
            System.VITA, "PCSE00021", "Gravity Rush", single(960, 544),
            hasCheats = true, hasOverride = false, hasPack = false, hasPatch = false,
            storage = listOf(
                StorageItem("Game data", 2600, false),
                StorageItem("Saves and states", 15, false),
                StorageItem("Shader cache", 520, true),
            ),
        ),
        game(
            System.PC, "steam-329310", "Tokyo Xanadu eX+", single(1920, 1080),
            hasCheats = false, hasOverride = true, hasPack = false, hasPatch = true,
            storage = listOf(
                StorageItem("Game data", 12400, false),
                StorageItem("Saves and states", 41, false),
                StorageItem("Shader cache", 1100, true),
            ),
        ),
    )

    // settingGroups was a hardcoded list of setting names for the settings
    // screen. It is DELETED rather than left unused: the screen now reads real
    // SettingSpec from the backends and groups by SettingSpec.group, so the
    // fake list would only tempt someone to render it again.

    /** The pinned driver. See CLAUDE.md, The driver baseline. */
    const val PINNED_DRIVER = "turnip_mrpurple_T30-toasted"
    const val DRIVER_STATUS = "loaded, matches a7xx"
}
