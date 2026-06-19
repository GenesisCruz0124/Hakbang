package ph.hakbang.app.util

enum class Badge(val id: String, val titleResKey: String) {
    FIRST_STEPS("first_steps", "badge_first_steps"),
    STREAK_5("streak_5", "badge_streak_5"),
    STREAK_7("streak_7", "badge_streak_7"),
    STREAK_30("streak_30", "badge_streak_30"),
    TEN_K_DAY("ten_k_day", "badge_10k_day"),
    HUNDRED_K_TOTAL("hundred_k_total", "badge_100k_total"),
    ONE_M_TOTAL("one_m_total", "badge_1m_total");

    companion object {
        /**
         * Determines which badges should now be earned given the user's stats.
         * goalMetCount: total number of days where the daily goal was reached.
         */
        fun evaluate(
            goalMetCount: Int,
            longestStreak: Int,
            bestDaySteps: Int,
            allTimeTotalSteps: Long
        ): Set<Badge> {
            val earned = mutableSetOf<Badge>()
            if (goalMetCount >= 1) earned += FIRST_STEPS
            if (longestStreak >= 5) earned += STREAK_5
            if (longestStreak >= 7) earned += STREAK_7
            if (longestStreak >= 30) earned += STREAK_30
            if (bestDaySteps >= 10_000) earned += TEN_K_DAY
            if (allTimeTotalSteps >= 100_000) earned += HUNDRED_K_TOTAL
            if (allTimeTotalSteps >= 1_000_000) earned += ONE_M_TOTAL
            return earned
        }
    }
}
