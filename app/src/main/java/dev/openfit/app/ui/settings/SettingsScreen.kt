package dev.openfit.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import dev.openfit.app.data.macro.MacroGoals
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.domain.WeightUnit
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.ConfirmDialog
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.components.TintedIconCircle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val container = appContainer()
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.settingsRepository,
                    container.backupManager,
                    container.dailyCoachScheduler,
                )
            }
        }
    )

    val unit by vm.unit.collectAsState()
    val restSeconds by vm.restSeconds.collectAsState()
    val macroSettings by vm.macroSettings.collectAsState()
    val dynamicColor by vm.dynamicColor.collectAsState()
    val coachEnabled by vm.coachEnabled.collectAsState()
    val coachTimeMinutes by vm.coachTimeMinutes.collectAsState()
    var showCoachTimePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var baseUrl by remember { mutableStateOf(macroSettings.baseUrl) }
    var apiKey by remember { mutableStateOf(macroSettings.apiKey) }
    var model by remember { mutableStateOf(macroSettings.model) }
    var goalCal by remember { mutableStateOf(macroSettings.goals.calories.toInt().toString()) }
    var goalProtein by remember { mutableStateOf(macroSettings.goals.protein.toInt().toString()) }
    var goalCarbs by remember { mutableStateOf(macroSettings.goals.carbs.toInt().toString()) }
    var goalFat by remember { mutableStateOf(macroSettings.goals.fat.toInt().toString()) }
    var showApiKey by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }

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

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.setCoachEnabled(true)
        } else {
            scope.launch { snackbar.showSnackbar("Allow notifications to receive daily coach messages") }
        }
    }

    fun enableCoach() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.setCoachEnabled(true)
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
                    SectionHeader("Appearance")
                    Card {
                        ListItem(
                            headlineContent = { Text("Dynamic color") },
                            supportingContent = {
                                Text(
                                    "Use Material You colors from your wallpaper (Android 12+)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                TintedIconCircle(icon = Icons.Filled.Palette)
                            },
                            trailingContent = {
                                Switch(
                                    checked = dynamicColor,
                                    onCheckedChange = vm::setDynamicColor
                                )
                            }
                        )
                    }
                }
            }

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Default rest between sets",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$restSeconds s",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Slider(
                                value = restSeconds.toFloat(),
                                onValueChange = { vm.setRestSeconds(it.toLong()) },
                                valueRange = 15f..600f,
                                steps = 39
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("15 s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("10 min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    SectionHeader("Daily Coach")
                    Card {
                        Column {
                            ListItem(
                                headlineContent = { Text("Daily coach notification") },
                                supportingContent = {
                                    Text(
                                        "One message a day from your AI coach, based on your last 7 days of " +
                                            "meals and workouts (rest day, get back to the gym, back on track). " +
                                            "Scheduled on-device with WorkManager - no Google services.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                leadingContent = {
                                    TintedIconCircle(icon = Icons.Filled.SelfImprovement)
                                },
                                trailingContent = {
                                    Switch(
                                        checked = coachEnabled,
                                        onCheckedChange = { checked ->
                                            if (checked) enableCoach() else vm.setCoachEnabled(false)
                                        }
                                    )
                                }
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = coachEnabled) { showCoachTimePicker = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Delivery time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatCoachTime(coachTimeMinutes),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (coachEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    vm.sendTestCoachNotification()
                                    scope.launch { snackbar.showSnackbar("Coach notification queued") }
                                },
                                enabled = coachEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp)
                            ) { Text("Send test notification") }
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
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showApiKey) "Hide token" else "Show token"
                                        )
                                    }
                                },
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
                            Button(
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
                                    scope.launch { snackbar.showSnackbar("Macro settings saved") }
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
                                onClick = { confirmImport = true },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Import backup") }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "OpenFit v0.3.0 · MIT · Open source, no tracking, no ads.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }

    if (confirmImport) {
        ConfirmDialog(
            title = "Import backup?",
            text = "Importing replaces all workouts and meals currently on this device.",
            confirmLabel = "Import",
            onConfirm = {
                confirmImport = false
                importLauncher.launch(arrayOf("application/json", "text/*"))
            },
            onDismiss = { confirmImport = false }
        )
    }

    if (showCoachTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = coachTimeMinutes / 60,
            initialMinute = coachTimeMinutes % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showCoachTimePicker = false },
            title = { Text("Delivery time") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCoachTimePicker = false
                        vm.setCoachTime(timeState.hour * 60 + timeState.minute)
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCoachTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimePicker(state = timeState)
                }
            },
        )
    }
}

private fun formatCoachTime(minutes: Int): String =
    "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"

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
