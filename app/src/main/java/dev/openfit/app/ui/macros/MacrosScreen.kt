package dev.openfit.app.ui.macros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.ui.components.ConfirmDialog
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.ScreenIntro
import dev.openfit.app.ui.components.SectionHeader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenIntro(
                eyebrow = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Calendar.getInstance().time),
                title = "A little more balance.",
                subtitle = "Your daily nourishment, one meal at a time.",
            )

            IntakeCard(totals = totals, settings = settings)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLogMeal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log a meal", style = MaterialTheme.typography.titleMedium)
                }

                OutlinedButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("View meal gallery")
                }
            }

            SectionHeader("Today's meals")
            if (totals.meals.isEmpty()) {
                EmptyState(
                    title = "A fresh page for today",
                    subtitle = "Start with a photo, then review the nutrients. AI estimates are optional.",
                    icon = Icons.Filled.Restaurant,
                )
            } else {
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "DAILY ENERGY",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (goal > 0) "${abs(remaining.toInt())}" else "${totals.calories.toInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    when {
                        goal <= 0 -> "kcal logged"
                        over -> "kcal above your daily goal"
                        else -> "kcal left in your day"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                LinearProgressIndicator(
                    progress = { if (goal > 0) (totals.calories / goal).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                )
                Text(
                    if (goal > 0) "${totals.calories.toInt()} eaten / ${goal.toInt()} kcal goal"
                    else "Set a daily goal in Settings to see your balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("The nutrient picture", style = MaterialTheme.typography.headlineMedium)
                MetricRow("Protein", totals.protein, settings.goals.protein, "g", MaterialTheme.colorScheme.primary)
                MetricRow("Carbs", totals.carbs, settings.goals.carbs, "g", MaterialTheme.colorScheme.secondary)
                MetricRow("Fat", totals.fat, settings.goals.fat, "g", MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: Double, goal: Double, unit: String, color: Color) {
    val over = goal > 0 && value > goal
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (goal > 0) "${value.toInt()} / ${goal.toInt()} $unit" else "${value.toInt()} $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
        val fraction = if (goal > 0) (value / goal).toFloat() else 0f
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(20.dp)),
            color = if (over) MaterialTheme.colorScheme.error else color,
            trackColor = if (over) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            } else {
                color.copy(alpha = 0.12f)
            }
        )
    }
}

@Composable
private fun MealRow(meal: MealEntry, timeText: String, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 4.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(52.dp),
            ) {
                if (meal.imagePath.isNotBlank() && File(meal.imagePath).exists()) {
                    AsyncImage(
                        model = File(meal.imagePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Restaurant, contentDescription = null)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(timeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(meal.dish, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${meal.calories.toInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "P ${meal.protein.toInt()}g  /  C ${meal.carbs.toInt()}g  /  F ${meal.fat.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${meal.dish}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
