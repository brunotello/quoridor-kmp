package com.btello.quoridor.presentation.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btello.quoridor.data.stats.StatisticsProvider
import com.btello.quoridor.domain.ai.AiDifficulty
import com.btello.quoridor.domain.ai.AiDifficulty.EASY
import com.btello.quoridor.domain.ai.AiDifficulty.EXPERT
import com.btello.quoridor.domain.ai.AiDifficulty.HARD
import com.btello.quoridor.domain.ai.AiDifficulty.MEDIUM
import com.btello.quoridor.domain.stats.DifficultyBreakdown
import com.btello.quoridor.domain.stats.GameStatistics
import com.btello.quoridor.presentation.theme.QuoridorTheme
import com.btello.quoridor.presentation.theme.safeAreaTopPadding
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.difficulty_easy
import quoridor.app.shared.generated.resources.difficulty_expert
import quoridor.app.shared.generated.resources.difficulty_hard
import quoridor.app.shared.generated.resources.difficulty_medium
import quoridor.app.shared.generated.resources.stats_best_time
import quoridor.app.shared.generated.resources.stats_count_value
import quoridor.app.shared.generated.resources.stats_difficulty_local
import quoridor.app.shared.generated.resources.stats_empty
import quoridor.app.shared.generated.resources.stats_fewest_moves
import quoridor.app.shared.generated.resources.stats_fewest_walls
import quoridor.app.shared.generated.resources.stats_games_count
import quoridor.app.shared.generated.resources.stats_result_summary
import quoridor.app.shared.generated.resources.stats_section_records
import quoridor.app.shared.generated.resources.stats_section_results
import quoridor.app.shared.generated.resources.stats_time_format
import quoridor.app.shared.generated.resources.stats_title
import quoridor.app.shared.generated.resources.stats_total_games
import quoridor.app.shared.generated.resources.stats_value_none

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

@Composable
internal fun StatisticsScreen(
    viewModel: StatisticsViewModel = viewModel { StatisticsViewModel(StatisticsProvider.repository) },
) {
    LaunchedEffect(Unit) { viewModel.refresh() }
    StatisticsContent(state = viewModel.uiState)
}

@Composable
private fun StatisticsContent(state: StatisticsUiState) {
    val statistics = state.statistics
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeAreaTopPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Text(
                text = stringResource(Res.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            if (statistics.totalGames == 0) {
                Text(
                    text = stringResource(Res.string.stats_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                StatRow(
                    icon = Icons.Filled.SportsEsports,
                    label = stringResource(Res.string.stats_total_games),
                    value = stringResource(Res.string.stats_count_value, statistics.totalGames),
                )
                RecordsSection(statistics)
                ResultsSection(statistics.breakdown)
            }
        }
    }
}

@Composable
private fun RecordsSection(statistics: GameStatistics) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(Res.string.stats_section_records))
        StatRow(
            icon = Icons.Filled.Timer,
            label = stringResource(Res.string.stats_best_time),
            value = statistics.bestTimeMillis?.let { formatTime(it) }
                ?: stringResource(Res.string.stats_value_none),
        )
        StatRow(
            icon = Icons.Filled.Flag,
            label = stringResource(Res.string.stats_fewest_moves),
            value = statistics.fewestMoves?.let { stringResource(Res.string.stats_count_value, it) }
                ?: stringResource(Res.string.stats_value_none),
        )
        StatRow(
            icon = Icons.Filled.Fence,
            label = stringResource(Res.string.stats_fewest_walls),
            value = statistics.fewestWalls?.let { stringResource(Res.string.stats_count_value, it) }
                ?: stringResource(Res.string.stats_value_none),
        )
    }
}

@Composable
private fun ResultsSection(breakdown: List<DifficultyBreakdown>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(Res.string.stats_section_results))
        breakdown.forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = difficultyLabel(entry.difficulty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(Res.string.stats_games_count, entry.total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(Res.string.stats_result_summary, entry.wins, entry.losses),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun difficultyLabel(difficulty: AiDifficulty?): String = when (difficulty) {
    EASY -> stringResource(Res.string.difficulty_easy)
    MEDIUM -> stringResource(Res.string.difficulty_medium)
    HARD -> stringResource(Res.string.difficulty_hard)
    EXPERT -> stringResource(Res.string.difficulty_expert)
    null -> stringResource(Res.string.stats_difficulty_local)
}

@Composable
private fun formatTime(durationMillis: Long): String {
    val totalSeconds = durationMillis / MILLIS_PER_SECOND
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return stringResource(
        Res.string.stats_time_format,
        minutes.toString(),
        seconds.toString().padStart(2, '0'),
    )
}

@Preview
@Composable
private fun StatisticsContentPreview() {
    QuoridorTheme {
        StatisticsContent(
            state = StatisticsUiState(
                statistics = GameStatistics(
                    totalGames = 7,
                    breakdown = listOf(
                        DifficultyBreakdown(EASY, wins = 3, losses = 1),
                        DifficultyBreakdown(HARD, wins = 1, losses = 1),
                        DifficultyBreakdown(null, wins = 1, losses = 0),
                    ),
                    bestTimeMillis = 95_000,
                    fewestMoves = 21,
                    fewestWalls = 2,
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun StatisticsEmptyPreview() {
    QuoridorTheme {
        StatisticsContent(state = StatisticsUiState())
    }
}
