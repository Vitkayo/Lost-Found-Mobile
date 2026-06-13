package com.example.lostfound.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lostfound.databinding.ItemListCardBinding
import com.example.lostfound.model.Item
import com.example.lostfound.util.DateUtils
import com.example.lostfound.util.ImageLoader
import com.example.lostfound.util.StatusUtils

class ItemAdapter(
    private val onItemClick: (Item) -> Unit
) : ListAdapter<Item, ItemAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemListCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ItemViewHolder(
        private val binding: ItemListCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            val context = binding.root.context
            binding.itemTitle.text = item.title.orEmpty()
            binding.itemMeta.text = context.getString(
                com.example.lostfound.R.string.item_meta_format,
                item.category.orEmpty(),
                item.location.orEmpty()
            )
            binding.itemDate.text = DateUtils.formatPostedDate(item.createdAt ?: item.date)
            StatusUtils.applyStatusBadge(context, item.status, binding.statusBadge)
            ImageLoader.loadThumbnail(binding.itemImage, item.imageUrl)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}
