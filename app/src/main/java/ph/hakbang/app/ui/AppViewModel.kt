package ph.hakbang.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ph.hakbang.app.data.preferences.AppLanguage
import ph.hakbang.app.data.preferences.UserPreferences
import ph.hakbang.app.data.preferences.UserProfile
import ph.hakbang.app.data.repository.StepRepository

/** Holds the user profile/settings shared across all screens. */
class AppViewModel(
    private val preferences: UserPreferences,
    private val repository: StepRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = preferences.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    fun setDailyGoal(goal: Int) = viewModelScope.launch {
        preferences.setDailyGoal(goal)
        repository.recalculateForCurrentSettings()
    }

    fun setWeightKg(weight: Double) = viewModelScope.launch {
        preferences.setWeightKg(weight)
        repository.recalculateForCurrentSettings()
    }

    fun setStrideMeters(stride: Double) = viewModelScope.launch {
        preferences.setStrideMeters(stride)
        repository.recalculateForCurrentSettings()
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        preferences.setLanguage(language)
    }

    fun setPermissionAsked(asked: Boolean) = viewModelScope.launch {
        preferences.setPermissionAsked(asked)
    }

    fun resetAllData() = viewModelScope.launch {
        repository.resetAllData()
    }
}
