package com.orbys.launcherts13.ui.drawer

import com.orbys.launcherts13.R

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                     WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }

        setContentView(R.layout.activity_menu_apps)
        AppMenuFragment().show(supportFragmentManager, "app_drawer")

        supportFragmentManager.addFragmentOnAttachListener { _, _ -> }
    }

    fun closeDrawer() = finish()
}