package com.orbys.launcherts13.services.overlay.controllers

import android.content.Context
import android.view.View
import android.view.WindowManager
import android.util.Log

abstract class BaseOverlayController(protected val context: Context) {
    protected val windowManager: WindowManager = 
        context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    var rootView: View? = null
    protected var layoutParams: WindowManager.LayoutParams? = null

    open fun isVisible(): Boolean = rootView?.isAttachedToWindow == true

    protected fun addViewSafely(view: View, params: WindowManager.LayoutParams) {
        try {
            // Detach any stale window only - NOT the virtual removeView(), which subclasses override
            // with extra teardown (e.g. nulling their own ViewBinding field). Calling that here would
            // wipe out state the subclass's show() just set up right before this call.
            detachCurrentView()

            windowManager.addView(view, params)
            rootView = view
            layoutParams = params
            Log.d("OverlayController", "View added: ${view.javaClass.simpleName} (Title: ${params.title})")
        } catch (e: Exception) {
            Log.e("OverlayController", "Error adding view: ${e.message}")
        }
    }

    protected fun updateViewSafely() {
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

    private fun detachCurrentView() {
        val view = rootView ?: return
        try {
            windowManager.removeViewImmediate(view)
            Log.d("OverlayController", "View removed successfully")
        } catch (e: Exception) {
            if (e.message?.contains("not attached to window manager") == false) {
                Log.e("OverlayController", "Error removing view: ${e.message}")
            }
        } finally {
            rootView = null
            layoutParams = null
        }
    }

    open fun removeView() {
        detachCurrentView()
    }
}
