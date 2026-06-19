package ph.hakbang.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ph.hakbang.app.data.preferences.UserPreferences
import ph.hakbang.app.data.repository.StepRepository
import ph.hakbang.app.ui.screens.badges.BadgesViewModel
import ph.hakbang.app.ui.screens.history.HistoryViewModel
import ph.hakbang.app.ui.screens.home.HomeViewModel

class ViewModelFactory(
    private val preferences: UserPreferences,
    private val repository: StepRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AppViewModel::class.java -> AppViewModel(preferences, repository) as T
        HomeViewModel::class.java -> HomeViewModel(repository, preferences) as T
        HistoryViewModel::class.java -> HistoryViewModel(repository) as T
        BadgesViewModel::class.java -> BadgesViewModel(preferences, repository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
