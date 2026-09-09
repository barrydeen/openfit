package dev.openfit.app.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import dev.openfit.app.domain.Stats
import dev.openfit.app.domain.TimeFormatter
import dev.openfit.app.domain.UnitConverter
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.ConfirmDialog
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.components.ScreenIntro
import dev.openfit.app.ui.components.StatCard
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
                title = { Text("Session summary", style = MaterialTheme.typography.titleMedium) },
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Preparing your summary", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return@Scaffold
        }

        val volume = Stats.volumeOf(w)
        val e1rm = Stats.e1rmOf(w)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    ScreenIntro(
                        eyebrow = "YOUR SESSION, IN REVIEW",
                        title = w.workout.name,
                        subtitle = TimeFormatter.dateTime(w.workout.startedAt),
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TOTAL VOLUME", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "${UnitConverter.displayWeight(volume.totalKg, unit)} ${unit.label}",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text("Every working rep adds up.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            label = "Duration",
                            value = TimeFormatter.duration(w.workout.startedAt, w.workout.endedAt),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Best estimated 1RM",
                            value = "${UnitConverter.displayWeight(e1rm, unit)} ${unit.label}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Working sets", volume.sets.toString(), Modifier.weight(1f))
                        StatCard("Total reps", volume.reps.toString(), Modifier.weight(1f))
                    }
                    Text(
                        "${w.exercises.size} exercises · Warm-up sets excluded from volume, sets, reps and estimated 1RM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (coachLoading || coachFeedback != null) {
                item {
                    CoachFeedbackCard(loading = coachLoading, text = coachFeedback)
                }
            }

            if (w.exercises.isNotEmpty()) {
                item { SectionHeader("Exercise breakdown", Modifier.padding(top = 8.dp)) }
                items(w.exercises, key = { it.entry.id }) { entry ->
                    ExerciseSummaryCard(entrySets = entry.sets, name = entry.exercise?.name ?: "Exercise", unit = unit)
                }
            }

            item {
                OutlinedButton(
                    onClick = { showDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(20.dp)
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
private fun ExerciseSummaryCard(
    entrySets: List<dev.openfit.app.data.local.entity.WorkoutSetEntity>,
    name: String,
    unit: dev.openfit.app.domain.WeightUnit
) {
    val working = entrySets.filter { !it.isWarmup }.sortedBy { it.position }
    val volume = Stats.volumeOf(working)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "${UnitConverter.displayWeight(volume.totalKg, unit)} ${unit.label} volume · ${working.size} working sets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            if (working.isNotEmpty()) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "SET",
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "WEIGHT (${unit.label})",
                        modifier = Modifier.weight(1.3f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                    Text(
                        "REPS",
                        modifier = Modifier.weight(0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
                working.forEach { set ->
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${set.position + 1}",
                            modifier = Modifier.weight(0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            UnitConverter.displayWeight(set.weightKg, unit),
                            modifier = Modifier.weight(1.3f),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.End
                        )
                        Text(
                            "${set.reps}",
                            modifier = Modifier.weight(0.9f),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.End
                        )
                    }
                }
            } else {
                Text(
                    "No working sets recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val warmups = entrySets.count { it.isWarmup }
            if (warmups > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "$warmups warm-up sets not shown",
                    style = MaterialTheme.typography.bodySmall,
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
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.TipsAndUpdates,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Text("Coach's notes", style = MaterialTheme.typography.titleMedium)
            }
            if (loading) {
                Column {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
