package com.orbys.launcherfakets13.ui.util

import android.content.res.Resources
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment


/**
 * Extension properties and functions for Android Views and Resources.
 */

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

/**
 * Ajusta el tamaño del diálogo basado en una fracción del ancho de la pantalla (dimen fraction).
 */
fun DialogFragment.setupDialogSize(widthFractionRes: Int, heightFractionRes: Int? = null) {
    dialog?.window?.let { window ->
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setElevation(0f)
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels * resources.getFraction(widthFractionRes, 1, 1)).toInt()
        val height = if (heightFractionRes != null) {
            (metrics.heightPixels * resources.getFraction(heightFractionRes, 1, 1)).toInt()
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }
        window.setLayout(width, height)
    }
}
