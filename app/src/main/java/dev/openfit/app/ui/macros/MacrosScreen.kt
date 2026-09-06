package dev.openfit.app.ui.macros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.ui.components.ConfirmDialog
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.SectionHeader
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosScreen(
    settings: MacroSettings,
    totals: DayTotals,
    formatTime: (Long) -> String,
    onLogMeal: () -> Unit,
    onGallery: () -> Unit,
    onDeleteMeal: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteMeal by remember { mutableStateOf<MealEntry?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Macros") }) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Calendar.getInstance().time),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            IntakeCard(totals = totals, settings = settings)

            Button(
                onClick = onLogMeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log a Meal", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Gallery")
            }

            if (totals.meals.isEmpty()) {
                EmptyState(
                    title = "No meals yet today",
                    subtitle = "Snap a photo of your plate to log calories and macros."
                )
            } else {
                SectionHeader("Logged today")
                totals.meals.forEach { m ->
                    MealRow(
                        meal = m,
                        timeText = formatTime(m.timestamp),
                        onDelete = { deleteMeal = m }
                    )
                }
            }
        }
    }

    deleteMeal?.let { meal ->
        ConfirmDialog(
            title = "Delete meal?",
            text = "“${meal.dish}” (${meal.calories.toInt()} kcal) will be permanently removed.",
            onConfirm = {
                onDeleteMeal(meal.id)
                deleteMeal = null
            },
            onDismiss = { deleteMeal = null }
        )
    }
}

@Composable
private fun IntakeCard(totals: DayTotals, settings: MacroSettings) {
    val goal = settings.goals.calories
    val remaining = goal - totals.calories
    val over = remaining < 0 && goal > 0

    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Today's intake",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${abs(remaining.toInt())}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.padding(bottom = 6.dp)) {
                    Text(
                        if (over) "kcal over goal" else "kcal remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${totals.calories.toInt()} / ${goal.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            MetricRow("Protein", totals.protein, settings.goals.protein, "g")
            MetricRow("Carbs", totals.carbs, settings.goals.carbs, "g")
            MetricRow("Fat", totals.fat, settings.goals.fat, "g")
        }
    }
}

@Composable
private fun MetricRow(label: String, value: Double, goal: Double, unit: String) {
    val over = goal > 0 && value > goal
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${value.toInt()} / ${goal.toInt()} $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
        val fraction = if (goal > 0) (value / goal).toFloat() else 0f
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = if (over) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            }
        )
    }
}

@Composable
private fun MealRow(meal: MealEntry, timeText: String, onDelete: () -> Unit) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(meal.dish, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "$timeText  •  P ${meal.protein.toInt()}g  C ${meal.carbs.toInt()}g  F ${meal.fat.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "${meal.calories.toInt()} kcal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
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
