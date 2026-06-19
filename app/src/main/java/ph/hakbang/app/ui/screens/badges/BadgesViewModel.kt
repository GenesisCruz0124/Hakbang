package ph.hakbang.app.ui.screens.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ph.hakbang.app.data.preferences.UserPreferences
import ph.hakbang.app.data.repository.StepRepository
import ph.hakbang.app.util.Badge

data class BadgesUiState(
    val earned: List<Badge> = emptyList(),
    val locked: List<Badge> = emptyList()
)

class BadgesViewModel(
    preferences: UserPreferences,
    repository: StepRepository
) : ViewModel() {

    val uiState: StateFlow<BadgesUiState> = combine(
        preferences.userProfile,
        repository.observeAllTimeTotalSteps()
    ) { profile, _ ->
        val earnedIds = profile.earnedBadgeIds
        val earned = Badge.entries.filter { it.id in earnedIds }
        val locked = Badge.entries.filter { it.id !in earnedIds }
        BadgesUiState(earned = earned, locked = locked)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BadgesUiState())
}
