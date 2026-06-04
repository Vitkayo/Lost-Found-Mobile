package com.example.lostfound.di

import android.content.Context
import com.example.lostfound.db.CampusDatabase
import com.example.lostfound.db.CachedItemDao
import com.example.lostfound.db.RecentItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CampusDatabase {
        return CampusDatabase.getInstance(context)
    }

    @Provides
    fun provideCachedItemDao(database: CampusDatabase): CachedItemDao {
        return database.cachedItemDao()
    }

    @Provides
    fun provideRecentItemDao(database: CampusDatabase): RecentItemDao {
        return database.recentItemDao()
    }
}
