package com.example.lostfound.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lostfound.data.ItemRepository
import com.example.lostfound.model.Item
import com.example.lostfound.service.SessionManager
import com.example.lostfound.util.CredentialUtils
import com.example.lostfound.util.ItemSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val repository: ItemRepository
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _myItems = MutableLiveData<List<Item>>(emptyList())
    val myItems: LiveData<List<Item>> = _myItems

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userName = MutableLiveData(sessionManager.getUserName())
    val userName: LiveData<String> = _userName

    private val _email = MutableLiveData(sessionManager.getEmail())
    val email: LiveData<String> = _email

    private val _studentId = MutableLiveData(sessionManager.getStudentId())
    val studentId: LiveData<String> = _studentId

    private val _phone = MutableLiveData(sessionManager.getPhone())
    val phone: LiveData<String> = _phone

    private val _profileImage = MutableLiveData(sessionManager.getProfileImage())
    val profileImage: LiveData<String> = _profileImage

    private val _totalPosts = MutableLiveData(0)
    val totalPosts: LiveData<Int> = _totalPosts

    private val _lostCount = MutableLiveData(0)
    val lostCount: LiveData<Int> = _lostCount

    private val _foundCount = MutableLiveData(0)
    val foundCount: LiveData<Int> = _foundCount

    fun refreshUserInfo() {
        _userName.value = sessionManager.getUserName()
        _email.value = sessionManager.getEmail()
        _studentId.value = sessionManager.getStudentId()
        _phone.value = sessionManager.getPhone()
        _profileImage.value = sessionManager.getProfileImage()
    }

    sealed class ProfileUpdateResult {
        data object Success : ProfileUpdateResult()
        data object NameRequired : ProfileUpdateResult()
        data object InvalidEmail : ProfileUpdateResult()
        data object InvalidPhone : ProfileUpdateResult()
        data object PasswordTooShort : ProfileUpdateResult()
        data object PasswordMismatch : ProfileUpdateResult()
    }

    fun updateAccount(
        name: String,
        email: String,
        phone: String,
        newPassword: String,
        confirmPassword: String,
        profileImage: String? = null
    ): ProfileUpdateResult {
        if (name.isBlank()) return ProfileUpdateResult.NameRequired
        if (!CredentialUtils.isValidEmail(email)) return ProfileUpdateResult.InvalidEmail
        if (!CredentialUtils.isValidPhone(phone)) return ProfileUpdateResult.InvalidPhone

        val passwordChanging = newPassword.isNotBlank() || confirmPassword.isNotBlank()
        if (passwordChanging) {
            if (newPassword.length < 6) return ProfileUpdateResult.PasswordTooShort
            if (newPassword != confirmPassword) return ProfileUpdateResult.PasswordMismatch
        }

        if (!sessionManager.updateAccount(name, email, phone, newPassword, confirmPassword, profileImage)) {
            return ProfileUpdateResult.InvalidEmail
        }

        refreshUserInfo()
        return ProfileUpdateResult.Success
    }

    fun loadMyItemsIfNeeded() {
        if (!_myItems.value.isNullOrEmpty() || _isLoading.value == true) return
        loadMyItems()
    }

    fun refreshMyItems() {
        loadMyItems()
    }

    fun loadMyItems() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = sessionManager.getUserName().lowercase()
            try {
                val data = repository.getItems()
                val mine = data.filter {
                    it.reporterName?.lowercase() == currentUser ||
                        it.reporterName?.lowercase() ==
                        sessionManager.getEmail().substringBefore("@").lowercase()
                }
                _myItems.value = ItemSort.newestFirst(mine)
                _totalPosts.value = mine.size
                _lostCount.value = mine.count { it.status.equals("lost", ignoreCase = true) }
                _foundCount.value = mine.count { it.status.equals("found", ignoreCase = true) }
            } catch (e: Exception) {
                _myItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun deleteItem(itemId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteItem(itemId)
                loadMyItems()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}
