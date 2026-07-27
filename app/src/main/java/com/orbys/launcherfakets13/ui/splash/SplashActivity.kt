package com.orbys.launcherfakets13.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.orbys.launcherfakets13.R
import com.orbys.launcherfakets13.ui.home.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        hideSystemUI()

        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        lifecycleScope.launch {
            // Simulated sequence
            delay(1000.milliseconds)
            tvStatus.text = getString(R.string.splash_authenticating)
            
            delay(700.milliseconds)
            tvStatus.text = getString(R.string.splash_loading)
            
            delay(500.milliseconds)
            tvStatus.text = getString(R.string.splash_success)
            
            delay(500.milliseconds)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun hideSystemUI() {
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.let {
            it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
