package com.example.lostfound.ui.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lostfound.data.ItemRepository
import com.example.lostfound.model.Item
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostItemViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _submitSuccess = MutableLiveData<Boolean?>(null)
    val submitSuccess: LiveData<Boolean?> = _submitSuccess

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var createdItem: Item? = null

    // Draft properties from SavedStateHandle
    val title = savedStateHandle.getLiveData("draft_title", "")
    val category = savedStateHandle.getLiveData("draft_category", "")
    val description = savedStateHandle.getLiveData("draft_description", "")
    val location = savedStateHandle.getLiveData("draft_location", "")
    val contact = savedStateHandle.getLiveData("draft_contact", "")
    val date = savedStateHandle.getLiveData("draft_date", "")
    val status = savedStateHandle.getLiveData("draft_status", "lost")
    val imageUrl = savedStateHandle.getLiveData("draft_image_url", "")

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
            _error.value = "Title and Category are required"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
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
                _submitSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Submission failed"
                _submitSuccess.value = false
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun takeCreatedItem(): Item? {
        val item = createdItem
        createdItem = null
        return item
    }

    fun clearSubmitSuccess() {
        _submitSuccess.value = null
    }

    fun clearDraft() {
        saveDraft("", "", "", "", "", "", "lost", "")
    }
}
