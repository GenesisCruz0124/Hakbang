package ph.hakbang.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ph.hakbang.app.data.local.entity.DailySteps
import java.time.LocalDate

@Dao
interface DailyStepsDao {

    @Upsert
    suspend fun upsert(dailySteps: DailySteps)

    @Query("SELECT * FROM daily_steps WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: LocalDate): DailySteps?

    @Query("SELECT * FROM daily_steps WHERE date = :date LIMIT 1")
    fun observeByDate(date: LocalDate): Flow<DailySteps?>

    @Query("SELECT * FROM daily_steps WHERE date >= :fromDate ORDER BY date ASC")
    fun observeSince(fromDate: LocalDate): Flow<List<DailySteps>>

    @Query("SELECT * FROM daily_steps ORDER BY date ASC")
    fun observeAll(): Flow<List<DailySteps>>

    @Query("SELECT * FROM daily_steps ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailySteps>>

    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    suspend fun getAllDescending(): List<DailySteps>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_steps")
    fun observeAllTimeTotalSteps(): Flow<Long>

    @Query("DELETE FROM daily_steps")
    suspend fun deleteAll()
}
