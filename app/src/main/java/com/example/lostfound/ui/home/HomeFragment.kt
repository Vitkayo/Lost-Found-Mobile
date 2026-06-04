package com.example.lostfound.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentHomeBinding
import com.example.lostfound.model.Item
import com.example.lostfound.ui.detail.ItemDetailActivity
import com.example.lostfound.util.ThemeToggleBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: ItemAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private val searchHandler = Handler(Looper.getMainLooper())
    private var pendingSearchQuery: Runnable? = null

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupFilters()
        setupFilterScroll()
        setupSearch()
        observeViewModel()
        ThemeToggleBinding.bind(binding.darkModeButton, requireActivity() as AppCompatActivity)

        binding.retryButton.setOnClickListener { viewModel.loadItems() }
        binding.addItemFab.setOnClickListener {
            findNavController().navigate(R.id.postItemFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeToggleBinding.refreshForFragment(this)
        viewModel.getScrollState()?.let { layoutManager.onRestoreInstanceState(it) }
    }

    override fun onPause() {
        viewModel.saveScrollState(layoutManager.onSaveInstanceState())
        super.onPause()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.primary),
        )
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshItems()
        }
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter { item -> openItemDetail(item) }
        layoutManager = LinearLayoutManager(requireContext())
        binding.itemsRecyclerView.layoutManager = layoutManager
        binding.itemsRecyclerView.adapter = adapter
        binding.itemsRecyclerView.setHasFixedSize(true)
        binding.itemsRecyclerView.itemAnimator?.changeDuration = 0
    }

    private fun setupFilters() {
        val filters = resources.getStringArray(R.array.filters)
        val selected = viewModel.uiState.value.selectedFilter
        binding.filterChipGroup.setOnCheckedStateChangeListener(null)
        binding.filterChipGroup.removeAllViews()

        filters.forEach { filter ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = filter
                isCheckable = true
                isChecked = filter == selected
            }
            binding.filterChipGroup.addView(chip)
        }

        binding.filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                syncChipSelection(viewModel.uiState.value.selectedFilter)
                return@setOnCheckedStateChangeListener
            }
            val chipId = checkedIds.first()
            val chip = group.findViewById<Chip>(chipId) ?: return@setOnCheckedStateChangeListener
            val filter = chip.text.toString()
            if (filter != viewModel.uiState.value.selectedFilter) {
                viewModel.setSelectedFilter(filter)
            }
        }

        updateChipStyles(selected)
    }

    private fun syncChipSelection(filter: String) {
        for (i in 0 until binding.filterChipGroup.childCount) {
            val chip = binding.filterChipGroup.getChildAt(i) as Chip
            if (chip.text.toString() == filter) {
                if (binding.filterChipGroup.checkedChipId != chip.id) {
                    binding.filterChipGroup.check(chip.id)
                }
                updateChipStyles(filter)
                return
            }
        }
    }

    private fun setupFilterScroll() {
        binding.filterScrollView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    view.parent?.requestDisallowInterceptTouchEvent(true)

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        view.performClick()
                    }
                }
            }
            false
        }
    }

    private fun updateChipStyles(selectedFilter: String) {
        for (i in 0 until binding.filterChipGroup.childCount) {
            val chip = binding.filterChipGroup.getChildAt(i) as Chip
            val selected = chip.text.toString() == selectedFilter
            chip.setChipBackgroundColorResource(
                if (selected) R.color.primary else R.color.surface_container_high,
            )
            val textColor = ContextCompat.getColor(
                requireContext(),
                if (selected) R.color.on_primary else R.color.on_surface_variant
            )
            chip.setTextColor(textColor)
        }
    }

    private fun setupSearch() {
        binding.searchInput.setText(viewModel.uiState.value.searchQuery)
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                pendingSearchQuery?.let { searchHandler.removeCallbacks(it) }
                val query = s?.toString().orEmpty()
                pendingSearchQuery = Runnable { viewModel.setSearchQuery(query) }
                searchHandler.postDelayed(pendingSearchQuery!!, SEARCH_DEBOUNCE_MS)
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                adapter.submitList(state.items) {
                    if (state.items.isNotEmpty() && (viewModel.getScrollState() == null || state.isRefreshing)) {
                        binding.itemsRecyclerView.scrollToPosition(0)
                    }
                }

                binding.emptyStateLayout.visibility =
                    if (state.items.isEmpty() && !state.isLoading && !state.isRefreshing) View.VISIBLE else View.GONE

                binding.loadingProgress.visibility =
                    if (state.isLoading && !state.isRefreshing) View.VISIBLE else View.GONE

                binding.swipeRefresh.isRefreshing = state.isRefreshing

                binding.errorStateLayout.visibility =
                    if (!state.error.isNullOrBlank()) View.VISIBLE else View.GONE
                binding.errorMessageText.text = state.error

                syncChipSelection(state.selectedFilter)

                val currentSearch = binding.searchInput.text?.toString().orEmpty()
                if (currentSearch != state.searchQuery) {
                    binding.searchInput.setText(state.searchQuery)
                    binding.searchInput.setSelection(state.searchQuery.length)
                }
            }
        }
    }

    private fun openItemDetail(item: Item) {
        val intent = Intent(requireContext(), ItemDetailActivity::class.java).apply {
            putExtra(ItemDetailActivity.EXTRA_ITEM_ID, item.id)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        searchHandler.removeCallbacksAndMessages(null)
        pendingSearchQuery = null
        super.onDestroyView()
        _binding = null
    }
}
