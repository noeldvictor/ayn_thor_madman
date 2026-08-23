package com.aynthor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0E1116)
private val Panel = Color(0xFF161B22)
private val Line = Color(0xFF2A313A)
private val Text = Color(0xFFE6EDF3)
private val Dim = Color(0xFF9BA7B4)
private val Accent = Color(0xFF4C8DF6)
private val Warn = Color(0xFFE3A008)

private sealed interface Route {
    data object Library : Route
    data class Detail(val game: Game) : Route
    data object Settings : Route
    data object Storage : Route
    data object Drivers : Route
    data object Systems : Route
}

@Composable
fun ShellApp(activity: MainActivity) {
    var route by remember { mutableStateOf<Route>(Route.Library) }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            TopBar(
                route = route,
                onHome = { route = Route.Library },
                onSettings = { route = Route.Settings },
                onStorage = { route = Route.Storage },
                onDrivers = { route = Route.Drivers },
                onSystems = { route = Route.Systems },
            )
            Box(Modifier.weight(1f)) {
                when (val r = route) {
                    is Route.Library -> LibraryScreen(activity) { route = Route.Detail(it) }
                    is Route.Detail -> DetailScreen(activity, r.game)
                    is Route.Settings -> SettingsScreen(activity)
                    is Route.Storage -> StorageScreen()
                    is Route.Drivers -> DriversScreen()
                    is Route.Systems -> SystemsScreen()
                }
            }
            StatusBar(activity)
        }
    }
}

@Composable
private fun TopBar(
    route: Route,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onStorage: () -> Unit,
    onDrivers: () -> Unit,
    onSystems: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(Panel).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("AYN THOR", color = Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.width(6.dp))
        Text("shell", color = Dim, fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        NavChip("Library", route is Route.Library, onHome)
        Spacer(Modifier.width(8.dp))
        NavChip("Settings", route is Route.Settings, onSettings)
        Spacer(Modifier.width(8.dp))
        NavChip("Storage", route is Route.Storage, onStorage)
        Spacer(Modifier.width(8.dp))
        NavChip("Drivers", route is Route.Drivers, onDrivers)
        Spacer(Modifier.width(8.dp))
        NavChip("Systems", route is Route.Systems, onSystems)
    }
}

@Composable
private fun NavChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Accent.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (active) Accent else Line, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = if (active) Accent else Dim, fontSize = 14.sp)
    }
}

@Composable
private fun StatusBar(activity: MainActivity) {
    Row(
        Modifier.fillMaxWidth().background(Panel).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Screen-2: ${activity.screen2Status}", color = Dim, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text("driver: ${Fake.PINNED_DRIVER}", color = Dim, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------- library

@Composable
private fun LibraryScreen(activity: MainActivity, onOpen: (Game) -> Unit) {
    var sortBySize by remember { mutableStateOf(false) }
    val games = remember(sortBySize) {
        if (sortBySize) Fake.games.sortedByDescending { it.totalMb } else Fake.games
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Library", color = Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(12.dp))
            Text("${Fake.games.size} games across ${System.entries.size} systems", color = Dim, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            NavChip(if (sortBySize) "Sorted by size" else "Sort by size", sortBySize) {
                sortBySize = !sortBySize
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            items(games) { g ->
                GameRow(g) {
                    activity.pushToScreen2(
                        g.title,
                        listOf(
                            g.system.label + "  ·  " + g.system.backend,
                            "",
                            if (g.hasCheats) "Cheats available" else "No cheats",
                            if (g.hasOverride) "Per-game override set" else "No override",
                            if (g.hasPack) "HD pack installed" else "No HD pack",
                            if (g.hasPatch) "Patch applied" else "No patch",
                            "",
                            "${g.totalMb} MB total",
                            if (g.isDualScreen) "Dual screen title" else "Single screen title",
                        ),
                    )
                    onOpen(g)
                }
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun GameRow(g: Game, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cover art placeholder. Where art comes from is an open question.
        Box(
            Modifier.size(46.dp, 62.dp).clip(RoundedCornerShape(4.dp)).background(Line),
            contentAlignment = Alignment.Center,
        ) {
            Text(g.system.name.take(3), color = Dim, fontSize = 11.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(g.title, color = Text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text("${g.system.label}  ·  ${g.system.backend}", color = Dim, fontSize = 12.sp)
            Spacer(Modifier.height(7.dp))
            Row {
                if (g.hasCheats) Badge("cheats")
                if (g.hasOverride) Badge("override")
                if (g.hasPack) Badge("HD pack")
                if (g.hasPatch) Badge("patch")
                if (g.isDualScreen) Badge("2 screens", Accent)
            }
        }
        Text("${g.totalMb} MB", color = Dim, fontSize = 13.sp)
    }
}

@Composable
private fun Badge(label: String, color: Color = Dim) {
    Box(
        Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(label, color = color, fontSize = 10.sp)
    }
}

// ----------------------------------------------------------------- detail

@Composable
private fun DetailScreen(activity: MainActivity, g: Game) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(g.title, color = Text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("${g.system.label}  ·  backend ${g.system.backend}", color = Dim, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        Section("Display and layout") {
            Text(
                if (g.isDualScreen)
                    "This title has ${g.guestScreens.size} guest screens. The app routes them."
                else
                    "Single guest screen. Screen-2 is free for a companion view.",
                color = Dim, fontSize = 13.sp,
            )
            Spacer(Modifier.height(10.dp))
            g.guestScreens.forEach { s ->
                Text(
                    "· ${s.name}  ${s.width}x${s.height}" +
                        (if (s.takesTouch) "  touch" else "") +
                        (if (s.requiredByTitle) "  required" else "  optional"),
                    color = Dim, fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (g.isDualScreen) {
                val current = activity.layoutChoice[g.title] ?: ScreenLayout.ONE_EACH
                ScreenLayout.entries.forEach { l ->
                    ChoiceRow(l.label, l.detail, current == l) {
                        activity.layoutChoice[g.title] = l
                        activity.pushToScreen2(g.title, listOf("Layout", "", l.label, l.detail))
                    }
                }
            } else {
                val current = activity.companionChoice[g.title] ?: Companion.CHEATS
                Text("Screen-2 shows", color = Text, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Companion.entries.forEach { c ->
                    ChoiceRow(c.label, "", current == c) {
                        activity.companionChoice[g.title] = c
                        activity.pushToScreen2(g.title, listOf("Screen-2", "", c.label))
                    }
                }
            }
        }

        Section("Storage  ·  ${g.totalMb} MB") {
            g.storage.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(item.label, color = Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (item.rebuildable) Badge("rebuildable") else Badge("keep", Warn)
                    Text("${item.megabytes} MB", color = Dim, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Clearing a rebuildable cache frees space and brings the stutter back " +
                    "until it rebuilds. Saves and states are never cleared here.",
                color = Warn, fontSize = 12.sp,
            )
        }

        Section("Cheats and patches") {
            Text(
                if (g.hasCheats) "Cheats available for this title." else "No cheats found.",
                color = Dim, fontSize = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (g.hasPatch) "One patch applied. Intent: speed." else "No patches applied.",
                color = Dim, fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun ChoiceRow(label: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (selected) Accent else Line, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = if (selected) Accent else Text, fontSize = 13.sp)
            if (detail.isNotEmpty()) {
                Text(detail, color = Dim, fontSize = 11.sp)
            }
        }
        if (selected) Text("selected", color = Accent, fontSize = 11.sp)
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(16.dp),
    ) {
        Text(title, color = Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

// --------------------------------------------------------------- settings

/**
 * Settings, rendered from the real contract.
 *
 * This screen exists to EXERCISE the contract, not to look finished. It reads
 * SettingSpec from every backend, resolves each value through
 * [SettingResolver], and shows which tier answered. If a row says the wrong
 * thing, the contract is wrong.
 *
 * Tapping a row sets a per-game override through
 * [SettingResolver.writeOverride], so the sparse, sticky and change-tracked
 * rules are driven by the UI rather than only by the tests.
 */
@Composable
private fun SettingsScreen(activity: MainActivity) {
    val specs = remember { Backends.all.flatMap { it.settings() } }
    val defaults = remember { Backends.all.flatMap { it.defaults().entries }.associate { it.key to it.value } }
    // A stand-in for the two stored tiers. Fake persistence, like the rest of
    // the shell.
    val global = activity.globalSettings
    val perGame = activity.perGameSettings

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Settings", color = Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "One schema for every system. Resolved live through the contract: " +
                "per-game, then global, then Thor profile, then backend default.",
            color = Dim, fontSize = 13.sp,
        )
        Spacer(Modifier.height(18.dp))

        specs.groupBy { it.group }.forEach { (group, rows) ->
            Section(group) {
                rows.forEach { spec ->
                    val resolved = SettingResolver.resolve(
                        spec,
                        perGame = perGame,
                        global = global,
                        thorProfile = emptyMap(),
                        backendDefault = defaults,
                    )
                    SettingRow(spec, resolved) {
                        // Cycle to the next option, in game scope.
                        val current = resolved?.value ?: spec.default
                        val next = spec.options
                            .getOrNull((spec.options.indexOf(current) + 1) % spec.options.size.coerceAtLeast(1))
                            ?: if (current == "true") "false" else "true"

                        val before = specs.associate { s ->
                            s.key to (
                                SettingResolver.resolve(s, perGame, global, emptyMap(), defaults)
                                    ?.value ?: s.default
                                )
                        }
                        val after = before + (spec.key to next)
                        val out = SettingResolver.writeOverride(
                            specs, perGame, global, after, before,
                        )
                        activity.perGameSettings = out.perGame
                        // A promoted field leaves the per-game tier entirely.
                        if (out.globalPatch.isNotEmpty()) {
                            activity.globalSettings = global + out.globalPatch
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Overrides held: ${perGame.size}. Global values set: ${global.size}.",
            color = Dim, fontSize = 11.sp,
        )
    }
}

@Composable
private fun SettingRow(
    spec: SettingSpec,
    resolved: ResolvedSetting?,
    onCycle: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onCycle() }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(spec.label, color = Text, fontSize = 13.sp)
            Row {
                // The scope is visible, because it changes what the row does.
                when (spec.scope) {
                    SettingScope.PROMOTED -> Badge("global-backed")
                    SettingScope.GLOBAL_ONLY -> Badge("global only")
                    SettingScope.PER_GAME -> Unit
                }
                if (!spec.liveChangeable) Badge("needs restart")
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(resolved?.value ?: "unset", color = Text, fontSize = 12.sp)
            Text(
                when (resolved?.source) {
                    SettingSource.PER_GAME -> "per game"
                    SettingSource.GLOBAL -> "global"
                    SettingSource.THOR_PROFILE -> "Thor profile"
                    SettingSource.BACKEND_DEFAULT -> "backend default"
                    null -> "no value"
                },
                color = Dim, fontSize = 10.sp,
            )
        }
    }
}

// ---------------------------------------------------------------- drivers

@Composable
private fun DriversScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Drivers", color = Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Section("Pinned driver") {
            Text(Fake.PINNED_DRIVER, color = Text, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(Fake.DRIVER_STATUS, color = Accent, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "One pinned build is the reference configuration. Every measurement " +
                    "states this driver. A different driver is a per-game override.",
                color = Dim, fontSize = 12.sp,
            )
        }
        Section("Validation") {
            Text("Target GPU: Adreno 740, a7xx", color = Text, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Builds that target a8xx are rejected. Two are present on this device " +
                    "and neither will be offered.",
                color = Warn, fontSize = 12.sp,
            )
        }
    }
}


// ---------------------------------------------------------------- systems

/**
 * What each backend declares through the contract.
 *
 * This screen exists to check the contract rather than to serve a user. If a
 * backend cannot describe itself here, the contract is missing something.
 */
@Composable
private fun SystemsScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Systems", color = Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "What each backend declares. The app renders extension UI only when " +
                "the extension is present.",
            color = Dim, fontSize = 13.sp,
        )
        Spacer(Modifier.height(18.dp))

        Backends.all.forEach { b ->
            Section("${b.info.name}  ·  ${b.info.version}") {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    b.info.systems.forEach { Badge(it.label, Accent) }
                }
                Kv("Lifecycle", b.supportedOps().joinToString(", ") { it.name.lowercase() })
                Kv("Settings", "${b.settings().size} keys")
                Kv("Counters", b.counters().joinToString(", ") { it.name.lowercase() })
                Kv(
                    "Cheats",
                    b.cheats()?.let { c ->
                        c.formats.joinToString(", ") +
                            (if (c.liveToggle) ", live toggle" else ", restart needed")
                    } ?: "none",
                )
                Kv(
                    "Patches",
                    b.patches()?.let { p ->
                        p.format + (if (p.applyAtLoadOnly) ", at load only" else ", at run time")
                    } ?: "none",
                )
                Kv("Storage", b.storage(TitleId("x", "x", null, null)).size.toString() + " categories")

                Spacer(Modifier.height(8.dp))
                Text("Extensions", color = Text, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                if (b.extensions().isEmpty()) {
                    Text("none declared", color = Dim, fontSize = 12.sp)
                }
                b.extensions().forEach { e ->
                    val line = when (e) {
                        is Extension.TextureClasses ->
                            "Texture classes: " + e.classes.joinToString(", ")
                        is Extension.TexturePacks -> "Texture packs: " + e.format
                        is Extension.GraphicPacks -> "Graphic packs: " + e.format
                        is Extension.HotSettings ->
                            "Overlay quick settings: " + e.keys.size
                    }
                    Text("· " + line, color = Dim, fontSize = 12.sp)
                }
            }
        }

        Section("PlayStation 3") {
            Text("Deferred. Optional separate install.", color = Warn, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Every PS3 emulator is GPL-2.0-only and cannot share this binary. " +
                    "See CLAUDE.md, Licences.",
                color = Dim, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun Kv(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, color = Dim, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Text(v, color = Text, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------- storage

/**
 * Storage across the whole library.
 *
 * The one screen in SCREENS.md with no prior art in the fleet, so it is
 * designed here rather than harvested. Three rules, all from CLAUDE.md:
 *
 *  - **Sort by size, biggest first.** The question a person actually has is
 *    "which game should I delete", not "how big is this one".
 *  - **A cache is an asset, not junk.** State the cost BEFORE the action.
 *  - **Never put saves near a bulk action.** They are the only irreplaceable
 *    category, so they are excluded from every total that a button acts on.
 */
@Composable
private fun StorageScreen() {
    val games = Fake.games
    val total = StorageRollup.totalMb(games)
    val reclaimable = StorageRollup.reclaimableMb(games)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Storage", color = Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "$total MB across ${games.size} games. $reclaimable MB is rebuildable.",
            color = Dim, fontSize = 13.sp,
        )
        Spacer(Modifier.height(18.dp))

        Section("Biggest games first") {
            StorageRollup.byGame(games).forEach { (game, mb) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(game.title, color = Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("$mb MB", color = Dim, fontSize = 13.sp)
                }
            }
        }

        Section("By category, across every system") {
            StorageRollup.byCategory(games).forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.label, color = Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    if (item.rebuildable) Badge("rebuildable") else Badge("keep", Warn)
                    Text("${item.megabytes} MB", color = Dim, fontSize = 13.sp)
                }
            }
        }

        Section("Clearing") {
            // The cost, stated before the action, per CLAUDE.md.
            StorageRollup.byCategory(games).filter { it.rebuildable }.forEach { item ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(
                        "Clear ${item.label.lowercase()}. Frees ${item.megabytes} MB.",
                        color = Text, fontSize = 13.sp,
                    )
                    Text(costOf(item.label), color = Warn, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "There is no clean-up button. Every category costs something different, " +
                    "and saves and states are never offered here.",
                color = Warn, fontSize = 12.sp,
            )
        }
    }
}

/**
 * What clearing a category actually costs.
 *
 * A size alone is not enough to decide with. This is the sentence that turns a
 * number into a decision, and it is why a single "free up space" button is
 * refused.
 */
private fun costOf(label: String): String = when {
    label.contains("Shader", true) ->
        "Shader stutter returns until the cache rebuilds."
    label.contains("Texture cache", true) ->
        "Textures are re-upscaled on next load. First minutes are slower."
    label.contains("code", true) || label.contains("Recompiled", true) ->
        "Guest code is re-translated. The next boot is slower."
    label.contains("Screenshot", true) ->
        "The images are gone. Nothing rebuilds them."
    else -> "Rebuilt on demand."
}
