package com.example.lostfound.ui.post

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.lostfound.ui.home.HomeViewModel
import com.example.lostfound.R
import com.example.lostfound.databinding.FragmentPostItemBinding
import com.example.lostfound.service.SessionManager
import com.example.lostfound.util.ImageLoader
import com.example.lostfound.util.ImageStorageUtil
import com.example.lostfound.util.LocationHelper
import com.example.lostfound.util.ThemeToggleBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar
import java.util.Locale

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PostItemFragment : Fragment() {

    private var _binding: FragmentPostItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostItemViewModel by viewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager
    private var savedImagePath: String? = null
    private var isLostSelected = true

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val path = ImageStorageUtil.persistImage(requireContext(), uri)
            if (path == null) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            savedImagePath = path
            showImagePreview(path)
            saveDraftFromForm()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            requestCurrentLocation()
        } else {
            Snackbar.make(binding.root, R.string.location_permission_needed, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        setupCategoryDropdown()
        setupLocationDropdown()
        setupStatusSelector()
        restoreDraft()
        setupListeners()
        observeViewModel()
        ThemeToggleBinding.bind(binding.darkModeButton, requireActivity() as AppCompatActivity)
    }

    private fun setupCategoryDropdown() {
        val categories = resources.getStringArray(R.array.categories)
        val adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            categories
        )
        binding.categoryInput.setAdapter(adapter)
    }

    private fun setupLocationDropdown() {
        val locations = resources.getStringArray(R.array.rupp_locations)
        val adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            locations
        )
        binding.locationInput.setAdapter(adapter)
    }

    private fun setupStatusSelector() {
        binding.statusSelector.doOnLayout {
            updateStatusSelection(isLostSelected, animate = false)
        }

        binding.lostOption.setOnClickListener {
            updateStatusSelection(isLost = true, animate = true)
            saveDraftFromForm()
        }

        binding.foundOption.setOnClickListener {
            updateStatusSelection(isLost = false, animate = true)
            saveDraftFromForm()
        }
    }

    private fun updateStatusSelection(isLost: Boolean, animate: Boolean) {
        isLostSelected = isLost
        binding.lostOption.isSelected = isLost
        binding.foundOption.isSelected = !isLost

        val label = if (isLost) getString(R.string.lost) else getString(R.string.found)
        binding.selectedStatusLabel.text = getString(R.string.selected_status, label)

        val selectorWidth = binding.statusSelector.width
        if (selectorWidth == 0) {
            binding.statusSelector.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        binding.statusSelector.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        moveStatusIndicator(isLost, animate)
                    }
                }
            )
        } else {
            moveStatusIndicator(isLost, animate)
        }
    }

    private fun moveStatusIndicator(isLost: Boolean, animate: Boolean) {
        val selectorWidth = binding.statusSelector.width
        val padding = binding.statusSelector.paddingLeft + binding.statusSelector.paddingRight
        val indicatorWidth = (selectorWidth - padding) / 2f

        val params = binding.statusIndicator.layoutParams
        params.width = indicatorWidth.toInt()
        binding.statusIndicator.layoutParams = params

        val targetX = if (isLost) {
            binding.statusSelector.paddingLeft.toFloat()
        } else {
            binding.statusSelector.paddingLeft + indicatorWidth
        }

        if (animate) {
            binding.statusIndicator.animate().x(targetX).setDuration(200).start()
        } else {
            binding.statusIndicator.x = targetX
        }
    }

    private fun restoreDraft() {
        binding.titleInput.setText(viewModel.title.value)
        binding.categoryInput.setText(viewModel.category.value, false)
        binding.descriptionInput.setText(viewModel.description.value)
        binding.locationInput.setText(viewModel.location.value)
        val draftContact = viewModel.contact.value.orEmpty()
        binding.contactInput.setText(
            draftContact.ifBlank { sessionManager.getDefaultContact() }
        )
        binding.dateInput.setText(viewModel.date.value)

        isLostSelected = viewModel.status.value != "found"
        updateStatusSelection(isLostSelected, animate = false)

        val image = viewModel.imageUrl.value.orEmpty()
        ImageStorageUtil.pathFromDraftValue(image)?.let { path ->
            savedImagePath = path
            showImagePreview(path)
        }
    }

    private fun showImagePreview(path: String) {
        binding.previewImage.visibility = View.VISIBLE
        binding.uploadPlaceholder.visibility = View.GONE
        ImageLoader.load(binding.previewImage, path)
    }

    private fun setupListeners() {
        binding.uploadArea.setOnClickListener { imagePicker.launch("image/*") }
        binding.dateInput.setOnClickListener { showDatePicker() }
        binding.useLocationButton.setOnClickListener { onUseLocationClicked() }

        binding.submitButton.setOnClickListener {
            val status = if (isLostSelected) "lost" else "found"
            val imagePath = savedImagePath?.takeIf { path ->
                ImageStorageUtil.isReadableLocalImage(requireContext(), path)
            }.orEmpty()
            viewModel.submitItem(
                titleValue = binding.titleInput.text?.toString().orEmpty(),
                categoryValue = binding.categoryInput.text?.toString().orEmpty(),
                descriptionValue = binding.descriptionInput.text?.toString().orEmpty(),
                locationValue = binding.locationInput.text?.toString().orEmpty(),
                contactValue = binding.contactInput.text?.toString().orEmpty(),
                dateValue = binding.dateInput.text?.toString().orEmpty(),
                statusValue = status,
                imagePathOrUri = imagePath
            )
        }
    }

    private fun onUseLocationClicked() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            requestCurrentLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestCurrentLocation() {
        binding.useLocationButton.isEnabled = false
        LocationHelper.fetchCurrentLocation(
            context = requireContext(),
            onSuccess = { address ->
                binding.locationInput.setText(address)
                binding.useLocationButton.isEnabled = true
                saveDraftFromForm()
            },
        ) { message ->
            binding.useLocationButton.isEnabled = true
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                binding.dateInput.setText(formatted)
                saveDraftFromForm()
            },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        ).show()
    }

    private fun saveDraftFromForm() {
        val status = if (isLostSelected) "lost" else "found"
        viewModel.saveDraft(
            titleValue = binding.titleInput.text?.toString().orEmpty(),
            categoryValue = binding.categoryInput.text?.toString().orEmpty(),
            descriptionValue = binding.descriptionInput.text?.toString().orEmpty(),
            locationValue = binding.locationInput.text?.toString().orEmpty(),
            contactValue = binding.contactInput.text?.toString().orEmpty(),
            dateValue = binding.dateInput.text?.toString().orEmpty(),
            statusValue = status,
            imageUrlValue = savedImagePath.orEmpty()
        )
    }

    override fun onResume() {
        super.onResume()
        ThemeToggleBinding.refreshForFragment(this)
    }

    override fun onPause() {
        saveDraftFromForm()
        super.onPause()
    }

    private fun observeViewModel() {
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                binding.formErrorText.visibility = View.VISIBLE
                binding.formErrorText.text = error
            } else {
                binding.formErrorText.visibility = View.GONE
            }
        }

        viewModel.isSubmitting.observe(viewLifecycleOwner) { submitting ->
            binding.submitProgress.visibility = if (submitting) View.VISIBLE else View.GONE
            binding.submitButton.isEnabled = !submitting
        }

        viewModel.submitSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Snackbar.make(binding.root, R.string.success_post, Snackbar.LENGTH_SHORT).show()
                viewModel.clearDraft()
                clearFormUi()
                homeViewModel.refreshAfterNewPost(viewModel.takeCreatedItem())
                findNavController().navigate(R.id.homeFragment)
                viewModel.clearSubmitSuccess()
            }
        }
    }

    private fun clearFormUi() {
        binding.titleInput.text = null
        binding.categoryInput.text = null
        binding.descriptionInput.text = null
        binding.locationInput.text = null
        binding.contactInput.text = null
        binding.dateInput.text = null
        binding.previewImage.visibility = View.GONE
        binding.uploadPlaceholder.visibility = View.VISIBLE
        savedImagePath = null
        isLostSelected = true
        updateStatusSelection(isLost = true, animate = false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
