package com.example.lostfound.ui.detail

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.lostfound.R
import com.example.lostfound.databinding.ActivityItemDetailBinding
import com.example.lostfound.util.ContactLinkHelper
import com.example.lostfound.util.DateUtils
import com.example.lostfound.util.ImageLoader
import com.example.lostfound.util.MapHelper
import com.example.lostfound.util.StatusUtils
import com.example.lostfound.util.SystemBars
import com.google.android.material.snackbar.Snackbar

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding
    private val viewModel: ItemDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemBars.apply(activity = this, root = binding.root)

        val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        if (itemId.isNullOrBlank()) {
            finish()
            return
        }

        binding.backButton.setOnClickListener { finish() }
        observeViewModel()
        viewModel.loadItem(itemId)
    }

    private fun observeViewModel() {
        viewModel.item.observe(this) { item ->
            if (item == null) return@observe
            binding.detailTitle.text = item.title.orEmpty()
            binding.detailDescription.text = item.description.orEmpty()

            val location = item.location.orEmpty().trim()
            binding.detailLocation.text = location.ifBlank { getString(R.string.location_not_set) }
            setupLocationActions(location)

            val reporter = item.reporterName?.ifBlank { null } ?: "Unknown"
            binding.detailReporter.text = reporter

            val contact = item.contactInfo.orEmpty().trim()
            if (contact.isNotBlank()) {
                ContactLinkHelper.bindContactView(
                    context = this,
                    textView = binding.detailContact,
                    contactText = contact,
                    emptyText = "",
                )
                binding.contactTapHint.visibility = View.VISIBLE
            } else {
                binding.detailContact.text = getString(R.string.contact_missing_hint, reporter)
                binding.detailContact.paint.isUnderlineText = false
                binding.detailContact.isClickable = false
                binding.contactTapHint.visibility = View.GONE
            }

            binding.detailDate.text = DateUtils.formatDetailDate(item.createdAt ?: item.date)
            binding.detailTime.text = DateUtils.formatDetailTime(item.createdAt ?: item.date)
            StatusUtils.applyStatusBadge(this, item.status, binding.detailStatusBadge)
            ImageLoader.load(binding.detailImage, item.imageUrl)
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.detailLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            if (!error.isNullOrBlank()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLocationActions(location: String) {
        if (location.isBlank()) {
            binding.openMapsButton.visibility = View.GONE
            binding.detailLocation.setOnClickListener(null)
            binding.detailLocation.isClickable = false
            return
        }

        binding.openMapsButton.visibility = View.VISIBLE
        val openMaps = {
            if (!MapHelper.openLocation(this, location)) {
                Snackbar.make(binding.root, R.string.maps_not_available, Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.openMapsButton.setOnClickListener { openMaps() }
        binding.detailLocation.setOnClickListener { openMaps() }
        binding.detailLocation.isClickable = true
        binding.detailLocation.paint.isUnderlineText = true
    }

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
    }
}
