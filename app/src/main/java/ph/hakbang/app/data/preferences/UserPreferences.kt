package ph.hakbang.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "hakbang_settings")

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    TAGLISH("tl")
}

data class UserProfile(
    val dailyGoal: Int = 10_000,
    val weightKg: Double = 60.0,
    val strideMeters: Double = 0.762,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val streakCount: Int = 0,
    val longestStreak: Int = 0,
    val earnedBadgeIds: Set<String> = emptySet(),
    val onboardingPermissionAsked: Boolean = false
)

/**
 * DataStore-backed user profile and app settings. Also stores step-sensor bookkeeping
 * needed to keep cumulative step counts correct across device reboots.
 */
class UserPreferences(private val context: Context) {

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val WEIGHT_KG = doublePreferencesKey("weight_kg")
        val STRIDE_METERS = doublePreferencesKey("stride_meters")
        val LANGUAGE = stringPreferencesKey("language")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val EARNED_BADGES = stringPreferencesKey("earned_badges")
        val PERMISSION_ASKED = booleanPreferencesKey("permission_asked")

        // Sensor bookkeeping: see StepSensorManager for how these are used to
        // derive an ever-increasing all-time step total across reboots.
        val LAST_RAW_SENSOR_VALUE = longPreferencesKey("last_raw_sensor_value")
        val BOOT_OFFSET = longPreferencesKey("boot_offset")
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            dailyGoal = prefs[Keys.DAILY_GOAL] ?: 10_000,
            weightKg = prefs[Keys.WEIGHT_KG] ?: 60.0,
            strideMeters = prefs[Keys.STRIDE_METERS] ?: 0.762,
            language = if (prefs[Keys.LANGUAGE] == AppLanguage.TAGLISH.code) AppLanguage.TAGLISH else AppLanguage.ENGLISH,
            streakCount = prefs[Keys.STREAK_COUNT] ?: 0,
            longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
            earnedBadgeIds = prefs[Keys.EARNED_BADGES]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
            onboardingPermissionAsked = prefs[Keys.PERMISSION_ASKED] ?: false
        )
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL] = goal }
    }

    suspend fun setWeightKg(weight: Double) {
        context.dataStore.edit { it[Keys.WEIGHT_KG] = weight }
    }

    suspend fun setStrideMeters(stride: Double) {
        context.dataStore.edit { it[Keys.STRIDE_METERS] = stride }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setStreak(current: Int, longest: Int) {
        context.dataStore.edit {
            it[Keys.STREAK_COUNT] = current
            it[Keys.LONGEST_STREAK] = longest
        }
    }

    suspend fun setEarnedBadges(badgeIds: Set<String>) {
        context.dataStore.edit { it[Keys.EARNED_BADGES] = badgeIds.joinToString(",") }
    }

    suspend fun setPermissionAsked(asked: Boolean) {
        context.dataStore.edit { it[Keys.PERMISSION_ASKED] = asked }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }

    // --- Sensor bookkeeping ---

    val lastRawSensorValue: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_RAW_SENSOR_VALUE] ?: -1L }
    val bootOffset: Flow<Long> = context.dataStore.data.map { it[Keys.BOOT_OFFSET] ?: 0L }

    suspend fun saveSensorState(lastRawValue: Long, offset: Long) {
        context.dataStore.edit {
            it[Keys.LAST_RAW_SENSOR_VALUE] = lastRawValue
            it[Keys.BOOT_OFFSET] = offset
        }
    }
}
