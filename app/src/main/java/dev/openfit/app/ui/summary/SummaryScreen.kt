package dev.openfit.app.ui.summary

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import dev.openfit.app.domain.OneRepMax
import dev.openfit.app.domain.Stats
import dev.openfit.app.domain.TimeFormatter
import dev.openfit.app.domain.UnitConverter
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.ConfirmDialog
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(navController: NavHostController, workoutId: Long) {
    val container = appContainer()
    val vm: SummaryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SummaryViewModel(
                    workoutId,
                    container.workoutRepository,
                    container.settingsRepository,
                    container.postWorkoutCoach,
                )
            }
        }
    )

    val workout by vm.workout.collectAsState()
    val unit by vm.unit.collectAsState()
    val coachFeedback by vm.coachFeedback.collectAsState()
    val coachLoading by vm.coachLoading.collectAsState()
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val w = workout
        if (w == null) {
            Column(Modifier.padding(padding)) { Text("Loading…") }
            return@Scaffold
        }

        val volume = Stats.volumeOf(w)
        val e1rm = Stats.e1rmOf(w)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(w.workout.name, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        TimeFormatter.dateTime(w.workout.startedAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Duration",
                        value = TimeFormatter.duration(w.workout.startedAt, w.workout.endedAt),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Volume",
                        value = "${UnitConverter.displayWeight(volume.totalKg, unit)} ${unit.label}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "e1RM",
                        value = "${UnitConverter.displayWeight(e1rm, unit)} ${unit.label}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (coachLoading || coachFeedback != null) {
                item {
                    CoachFeedbackCard(loading = coachLoading, text = coachFeedback)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Exercises",
                        value = w.exercises.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Sets",
                        value = volume.sets.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Reps",
                        value = volume.reps.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (w.exercises.isNotEmpty()) {
                item { SectionHeader("Exercises") }
                items(w.exercises, key = { it.entry.id }) { entry ->
                    ExerciseSummaryCard(entrySets = entry.sets, name = entry.exercise?.name ?: "Exercise", unit = unit)
                }
            }

            item {
                OutlinedButton(
                    onClick = { showDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Delete workout", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete workout?",
            text = "“${workout?.workout?.name ?: "This workout"}” will be permanently removed, including all logged sets.",
            onConfirm = {
                showDelete = false
                vm.deleteWorkout {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExerciseSummaryCard(
    entrySets: List<dev.openfit.app.data.local.entity.WorkoutSetEntity>,
    name: String,
    unit: dev.openfit.app.domain.WeightUnit
) {
    val working = entrySets.filter { !it.isWarmup }
    val volume = Stats.volumeOf(working)
    val setsText = working.joinToString { "${it.reps}@${UnitConverter.displayWeight(it.weightKg, unit)}" }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${UnitConverter.displayWeight(volume.totalKg, unit)} ${unit.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            if (working.any()) {
                Text(
                    text = setsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CoachFeedbackCard(loading: Boolean, text: String?) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Filled.TipsAndUpdates,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(12.dp))
            if (loading) {
                Column {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Asking your coach for some tips…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else {
                Text(
                    text = text ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
