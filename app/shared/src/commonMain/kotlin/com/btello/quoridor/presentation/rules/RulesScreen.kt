package com.btello.quoridor.presentation.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.btello.quoridor.presentation.theme.QuoridorTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import quoridor.app.shared.generated.resources.Res
import quoridor.app.shared.generated.resources.rules_jumps_body
import quoridor.app.shared.generated.resources.rules_jumps_title
import quoridor.app.shared.generated.resources.rules_movement_body
import quoridor.app.shared.generated.resources.rules_movement_title
import quoridor.app.shared.generated.resources.rules_objective_body
import quoridor.app.shared.generated.resources.rules_objective_title
import quoridor.app.shared.generated.resources.rules_title
import quoridor.app.shared.generated.resources.rules_walls_body
import quoridor.app.shared.generated.resources.rules_walls_count_body
import quoridor.app.shared.generated.resources.rules_walls_count_title
import quoridor.app.shared.generated.resources.rules_walls_title
import quoridor.app.shared.generated.resources.rules_winning_body
import quoridor.app.shared.generated.resources.rules_winning_title

private data class RuleSection(val titleRes: StringResource, val bodyRes: StringResource)

private val ruleSections: List<RuleSection> = listOf(
    RuleSection(Res.string.rules_objective_title, Res.string.rules_objective_body),
    RuleSection(Res.string.rules_movement_title, Res.string.rules_movement_body),
    RuleSection(Res.string.rules_jumps_title, Res.string.rules_jumps_body),
    RuleSection(Res.string.rules_walls_title, Res.string.rules_walls_body),
    RuleSection(Res.string.rules_walls_count_title, Res.string.rules_walls_count_body),
    RuleSection(Res.string.rules_winning_title, Res.string.rules_winning_body),
)

@Composable
internal fun RulesScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(Res.string.rules_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            ruleSections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(section.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(section.bodyRes),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RulesScreenPreview() {
    QuoridorTheme {
        RulesScreen()
    }
}
