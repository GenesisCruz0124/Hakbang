package ph.hakbang.app.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import ph.hakbang.app.util.LocalAppStrings
import ph.hakbang.app.util.StringKey

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.period == HistoryPeriod.DAILY,
                onClick = { viewModel.selectPeriod(HistoryPeriod.DAILY) },
                label = { Text(strings[StringKey.DAILY]) }
            )
            FilterChip(
                selected = uiState.period == HistoryPeriod.WEEKLY,
                onClick = { viewModel.selectPeriod(HistoryPeriod.WEEKLY) },
                label = { Text(strings[StringKey.WEEKLY]) }
            )
            FilterChip(
                selected = uiState.period == HistoryPeriod.MONTHLY,
                onClick = { viewModel.selectPeriod(HistoryPeriod.MONTHLY) },
                label = { Text(strings[StringKey.MONTHLY]) }
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

        if (uiState.entries.isNotEmpty()) {
            val chartEntryModel = remember(uiState.entries) {
                val floatEntries = uiState.entries.mapIndexed { index, entry ->
                    com.patrykandpatrick.vico.core.entry.FloatEntry(index.toFloat(), entry.steps.toFloat())
                }
                entryModelOf(floatEntries)
            }
            Chart(
                chart = columnChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryCard(title = strings[StringKey.TOTAL_STEPS], value = uiState.totalSteps.toString())
            SummaryCard(title = strings[StringKey.AVERAGE_PER_DAY], value = uiState.averagePerDay.toString())
            SummaryCard(title = strings[StringKey.BEST_DAY], value = uiState.bestDaySteps.toString())
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
