package dev.openfit.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.openfit.app.domain.TimeFormatter
import dev.openfit.app.domain.UnitConverter
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.components.ScreenIntro
import dev.openfit.app.ui.components.TintedIconCircle
import dev.openfit.app.ui.navigation.Routes
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(container.workoutRepository, container.settingsRepository) }
        }
    )

    val activeWorkout by vm.activeWorkout.collectAsState()
    val history by vm.history.collectAsState()
    val unit by vm.unit.collectAsState()
    val launchedId by vm.launchedWorkoutId.collectAsState()
    var showNewWorkout by remember { mutableStateOf(false) }

    LaunchedEffect(launchedId) {
        if (launchedId != null) {
            navController.navigate(Routes.workout(launchedId!!))
            vm.consumeLaunch()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TintedIconCircle(Icons.Filled.FitnessCenter,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary)
                        Text("OpenFit", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ScreenIntro(
                        eyebrow = "YOUR DAILY PRACTICE",
                        title = "A little stronger,\nevery day.",
                        subtitle = "Make room for movement. Build something that lasts."
                    )
                    if (activeWorkout != null) {
                        ActiveWorkoutCard(
                            name = activeWorkout!!.name,
                            startedAt = activeWorkout!!.startedAt,
                            onClick = { navController.navigate(Routes.workout(activeWorkout!!.id)) }
                        )
                        OutlinedButton(
                            onClick = { showNewWorkout = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("New Workout")
                        }
                    } else {
                        StartWorkoutCard(onClick = { showNewWorkout = true })
                    }
                }
            }

            item {
                CoachCard(onClick = { navController.navigate(Routes.COACH) })
            }

            item { SectionHeader("Recent Sessions") }

            if (history.isEmpty()) {
                item {
                    EmptyState(
                        title = "No sessions yet",
                        subtitle = "Create a workout and log your first lifts to see them here.",
                        icon = Icons.Filled.FitnessCenter
                    )
                }
            } else {
                items(history, key = { it.workoutId }) { row ->
                    SessionCard(
                        name = row.name,
                        dateText = TimeFormatter.date(row.startedAt),
                        volumeText = "${UnitConverter.displayWeight(row.totalKg, unit)} ${unit.label}",
                        exerciseCount = row.exerciseCount,
                        setCount = row.setCount,
                        durationText = TimeFormatter.duration(row.startedAt, row.endedAt),
                        onClick = { navController.navigate(Routes.summary(row.workoutId)) }
                    )
                }
            }
        }
    }

    if (showNewWorkout) {
        ShowNewWorkoutConfirm(
            onConfirm = {
                vm.startWorkout()
                showNewWorkout = false
            },
            onDismiss = { showNewWorkout = false }
        )
    }
}

@Composable
private fun StartWorkoutCard(onClick: () -> Unit) {
    val ornamentColor = MaterialTheme.colorScheme.onPrimary
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                // Cropped concentric rings echo the plates on a barbell.
                val center = Offset(size.width * 1.04f, size.height * 0.18f)
                repeat(4) { index ->
                    drawCircle(ornamentColor.copy(alpha = 0.08f),
                        radius = (52 + index * 26).dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                }
            }
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("THE NEXT REP STARTS HERE", style = MaterialTheme.typography.labelSmall)
                Text("Your time.\nYour pace.", style = MaterialTheme.typography.headlineLarge)
                Text("Show up for yourself. We'll keep track.",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start workout")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ShowNewWorkoutConfirm(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Workout") },
        text = { Text("Start a new workout session now?") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CoachCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TintedIconCircle(
                icon = Icons.Filled.SupportAgent,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Ask your coach",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "A fresh perspective on your training and nutrition.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ActiveWorkoutCard(name: String, startedAt: Long, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "IN PROGRESS / PICK UP WHERE YOU LEFT OFF",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Started ${TimeFormatter.dateTime(startedAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SessionCard(
    name: String,
    dateText: String,
    volumeText: String,
    exerciseCount: Int,
    setCount: Int,
    durationText: String,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TintedIconCircle(icon = Icons.Filled.FitnessCenter)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$exerciseCount exercises · $setCount sets · $durationText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "$volumeText total volume",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}
