package dev.openfit.app.ui.macros

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.ui.components.ConfirmDialog
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.ScreenIntro
import dev.openfit.app.ui.components.StatCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    meals: List<MealEntry>,
    onBack: () -> Unit,
    onDeleteMeal: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val withImages = meals.filter { it.imagePath.isNotBlank() && File(it.imagePath).exists() }
    var selected by remember { mutableStateOf<MealEntry?>(null) }
    var confirmDelete by remember { mutableStateOf<MealEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(148.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScreenIntro(
                    eyebrow = "THE MEAL JOURNAL",
                    title = "Good things, remembered.",
                    subtitle = if (withImages.isEmpty()) "A place for the meals that make up your days."
                    else "${withImages.size} ${if (withImages.size == 1) "photo" else "photos"} saved. Tap a meal to revisit the details.",
                )
            }
            if (withImages.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            EmptyState(
                                title = "Your table, in pictures",
                                subtitle = "Take a photo when you log a meal and it will appear here. Meals without photos still count toward your daily intake.",
                                icon = Icons.Filled.PhotoLibrary,
                            )
                            OutlinedButton(onClick = onBack) { Text("Back to nutrition") }
                        }
                    }
                }
            } else {
                items(withImages, key = { it.id }) { m ->
                    Card(
                        onClick = { selected = m },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Box {
                            AsyncImage(
                                model = File(m.imagePath),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 5f),
                            )
                            Text(
                                "${m.calories.toInt()} kcal",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(m.dish, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(m.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    selected?.let { meal ->
        AlertDialog(
            onDismissRequest = { selected = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text(meal.dish, style = MaterialTheme.typography.headlineMedium) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AsyncImage(
                        model = File(meal.imagePath),
                        contentDescription = "Photo of ${meal.dish}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    )
                    Text(
                        mealDateTime(meal.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${meal.calories.toInt()} kcal",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    StatCard("Protein", "${meal.protein.toInt()} g", Modifier.fillMaxWidth())
                    StatCard("Carbs", "${meal.carbs.toInt()} g", Modifier.fillMaxWidth())
                    StatCard("Fat", "${meal.fat.toInt()} g", Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selected = null
                    confirmDelete = meal
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("Close") }
            }
        )
    }

    confirmDelete?.let { meal ->
        ConfirmDialog(
            title = "Delete meal?",
            text = "“${meal.dish}” (${meal.calories.toInt()} kcal) will be permanently removed.",
            onConfirm = {
                onDeleteMeal(meal.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null }
        )
    }
}

private fun mealDateTime(ts: Long): String =
    SimpleDateFormat("EEE, MMM d yyyy · HH:mm", Locale.getDefault()).format(Date(ts))
