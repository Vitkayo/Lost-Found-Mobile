package com.example.lostfound.ui.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lostfound.data.ItemRepository
import com.example.lostfound.model.Item
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostItemUiState(
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean? = null,
    val error: String? = null,
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val location: String = "",
    val contact: String = "",
    val date: String = "",
    val status: String = "lost",
    val imageUrl: String = ""
)

@HiltViewModel
class PostItemViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostItemUiState())
    val uiState: StateFlow<PostItemUiState> = _uiState.asStateFlow()

    private var createdItem: Item? = null

    init {
        _uiState.update { it.copy(
            title = savedStateHandle["draft_title"] ?: "",
            category = savedStateHandle["draft_category"] ?: "",
            description = savedStateHandle["draft_description"] ?: "",
            location = savedStateHandle["draft_location"] ?: "",
            contact = savedStateHandle["draft_contact"] ?: "",
            date = savedStateHandle["draft_date"] ?: "",
            status = savedStateHandle["draft_status"] ?: "lost",
            imageUrl = savedStateHandle["draft_image_url"] ?: ""
        )}
    }

    fun saveDraft(
        titleValue: String,
        categoryValue: String,
        descriptionValue: String,
        locationValue: String,
        contactValue: String,
        dateValue: String,
        statusValue: String,
        imageUrlValue: String
    ) {
        savedStateHandle["draft_title"] = titleValue
        savedStateHandle["draft_category"] = categoryValue
        savedStateHandle["draft_description"] = descriptionValue
        savedStateHandle["draft_location"] = locationValue
        savedStateHandle["draft_contact"] = contactValue
        savedStateHandle["draft_date"] = dateValue
        savedStateHandle["draft_status"] = statusValue
        savedStateHandle["draft_image_url"] = imageUrlValue
        
        _uiState.update { it.copy(
            title = titleValue,
            category = categoryValue,
            description = descriptionValue,
            location = locationValue,
            contact = contactValue,
            date = dateValue,
            status = statusValue,
            imageUrl = imageUrlValue
        )}
    }

    fun submitItem(
        titleValue: String,
        categoryValue: String,
        descriptionValue: String,
        locationValue: String,
        contactValue: String,
        dateValue: String,
        statusValue: String,
        imagePathOrUri: String
    ) {
        if (titleValue.isBlank() || categoryValue.isBlank()) {
            _uiState.update { it.copy(error = "Title and Category are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val newItem = Item(
                    title = titleValue,
                    category = categoryValue,
                    description = descriptionValue,
                    location = locationValue,
                    contactInfo = contactValue,
                    date = dateValue,
                    status = statusValue,
                    imageUrl = imagePathOrUri
                )
                createdItem = repository.createItem(newItem)
                _uiState.update { it.copy(submitSuccess = true, isSubmitting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    error = e.message ?: "Submission failed",
                    submitSuccess = false,
                    isSubmitting = false
                )}
            }
        }
    }

    fun takeCreatedItem(): Item? {
        val item = createdItem
        createdItem = null
        return item
    }

    fun clearSubmitSuccess() {
        _uiState.update { it.copy(submitSuccess = null) }
    }

    fun clearDraft() {
        saveDraft("", "", "", "", "", "", "lost", "")
    }
}
