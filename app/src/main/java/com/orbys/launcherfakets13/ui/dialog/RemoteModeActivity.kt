package com.orbys.launcherfakets13.ui.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable

class RemoteModeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }

        RemoteModeDialog.newInstance().apply {
            onDismissListener = { finish() }
            show(supportFragmentManager, "remote_mode")
        }
    }
}
