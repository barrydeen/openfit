package dev.openfit.app.ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import dev.openfit.app.data.local.entity.Equipment
import dev.openfit.app.ui.appContainer
import dev.openfit.app.ui.components.SectionHeader
import dev.openfit.app.ui.components.EmptyState
import dev.openfit.app.ui.components.ScreenIntro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(navController: NavHostController, workoutId: Long) {
    val container = appContainer()
    val vm: ExercisePickerViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ExercisePickerViewModel(workoutId, container.exerciseRepository, container.workoutRepository)
            }
        }
    )

    val query by vm.query.collectAsState()
    val mostUsed by vm.mostUsed.collectAsState()
    val grouped by vm.grouped.collectAsState()
    val added by vm.added.collectAsState()
    val inWorkout by vm.workoutExerciseIds.collectAsState()
    var showCustom by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { searchFocus.requestFocus() }

    LaunchedEffect(added) {
        if (added) {
            navController.popBackStack()
            vm.consumeAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise library", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showCustom = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Custom")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize().imePadding()) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).focusRequester(searchFocus),
                label = { Text("Search exercises") },
                placeholder = { Text("Find your next movement") },
                shape = RoundedCornerShape(20.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ScreenIntro(
                        eyebrow = if (query.isBlank()) "BUILD YOUR SESSION" else "SEARCH RESULTS",
                        title = if (query.isBlank()) "Find your movement." else "Make it your own.",
                        subtitle = if (query.isBlank()) "Choose an exercise or create one of your own."
                        else "${grouped.sumOf { it.exercises.size }} exercises found"
                    )
                }
                if (query.isBlank() && mostUsed.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            SectionHeader("Most used")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(mostUsed) { exercise ->
                                    AssistChip(
                                        onClick = { vm.addExercise(exercise.id) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        leadingIcon = {
                                            Icon(
                                                if (exercise.id in inWorkout) Icons.Filled.Check else Icons.Filled.History,
                                                contentDescription = if (exercise.id in inWorkout) "In this workout" else null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        label = { Text(exercise.name) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (grouped.isEmpty() && query.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                                EmptyState(
                                    title = "No matching exercises",
                                    subtitle = "Try a different search, or add a custom exercise.",
                                    icon = Icons.Filled.Search
                                )
                                Button(
                                    onClick = { showCustom = true },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                ) { Text("Create an exercise") }
                            }
                        }
                    }
                }

                grouped.forEach { group ->
                    item {
                        SectionHeader("${group.muscleGroup} · ${group.exercises.size}", Modifier.padding(top = 16.dp))
                    }
                    items(group.exercises, key = { it.id }) { exercise ->
                        val alreadyInWorkout = exercise.id in inWorkout
                        Surface(
                            onClick = { vm.addExercise(exercise.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = if (alreadyInWorkout) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = if (alreadyInWorkout) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(
                                modifier = Modifier.heightIn(min = 80.dp).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        exercise.equipment.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (alreadyInWorkout) {
                                        Text("In this workout", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Icon(
                                    if (alreadyInWorkout) Icons.Filled.Check else Icons.Filled.Add,
                                    contentDescription = "Add exercise"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustom) {
        CustomExerciseDialog(
            onConfirm = { name, group, equipment -> vm.createAndAdd(name, group, equipment) },
            onDismiss = { showCustom = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomExerciseDialog(
    onConfirm: (String, String, Equipment) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("Other") }
    var equipment by remember { mutableStateOf(Equipment.DUMBBELL) }
    var groupExpanded by remember { mutableStateOf(false) }

    val groups = listOf(
        "Chest", "Back", "Shoulders", "Quads", "Hamstrings", "Glutes",
        "Calves", "Biceps", "Triceps", "Forearms", "Core", "Full Body", "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("A movement of your own", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Add it to your library and this session.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = groupExpanded,
                    onExpandedChange = { groupExpanded = it }
                ) {
                    OutlinedTextField(
                        value = group,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Muscle group") },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = groupExpanded,
                        onDismissRequest = { groupExpanded = false }
                    ) {
                        groups.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    group = g
                                    groupExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Equipment", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                EquipmentSelector(
                    selected = equipment,
                    onSelect = { equipment = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp),
                onClick = { onConfirm(name, group, equipment) }
            ) { Text("Create & add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentSelector(selected: Equipment, onSelect: (Equipment) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Equipment.entries.forEach { eq ->
            FilterChip(
                selected = eq == selected,
                onClick = { onSelect(eq) },
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text(eq.name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }) }
            )
        }
    }
}
