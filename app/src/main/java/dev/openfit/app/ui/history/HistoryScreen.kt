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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import dev.openfit.app.ui.components.ScreenIntro
import dev.openfit.app.ui.components.StatCard
import dev.openfit.app.ui.components.TintedIconCircle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenIntro(
                    eyebrow = "YOUR JOURNAL",
                    title = "History",
                    subtitle = "The sessions and meals that make up your routine."
                )
            }
            if (history.isEmpty() && meals.isEmpty()) {
                item {
                    EmptyState(
                        title = "Your story starts here",
                        subtitle = "Complete a workout or log a meal. Every entry is a step worth keeping.",
                        icon = Icons.Filled.History
                    )
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Workouts", history.size.toString(), Modifier.weight(1f))
                        StatCard("Meals logged", meals.size.toString(), Modifier.weight(1f))
                    }
                }
                if (meals.isNotEmpty()) {
                    item { SectionHeader("Meals", Modifier.padding(top = 12.dp)) }
                    items(meals, key = { "m${it.id}" }) { m ->
                        MealCard(m, onDelete = { deleteMeal = m })
                    }
                }
                if (history.isNotEmpty()) {
                    item { SectionHeader("Completed sessions", Modifier.padding(top = 12.dp)) }
                    items(history, key = { "w${it.workoutId}" }) { row ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
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
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "${row.exerciseCount} exercises · ${row.setCount} sets",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${UnitConverter.displayWeight(row.totalKg, unit)} ${unit.label} total volume",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                    mealTime(meal.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
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
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Protein" to meal.protein, "Carbs" to meal.carbs, "Fat" to meal.fat).forEach { (label, value) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${value.toInt()} g", style = MaterialTheme.typography.titleSmall)
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun mealTime(ts: Long): String =
    SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault()).format(Date(ts))
