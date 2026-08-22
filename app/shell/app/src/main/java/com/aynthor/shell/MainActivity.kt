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

    /** What Screen-2 is currently told to show. */
    var screen2Title by mutableStateOf("AYN Thor Shell")
    var screen2Lines by mutableStateOf(listOf("No game selected"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShellApp(this) }
    }

    override fun onStart() {
        super.onStart()
        attachSecondDisplay()
    }

    override fun onStop() {
        super.onStop()
        presentation?.dismiss()
        presentation = null
    }

    /** Find a display that is not the main one and put a Presentation on it. */
    private fun attachSecondDisplay() {
        val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val second = dm.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        if (second == null) {
            screen2Status = "no second display found"
            return
        }
        screen2Status = "display ${second.displayId}, ${second.name}"
        presentation = Screen2Presentation(this, second).also { it.show() }
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
