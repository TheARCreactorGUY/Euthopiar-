package com.euthopiar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [WatchlistEntity::class, PlaybackHistoryEntity::class, TopTenEntity::class, DownloadAuthContextEntity::class],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun topTenDao(): TopTenDao
    abstract fun downloadAuthContextDao(): DownloadAuthContextDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "euthopiar_database"
                )
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
            }
        }
    }
}
