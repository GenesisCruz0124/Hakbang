package ph.hakbang.app.util

import androidx.compose.runtime.compositionLocalOf
import ph.hakbang.app.data.preferences.AppLanguage

/**
 * App-wide Taglish/English string table. The language toggle is a user setting (not the
 * device locale), so strings are resolved from this in-app map rather than res/values-*.
 */
enum class StringKey {
    STEPS, DAILY_GOAL, DISTANCE, CALORIES, STREAK, HISTORY, SETTINGS, BADGES, HOME,
    PERCENT_OF_GOAL, STREAK_BADGE, KM_UNIT, KCAL_UNIT,
    DAILY, WEEKLY, MONTHLY, TOTAL_STEPS, AVERAGE_PER_DAY, BEST_DAY,
    WEIGHT, STRIDE, LANGUAGE, THEME, RESET_DATA, RESET_DATA_TITLE, RESET_DATA_MESSAGE,
    CANCEL, CONFIRM, SAVE,
    PERMISSION_TITLE, PERMISSION_MESSAGE, PERMISSION_GRANT,
    BADGE_FIRST_STEPS, BADGE_STREAK_5, BADGE_STREAK_7, BADGE_STREAK_30,
    BADGE_10K_DAY, BADGE_100K_TOTAL, BADGE_1M_TOTAL, BADGES_EARNED, BADGES_LOCKED,
    NOTIFICATION_TITLE, NOTIFICATION_TEXT
}

private val englishStrings: Map<StringKey, String> = mapOf(
    StringKey.STEPS to "Steps",
    StringKey.DAILY_GOAL to "Daily Goal",
    StringKey.DISTANCE to "Distance",
    StringKey.CALORIES to "Calories",
    StringKey.STREAK to "Streak",
    StringKey.HISTORY to "History",
    StringKey.SETTINGS to "Settings",
    StringKey.BADGES to "Badges",
    StringKey.HOME to "Home",
    StringKey.PERCENT_OF_GOAL to "%d%% of goal",
    StringKey.STREAK_BADGE to "🔥 %d-day streak",
    StringKey.KM_UNIT to "km",
    StringKey.KCAL_UNIT to "kcal",
    StringKey.DAILY to "Daily",
    StringKey.WEEKLY to "Weekly",
    StringKey.MONTHLY to "Monthly",
    StringKey.TOTAL_STEPS to "Total Steps",
    StringKey.AVERAGE_PER_DAY to "Average / Day",
    StringKey.BEST_DAY to "Best Day",
    StringKey.WEIGHT to "Weight (kg)",
    StringKey.STRIDE to "Stride Length (m)",
    StringKey.LANGUAGE to "Language",
    StringKey.THEME to "Theme",
    StringKey.RESET_DATA to "Reset All Data",
    StringKey.RESET_DATA_TITLE to "Reset all data?",
    StringKey.RESET_DATA_MESSAGE to "This will permanently delete all your step history, streaks, and badges. This cannot be undone.",
    StringKey.CANCEL to "Cancel",
    StringKey.CONFIRM to "Confirm",
    StringKey.SAVE to "Save",
    StringKey.PERMISSION_TITLE to "Activity Permission",
    StringKey.PERMISSION_MESSAGE to "Kailangan namin ito para mabilang ang hakbang mo",
    StringKey.PERMISSION_GRANT to "Grant Permission",
    StringKey.BADGE_FIRST_STEPS to "First Steps",
    StringKey.BADGE_STREAK_5 to "5-Day Streak",
    StringKey.BADGE_STREAK_7 to "7-Day Streak",
    StringKey.BADGE_STREAK_30 to "30-Day Streak",
    StringKey.BADGE_10K_DAY to "10K in a Day",
    StringKey.BADGE_100K_TOTAL to "100K Total Steps",
    StringKey.BADGE_1M_TOTAL to "1M Total Steps",
    StringKey.BADGES_EARNED to "Earned",
    StringKey.BADGES_LOCKED to "Locked",
    StringKey.NOTIFICATION_TITLE to "Hakbang is counting your steps",
    StringKey.NOTIFICATION_TEXT to "%d steps today"
)

private val taglishStrings: Map<StringKey, String> = mapOf(
    StringKey.STEPS to "Hakbang",
    StringKey.DAILY_GOAL to "Araw-araw na Target",
    StringKey.DISTANCE to "Layo",
    StringKey.CALORIES to "Calories",
    StringKey.STREAK to "Sunod-sunod",
    StringKey.HISTORY to "Kasaysayan",
    StringKey.SETTINGS to "Mga Setting",
    StringKey.BADGES to "Mga Badge",
    StringKey.HOME to "Home",
    StringKey.PERCENT_OF_GOAL to "%d%% ng target",
    StringKey.STREAK_BADGE to "🔥 %d araw nang sunod-sunod",
    StringKey.KM_UNIT to "km",
    StringKey.KCAL_UNIT to "kcal",
    StringKey.DAILY to "Araw-araw",
    StringKey.WEEKLY to "Lingguhan",
    StringKey.MONTHLY to "Buwanan",
    StringKey.TOTAL_STEPS to "Kabuuang Hakbang",
    StringKey.AVERAGE_PER_DAY to "Average kada araw",
    StringKey.BEST_DAY to "Pinakamagandang Araw",
    StringKey.WEIGHT to "Timbang (kg)",
    StringKey.STRIDE to "Haba ng Hakbang (m)",
    StringKey.LANGUAGE to "Wika",
    StringKey.THEME to "Tema",
    StringKey.RESET_DATA to "I-reset ang Lahat ng Data",
    StringKey.RESET_DATA_TITLE to "I-reset ang lahat ng data?",
    StringKey.RESET_DATA_MESSAGE to "Permanenteng mabubura ang lahat ng history ng hakbang, streaks, at badges mo. Hindi na ito mababalik pa.",
    StringKey.CANCEL to "Kanselahin",
    StringKey.CONFIRM to "Kumpirmahin",
    StringKey.SAVE to "I-save",
    StringKey.PERMISSION_TITLE to "Permiso para sa Activity",
    StringKey.PERMISSION_MESSAGE to "Kailangan namin ito para mabilang ang hakbang mo",
    StringKey.PERMISSION_GRANT to "Pumayag",
    StringKey.BADGE_FIRST_STEPS to "Unang Hakbang",
    StringKey.BADGE_STREAK_5 to "5-Araw na Streak",
    StringKey.BADGE_STREAK_7 to "7-Araw na Streak",
    StringKey.BADGE_STREAK_30 to "30-Araw na Streak",
    StringKey.BADGE_10K_DAY to "10K sa Isang Araw",
    StringKey.BADGE_100K_TOTAL to "100K Kabuuang Hakbang",
    StringKey.BADGE_1M_TOTAL to "1M Kabuuang Hakbang",
    StringKey.BADGES_EARNED to "Nakuha",
    StringKey.BADGES_LOCKED to "Naka-lock",
    StringKey.NOTIFICATION_TITLE to "Binibilang ng Hakbang ang mga hakbang mo",
    StringKey.NOTIFICATION_TEXT to "%d hakbang ngayong araw"
)

class AppStrings(private val language: AppLanguage) {
    private val table = if (language == AppLanguage.TAGLISH) taglishStrings else englishStrings

    operator fun get(key: StringKey): String = table[key] ?: englishStrings[key].orEmpty()

    fun format(key: StringKey, vararg args: Any): String = String.format(get(key), *args)
}

val LocalAppStrings = compositionLocalOf { AppStrings(AppLanguage.ENGLISH) }
