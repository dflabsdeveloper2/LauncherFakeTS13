package com.orbys.launcherts13.ui.util

import android.content.res.Resources
import android.util.TypedValue
import android.view.View

/**
 * Extension properties and functions for Android Views and Resources.
 */

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density
