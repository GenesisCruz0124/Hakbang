package ph.hakbang.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ph.hakbang.app.data.local.entity.DailySteps
import ph.hakbang.app.data.repository.StepRepository
import java.time.LocalDate
import java.time.temporal.IsoFields

enum class HistoryPeriod { DAILY, WEEKLY, MONTHLY }

data class HistoryEntry(val label: String, val steps: Int)

data class HistoryUiState(
    val period: HistoryPeriod = HistoryPeriod.DAILY,
    val entries: List<HistoryEntry> = emptyList(),
    val totalSteps: Long = 0,
    val averagePerDay: Int = 0,
    val bestDaySteps: Int = 0
)

class HistoryViewModel(private val repository: StepRepository) : ViewModel() {

    private val period = MutableStateFlow(HistoryPeriod.DAILY)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeAll(),
        period
    ) { all, selectedPeriod ->
        buildUiState(all, selectedPeriod)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())

    fun selectPeriod(newPeriod: HistoryPeriod) {
        period.value = newPeriod
    }

    private fun buildUiState(all: List<DailySteps>, selectedPeriod: HistoryPeriod): HistoryUiState {
        val totalSteps = all.sumOf { it.steps.toLong() }
        val averagePerDay = if (all.isNotEmpty()) (totalSteps / all.size).toInt() else 0
        val bestDaySteps = all.maxOfOrNull { it.steps } ?: 0

        val entries = when (selectedPeriod) {
            HistoryPeriod.DAILY -> all.takeLast(14).map { HistoryEntry(it.date.toString().takeLast(5), it.steps) }
            HistoryPeriod.WEEKLY -> all.groupBy { it.date.get(IsoFields.WEEK_BASED_YEAR) to it.date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) }
                .toSortedMap(compareBy({ it.first }, { it.second }))
                .map { (key, days) -> HistoryEntry("W${key.second}", days.sumOf { it.steps }) }
                .takeLast(12)
            HistoryPeriod.MONTHLY -> all.groupBy { it.date.year to it.date.monthValue }
                .toSortedMap(compareBy({ it.first }, { it.second }))
                .map { (key, days) -> HistoryEntry("${key.second}/${key.first.toString().takeLast(2)}", days.sumOf { it.steps }) }
                .takeLast(12)
        }

        return HistoryUiState(
            period = selectedPeriod,
            entries = entries,
            totalSteps = totalSteps,
            averagePerDay = averagePerDay,
            bestDaySteps = bestDaySteps
        )
    }
}
