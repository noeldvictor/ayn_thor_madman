package com.aynthor.shell

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf

/**
 * The shell.
 *
 * One activity on the main panel, one Presentation on Screen-2. The Thor has
 * two internal displays; see CLAUDE.md, Two displays.
 *
 * Screen-2 uses classic Views on purpose. Compose inside a Presentation needs
 * ViewTree owners wired by hand, and that is not what this shell is proving.
 * Convert it once the layout is settled.
 */
class MainActivity : ComponentActivity() {

    private var presentation: Screen2Presentation? = null

    /** Per-game layout choices. Fake persistence; the shell holds them in memory. */
    val layoutChoice: SnapshotStateMap<String, ScreenLayout> = mutableStateMapOf()
    val companionChoice: SnapshotStateMap<String, Companion> = mutableStateMapOf()

    /**
     * The two settings tiers, stand-ins for real storage.
     *
     * Separate on purpose: the override rules only mean anything when global
     * and per-game are distinct places. See Backend.kt SettingResolver.
     */
    var globalSettings by mutableStateOf<Map<String, String>>(emptyMap())
    var perGameSettings by mutableStateOf<Map<String, String>>(emptyMap())

    /** What Screen-2 is currently told to show. */
    var screen2Title by mutableStateOf("AYN Thor Shell")
    var screen2Lines by mutableStateOf(listOf("No game selected"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShellApp(this) }
    }

    /**
     * onResume and onPause, not onStart and onStop.
     *
     * A Presentation belongs to the app's window token and is NOT torn down
     * when the activity merely stops, so a panel can sit on Screen-2 showing
     * stale content while the person is somewhere else entirely. ARMSX2 shipped
     * that bug and fixed it here; see research_log/20260822_2154.
     */
    override fun onResume() {
        super.onResume()
        attachSecondDisplay()
    }

    override fun onPause() {
        super.onPause()
        detachSecondDisplay()
    }

    private var displayListener: DisplayManager.DisplayListener? = null
    private var presentationDisplayId: Int = Display.INVALID_DISPLAY

    /**
     * Put a Presentation on a display the activity is not already on.
     *
     * Idempotent. Called on every resume rather than only on a change, because
     * a dual-screen handheld lets a person move the app, so the activity can
     * come back on a different display than it left on. That also means the
     * target cannot be "not DEFAULT_DISPLAY": if the activity itself has been
     * moved to Screen-2, the display to avoid is Screen-2.
     */
    private fun attachSecondDisplay() {
        val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        if (displayListener == null) {
            val l = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = attachSecondDisplay()
                override fun onDisplayRemoved(displayId: Int) = attachSecondDisplay()
                override fun onDisplayChanged(displayId: Int) = Unit
            }
            runCatching { dm.registerDisplayListener(l, null) }.onSuccess { displayListener = l }
        }

        val hostId = display?.displayId ?: Display.DEFAULT_DISPLAY
        val second = dm.displays.firstOrNull { it.displayId != hostId }

        if (second == null) {
            dismissPresentation()
            screen2Status = "no second display found (app is on $hostId)"
            return
        }

        // Already on the right display. Leave it alone.
        if (presentation != null && presentationDisplayId == second.displayId) return

        dismissPresentation()
        screen2Status = "display ${second.displayId}, ${second.name}"
        presentationDisplayId = second.displayId
        presentation = Screen2Presentation(this, second).also { it.show() }
        // A fresh Presentation starts blank. Restore what Screen-2 was showing.
        presentation?.render(screen2Title, screen2Lines)
    }

    private fun detachSecondDisplay() {
        displayListener?.let { l ->
            runCatching {
                (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                    .unregisterDisplayListener(l)
            }
        }
        displayListener = null
        dismissPresentation()
        screen2Status = "detached"
    }

    private fun dismissPresentation() {
        runCatching { presentation?.dismiss() }
        presentation = null
        presentationDisplayId = Display.INVALID_DISPLAY
    }

    var screen2Status by mutableStateOf("not attached")

    /** Push content to Screen-2. The app owns this panel; no backend does. */
    fun pushToScreen2(title: String, lines: List<String>) {
        screen2Title = title
        screen2Lines = lines
        presentation?.render(title, lines)
    }
}

/**
 * The Screen-2 companion view.
 *
 * Drawn on change only. Do not redraw an idle second panel every frame; it
 * costs power and thermal headroom, and both are the budget on a handheld.
 */
class Screen2Presentation(context: Context, display: Display) :
    Presentation(context, display) {

    private lateinit var titleView: TextView
    private lateinit var bodyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0E1116"))
            setPadding(48, 48, 48, 48)
            gravity = Gravity.TOP
        }

        titleView = TextView(context).apply {
            textSize = 26f
            setTextColor(Color.parseColor("#E6EDF3"))
            setPadding(0, 0, 0, 24)
        }
        bodyView = TextView(context).apply {
            textSize = 17f
            setTextColor(Color.parseColor("#9BA7B4"))
            setLineSpacing(10f, 1f)
        }

        root.addView(
            titleView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        root.addView(
            bodyView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        setContentView(root)
        render("AYN Thor Shell", listOf("Screen-2 companion", "No game selected"))
    }

    fun render(title: String, lines: List<String>) {
        if (!::titleView.isInitialized) return
        titleView.text = title
        bodyView.text = lines.joinToString("\n")
    }
}
