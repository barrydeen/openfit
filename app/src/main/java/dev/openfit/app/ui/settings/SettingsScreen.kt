package dev.openfit.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import dev.openfit.app.data.macro.MacroGoals
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.domain.WeightUnit
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val container = appContainer()
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsRepository, container.backupManager) }
        }
    )

    val unit by vm.unit.collectAsState()
    val restSeconds by vm.restSeconds.collectAsState()
    val macroSettings by vm.macroSettings.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var baseUrl by remember { mutableStateOf(macroSettings.baseUrl) }
    var apiKey by remember { mutableStateOf(macroSettings.apiKey) }
    var model by remember { mutableStateOf(macroSettings.model) }
    var goalCal by remember { mutableStateOf(macroSettings.goals.calories.toInt().toString()) }
    var goalProtein by remember { mutableStateOf(macroSettings.goals.protein.toInt().toString()) }
    var goalCarbs by remember { mutableStateOf(macroSettings.goals.carbs.toInt().toString()) }
    var goalFat by remember { mutableStateOf(macroSettings.goals.fat.toInt().toString()) }

    LaunchedEffect(macroSettings) {
        baseUrl = macroSettings.baseUrl
        apiKey = macroSettings.apiKey
        model = macroSettings.model
        goalCal = macroSettings.goals.calories.toInt().toString()
        goalProtein = macroSettings.goals.protein.toInt().toString()
        goalCarbs = macroSettings.goals.carbs.toInt().toString()
        goalFat = macroSettings.goals.fat.toInt().toString()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.export(uri) { ok, message -> scope.launch { snackbar.showSnackbar(message) } }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.import(uri) { ok, message -> scope.launch { snackbar.showSnackbar(message) } }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    SectionHeader("Units")
                    Card {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WeightUnit.entries.forEach { u ->
                                FilterChip(
                                    selected = u == unit,
                                    onClick = { vm.setUnit(u) },
                                    label = { Text(u.label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    SectionHeader("Rest Timer")
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Default rest between sets",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = { vm.setRestSeconds((restSeconds - 15).coerceAtLeast(5)) }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease rest")
                                }
                                Text(
                                    text = "$restSeconds s",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                IconButton(onClick = { vm.setRestSeconds((restSeconds + 15).coerceAtMost(600)) }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase rest")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    SectionHeader("Macro Analysis & Goals")
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "AI vision endpoint",
                                style = MaterialTheme.typography.titleSmall
                            )
                            OutlinedTextField(
                                value = baseUrl, onValueChange = { baseUrl = it },
                                label = { Text("Base URL") },
                                placeholder = { Text("http://…/v1") },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = apiKey, onValueChange = { apiKey = it },
                                label = { Text("API Token") },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                            )
                            OutlinedTextField(
                                value = model, onValueChange = { model = it },
                                label = { Text("Model") },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Daily goals",
                                style = MaterialTheme.typography.titleSmall
                            )
                            GoalRow("Calories (kcal)", goalCal) { goalCal = it }
                            GoalRow("Protein (g)", goalProtein) { goalProtein = it }
                            GoalRow("Carbs (g)", goalCarbs) { goalCarbs = it }
                            GoalRow("Fat (g)", goalFat) { goalFat = it }
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    vm.saveMacroSettings(
                                        MacroSettings(
                                            baseUrl = baseUrl.trim(),
                                            apiKey = apiKey.trim(),
                                            model = model.trim(),
                                            goals = MacroGoals(
                                                goalCal.toDoubleOrNull() ?: 0.0,
                                                goalProtein.toDoubleOrNull() ?: 0.0,
                                                goalCarbs.toDoubleOrNull() ?: 0.0,
                                                goalFat.toDoubleOrNull() ?: 0.0,
                                            ),
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Save macro settings") }
                            Text(
                                text = "Photos are only sent to the endpoint you configure above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    SectionHeader("Data")
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Your data is stored only on this device. Export a backup to keep it safe or move it to another device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { exportLauncher.launch("openfit-backup-${System.currentTimeMillis()}.json") },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Export backup") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Import backup") }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "OpenFit v0.2.0 · MIT · Open source, no tracking, no ads.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalRow(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
