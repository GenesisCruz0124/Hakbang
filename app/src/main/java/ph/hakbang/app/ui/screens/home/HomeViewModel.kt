package ph.hakbang.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ph.hakbang.app.data.preferences.UserPreferences
import ph.hakbang.app.data.repository.StepRepository
import ph.hakbang.app.util.StepCalculations
import java.time.LocalDate

data class HomeUiState(
    val steps: Int = 0,
    val dailyGoal: Int = 10_000,
    val distanceKm: Double = 0.0,
    val calories: Double = 0.0,
    val percentOfGoal: Int = 0,
    val currentStreak: Int = 0
)

class HomeViewModel(
    repository: StepRepository,
    preferences: UserPreferences
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeToday(LocalDate.now()),
        preferences.userProfile
    ) { today, profile ->
        val steps = today?.steps ?: 0
        HomeUiState(
            steps = steps,
            dailyGoal = profile.dailyGoal,
            distanceKm = StepCalculations.distanceKm(steps, profile.strideMeters),
            calories = StepCalculations.calories(steps, profile.weightKg),
            percentOfGoal = if (profile.dailyGoal > 0) ((steps * 100) / profile.dailyGoal).coerceAtMost(999) else 0,
            currentStreak = profile.streakCount
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())
}
