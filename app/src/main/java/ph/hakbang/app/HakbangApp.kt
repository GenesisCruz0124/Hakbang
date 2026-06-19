package ph.hakbang.app

import android.app.Application
import ph.hakbang.app.data.local.db.HakbangDatabase
import ph.hakbang.app.data.preferences.UserPreferences
import ph.hakbang.app.data.repository.StepRepository

/** Manual DI container: keeps the dependency graph simple and explicit for this single-module app. */
class HakbangApp : Application() {

    val database: HakbangDatabase by lazy { HakbangDatabase.getInstance(this) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
    val stepRepository: StepRepository by lazy { StepRepository(database.dailyStepsDao(), userPreferences) }
}
