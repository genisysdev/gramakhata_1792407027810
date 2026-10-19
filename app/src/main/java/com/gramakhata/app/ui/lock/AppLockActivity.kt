package com.gramakhata.app.ui.lock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.gramakhata.app.R
import com.gramakhata.app.databinding.ActivityAppLockBinding
import com.gramakhata.app.ui.MainActivity

/**
 * App Lock screen with PIN and Biometric authentication.
 * Shown at app launch when lock is enabled (NFR-05).
 */
class AppLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppLockBinding
    private var enteredPin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPinPad()
        attemptBiometric()
    }

    private fun setupPinPad() {
        val pinButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )

        pinButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                if (enteredPin.length < 4) {
                    enteredPin += index.toString()
                    updatePinDots()
                    if (enteredPin.length == 4) {
                        verifyPin()
                    }
                }
            }
        }

        binding.btnBackspace.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.dropLast(1)
                updatePinDots()
            }
        }

        binding.btnBiometric.setOnClickListener {
            attemptBiometric()
        }
    }

    private fun updatePinDots() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        dots.forEachIndexed { index, view ->
            if (index < enteredPin.length) {
                view.setBackgroundResource(R.drawable.pin_dot_filled)
            } else {
                view.setBackgroundResource(R.drawable.pin_dot_empty)
            }
        }
    }

    private fun verifyPin() {
        val prefs = getSharedPreferences("app_lock_prefs", MODE_PRIVATE)
        val savedPin = prefs.getString("pin", null)

        if (savedPin == null) {
            // First time — set this as the PIN
            prefs.edit().putString("pin", enteredPin).apply()
            Toast.makeText(this, "PIN set successfully!", Toast.LENGTH_SHORT).show()
            proceedToMain()
        } else if (enteredPin == savedPin) {
            proceedToMain()
        } else {
            Toast.makeText(this, getString(R.string.lock_wrong_pin), Toast.LENGTH_SHORT).show()
            enteredPin = ""
            updatePinDots()
            // Shake animation for feedback
            binding.pinDotsContainer.animate()
                .translationX(20f).setDuration(50)
                .withEndAction {
                    binding.pinDotsContainer.animate()
                        .translationX(-20f).setDuration(50)
                        .withEndAction {
                            binding.pinDotsContainer.animate()
                                .translationX(0f).setDuration(50)
                        }
                }
        }
    }

    private fun attemptBiometric() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // Biometric not available, stay on PIN screen
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    proceedToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // User cancelled or error — stay on PIN screen
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(getString(R.string.lock_biometric_prompt))
            .setNegativeButtonText(getString(R.string.lock_set_pin))
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun proceedToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        // Prevent going back from lock screen
        finishAffinity()
    }
}
