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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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

    private var scrollState: Parcelable? = savedStateHandle.get<Parcelable>("recycler_scroll_state")

    init {
        _uiState.update { state ->
            state.copy(
                searchQuery = savedStateHandle["search_query"] ?: "",
                selectedFilter = savedStateHandle["selected_filter"] ?: "All"
            )
        }

        viewModelScope.launch {
            repository.recentItemsFlow.collectLatest { recent ->
                _uiState.update { it.copy(recentItems = recent) }
            }
        }

        viewModelScope.launch {
            combine(
                repository.itemsFlow,
                _uiState.map { it.searchQuery }.distinctUntilChanged().debounce(300).onStart { emit(_uiState.value.searchQuery) },
                _uiState.map { it.selectedFilter }.distinctUntilChanged()
            ) { items, query, filter ->
                ItemSort.newestFirst(
                    items.filter { item ->
                        StatusUtils.matchesSearch(item, query) && StatusUtils.matchesFilter(item, filter)
                    }
                )
            }.flowOn(Dispatchers.Default)
                .collectLatest { filteredItems ->
                    _uiState.update { it.copy(items = filteredItems, isLoading = false) }
                }
        }

        refreshItems()
    }

    fun loadItems() {
        refreshItems()
    }

    fun refreshItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                repository.refreshItems()
                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRefreshing = false,
                    error = e.message ?: "Refresh failed"
                )}
            }
        }
    }

    fun refreshAfterNewPost(createdItem: Item? = null) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = "All",
                searchQuery = ""
            )
        }
        savedStateHandle["selected_filter"] = "All"
        savedStateHandle["search_query"] = ""
        clearScrollState()
        refreshItems()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        savedStateHandle["search_query"] = query
    }

    fun setSelectedFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        savedStateHandle["selected_filter"] = filter
    }

    fun saveScrollState(state: Parcelable?) {
        scrollState = state
        savedStateHandle["recycler_scroll_state"] = state
    }

    fun getScrollState(): Parcelable? = scrollState

    fun clearScrollState() {
        scrollState = null
        savedStateHandle.remove<Parcelable>("recycler_scroll_state")
    }
}
