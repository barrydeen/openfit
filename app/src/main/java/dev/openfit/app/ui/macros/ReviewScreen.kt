package dev.openfit.app.ui.macros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.llm.Draft
import dev.openfit.app.ui.components.rememberHaptics
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    imagePath: String?,
    draft: Draft?,
    isAnalyzing: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSave: (String, Double, Double, Double, Double) -> Unit,
    onUseRecent: (MealEntry) -> Unit,
    onCancel: () -> Unit,
    recent: List<MealEntry>,
    modifier: Modifier = Modifier,
) {
    var dish by remember { mutableStateOf(draft?.dish ?: "") }
    var cal by remember { mutableStateOf(draft?.calories?.roundToInt()?.toString() ?: "") }
    var protein by remember { mutableStateOf(draft?.protein?.roundToInt()?.toString() ?: "") }
    var carbs by remember { mutableStateOf(draft?.carbs?.roundToInt()?.toString() ?: "") }
    var fat by remember { mutableStateOf(draft?.fat?.roundToInt()?.toString() ?: "") }
    val haptic = rememberHaptics()

    LaunchedEffect(draft) {
        if (draft != null) {
            dish = draft.dish
            cal = draft.calories.roundToInt().toString()
            protein = draft.protein.roundToInt().toString()
            carbs = draft.carbs.roundToInt().toString()
            fat = draft.fat.roundToInt().toString()
        }
    }

    val quickPicks = remember(recent) {
        val seen = mutableSetOf<String>()
        recent.filter { it.dish.isNotBlank() && seen.add(it.dish.lowercase()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review meal") },
                actions = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (imagePath != null) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "Meal photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }

                Text("Confirm nutrients", style = MaterialTheme.typography.titleLarge)

                when {
                    isAnalyzing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(Modifier.height(24.dp))
                            Text("AI is analyzing the photo…")
                        }
                    }
                    error != null -> {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                    draft != null -> {
                        if (quickPicks.isNotEmpty()) {
                            Text("Recent meals", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                quickPicks.take(3).forEach { m ->
                                    SuggestionChip(
                                        onClick = { onUseRecent(m) },
                                        icon = {
                                            Icon(Icons.Filled.History, contentDescription = null)
                                        },
                                        label = { Text(m.dish) },
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = dish, onValueChange = { dish = it },
                            label = { Text("Dish") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                        MacroField("Calories (kcal)", cal) { cal = it }
                        MacroField("Protein (g)", protein) { protein = it }
                        MacroField("Carbs (g)", carbs) { carbs = it }
                        MacroField("Fat (g)", fat) { fat = it }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    enabled = draft != null && dish.isNotBlank(),
                    onClick = {
                        haptic()
                        onSave(
                            dish,
                            cal.toDoubleOrNull() ?: 0.0,
                            protein.toDoubleOrNull() ?: 0.0,
                            carbs.toDoubleOrNull() ?: 0.0,
                            fat.toDoubleOrNull() ?: 0.0,
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun MacroField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
