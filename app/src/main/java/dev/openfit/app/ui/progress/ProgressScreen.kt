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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import dev.openfit.app.domain.ProgressCalculator
import dev.openfit.app.domain.TimeFormatter
import dev.openfit.app.domain.UnitConverter
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.E1RmChart
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.components.StatCard

@OptIn(ExperimentalMaterial3Api::class)
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
    val filteredExercises by vm.filteredExercises.collectAsState()
    val query by vm.query.collectAsState()
    val selectedId by vm.selectedId.collectAsState()
    val points by vm.points.collectAsState()
    val unit by vm.unit.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Progress") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            if (exercises.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    EmptyState(
                        title = "No exercises logged yet",
                        subtitle = "Finish a workout and your estimated 1RM progress will be charted here.",
                        icon = Icons.Filled.BarChart
                    )
                }
                return@Scaffold
            }

            SectionHeader("Select Exercise", Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExercises, key = { it.id }) { exercise ->
                    FilterChip(
                        selected = exercise.id == selectedId,
                        onClick = { vm.select(exercise.id) },
                        label = { Text(exercise.name) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val selected = exercises.firstOrNull { it.id == selectedId }
                item {
                    if (selected == null) {
                        Card {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    "Pick an exercise",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Select an exercise above to chart your estimated 1RM over time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Card {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    selected.name,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${selected.muscleGroup} · estimated 1RM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                if (points.isEmpty()) {
                                    EmptyState(
                                        title = "No data yet",
                                        subtitle = "Log sets for this exercise to start charting progress."
                                    )
                                } else {
                                    E1RmChart(points = points, unit = unit)
                                }
                            }
                        }
                    }
                }

                if (selected != null && points.isNotEmpty()) {
                    item {
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
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = "Best",
            value = format(point = best, unit = unit),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Latest",
            value = format(point = latest, unit = unit),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Change",
            value = "${UnitConverter.displayWeight(delta, unit).let { if (delta >= 0) "+$it" else it }} ${unit.label}",
            modifier = Modifier.weight(1f)
        )
    }
}

private fun format(point: dev.openfit.app.domain.E1RmPoint?, unit: dev.openfit.app.domain.WeightUnit): String =
    if (point == null) "—" else "${UnitConverter.displayWeight(point.valueKg, unit)} ${unit.label}"
