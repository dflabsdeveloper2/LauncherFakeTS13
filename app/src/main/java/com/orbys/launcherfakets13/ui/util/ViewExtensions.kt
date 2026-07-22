package com.orbys.launcherfakets13.ui.util

import android.content.res.Resources

/**
 * Extension properties and functions for Android Views and Resources.
 */

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density
