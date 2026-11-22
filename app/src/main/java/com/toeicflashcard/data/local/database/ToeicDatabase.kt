package com.toeicflashcard.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.toeicflashcard.data.local.dao.VocabularyDao
import com.toeicflashcard.data.local.entity.VocabularyEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [VocabularyEntity::class],
    version = 4,
    exportSchema = false
)
abstract class ToeicDatabase : RoomDatabase() {

    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        private const val DATABASE_NAME = "toeic_flashcard.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vocabulary_words_topic` ON `vocabulary_words` (`topic`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vocabulary_words_nextReviewTimestamp` ON `vocabulary_words` (`nextReviewTimestamp`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `vocabulary_words` ADD COLUMN `deckId` TEXT NOT NULL DEFAULT 'core_775'"
                )
                db.execSQL(
                    "ALTER TABLE `vocabulary_words` ADD COLUMN `deckName` TEXT NOT NULL DEFAULT 'TOEIC Cốt Lõi (775)'"
                )
                db.execSQL(
                    "UPDATE `vocabulary_words` SET `deckId` = 'custom_user', `deckName` = 'Từ vựng tự thêm' WHERE `id` > 775 AND `id` < 1001"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_vocabulary_words_deckId` ON `vocabulary_words` (`deckId`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `vocabulary_words` ADD COLUMN `isStarred` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        @Volatile
        private var instance: ToeicDatabase? = null

        fun getInstance(context: Context, seeder: VocabularySeeder): ToeicDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext, seeder).also {
                    instance = it
                }
            }
        }

        private fun buildDatabase(
            appContext: Context,
            seeder: VocabularySeeder
        ): ToeicDatabase {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            return Room.databaseBuilder(
                appContext,
                ToeicDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch {
                            val database = instance ?: return@launch
                            seeder.seedIfEmpty(database.vocabularyDao())
                        }
                    }
                })
                .build()
        }
    }
}
