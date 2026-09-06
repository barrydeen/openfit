package dev.openfit.app.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.openfit.app.data.ExerciseRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.local.entity.Equipment
import dev.openfit.app.domain.ExerciseGroup
import dev.openfit.app.domain.PickerOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExercisePickerViewModel(
    private val workoutId: Long,
    private val exercises: ExerciseRepository,
    private val workouts: WorkoutRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
    }

    val mostUsed: StateFlow<List<dev.openfit.app.data.local.entity.ExerciseEntity>> =
        combine(exercises.observeMostUsed(), _query) { list, q ->
            PickerOrder.filterLongestFirst(q, list)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grouped: StateFlow<List<ExerciseGroup>> =
        combine(exercises.observeAll(), _query) { list, q ->
            PickerOrder.group(PickerOrder.filterLongestFirst(q, list))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutExerciseIds: StateFlow<Set<Long>> =
        workouts.observeWorkoutExercises(workoutId)
            .map { entries -> entries.map { it.exerciseId }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _added = MutableStateFlow(false)
    val added: StateFlow<Boolean> = _added.asStateFlow()

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            workouts.addExercise(workoutId, exerciseId)
            _added.value = true
        }
    }

    fun createAndAdd(name: String, muscleGroup: String, equipment: Equipment) {
        viewModelScope.launch {
            val id = exercises.createCustom(name, muscleGroup, equipment)
            workouts.addExercise(workoutId, id)
            _added.value = true
        }
    }

    fun consumeAdded() {
        _added.value = false
    }
}
