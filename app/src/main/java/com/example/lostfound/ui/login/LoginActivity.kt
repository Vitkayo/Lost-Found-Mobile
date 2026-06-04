package com.example.lostfound.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.lostfound.R
import com.example.lostfound.databinding.ActivityLoginBinding
import com.example.lostfound.databinding.DialogRegisterBinding
import com.example.lostfound.service.SessionManager
import com.example.lostfound.ui.main.MainActivity
import com.example.lostfound.util.CredentialUtils
import com.example.lostfound.util.SystemBars
import com.example.lostfound.util.ThemeToggleBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBars.apply(activity = this, root = binding.root)

        ThemeToggleBinding.bind(binding.darkModeButton, this)

        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.registerButton.setOnClickListener { showRegisterDialog() }

        binding.emailInput.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            ThemeToggleBinding.refreshIcon(binding.darkModeButton, this)
        }
    }

    private fun attemptLogin() {
        val identifier = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val rememberMe = binding.rememberCheckBox.isChecked

        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        binding.errorText.visibility = View.GONE

        if (identifier.isBlank()) {
            binding.emailLayout.error = getString(R.string.login_identifier_required)
            return
        }
        if (!CredentialUtils.isValidLoginIdentifier(identifier)) {
            binding.emailLayout.error = getString(R.string.invalid_login_identifier)
            return
        }
        if (password.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            return
        }

        if (!sessionManager.validateLogin(identifier, password)) {
            binding.errorText.text = getString(R.string.invalid_login_credentials)
            binding.errorText.setTextColor(getColor(R.color.error))
            binding.errorText.visibility = View.VISIBLE
            return
        }

        val email = sessionManager.getRegisteredEmail().ifBlank {
            if (CredentialUtils.isValidEmail(identifier)) identifier else ""
        }
        val name = sessionManager.getRegisteredName().ifBlank {
            email.substringBefore("@").ifBlank { identifier }
        }
        val phone = sessionManager.getRegisteredPhone()
        val studentId = sessionManager.getRegisteredStudentId()
        val profileImage = sessionManager.getRegisteredProfileImage()

        sessionManager.saveSession(
            email = email,
            name = name,
            phone = phone,
            studentId = studentId,
            rememberMe = rememberMe,
            profileImage = profileImage
        )
        navigateToMain()
    }

    private fun showRegisterDialog() {
        val dialogBinding = DialogRegisterBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.register, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.setOnShowListener {
            dialogBinding.closeRegisterButton.setOnClickListener { dialog.dismiss() }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val username = dialogBinding.registerUsernameInput.text?.toString()?.trim().orEmpty()
                val email = dialogBinding.registerEmailInput.text?.toString()?.trim().orEmpty()
                val phone = dialogBinding.registerPhoneInput.text?.toString()?.trim().orEmpty()
                val password = dialogBinding.registerPasswordInput.text?.toString().orEmpty()

                if (!sessionManager.registerUser(username, email, phone, password)) {
                    AlertDialog.Builder(this)
                        .setMessage(R.string.register_validation_error)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    return@setOnClickListener
                }

                dialog.dismiss()
                binding.emailInput.setText(email)
                binding.passwordInput.setText(password)
                binding.errorText.text = getString(R.string.registration_success)
                binding.errorText.setTextColor(getColor(R.color.secondary))
                binding.errorText.visibility = View.VISIBLE
            }
        }
        dialog.show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
