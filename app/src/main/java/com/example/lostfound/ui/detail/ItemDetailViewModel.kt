package com.example.lostfound.ui.detail

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

data class ItemDetailUiState(
    val item: Item? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val repository: ItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    fun loadItem(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val item = repository.getItemById(id)
                _uiState.update { it.copy(item = item, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load item", isLoading = false) }
            }
        }
    }

    fun updateItemStatus(id: String, newStatus: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val current = _uiState.value.item ?: return@launch
                val updated = current.copy(status = newStatus)
                repository.createItem(updated) // MockAPI update often uses POST/PUT to same endpoint
                _uiState.update { it.copy(item = updated, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Update failed", isLoading = false) }
            }
        }
    }
}
