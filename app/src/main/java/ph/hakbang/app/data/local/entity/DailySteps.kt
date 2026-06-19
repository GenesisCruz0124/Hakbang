package ph.hakbang.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey
    val date: LocalDate,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val calories: Double = 0.0,
    val goalMet: Boolean = false,
    /** Cumulative sensor reading captured the last time [steps] was updated for this day. */
    val lastCumulativeSensorValue: Long = 0L,
    /** Cumulative sensor reading at the start of this day, used to derive [steps]. */
    val baselineSensorValue: Long = 0L
)
