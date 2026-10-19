package com.gramakhata.app.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.gramakhata.app.R
import com.gramakhata.app.ui.lock.AppLockActivity

/**
 * Splash screen that displays the app branding for 2 seconds,
 * then navigates to the lock screen (if enabled) or main activity.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animate the splash elements
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 800 }
        val slideUp = TranslateAnimation(0f, 0f, 100f, 0f).apply { duration = 800 }
        val animSet = AnimationSet(true).apply {
            addAnimation(fadeIn)
            addAnimation(slideUp)
        }

        findViewById<android.widget.TextView>(R.id.tvSplashTitle)?.startAnimation(animSet)
        findViewById<android.widget.TextView>(R.id.tvSplashTagline)?.startAnimation(
            AlphaAnimation(0f, 1f).apply {
                duration = 1000
                startOffset = 400
            }
        )

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("app_lock_prefs", MODE_PRIVATE)
            val lockEnabled = prefs.getBoolean("lock_enabled", false)

            val targetActivity = if (lockEnabled) {
                AppLockActivity::class.java
            } else {
                MainActivity::class.java
            }

            startActivity(Intent(this, targetActivity))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2000)
    }
}

