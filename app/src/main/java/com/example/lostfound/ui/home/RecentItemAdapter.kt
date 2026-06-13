package com.example.lostfound.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lostfound.databinding.ItemRecentCardBinding
import com.example.lostfound.model.Item
import com.example.lostfound.util.ImageLoader

class RecentItemAdapter(
    private val onItemClick: (Item) -> Unit
) : ListAdapter<Item, RecentItemAdapter.RecentViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val binding = ItemRecentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentViewHolder(
        private val binding: ItemRecentCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.itemTitle.text = item.title.orEmpty()
            binding.itemCategory.text = item.category.orEmpty()
            ImageLoader.loadThumbnail(binding.itemImage, item.imageUrl)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem == newItem
    }
}
