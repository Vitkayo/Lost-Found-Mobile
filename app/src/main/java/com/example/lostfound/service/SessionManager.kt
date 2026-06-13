package com.example.lostfound.service

import android.content.Context
import android.content.SharedPreferences
import com.example.lostfound.util.CredentialUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun saveSession(
        email: String,
        name: String,
        phone: String,
        studentId: String,
        rememberMe: Boolean,
        profileImage: String = ""
    ) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
            .putString(KEY_STUDENT_ID, studentId)
            .putBoolean(KEY_REMEMBER, rememberMe)
            .putString(KEY_PROFILE_IMAGE, profileImage)
            .apply()
    }

    fun clearSession() {
        val darkMode = isDarkMode()
        prefs.edit().clear().apply()
        setDarkMode(darkMode)
    }

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    fun getUserName(): String = prefs.getString(KEY_NAME, "") ?: "Student"

    fun getPhone(): String = prefs.getString(KEY_PHONE, "") ?: ""

    fun getStudentId(): String = prefs.getString(KEY_STUDENT_ID, "") ?: ""

    fun getProfileImage(): String = prefs.getString(KEY_PROFILE_IMAGE, "") ?: ""

    fun getDefaultContact(): String = prefs.getString(KEY_DEFAULT_CONTACT, "") ?: ""

    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER, false)

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun updateAccount(
        name: String,
        email: String,
        phone: String,
        newPassword: String,
        confirmPassword: String,
        profileImage: String? = null
    ): Boolean {
        if (name.isBlank()) return false
        if (!CredentialUtils.isValidEmail(email)) return false
        if (!CredentialUtils.isValidPhone(phone)) return false

        val passwordChanging = newPassword.isNotBlank() || confirmPassword.isNotBlank()
        if (passwordChanging) {
            if (newPassword.length < 6 || newPassword != confirmPassword) return false
        }

        val normalizedPhone = CredentialUtils.normalizePhone(phone)
        val password = if (passwordChanging) {
            newPassword
        } else {
            prefs.getString(KEY_REGISTERED_PASSWORD, "") ?: ""
        }

        val editor = prefs.edit()
            .putString(KEY_NAME, name.trim())
            .putString(KEY_EMAIL, email.trim())
            .putString(KEY_PHONE, normalizedPhone)
            .putString(KEY_REGISTERED_NAME, name.trim())
            .putString(KEY_REGISTERED_EMAIL, email.trim())
            .putString(KEY_REGISTERED_PHONE, normalizedPhone)
            .putString(KEY_REGISTERED_PASSWORD, password)

        if (profileImage != null) {
            editor.putString(KEY_PROFILE_IMAGE, profileImage)
        }

        editor.apply()
        return true
    }

    /** @deprecated kept for default contact used when posting items */
    fun updateDefaultContact(defaultContact: String) {
        prefs.edit().putString(KEY_DEFAULT_CONTACT, defaultContact).apply()
    }

    fun registerUser(
        username: String,
        email: String,
        phone: String,
        password: String
    ): Boolean {
        if (username.isBlank() ||
            !CredentialUtils.isValidEmail(email) ||
            !CredentialUtils.isValidPhone(phone) ||
            password.length < 6
        ) {
            return false
        }
        prefs.edit()
            .putString(KEY_REGISTERED_EMAIL, email.trim())
            .putString(KEY_REGISTERED_PHONE, CredentialUtils.normalizePhone(phone))
            .putString(KEY_REGISTERED_PASSWORD, password)
            .putString(KEY_REGISTERED_NAME, username.trim())
            .putString(KEY_REGISTERED_STUDENT_ID, "")
            .apply()
        return true
    }

    fun validateLogin(identifier: String, password: String): Boolean {
        val savedEmail = prefs.getString(KEY_REGISTERED_EMAIL, "") ?: ""
        val savedPhone = prefs.getString(KEY_REGISTERED_PHONE, "") ?: ""
        val savedPassword = prefs.getString(KEY_REGISTERED_PASSWORD, "") ?: ""

        if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
            return CredentialUtils.identifierMatchesLogin(identifier, savedEmail, savedPhone) &&
                password == savedPassword
        }

        return CredentialUtils.isValidLoginIdentifier(identifier) && password.length >= 6
    }

    fun getRegisteredName(): String =
        prefs.getString(KEY_REGISTERED_NAME, "") ?: ""

    fun getRegisteredEmail(): String =
        prefs.getString(KEY_REGISTERED_EMAIL, "") ?: ""

    fun getRegisteredPhone(): String =
        prefs.getString(KEY_REGISTERED_PHONE, "") ?: ""

    fun getRegisteredStudentId(): String =
        prefs.getString(KEY_REGISTERED_STUDENT_ID, "") ?: ""

    fun getRegisteredProfileImage(): String =
        prefs.getString(KEY_PROFILE_IMAGE, "") ?: ""

    companion object {
        private const val PREF_NAME = "campus_found_session"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_PHONE = "phone"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_DEFAULT_CONTACT = "default_contact"
        private const val KEY_REMEMBER = "remember_me"
        private const val KEY_REGISTERED_EMAIL = "registered_email"
        private const val KEY_REGISTERED_PHONE = "registered_phone"
        private const val KEY_REGISTERED_PASSWORD = "registered_password"
        private const val KEY_REGISTERED_NAME = "registered_name"
        private const val KEY_REGISTERED_STUDENT_ID = "registered_student_id"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_PROFILE_IMAGE = "profile_image"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }
}
