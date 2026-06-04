package com.example.lostfound.data

import com.example.lostfound.api.ApiService
import com.example.lostfound.db.CachedItemDao
import com.example.lostfound.db.RecentItemDao
import com.example.lostfound.db.CachedItemRecord
import com.example.lostfound.db.RecentItemRecord
import com.example.lostfound.model.Item
import com.example.lostfound.util.ItemSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val apiService: ApiService,
    private val cachedDao: CachedItemDao,
    private val recentDao: RecentItemDao
) {

    suspend fun getItems(): List<Item> = withContext(Dispatchers.IO) {
        try {
            val remoteItems = apiService.getItems()
            val sorted = ItemSort.newestFirst(remoteItems)
            cachedDao.clearAll()
            cachedDao.insertAll(sorted.map { it.toCachedRecord() })
            sorted
        } catch (e: Exception) {
            val cached = cachedDao.getAll().map { it.toItem() }
            if (cached.isNotEmpty()) cached else throw e
        }
    }

    suspend fun getCachedItems(): List<Item> = withContext(Dispatchers.IO) {
        cachedDao.getAll().map { it.toItem() }
    }

    suspend fun getItemById(id: String): Item = withContext(Dispatchers.IO) {
        val item = apiService.getItem(id)
        recentDao.insert(item.toRecentRecord())
        item
    }

    suspend fun createItem(item: Item): Item = withContext(Dispatchers.IO) {
        apiService.createItem(item)
    }

    suspend fun deleteItem(id: String): Item = withContext(Dispatchers.IO) {
        apiService.deleteItem(id)
    }

    suspend fun getRecentlyViewed(): List<Item> = withContext(Dispatchers.IO) {
        recentDao.getAll().map { it.toItem() }
    }

    private fun Item.toCachedRecord() = CachedItemRecord(
        id = id ?: "",
        title = title ?: "",
        description = description ?: "",
        category = category ?: "",
        status = status ?: "",
        location = location ?: "",
        imageUrl = imageUrl ?: "",
        reporterName = reporterName ?: "",
        createdAt = createdAt ?: date ?: "",
        contactInfo = contactInfo ?: ""
    )

    private fun Item.toRecentRecord() = RecentItemRecord(
        id = id ?: "",
        title = title ?: "",
        description = description ?: "",
        category = category ?: "",
        status = status ?: "",
        location = location ?: "",
        imageUrl = imageUrl ?: "",
        reporterName = reporterName ?: "",
        createdAt = createdAt ?: date ?: "",
        contactInfo = contactInfo ?: ""
    )

    private fun CachedItemRecord.toItem() = Item(
        id = id,
        title = title,
        description = description,
        category = category,
        status = status,
        location = location,
        imageUrl = imageUrl,
        reporterName = reporterName,
        createdAt = createdAt,
        contactInfo = contactInfo
    )

    private fun RecentItemRecord.toItem() = Item(
        id = id,
        title = title,
        description = description,
        category = category,
        status = status,
        location = location,
        imageUrl = imageUrl,
        reporterName = reporterName,
        createdAt = createdAt,
        contactInfo = contactInfo
    )
}
