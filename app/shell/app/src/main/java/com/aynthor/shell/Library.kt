package com.aynthor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The library: system first, then games.
 *
 * **EmulationStation's navigation model**, rendered in the light palette. See
 * CLAUDE.md, the working rules.
 *
 * **Cheap by construction**, and every choice here is that rule rather than
 * taste:
 *
 *  - **Nothing animates.** No carousel easing, no crossfade, no idle motion.
 *    Selection is a colour change and a left rule, both free.
 *  - **No shadow and no blur.** Panels are separated by a flat fill and a
 *    one-pixel line, which is why the light palette has a `line` role.
 *  - **One opaque background.** No stacked translucency, so a tiler pays for
 *    each pixel once.
 *  - **Both lists are lazy and keyed**, so scrolling recomposes rows instead of
 *    rebuilding them.
 *  - **Cover art is decoded once at display size** and cached, never decoded at
 *    full resolution and scaled per frame. See Cover.kt.
 */

/** `null` means the All entry. */
private data class SystemTab(val system: System?, val label: String, val count: Int)

@Composable
fun LibraryScreen(activity: MainActivity, onOpen: (Game) -> Unit) {
    var selected by remember { mutableStateOf<System?>(null) }
    var focused by remember { mutableStateOf<Game?>(null) }

    val tabs = remember {
        buildList {
            add(SystemTab(null, "All", Fake.games.size))
            System.entries.forEach { sys ->
                add(SystemTab(sys, sys.label, Fake.games.count { it.system == sys }))
            }
        }
    }
    val games = remember(selected) {
        if (selected == null) Fake.games else Fake.games.filter { it.system == selected }
    }

    Column(Modifier.fillMaxSize()) {
        SystemStrip(tabs, selected) { selected = it; focused = null }
        Divider()
        Row(Modifier.fillMaxSize()) {
            GameList(
                games = games,
                focused = focused,
                modifier = Modifier.weight(1.4f),
                onFocus = { g ->
                    focused = g
                    activity.pushToScreen2(g.title, screen2Lines(g))
                },
                onOpen = onOpen,
            )
            VerticalRule()
            MetadataPanel(focused, selected, games.size, Modifier.weight(1f))
        }
    }
}

/**
 * The system selector.
 *
 * **The top level, not a filter bolted onto a mixed list.** A horizontal strip
 * rather than ES's rotating carousel: the carousel exists to show one large
 * logo at a time on a TV, and on a handheld held at arm's length a strip shows
 * more systems for less drawing.
 */
@Composable
private fun SystemStrip(
    tabs: List<SystemTab>,
    selected: System?,
    onSelect: (System?) -> Unit,
) {
    LazyRow(
        Modifier.fillMaxWidth().background(Panel).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
    ) {
        items(tabs, key = { it.label }) { tab ->
            val active = tab.system == selected
            Column(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) Accent.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(tab.system) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    tab.label,
                    color = if (active) Accent else Text,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (tab.count == 1) "1 game" else "${tab.count} games",
                    color = Dim,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun GameList(
    games: List<Game>,
    focused: Game?,
    modifier: Modifier,
    onFocus: (Game) -> Unit,
    onOpen: (Game) -> Unit,
) {
    if (games.isEmpty()) {
        Box(modifier.fillMaxHeight().padding(24.dp)) {
            Text("No games for this system yet.", color = Dim, fontSize = 13.sp)
        }
        return
    }
    LazyColumn(modifier.fillMaxHeight()) {
        // Keyed by identity, not by title. A key that is a display string
        // breaks the moment two systems share a name or a title is corrected.
        items(games, key = { it.key.storageId }) { g ->
            val active = g.key == focused?.key
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (active) Accent.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { if (active) onOpen(g) else onFocus(g) }
                    .padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A left rule marks selection. A shadow would cost fill rate;
                // three pixels of accent costs nothing.
                Box(
                    Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(if (active) Accent else Color.Transparent),
                )
                Spacer(Modifier.width(12.dp))
                CoverSlot(g, width = 34.dp, height = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.title, color = Text, fontSize = 14.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(g.system.label, color = Dim, fontSize = 11.sp)
                }
                if (g.hasCheats) Dot(Accent)
                if (g.hasPack) Dot(Warn)
                if (g.hasOverride) Dot(Dim)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** Badges as dots: three characters of colour instead of three drawn pills. */
@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
            .width(7.dp)
            .height(7.dp),
    )
}

@Composable
private fun MetadataPanel(
    game: Game?,
    selected: System?,
    count: Int,
    modifier: Modifier,
) {
    // Scrolls, because the content is taller than the panel. At 1080p
    // landscape the panel is about 355 dp of usable height and a selected game
    // needs roughly 445 dp of it, before any real description arrives. A fixed
    // panel would silently clip the last rows.
    Column(
        modifier
            .fillMaxHeight()
            .background(Panel)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        if (game == null) {
            Text(
                selected?.label ?: "All systems",
                color = Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (count == 1) "1 game" else "$count games",
                color = Dim, fontSize = 13.sp,
            )
            Spacer(Modifier.height(18.dp))
            Text("Select a game to see its details.", color = Dim, fontSize = 12.sp)
            return@Column
        }

        // The slot is fixed and the art fits inside it. Letting box art decide
        // the panel height would move every row below it per selection.
        CoverSlot(game, width = 132.dp, height = 180.dp, corner = 6.dp)
        Spacer(Modifier.height(14.dp))
        Text(game.title, color = Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("${game.system.label}  ·  ${game.system.backend}", color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))

        Meta("Size", "${game.totalMb} MB")
        Meta("Screens", if (game.isDualScreen) "Dual, lower is touch" else "Single")
        Meta("Cheats", if (game.hasCheats) "Available" else "None found")
        Meta("Overrides", if (game.hasOverride) "Set for this game" else "Inheriting")
        Meta("HD pack", if (game.hasPack) "Installed" else "None")
        Meta("Patches", if (game.hasPatch) "One applied" else "None")

        Spacer(Modifier.height(18.dp))
        Text("Select again to open.", color = Dim, fontSize = 11.sp)
    }
}

@Composable
private fun Meta(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = Dim, fontSize = 12.sp, modifier = Modifier.width(88.dp))
        Text(value, color = Text, fontSize = 12.sp)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
private fun VerticalRule() {
    Box(Modifier.width(1.dp).fillMaxHeight().background(Line))
}

private fun screen2Lines(g: Game): List<String> = listOf(
    g.system.label + "  ·  " + g.system.backend,
    "",
    if (g.hasCheats) "Cheats available" else "No cheats",
    if (g.hasOverride) "Per-game override set" else "No override",
    if (g.hasPack) "HD pack installed" else "No HD pack",
    if (g.hasPatch) "Patch applied" else "No patch",
    "",
    "${g.totalMb} MB total",
    if (g.isDualScreen) "Dual screen title" else "Single screen title",
)
