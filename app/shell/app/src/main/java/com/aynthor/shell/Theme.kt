package com.aynthor.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/**
 * The app's colour roles.
 *
 * **Light is the default and light is the one that gets the attention.** The
 * person this is built for prefers it, and a dark-first design with a light
 * option bolted on is not the same thing as a light design.
 *
 * Seven roles, deliberately. A palette a person cannot hold in their head gets
 * used inconsistently, and this shell has to stay legible on two panels with
 * different peak luminance — 420 nits on the built-in screen, 500 on Screen-2.
 */
data class ThorColors(
    val bg: Color,
    val panel: Color,
    val line: Color,
    val text: Color,
    val dim: Color,
    val accent: Color,
    val warn: Color,
)

/**
 * Light.
 *
 * **Not white.** A pure `#FFFFFF` background under a handheld's brightness is
 * fatiguing, and the Thor's panels reach 420 and 500 nits. This is a warm
 * near-white with a panel one step lighter, so cards lift off the page rather
 * than being outlined.
 */
val ThorLight = ThorColors(
    bg = Color(0xFFF4F5F7),
    panel = Color(0xFFFFFFFF),
    line = Color(0xFFDCE0E5),
    text = Color(0xFF1B1F24),
    dim = Color(0xFF5C6773),
    accent = Color(0xFF1F6FEB),
    warn = Color(0xFF9A6700),
)

/**
 * Dark, kept as an option rather than as the default.
 *
 * The accent and warn hues are shifted, not reused: `#1F6FEB` is legible on
 * near-white and muddy on near-black, and `#9A6700` is unreadable there.
 */
val ThorDark = ThorColors(
    bg = Color(0xFF0E1116),
    panel = Color(0xFF161B22),
    line = Color(0xFF2A313A),
    text = Color(0xFFE6EDF3),
    dim = Color(0xFF9BA7B4),
    accent = Color(0xFF4C8DF6),
    warn = Color(0xFFE3A008),
)

val LocalThorColors = compositionLocalOf { ThorLight }

/**
 * Whether the app is in dark mode.
 *
 * **Defaults to false.** Held here rather than in the activity so Screen-2 and
 * the settings screen read the same value.
 */
val darkMode: MutableState<Boolean> = mutableStateOf(false)

@Composable
fun ThorTheme(dark: Boolean = darkMode.value, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalThorColors provides if (dark) ThorDark else ThorLight,
        content = content,
    )
}

// The palette, read from the theme so every screen follows the light/dark
// switch. Internal rather than private so all UI files share one definition.
internal val Bg: Color @Composable get() = LocalThorColors.current.bg
internal val Panel: Color @Composable get() = LocalThorColors.current.panel
internal val Line: Color @Composable get() = LocalThorColors.current.line
internal val Text: Color @Composable get() = LocalThorColors.current.text
internal val Dim: Color @Composable get() = LocalThorColors.current.dim
internal val Accent: Color @Composable get() = LocalThorColors.current.accent
internal val Warn: Color @Composable get() = LocalThorColors.current.warn
