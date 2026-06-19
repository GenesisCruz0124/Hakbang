package ph.hakbang.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ph.hakbang.app.data.local.dao.DailyStepsDao
import ph.hakbang.app.data.local.entity.Converters
import ph.hakbang.app.data.local.entity.DailySteps

@Database(entities = [DailySteps::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class HakbangDatabase : RoomDatabase() {

    abstract fun dailyStepsDao(): DailyStepsDao

    companion object {
        @Volatile
        private var instance: HakbangDatabase? = null

        fun getInstance(context: Context): HakbangDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HakbangDatabase::class.java,
                    "hakbang.db"
                ).build().also { instance = it }
            }
    }
}
