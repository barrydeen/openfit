package dev.openfit.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import dev.openfit.app.data.local.entity.WorkoutExerciseWithRelation
import dev.openfit.app.data.local.entity.WorkoutSetEntity
import dev.openfit.app.domain.TimeFormatter
import dev.openfit.app.domain.WeightUnit
import dev.openfit.app.domain.UnitConverter
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.ConfirmDialog
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.rememberHaptics
import dev.openfit.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(navController: NavHostController, workoutId: Long) {
    val container = appContainer()
    val vm: WorkoutViewModel = viewModel(
        factory = viewModelFactory {
            initializer { WorkoutViewModel(workoutId, container.workoutRepository, container.settingsRepository) }
        }
    )

    val workout by vm.workout.collectAsState()
    val unit by vm.unit.collectAsState()
    val restRemaining by vm.restRemaining.collectAsState()
    val restRunning by vm.restRunning.collectAsState()
    val restTotal by vm.sessionRestSeconds.collectAsState()
    var showFinish by remember { mutableStateOf(false) }
    var removeExerciseEntry by remember { mutableStateOf<WorkoutExerciseWithRelation?>(null) }
    var deleteSetTarget by remember { mutableStateOf<WorkoutSetEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.workout?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showFinish = true }) { Text("Finish") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (restRunning) {
                RestTimerBar(
                    remaining = restRemaining,
                    total = restTotal.toInt(),
                    onSkip = { vm.skipRest() }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (workout == null) {
                    item { EmptyState("Loading", "Preparing your workout…") }
                } else if (workout!!.exercises.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No exercises yet",
                            subtitle = "Add an exercise to start logging your sets.",
                            icon = Icons.Filled.FitnessCenter
                        )
                    }
                    item {
                        AddExerciseButton { navController.navigate(Routes.pickExercise(workoutId)) }
                    }
                } else {
                    items(workout!!.exercises, key = { it.entry.id }) { entry ->
                        ExerciseCard(
                            entry = entry,
                            unit = unit,
                            onAddDraft = { weightKg, reps, warmup -> vm.addDraftSet(entry.entry.id, weightKg, reps, warmup) },
                            onComplete = { vm.completeSet(it) },
                            onUpdate = { id, wkg, reps -> vm.updateSet(id, wkg, reps) },
                            onToggleWarmup = { vm.toggleWarmup(it) },
                            onDeleteSet = { set ->
                                if (set.completedAt == null) vm.deleteSet(set.id)
                                else deleteSetTarget = set
                            },
                            onRemove = { removeExerciseEntry = entry }
                        )
                    }
                    item { AddExerciseButton { navController.navigate(Routes.pickExercise(workoutId)) } }
                }
            }
        }
    }

    if (showFinish) {
        AlertDialog(
            onDismissRequest = { showFinish = false },
            title = { Text("Finish workout?") },
            text = { Text("You're about to end this session. Make sure all your sets are logged.") },
            confirmButton = {
                Button(onClick = {
                    showFinish = false
                    vm.finishWorkout {
                        navController.navigate(Routes.summary(workoutId)) {
                            popUpTo(Routes.HOME)
                        }
                    }
                }) { Text("Finish") }
            },
            dismissButton = {
                TextButton(onClick = { showFinish = false }) { Text("Cancel") }
            }
        )
    }

    removeExerciseEntry?.let { entry ->
        ConfirmDialog(
            title = "Remove exercise?",
            text = "“${entry.exercise?.name ?: "This exercise"}” and its logged sets will be removed from this workout.",
            confirmLabel = "Remove",
            onConfirm = {
                vm.removeExercise(entry.entry.id)
                removeExerciseEntry = null
            },
            onDismiss = { removeExerciseEntry = null }
        )
    }

    deleteSetTarget?.let { set ->
        ConfirmDialog(
            title = "Delete set?",
            text = "Delete this completed set (${UnitConverter.displayWeight(set.weightKg, unit)} ${unit.label} × ${set.reps})?",
            onConfirm = {
                vm.deleteSet(set.id)
                deleteSetTarget = null
            },
            onDismiss = { deleteSetTarget = null }
        )
    }
}

@Composable
private fun RestTimerBar(remaining: Int, total: Int, onSkip: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val prevRemaining = remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(remaining) {
        if (remaining == 0 && prevRemaining.value > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevRemaining.value = remaining
    }

    val fraction = if (total > 0) remaining.toFloat() / total else 0f
    val nearEnd = remaining <= 5
    val accent = if (nearEnd) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Rest ${TimeFormatter.countdown(remaining)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Filled.TimerOff, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Skip")
                }
            }
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).height(4.dp),
                color = accent,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun AddExerciseButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Add Exercise", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ExerciseCard(
    entry: WorkoutExerciseWithRelation,
    unit: WeightUnit,
    onAddDraft: (Double, Int, Boolean) -> Unit,
    onComplete: (Long) -> Unit,
    onUpdate: (Long, Double, Int) -> Unit,
    onToggleWarmup: (Long) -> Unit,
    onDeleteSet: (WorkoutSetEntity) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.exercise?.name ?: "Exercise",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove exercise",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (entry.exercise != null) {
                Text(
                    text = "${entry.exercise.muscleGroup} · ${entry.exercise.equipment}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))

            val sets = entry.sets.sortedBy { it.position }
            sets.forEachIndexed { index, set ->
                if (set.completedAt == null) {
                    DraftSetRow(
                        set = set,
                        unit = unit,
                        onUpdate = onUpdate,
                        onComplete = onComplete,
                        onDelete = { onDeleteSet(set) }
                    )
                } else {
                    CompletedSetRow(
                        set = set,
                        unit = unit,
                        onToggleWarmup = onToggleWarmup,
                        onDelete = { onDeleteSet(set) }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            val last = sets.lastOrNull()
            AddSetRow(
                unit = unit,
                defaultWeightKg = last?.weightKg ?: 0.0,
                defaultReps = last?.reps ?: 0,
                onAdd = { weightKg, reps -> onAddDraft(weightKg, reps, false) }
            )
        }
    }
}

@Composable
private fun DraftSetRow(
    set: WorkoutSetEntity,
    unit: WeightUnit,
    onUpdate: (Long, Double, Int) -> Unit,
    onComplete: (Long) -> Unit,
    onDelete: () -> Unit
) {
    var weightText by remember(set.id) { mutableStateOf(UnitConverter.displayWeight(set.weightKg, unit)) }
    var repsText by remember(set.id) { mutableStateOf(set.reps.toString()) }
    val haptic = rememberHaptics()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${set.position + 1}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        NumberField(
            value = weightText,
            onValueChange = { weightText = it },
            placeholder = unit.label,
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f)
        )
        Text(
            "×",
            modifier = Modifier.padding(horizontal = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        NumberField(
            value = repsText,
            onValueChange = { repsText = it },
            placeholder = "reps",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = {
                val w = UnitConverter.parseWeight(weightText, unit) ?: set.weightKg
                val r = repsText.toIntOrNull() ?: set.reps
                onUpdate(set.id, w, r)
                onComplete(set.id)
                haptic()
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = "Log set", modifier = Modifier.size(24.dp))
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Discard set",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompletedSetRow(
    set: WorkoutSetEntity,
    unit: WeightUnit,
    onToggleWarmup: (Long) -> Unit,
    onDelete: () -> Unit
) {
    val warmup = set.isWarmup
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${set.position + 1}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "${UnitConverter.displayWeight(set.weightKg, unit)} ${unit.label} × ${set.reps}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (warmup) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Surface(
            onClick = { onToggleWarmup(set.id) },
            shape = CircleShape,
            color = if (warmup) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (warmup) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "W",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete set",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddSetRow(
    unit: WeightUnit,
    defaultWeightKg: Double,
    defaultReps: Int,
    onAdd: (Double, Int) -> Unit
) {
    var weightText by remember(defaultWeightKg, defaultReps) {
        mutableStateOf(UnitConverter.displayWeight(defaultWeightKg, unit))
    }
    var repsText by remember(defaultWeightKg, defaultReps) {
        mutableStateOf(if (defaultReps > 0) defaultReps.toString() else "")
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            NumberField(
                value = weightText,
                onValueChange = { weightText = it },
                placeholder = unit.label,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            Text(
                "×",
                modifier = Modifier.padding(horizontal = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NumberField(
                value = repsText,
                onValueChange = { repsText = it },
                placeholder = "reps",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = {
                val w = UnitConverter.parseWeight(weightText, unit) ?: defaultWeightKg
                val r = repsText.toIntOrNull() ?: continueWithReps(defaultReps)
                onAdd(w, r)
            }) { Text("Add") }
        }
    }
}

private fun continueWithReps(reps: Int): Int = if (reps > 0) reps else 10

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) }
    )
}
