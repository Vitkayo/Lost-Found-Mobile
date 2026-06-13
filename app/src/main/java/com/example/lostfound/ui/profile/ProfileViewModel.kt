package com.example.lostfound.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lostfound.data.ItemRepository
import com.example.lostfound.model.Item
import com.example.lostfound.service.SessionManager
import com.example.lostfound.util.CredentialUtils
import com.example.lostfound.util.ItemSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val myItems: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val userName: String = "",
    val email: String = "",
    val studentId: String = "",
    val phone: String = "",
    val profileImage: String = "",
    val totalPosts: Int = 0,
    val lostCount: Int = 0,
    val foundCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refreshUserInfo()
    }

    fun refreshUserInfo() {
        _uiState.update { it.copy(
            userName = sessionManager.getUserName(),
            email = sessionManager.getEmail(),
            studentId = sessionManager.getStudentId(),
            phone = sessionManager.getPhone(),
            profileImage = sessionManager.getProfileImage()
        )}
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
        if (_uiState.value.myItems.isNotEmpty() || _uiState.value.isLoading) return
        loadMyItems()
    }

    fun refreshMyItems() {
        loadMyItems()
    }

    fun loadMyItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentUser = sessionManager.getUserName().lowercase()
            try {
                val data = repository.getItems()
                val mine = data.filter {
                    it.reporterName?.lowercase() == currentUser ||
                        it.reporterName?.lowercase() ==
                        sessionManager.getEmail().substringBefore("@").lowercase()
                }
                _uiState.update { it.copy(
                    myItems = ItemSort.newestFirst(mine),
                    totalPosts = mine.size,
                    lostCount = mine.count { item -> item.status.equals("lost", ignoreCase = true) },
                    foundCount = mine.count { item -> item.status.equals("found", ignoreCase = true) },
                    isLoading = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(myItems = emptyList(), isLoading = false) }
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
