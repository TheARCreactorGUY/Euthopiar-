package com.euthopiar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WatchlistEntity::class, PlaybackHistoryEntity::class, TopTenEntity::class, DownloadAuthContextEntity::class],
    version = 8,
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

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playback_history ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playback_history ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playback_history ADD COLUMN clientUpdatedAtMs INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE watchlist ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE watchlist ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE watchlist ADD COLUMN clientUpdatedAtMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "euthopiar_database"
                )
                .addMigrations(MIGRATION_7_8)
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
            }
        }
    }
}
