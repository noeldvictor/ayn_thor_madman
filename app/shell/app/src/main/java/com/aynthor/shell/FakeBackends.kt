package com.aynthor.shell

/**
 * Fake backends, written to test the contract rather than to run anything.
 *
 * The point is adversarial: pick the backends that differ most and see whether
 * one contract holds them without either pretending to be the other.
 *
 *  - melonDS: two guest screens, the lower one touch, three texture classes
 *    by producer, live cheat toggle.
 *  - Cemu: two guest screens where the second is often unused, graphic packs
 *    that carry textures and shaders and ASM patches together, no texture
 *    class concept at all.
 *  - ARMSX2: one screen, two texture classes, no packs.
 *
 * Where these disagree is where the contract earns its shape.
 */

/** DS. Two screens, both required, lower one touch. */
object FakeMelonDs : Backend {

    override val info = BackendInfo(
        id = "melonds-thor",
        name = "melonDS Thor",
        version = "0.1-fake",
        systems = listOf(System.NDS),
    )

    override fun identify(path: String): TitleId? =
        if (path.endsWith(".nds")) TitleId("AZEE", "Phantom Hourglass", "USA", "1.0") else null

    override fun supportedOps() = setOf(
        LifecycleOp.LOAD, LifecycleOp.RUN, LifecycleOp.PAUSE, LifecycleOp.STOP,
        LifecycleOp.SAVE_STATE, LifecycleOp.LOAD_STATE,
    )

    override fun guestScreens(title: TitleId) = listOf(
        GuestScreenSpec("top", 256, 192, takesTouch = false, requiredByTitle = true),
        GuestScreenSpec("bottom", 256, 192, takesTouch = true, requiredByTitle = true),
    )

    override fun settings() = listOf(
        SettingSpec(
            "melonds.filter.3d", "3D texture filter", SettingType.ENUM,
            "Image quality", "ScaleFX",
            listOf("Nearest", "Linear", "ScaleFX", "Anime4K", "MMPX", "xBR"),
            liveChangeable = true,
        ),
        SettingSpec(
            "melonds.filter.obj", "Sprite filter", SettingType.ENUM,
            "Image quality", "Anime4K",
            listOf("Nearest", "Linear", "ScaleFX", "Anime4K", "MMPX", "xBR"),
            liveChangeable = true,
        ),
        SettingSpec(
            "melonds.filter.bg", "Background filter", SettingType.ENUM,
            "Image quality", "Linear",
            listOf("Nearest", "Linear", "ScaleFX", "Anime4K", "MMPX", "xBR"),
            liveChangeable = true,
        ),
        SettingSpec(
            "melonds.audio.latency", "Audio latency", SettingType.ENUM,
            "Audio", "Medium", listOf("Low", "Medium", "High"),
        ),
    )

    override fun defaults() = settings().associate { it.key to it.default }

    override fun storage(title: TitleId) = listOf(
        StorageCategory("Game data", "/roms/nds", rebuildable = false),
        StorageCategory("Saves and states", "/saves/nds", rebuildable = false),
        StorageCategory("HD texture pack", "/packs/nds", rebuildable = false),
        StorageCategory("Texture cache", "/cache/nds/tex", rebuildable = true),
    )

    override fun counters() = setOf(Counter.FPS, Counter.FRAME_TIME_US)

    override fun cheats() = CheatSpec(formats = listOf("mch"), liveToggle = true)

    override fun patches(): PatchSpec? = null

    override fun extensions() = listOf(
        // The real per-producer split. Not "World and Ui".
        Extension.TextureClasses(listOf("3d", "obj_sprite", "bg_layer")),
        Extension.TexturePacks("content-hash"),
        Extension.HotSettings(listOf("melonds.filter.3d", "melonds.filter.obj")),
    )
}

/** Wii U. Second screen exists but many titles never draw it. */
object FakeCemu : Backend {

    override val info = BackendInfo(
        id = "cemu-thor",
        name = "Cemu Thor",
        version = "0.1-fake",
        systems = listOf(System.WIIU),
    )

    override fun identify(path: String): TitleId? =
        if (path.contains("wud") || path.contains("rpx")) {
            TitleId("0005000010176A00", "Star Fox Zero", "USA", "1.0")
        } else {
            null
        }

    override fun supportedOps() = setOf(
        LifecycleOp.LOAD, LifecycleOp.RUN, LifecycleOp.PAUSE, LifecycleOp.STOP,
        LifecycleOp.SAVE_STATE, LifecycleOp.LOAD_STATE,
    )

    /**
     * The GamePad screen is real but optional. Star Fox Zero uses it; most
     * titles do not. This is why [GuestScreenSpec.requiredByTitle] exists.
     */
    override fun guestScreens(title: TitleId) = listOf(
        GuestScreenSpec("tv", 1280, 720, takesTouch = false, requiredByTitle = true),
        GuestScreenSpec("gamepad", 854, 480, takesTouch = true, requiredByTitle = true),
    )

    override fun settings() = listOf(
        SettingSpec(
            "cemu.gfx.scale", "Internal resolution", SettingType.ENUM,
            "Image quality", "1x", listOf("1x", "1.5x", "2x"),
        ),
        SettingSpec(
            "cemu.shader.async", "Async shader compile", SettingType.BOOL,
            "Performance and power", "true",
        ),
        SettingSpec(
            "cemu.adpf", "Performance hints", SettingType.BOOL,
            "Performance and power", "true",
        ),
    )

    override fun defaults() = settings().associate { it.key to it.default }

    override fun storage(title: TitleId) = listOf(
        StorageCategory("Game data", "/roms/wiiu", rebuildable = false),
        StorageCategory("Saves and states", "/saves/wiiu", rebuildable = false),
        StorageCategory("Shader cache", "/cache/wiiu/shader", rebuildable = true),
        StorageCategory("Graphic packs", "/packs/wiiu", rebuildable = false),
    )

    override fun counters() = setOf(Counter.FPS, Counter.FRAME_TIME_US, Counter.DRAW_CALLS)

    override fun cheats() = CheatSpec(formats = listOf("graphicpack"), liveToggle = false)

    /** Cemu patches guest ASM at run time, through the same pack format. */
    override fun patches() = PatchSpec(format = "graphicpack", applyAtLoadOnly = false)

    override fun extensions() = listOf(
        // No texture classes. Cemu does not split by producer.
        Extension.GraphicPacks("cemu-graphicpack-v2"),
        Extension.HotSettings(listOf("cemu.gfx.scale")),
    )
}

/** PS2. One screen, two texture classes, no packs. */
object FakeArmsx2 : Backend {

    override val info = BackendInfo(
        id = "armsx2",
        name = "ARMSX2",
        version = "0.1-fake",
        systems = listOf(System.PS2),
    )

    override fun identify(path: String): TitleId? =
        if (path.endsWith(".iso")) TitleId("SCUS-97472", "Shadow of the Colossus", "USA", "1.0") else null

    override fun supportedOps() = setOf(
        LifecycleOp.LOAD, LifecycleOp.RUN, LifecycleOp.PAUSE, LifecycleOp.STOP,
        LifecycleOp.SAVE_STATE, LifecycleOp.LOAD_STATE,
    )

    override fun guestScreens(title: TitleId) = listOf(
        GuestScreenSpec("screen", 640, 448, takesTouch = false, requiredByTitle = true),
    )

    override fun settings() = listOf(
        SettingSpec(
            "armsx2.upscale.world", "World texture algorithm", SettingType.ENUM,
            "Image quality", "LanczosCAS",
            listOf("Bilinear", "LanczosCAS", "xBRZ", "ScaleFX", "Anime4K", "FSRCNN"),
        ),
        SettingSpec(
            "armsx2.upscale.ui", "UI texture algorithm", SettingType.ENUM,
            "Image quality", "MMPX",
            listOf("Bilinear", "LanczosCAS", "xBRZ", "ScaleFX", "Anime4K", "MMPX"),
        ),
        SettingSpec(
            "armsx2.upscale.scale", "Upscale factor", SettingType.ENUM,
            "Image quality", "2", listOf("1", "2", "4"),
        ),
    )

    override fun defaults() = settings().associate { it.key to it.default }

    override fun storage(title: TitleId) = listOf(
        StorageCategory("Game data", "/roms/ps2", rebuildable = false),
        StorageCategory("Saves and states", "/saves/ps2", rebuildable = false),
        StorageCategory("Texture cache", "/cache/ps2/tex", rebuildable = true),
        StorageCategory("Shader cache", "/cache/ps2/shader", rebuildable = true),
    )

    override fun counters() = setOf(Counter.FPS, Counter.FRAME_TIME_US, Counter.GUEST_CPU_PCT)

    override fun cheats() = CheatSpec(formats = listOf("pnach"), liveToggle = true)

    override fun patches() = PatchSpec(format = "pnach", applyAtLoadOnly = true)

    override fun extensions() = listOf(
        // Two classes, not three. The PS2 has no sprite plane.
        Extension.TextureClasses(listOf("world", "ui")),
        Extension.HotSettings(listOf("armsx2.upscale.scale")),
    )
}

object Backends {
    val all: List<Backend> = listOf(FakeMelonDs, FakeCemu, FakeArmsx2)

    /**
     * What the contract had to absorb, recorded from writing these three.
     *
     *  - Texture classes differ in count and meaning, and Cemu has none. The
     *    contract carries a declared list, never a fixed enum. Confirmed.
     *  - Cheat formats share nothing: mch, graphicpack, pnach. Only the
     *    capability generalises.
     *  - Cemu applies patches at run time; ARMSX2 at load only. The flag
     *    earns its place.
     *  - Counters differ per backend. A fixed readout would force a backend
     *    to report a number it does not have.
     *  - Every backend needed HotSettings, which suggests the overlay's quick
     *    settings are a real part of the contract rather than an extension.
     */
    val contractNotes = Unit
}
