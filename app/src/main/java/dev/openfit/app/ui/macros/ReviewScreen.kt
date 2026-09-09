package dev.openfit.app.ui.macros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.llm.Draft
import dev.openfit.app.ui.components.ScreenIntro
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.components.rememberHaptics
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var dish by rememberSaveable { mutableStateOf(draft?.dish ?: "") }
    var cal by rememberSaveable { mutableStateOf(draft?.calories?.roundToInt()?.toString() ?: "") }
    var protein by rememberSaveable { mutableStateOf(draft?.protein?.roundToInt()?.toString() ?: "") }
    var carbs by rememberSaveable { mutableStateOf(draft?.carbs?.roundToInt()?.toString() ?: "") }
    var fat by rememberSaveable { mutableStateOf(draft?.fat?.roundToInt()?.toString() ?: "") }
    var dishEdited by rememberSaveable { mutableStateOf(false) }
    var calEdited by rememberSaveable { mutableStateOf(false) }
    var proteinEdited by rememberSaveable { mutableStateOf(false) }
    var carbsEdited by rememberSaveable { mutableStateOf(false) }
    var fatEdited by rememberSaveable { mutableStateOf(false) }
    val haptic = rememberHaptics()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(draft) {
        // A late estimate fills only fields the user has not touched yet.
        if (draft != null) {
            if (!dishEdited) dish = draft.dish
            if (!calEdited) cal = draft.calories.roundToInt().toString()
            if (!proteinEdited) protein = draft.protein.roundToInt().toString()
            if (!carbsEdited) carbs = draft.carbs.roundToInt().toString()
            if (!fatEdited) fat = draft.fat.roundToInt().toString()
        }
    }
    fun markAllEdited() {
        dishEdited = true
        calEdited = true
        proteinEdited = true
        carbsEdited = true
        fatEdited = true
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
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ScreenIntro(
                    eyebrow = "MAKE IT YOURS",
                    title = "What's on the table?",
                    subtitle = "Review an estimate or enter your own details. You're in control of what gets logged.",
                )
                if (imagePath != null) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "Meal photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                } else if (!isAnalyzing) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Photo unavailable", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "This restored entry no longer has its photo. Saving will log it without a picture.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (isAnalyzing || error != null || draft != null) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (error != null && !isAnalyzing) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            when {
                                isAnalyzing -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                        Text("Estimating your meal", style = MaterialTheme.typography.titleSmall)
                                    }
                                    Text("You can start entering details while you wait. Your edits will be kept.", style = MaterialTheme.typography.bodySmall)
                                }
                                error != null -> {
                                    Text("Continue with your own details", style = MaterialTheme.typography.titleSmall)
                                    Text(error, style = MaterialTheme.typography.bodySmall)
                                    if (imagePath != null) {
                                        TextButton(onClick = onRetry) { Text("Retry photo estimate") }
                                    }
                                }
                                else -> {
                                    Text("A starting point, not a verdict", style = MaterialTheme.typography.titleSmall)
                                    Text("Check the portions and nutrients below before saving.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (quickPicks.isNotEmpty()) {
                    Column {
                        SectionHeader("Have this often?")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            quickPicks.take(3).forEach { m ->
                                SuggestionChip(
                                    onClick = {
                                        markAllEdited()
                                        dish = m.dish
                                        cal = m.calories.roundToInt().toString()
                                        protein = m.protein.roundToInt().toString()
                                        carbs = m.carbs.roundToInt().toString()
                                        fat = m.fat.roundToInt().toString()
                                        onUseRecent(m)
                                    },
                                    icon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    label = { Text(m.dish) },
                                )
                            }
                        }
                    }
                }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("The meal", style = MaterialTheme.typography.headlineMedium)
                        OutlinedTextField(
                            value = dish,
                            onValueChange = {
                                dish = it
                                dishEdited = true
                            },
                            label = { Text("Dish name") },
                            placeholder = { Text("e.g. Roast vegetable bowl") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                        )
                    }
                }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("The nourishment", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Values for the whole meal. Leave unknown values blank to log them as zero.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        MacroField("Calories (kcal)", cal) {
                            cal = it
                            calEdited = true
                        }
                        MacroField("Protein (g)", protein) {
                            protein = it
                            proteinEdited = true
                        }
                        MacroField("Carbs (g)", carbs) {
                            carbs = it
                            carbsEdited = true
                        }
                        MacroField("Fat (g)", fat, imeAction = ImeAction.Done) {
                            fat = it
                            fatEdited = true
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                ) { Text("Cancel") }
                Button(
                    enabled = !isAnalyzing && dish.isNotBlank(),
                    onClick = {
                        focusManager.clearFocus()
                        haptic()
                        onSave(
                            dish,
                            cal.toDoubleOrNull() ?: 0.0,
                            protein.toDoubleOrNull() ?: 0.0,
                            carbs.toDoubleOrNull() ?: 0.0,
                            fat.toDoubleOrNull() ?: 0.0,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
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
private fun MacroField(label: String, value: String, imeAction: ImeAction = ImeAction.Next, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus() },
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}
