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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.domain.TimeFormatter
import dev.openfit.app.domain.UnitConverter
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.ConfirmDialog
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.components.TintedIconCircle
import dev.openfit.app.ui.navigation.Routes
import java.io.File
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
    var deleteWorkoutId by remember { mutableStateOf<Long?>(null) }
    var deleteMeal by remember { mutableStateOf<MealEntry?>(null) }

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
                        subtitle = "Finish a workout or log a meal to see them here.",
                        icon = Icons.Filled.History
                    )
                }
            } else {
                if (meals.isNotEmpty()) {
                    item { SectionHeader("Meals") }
                    items(meals, key = { "m${it.id}" }) { m ->
                        MealCard(m, onDelete = { deleteMeal = m })
                    }
                }
                if (history.isNotEmpty()) {
                    item { SectionHeader("Completed Sessions") }
                    items(history, key = { "w${it.workoutId}" }) { row ->
                        Card {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TintedIconCircle(icon = Icons.Filled.FitnessCenter)
                                Spacer(Modifier.width(16.dp))
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = { deleteWorkoutId = row.workoutId }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete workout",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteWorkoutId?.let { id ->
        ConfirmDialog(
            title = "Delete workout?",
            text = "The workout and all its logged sets will be permanently removed.",
            onConfirm = {
                vm.deleteWorkout(id)
                deleteWorkoutId = null
            },
            onDismiss = { deleteWorkoutId = null }
        )
    }

    deleteMeal?.let { meal ->
        ConfirmDialog(
            title = "Delete meal?",
            text = "“${meal.dish}” (${meal.calories.toInt()} kcal) will be permanently removed.",
            onConfirm = {
                vm.deleteMeal(meal.id)
                deleteMeal = null
            },
            onDismiss = { deleteMeal = null }
        )
    }
}

@Composable
private fun MealCard(meal: MealEntry, onDelete: () -> Unit) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageFile = if (meal.imagePath.isNotBlank()) File(meal.imagePath) else null
            if (imageFile != null && imageFile.exists()) {
                AsyncImage(
                    model = imageFile,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
            } else {
                TintedIconCircle(icon = Icons.Filled.Restaurant)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(meal.dish, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    mealTime(meal.timestamp) + "  •  P ${meal.protein.toInt()}g  C ${meal.carbs.toInt()}g  F ${meal.fat.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${meal.calories.toInt()} kcal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete meal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun mealTime(ts: Long): String =
    SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()).format(Date(ts))
