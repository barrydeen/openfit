package dev.openfit.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import dev.openfit.app.domain.ProgressCalculator
import dev.openfit.app.domain.UnitConverter
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.E1RmChart
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.components.ScreenIntro
import dev.openfit.app.ui.components.StatCard

@Composable
fun ProgressScreen(navController: NavHostController) {
    val container = appContainer()
    val vm: ProgressViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ProgressViewModel(container.exerciseRepository, container.workoutRepository, container.settingsRepository)
            }
        }
    )

    val exercises by vm.exercises.collectAsState()
    val selectedId by vm.selectedId.collectAsState()
    val points by vm.points.collectAsState()
    val unit by vm.unit.collectAsState()

    val selected = exercises.firstOrNull { it.id == selectedId }
    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenIntro(
                    eyebrow = "THE LONG VIEW",
                    title = "Progress",
                    subtitle = "Small efforts. Stronger over time."
                )
            }
            if (exercises.isEmpty()) {
                item {
                    EmptyState(
                        title = "Room to grow",
                        subtitle = "Add exercises and log your sets to start following your strength.",
                        icon = Icons.Filled.BarChart
                    )
                }
            } else {
                item {
                    SectionHeader("Explore an exercise")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(exercises, key = { it.id }) { exercise ->
                            FilterChip(
                                selected = exercise.id == selectedId,
                                onClick = { vm.select(exercise.id) },
                                label = { Text(exercise.name) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                item {
                    if (selected == null) {
                        EmptyState(
                            title = "See your strength take shape",
                            subtitle = "Choose an exercise above to explore your estimated one-rep max, session by session.",
                            icon = Icons.Filled.BarChart
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                                Text(
                                    "STRENGTH TREND · ${unit.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    selected.name,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${selected.muscleGroup} · Estimated one-rep max",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                if (points.isEmpty()) {
                                    EmptyState(
                                        title = "Your first point is ahead",
                                        subtitle = "Log sets for this exercise to begin your strength trend."
                                    )
                                } else {
                                    E1RmChart(points = points, unit = unit)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "An estimate from your logged weight and reps, not a tested maximum.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (selected != null && points.isNotEmpty()) {
                    item {
                        SectionHeader("By the numbers")
                        StatsRow(points = points, unit = unit)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    points: List<dev.openfit.app.domain.E1RmPoint>,
    unit: dev.openfit.app.domain.WeightUnit
) {
    val best = ProgressCalculator.best(points)
    val latest = ProgressCalculator.latest(points)
    val delta = ProgressCalculator.deltaKg(points)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Personal best",
                value = format(point = best, unit = unit),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Latest session",
                value = format(point = latest, unit = unit),
                modifier = Modifier.weight(1f)
            )
        }
        StatCard(
            label = "Change since first session",
            value = if (points.size < 2) "Needs 2 sessions" else
                "${UnitConverter.displayWeight(delta, unit).let { if (delta >= 0) "+$it" else it }} ${unit.label}",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun format(point: dev.openfit.app.domain.E1RmPoint?, unit: dev.openfit.app.domain.WeightUnit): String =
    if (point == null) "—" else "${UnitConverter.displayWeight(point.valueKg, unit)} ${unit.label}"
