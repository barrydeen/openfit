package dev.openfit.app.ui.macros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.SectionHeader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MacrosScreen(
    meals: List<MealEntry>,
    settings: MacroSettings,
    totals: DayTotals,
    formatTime: (Long) -> String,
    onLogMeal: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Today's Intake",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Calendar.getInstance().time),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRow("Calories", totals.calories, settings.goals.calories, "kcal")
                HorizontalDivider()
                MetricRow("Protein", totals.protein, settings.goals.protein, "g")
                MetricRow("Carbs", totals.carbs, settings.goals.carbs, "g")
                MetricRow("Fat", totals.fat, settings.goals.fat, "g")
            }
        }

        Button(onClick = onLogMeal, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Text("  Log a Meal")
        }

        OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
            Text("  Gallery")
        }

        if (totals.meals.isEmpty()) {
            EmptyState(
                title = "No meals yet today",
                subtitle = "Snap a photo of your plate to log calories and macros."
            )
        } else {
            SectionHeader("Logged today")
            totals.meals.forEach { m ->
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(m.dish, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatTime(m.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "${m.calories.toInt()} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: Double, goal: Double, unit: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${value.toInt()} / ${goal.toInt()} $unit",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
    val fraction = if (goal > 0) (value / goal).toFloat() else 0f
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth(),
    )
    if (value > goal) {
        Text(
            "Over by ${(value - goal).toInt()} $unit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
