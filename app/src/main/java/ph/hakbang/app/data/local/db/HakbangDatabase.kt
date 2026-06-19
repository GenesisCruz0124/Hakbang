package ph.hakbang.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ph.hakbang.app.data.local.dao.DailyStepsDao
import ph.hakbang.app.data.local.entity.Converters
import ph.hakbang.app.data.local.entity.DailySteps

@Database(entities = [DailySteps::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class HakbangDatabase : RoomDatabase() {

    abstract fun dailyStepsDao(): DailyStepsDao

    companion object {
        @Volatile
        private var instance: HakbangDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_steps ADD COLUMN floorsClimbed INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): HakbangDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HakbangDatabase::class.java,
                    "hakbang.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
