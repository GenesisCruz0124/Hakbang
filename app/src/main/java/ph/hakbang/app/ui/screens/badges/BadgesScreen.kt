package ph.hakbang.app.ui.screens.badges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ph.hakbang.app.util.Badge
import ph.hakbang.app.util.LocalAppStrings
import ph.hakbang.app.util.StringKey

private fun Badge.stringKeyFor(): StringKey = when (this) {
    Badge.FIRST_STEPS -> StringKey.BADGE_FIRST_STEPS
    Badge.STREAK_5 -> StringKey.BADGE_STREAK_5
    Badge.STREAK_7 -> StringKey.BADGE_STREAK_7
    Badge.STREAK_30 -> StringKey.BADGE_STREAK_30
    Badge.TEN_K_DAY -> StringKey.BADGE_10K_DAY
    Badge.HUNDRED_K_TOTAL -> StringKey.BADGE_100K_TOTAL
    Badge.ONE_M_TOTAL -> StringKey.BADGE_1M_TOTAL
}

@Composable
fun BadgesScreen(viewModel: BadgesViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current

    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = strings[StringKey.BADGES_EARNED],
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        BadgeGrid(badges = uiState.earned, earned = true, strings = strings)

        Text(
            text = strings[StringKey.BADGES_LOCKED],
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )
        BadgeGrid(badges = uiState.locked, earned = false, strings = strings)
    }
}

@Composable
private fun BadgeGrid(badges: List<Badge>, earned: Boolean, strings: ph.hakbang.app.util.AppStrings) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(badges) { badge ->
            BadgeCard(title = strings[badge.stringKeyFor()], earned = earned)
        }
    }
}

@Composable
private fun BadgeCard(title: String, earned: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (earned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (earned) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (earned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
