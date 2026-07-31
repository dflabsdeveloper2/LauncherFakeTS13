package com.orbys.launcherfakets13.services.overlay.controllers

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.util.Log

abstract class BaseOverlayController(
    protected val context: Context,
    protected val container: ViewGroup? = null
) {
    protected val windowManager: WindowManager = 
        context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    var rootView: View? = null
    protected var layoutParams: WindowManager.LayoutParams? = null

    open fun isVisible(): Boolean = rootView?.isAttachedToWindow == true

    protected fun addViewSafely(view: View, params: WindowManager.LayoutParams) {
        try {
            if (container != null) {
                if (view.parent != null) {
                    (view.parent as ViewGroup).removeView(view)
                }
                container.addView(view)
                rootView = view
                // We might need a different way to handle positioning if not using WindowManager
                // For now, let's keep it simple.
            } else {
                detachCurrentView()
                windowManager.addView(view, params)
                rootView = view
                layoutParams = params
            }
            Log.d("OverlayController", "View added: ${view.javaClass.simpleName} (Title: ${params.title})")
        } catch (e: Exception) {
            Log.e("OverlayController", "Error adding view: ${e.message}")
        }
    }

    protected fun updateViewSafely() {
        if (container != null) return // In-app views don't use updateViewLayout
        val view = rootView ?: return
        val params = layoutParams ?: return
        if (view.isAttachedToWindow) {
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.e("OverlayController", "Error updating view: ${e.message}")
            }
        }
    }

    protected fun removeViewImmediate(view: View) {
        try {
            if (container != null) {
                container.removeView(view)
            } else {
                windowManager.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            Log.e("OverlayController", "Error in removeViewImmediate: ${e.message}")
        }
    }

    private fun detachCurrentView() {
        val view = rootView ?: return
        removeViewImmediate(view)
        Log.d("OverlayController", "View removed successfully")
        rootView = null
        layoutParams = null
    }

    open fun removeView() {
        detachCurrentView()
    }
}
