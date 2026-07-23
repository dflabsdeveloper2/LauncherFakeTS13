package com.orbys.launcherfakets13.services

import android.content.Context
import android.content.Intent

object Broadcaster {
    private const val TARGET_PACKAGE = "com.orbys.launcher"
    private const val ACTION_OPEN = "com.orbys.digitalsignage.OPEN"
    private const val ACTION_CLOSE = "com.orbys.digitalsignage.CLOSE"

    fun sendOpen(context: Context) {
        sendBroadcast(context, ACTION_OPEN)
    }

    fun sendClose(context: Context) {
        sendBroadcast(context, ACTION_CLOSE)
    }

    private fun sendBroadcast(context: Context, action: String) {
        val intent = Intent(action).apply {
            setPackage(TARGET_PACKAGE)
        }
        context.sendBroadcast(intent)
    }
}
