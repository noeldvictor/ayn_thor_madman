package com.aynthor.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Cover art in the library.
 *
 * **Every choice here is the cheap-UI rule rather than taste:**
 *
 *  - **The size is known before layout**, converted from `Dp` with the density,
 *    so the store is asked once for the size that will actually be drawn.
 *    Measuring first would mean a zero-size pass, then a decode, then a
 *    recomposition, for every row.
 *  - **A fixed slot, art fitted inside it.** Rows stay one height, which is
 *    what makes a lazy list cheap to scroll. Box art aspect differs per system
 *    and must not be allowed to change row height.
 *  - **No shadow and no blur.** A one-pixel border separates the slot from the
 *    panel, which is what the `line` role exists for.
 *  - **No crossfade when art arrives.** An animation per row is per-frame work
 *    for a cosmetic transition, and a library scroll would run dozens at once.
 *  - **The placeholder is one opaque fill.** No gradient, no stacked layer.
 */

/** Injected so a real store replaces the fake one without touching any screen. */
val LocalMediaStore = staticCompositionLocalOf<MediaStore> {
    InMemoryMediaStore(FakeMediaSource)
}

@Composable
fun CoverSlot(
    game: Game,
    width: Dp,
    height: Dp,
    role: MediaRole = MediaRole.COVER,
    corner: Dp = 4.dp,
) {
    val store = LocalMediaStore.current
    val density = LocalDensity.current

    // Pixel size is derived, not measured. See the note above.
    val widthPx = with(density) { width.roundToPx() }
    val heightPx = with(density) { height.roundToPx() }

    val art = remember(game.key, role, widthPx, heightPx) {
        store.get(MediaRequest(game.key, role, widthPx, heightPx))
    }
    val shape = RoundedCornerShape(corner)
    val label = remember(game.title) { initialsFor(game.title) }

    Box(
        Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(if (art != null) coverFill(art.hue) else Panel)
            .then(if (art == null) Modifier.border(1.dp, Line, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (art != null) coverInk(art.hue) else Dim,
            fontSize = (height.value * 0.30f).coerceIn(9f, 34f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * The placeholder fill for a hue.
 *
 * **Resolved at draw time from the stored hue**, so switching theme repaints
 * without discarding a single cached entry.
 *
 * Saturation and lightness are fixed. Only the hue varies, so a shelf of
 * placeholders reads as one family rather than as confetti, and nothing in it
 * competes with the accent colour.
 */
@Composable
private fun coverFill(hue: Float): Color =
    if (darkMode.value) hsl(hue, 0.32f, 0.30f) else hsl(hue, 0.34f, 0.76f)

/** Ink that stays legible on [coverFill] in both themes. */
@Composable
private fun coverInk(hue: Float): Color =
    if (darkMode.value) hsl(hue, 0.30f, 0.78f) else hsl(hue, 0.45f, 0.26f)

/**
 * HSL to RGB.
 *
 * Written out rather than pulled from a library: this is the only colour
 * conversion in the shell, and a dependency for eleven lines is not worth the
 * link cost in a binary that already has seven emulators in it.
 */
private fun hsl(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val hp = h * 6f
    val x = c * (1f - abs(hp % 2f - 1f))
    val (r, g, b) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(r + m, g + m, b + m)
}
