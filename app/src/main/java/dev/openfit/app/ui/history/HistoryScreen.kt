package dev.openfit.app.ui.history

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.domain.TimeFormatter
import dev.openfit.app.domain.UnitConverter
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavHostController) {
    val container = appContainer()
    val vm: HistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HistoryViewModel(container.workoutRepository, container.mealRepository, container.settingsRepository) }
        }
    )

    val history by vm.history.collectAsState()
    val meals by vm.meals.collectAsState()
    val unit by vm.unit.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (history.isEmpty() && meals.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing yet",
                        subtitle = "Finish a workout or log a meal to see them here."
                    )
                }
            } else {
                if (meals.isNotEmpty()) {
                    item { SectionHeader("Meals") }
                    items(meals, key = { "m${it.id}" }) { m ->
                        MealCard(m, onDelete = { vm.deleteMeal(m.id) })
                    }
                }
                if (history.isNotEmpty()) {
                    item { SectionHeader("Completed Sessions") }
                    items(history, key = { "w${it.workoutId}" }) { row ->
                        Card(onClick = { navController.navigate(Routes.summary(row.workoutId)) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.name, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        TimeFormatter.date(row.startedAt),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${row.exerciseCount} exercises · ${row.setCount} sets · ${UnitConverter.displayWeight(row.totalKg, unit)} ${unit.label}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { vm.deleteWorkout(row.workoutId) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete workout")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: MealEntry, onDelete: () -> Unit) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(meal.dish, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    mealTime(meal.timestamp) + "  •  P ${meal.protein.toInt()}g  C ${meal.carbs.toInt()}g  F ${meal.fat.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${meal.calories.toInt()} kcal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete meal", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun mealTime(ts: Long): String =
    SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()).format(Date(ts))
