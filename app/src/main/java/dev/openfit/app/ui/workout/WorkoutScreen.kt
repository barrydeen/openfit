package dev.openfit.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import dev.openfit.app.ui.components.ScreenIntro
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
                title = { Text("Your session", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showFinish = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Finish") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize().imePadding()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (restRunning) {
                    item(key = "rest-timer") {
                        RestTimerBar(
                            remaining = restRemaining,
                            total = restTotal.toInt(),
                            onSkip = { vm.skipRest() }
                        )
                    }
                }
                workout?.let { session ->
                    item {
                        ScreenIntro(
                            eyebrow = "IN PROGRESS",
                            title = session.workout.name,
                            subtitle = "${session.exercises.size} exercises · ${session.exercises.sumOf { entry -> entry.sets.count { it.completedAt != null } }} sets logged"
                        )
                    }
                }
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
                            onAddDraft = { weightKg, reps, warmup -> vm.addSet(entry.entry.id, weightKg, reps, warmup) },
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
            shape = RoundedCornerShape(20.dp),
            title = { Text("Finish workout?", style = MaterialTheme.typography.headlineMedium) },
            text = { Text("You're about to end this session. Make sure all your sets are logged.") },
            confirmButton = {
                Button(modifier = Modifier.heightIn(min = 48.dp), onClick = {
                    showFinish = false
                    vm.finishWorkout {
                        navController.navigate(Routes.summary(workoutId)) {
                            popUpTo(Routes.HOME)
                        }
                    }
                }) { Text("Finish") }
            },
            dismissButton = {
                TextButton(onClick = { showFinish = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
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
    val accent = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (nearEnd) "GET READY" else "REST & RECOVER",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Text(
                        text = TimeFormatter.countdown(remaining),
                        style = MaterialTheme.typography.headlineMedium,
                        color = accent
                    )
                }
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.heightIn(min = 48.dp),
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
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(4.dp),
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
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(20.dp)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.exercise?.name ?: "Exercise",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
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
            if (sets.isNotEmpty()) {
                Text(
                    "${sets.count { it.completedAt != null }} OF ${sets.size} SETS LOGGED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }
            sets.forEach { set ->
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
    var weightText by remember(set.id, unit) { mutableStateOf(UnitConverter.displayWeight(set.weightKg, unit)) }
    var repsText by remember(set.id) { mutableStateOf(set.reps.toString()) }
    val haptic = rememberHaptics()

    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Set ${set.position + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val w = UnitConverter.parseWeight(weightText, unit) ?: set.weightKg
                        val r = repsText.toIntOrNull() ?: set.reps
                        onUpdate(set.id, w, r)
                        onComplete(set.id)
                        haptic()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Log set")
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Discard set", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    placeholder = unit.label,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    placeholder = "reps",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = "SET ${set.position + 1} · ${if (warmup) "WARM-UP" else "LOGGED"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${UnitConverter.displayWeight(set.weightKg, unit)} ${unit.label} × ${set.reps} reps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            onClick = { onToggleWarmup(set.id) },
            shape = CircleShape,
            color = if (warmup) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (warmup) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp).semantics {
                contentDescription = "Toggle warm-up for set ${set.position + 1}"
                stateDescription = if (warmup) "Warm-up set" else "Working set"
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "W",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
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
    var weightText by remember(defaultWeightKg, defaultReps, unit) {
        mutableStateOf(UnitConverter.displayWeight(defaultWeightKg, unit))
    }
    var repsText by remember(defaultWeightKg, defaultReps) {
        mutableStateOf(if (defaultReps > 0) defaultReps.toString() else "")
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Next set", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                FilledTonalButton(
                    onClick = {
                        val w = UnitConverter.parseWeight(weightText, unit) ?: defaultWeightKg
                        val r = repsText.toIntOrNull() ?: continueWithReps(defaultReps)
                        onAdd(w, r)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add set")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    placeholder = unit.label,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                NumberField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    placeholder = "reps",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
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
        modifier = modifier.heightIn(min = 56.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
        shape = RoundedCornerShape(12.dp),
        label = { Text(if (keyboardType == KeyboardType.Decimal) "Weight ($placeholder)" else "Reps") }
    )
}
