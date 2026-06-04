package com.example.lostfound.ui.home

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lostfound.data.ItemRepository
import com.example.lostfound.model.Item
import com.example.lostfound.util.ItemSort
import com.example.lostfound.util.StatusUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ItemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var allItems: List<Item> = emptyList()
    private var scrollState: Parcelable? = savedStateHandle.get<Parcelable>("recycler_scroll_state")

    init {
        _uiState.update { it.copy(
            searchQuery = savedStateHandle["search_query"] ?: "",
            selectedFilter = savedStateHandle["selected_filter"] ?: "All"
        )}
        loadItems()
    }

    fun saveScrollState(state: Parcelable?) {
        scrollState = state
        savedStateHandle["recycler_scroll_state"] = state
    }

    fun getScrollState(): Parcelable? = scrollState

    fun loadItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Try to load cached items first for immediate UI
            val cached = repository.getCachedItems()
            if (cached.isNotEmpty()) {
                allItems = cached
                applyFilters()
            }

            try {
                val remote = repository.getItems()
                allItems = remote
                _uiState.update { it.copy(isLoading = false) }
                applyFilters()
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load items"
                )}
            }
        }
    }

    fun refreshItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                allItems = repository.getItems()
                _uiState.update { it.copy(isRefreshing = false) }
                applyFilters()
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRefreshing = false,
                    error = e.message ?: "Refresh failed"
                )}
            }
        }
    }

    fun refreshAfterNewPost(createdItem: Item? = null) {
        _uiState.update { it.copy(
            selectedFilter = "All",
            searchQuery = ""
        )}
        savedStateHandle["selected_filter"] = "All"
        savedStateHandle["search_query"] = ""
        clearScrollState()
        
        if (createdItem != null) {
            allItems = ItemSort.newestFirst(
                listOf(createdItem) + allItems.filter { it.id != createdItem.id }
            )
            applyFilters()
        }
        refreshItems()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        savedStateHandle["search_query"] = query
        applyFilters()
    }

    fun setSelectedFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        savedStateHandle["selected_filter"] = filter
        applyFilters()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery
        val filter = _uiState.value.selectedFilter

        val filtered = ItemSort.newestFirst(
            allItems.filter { item ->
                StatusUtils.matchesSearch(item, query) && StatusUtils.matchesFilter(item, filter)
            }
        )
        _uiState.update { it.copy(items = filtered) }
    }

    fun clearScrollState() {
        scrollState = null
        savedStateHandle.remove<Parcelable>("recycler_scroll_state")
    }
}
