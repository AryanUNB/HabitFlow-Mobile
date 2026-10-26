package ca.unb.mobiledev.habitflow

import android.app.Activity
import android.os.Build
import android.view.ViewGroup
import android.view.WindowInsets

fun Activity.fitSystemBars() {
    val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    root.setOnApplyWindowInsetsListener { view, insets ->
        if (Build.VERSION.SDK_INT >= 30) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        } else {
            @Suppress("DEPRECATION")
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }
        insets
    }
    root.requestApplyInsets()
}
