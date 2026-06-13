package com.example.lostfound.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lostfound.R
import com.example.lostfound.databinding.DialogEditProfileBinding
import com.example.lostfound.databinding.FragmentProfileBinding
import com.example.lostfound.model.Item
import com.example.lostfound.service.SessionManager
import com.example.lostfound.ui.detail.ItemDetailActivity
import com.example.lostfound.ui.login.LoginActivity
import com.example.lostfound.util.ThemeToggleBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.lostfound.util.ImageLoader
import com.example.lostfound.util.ImageStorageUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var adapter: ProfileItemAdapter

    private var tempProfilePath: String? = null
    private var dialogAvatarImage: android.widget.ImageView? = null

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = ImageStorageUtil.persistImage(requireContext(), it)
            if (path != null) {
                tempProfilePath = path
                dialogAvatarImage?.let { iv -> ImageLoader.load(iv, path) }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        ThemeToggleBinding.bind(binding.darkModeButton, requireActivity() as AppCompatActivity)

        binding.logoutButton.setOnClickListener { confirmLogout() }
        binding.editProfileButton.setOnClickListener { showEditProfileDialog() }

        viewModel.refreshUserInfo()
        viewModel.loadMyItemsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        ThemeToggleBinding.refreshForFragment(this)
        viewModel.refreshUserInfo()
    }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        val state = viewModel.uiState.value
        dialogBinding.editNameInput.setText(state.userName)
        dialogBinding.editEmailInput.setText(state.email)
        dialogBinding.editPhoneInput.setText(state.phone)

        tempProfilePath = state.profileImage
        dialogAvatarImage = dialogBinding.editAvatarImage
        ImageLoader.load(dialogBinding.editAvatarImage, tempProfilePath)

        dialogBinding.editAvatarArea.setOnClickListener {
            imagePicker.launch("image/*")
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_profile)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialogBinding.editNameLayout.error = null
                dialogBinding.editEmailLayout.error = null
                dialogBinding.editPhoneLayout.error = null
                dialogBinding.editPasswordLayout.error = null
                dialogBinding.editConfirmPasswordLayout.error = null

                val name = dialogBinding.editNameInput.text?.toString()?.trim().orEmpty()
                val email = dialogBinding.editEmailInput.text?.toString()?.trim().orEmpty()
                val phone = dialogBinding.editPhoneInput.text?.toString()?.trim().orEmpty()
                val newPassword = dialogBinding.editPasswordInput.text?.toString().orEmpty()
                val confirmPassword = dialogBinding.editConfirmPasswordInput.text?.toString().orEmpty()

                when (
                    viewModel.updateAccount(name, email, phone, newPassword, confirmPassword, tempProfilePath)
                ) {
                    ProfileViewModel.ProfileUpdateResult.Success -> {
                        dialog.dismiss()
                        Snackbar.make(binding.root, R.string.profile_updated, Snackbar.LENGTH_SHORT).show()
                    }
                    ProfileViewModel.ProfileUpdateResult.NameRequired -> {
                        dialogBinding.editNameLayout.error = getString(R.string.edit_profile_name_required)
                    }
                    ProfileViewModel.ProfileUpdateResult.InvalidEmail -> {
                        dialogBinding.editEmailLayout.error = getString(R.string.invalid_login_identifier)
                    }
                    ProfileViewModel.ProfileUpdateResult.InvalidPhone -> {
                        dialogBinding.editPhoneLayout.error =
                            getString(R.string.edit_profile_invalid_phone)
                    }
                    ProfileViewModel.ProfileUpdateResult.PasswordTooShort -> {
                        dialogBinding.editPasswordLayout.error =
                            getString(R.string.edit_profile_password_short)
                    }
                    ProfileViewModel.ProfileUpdateResult.PasswordMismatch -> {
                        dialogBinding.editConfirmPasswordLayout.error =
                            getString(R.string.edit_profile_password_mismatch)
                    }
                }
            }
        }
        dialog.setOnDismissListener {
            dialogAvatarImage = null
        }
        dialog.show()
    }

    private fun setupRecyclerView() {
        adapter = ProfileItemAdapter(
            onItemClick = { item -> openItemDetail(item) },
            onDeleteClick = { item -> confirmDelete(item) }
        )
        binding.myItemsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myItemsRecyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.userNameText.text = state.userName
                    ImageLoader.load(binding.avatarImage, state.profileImage)
                    
                    updateProfileSubtitle(state)
                    
                    adapter.submitList(state.myItems)
                    binding.emptyPostsText.visibility = if (state.myItems.isEmpty()) View.VISIBLE else View.GONE
                    binding.profileLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    binding.totalPostsText.text = state.totalPosts.toString()
                    binding.lostCountText.text = state.lostCount.toString()
                    binding.foundCountText.text = state.foundCount.toString()
                }
            }
        }
    }

    private fun updateProfileSubtitle(state: ProfileUiState) {
        val studentId = state.studentId
        val email = state.email
        val phone = state.phone

        binding.studentIdText.text = when {
            studentId.isNotBlank() -> getString(R.string.student_id_label, studentId)
            email.isNotBlank() && phone.isNotBlank() -> "$email • $phone"
            email.isNotBlank() -> email
            phone.isNotBlank() -> getString(R.string.phone_label, phone)
            else -> ""
        }
        binding.studentIdText.visibility =
            if (binding.studentIdText.text.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun openItemDetail(item: Item) {
        startActivity(
            Intent(requireContext(), ItemDetailActivity::class.java).apply {
                putExtra(ItemDetailActivity.EXTRA_ITEM_ID, item.id)
            }
        )
    }

    private fun confirmDelete(item: Item) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage("Delete ${item.title}?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteItem(item.id.orEmpty()) { success ->
                    val message = if (success) R.string.success_deleted else R.string.error_loading
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout)
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton(R.string.logout) { _, _ ->
                viewModel.logout()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
