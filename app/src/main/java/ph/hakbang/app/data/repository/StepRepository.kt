package ph.hakbang.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ph.hakbang.app.data.local.dao.DailyStepsDao
import ph.hakbang.app.data.local.entity.DailySteps
import ph.hakbang.app.data.preferences.UserPreferences
import ph.hakbang.app.util.Badge
import ph.hakbang.app.util.StepCalculations
import java.time.LocalDate

class StepRepository(
    private val dao: DailyStepsDao,
    private val preferences: UserPreferences
) {

    fun observeToday(date: LocalDate = LocalDate.now()): Flow<DailySteps?> = dao.observeByDate(date)

    fun observeSince(fromDate: LocalDate): Flow<List<DailySteps>> = dao.observeSince(fromDate)

    fun observeAll(): Flow<List<DailySteps>> = dao.observeAll()

    fun observeAllTimeTotalSteps(): Flow<Long> = dao.observeAllTimeTotalSteps()

    /**
     * Applies a new all-time cumulative step total (derived from the hardware sensor) to
     * today's row, creating today's baseline from yesterday's data if this is the first
     * update of a new day. This is what makes the daily count reset at midnight while the
     * underlying sensor value just keeps climbing.
     */
    suspend fun applySensorAllTimeTotal(allTimeTotal: Long, today: LocalDate = LocalDate.now()) {
        val existing = dao.getByDate(today)
        val profile = preferences.userProfile.first()

        val baseline = existing?.baselineSensorValue ?: run {
            // New day: baseline is today's first observed all-time total, so steps start at 0.
            allTimeTotal
        }

        val steps = (allTimeTotal - baseline).toInt().coerceAtLeast(0)
        val distance = StepCalculations.distanceMeters(steps, profile.strideMeters)
        val calories = StepCalculations.calories(steps, profile.weightKg)
        val goalMet = steps >= profile.dailyGoal

        dao.upsert(
            DailySteps(
                date = today,
                steps = steps,
                distanceMeters = distance,
                calories = calories,
                goalMet = goalMet,
                lastCumulativeSensorValue = allTimeTotal,
                baselineSensorValue = baseline
            )
        )

        updateStreakAndBadges(today)
    }

    private suspend fun updateStreakAndBadges(today: LocalDate) {
        val history = dao.getAllDescending()
        val streak = calculateCurrentStreak(history, today)
        val profile = preferences.userProfile.first()
        val longestStreak = maxOf(profile.longestStreak, streak)
        preferences.setStreak(streak, longestStreak)

        val goalMetCount = history.count { it.goalMet }
        val bestDaySteps = history.maxOfOrNull { it.steps } ?: 0
        val allTimeTotalSteps = history.sumOf { it.steps.toLong() }

        val earned = Badge.evaluate(
            goalMetCount = goalMetCount,
            longestStreak = longestStreak,
            bestDaySteps = bestDaySteps,
            allTimeTotalSteps = allTimeTotalSteps
        )
        val current = profile.earnedBadgeIds
        val updated = current + earned.map { it.id }
        if (updated != current) {
            preferences.setEarnedBadges(updated)
        }
    }

    /** Consecutive days (counting back from [today]) where the goal was met. */
    private fun calculateCurrentStreak(historyDescending: List<DailySteps>, today: LocalDate): Int {
        var streak = 0
        var expectedDate = today
        for (day in historyDescending) {
            if (day.date != expectedDate) break
            if (!day.goalMet) break
            streak++
            expectedDate = expectedDate.minusDays(1)
        }
        return streak
    }

    suspend fun recalculateForCurrentSettings(today: LocalDate = LocalDate.now()) {
        val current = dao.getByDate(today) ?: return
        applySensorAllTimeTotal(current.lastCumulativeSensorValue, today)
    }

    suspend fun resetAllData() {
        dao.deleteAll()
        preferences.resetAll()
    }
}
