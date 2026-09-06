package dev.openfit.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.local.entity.WorkoutEntity
import dev.openfit.app.data.local.entity.WorkoutSummaryRow
import dev.openfit.app.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val workouts: WorkoutRepository,
    settings: SettingsRepository
) : ViewModel() {

    val activeWorkout: StateFlow<WorkoutEntity?> =
        workouts.observeActiveWorkout().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val history: StateFlow<List<WorkoutSummaryRow>> =
        workouts.observeHistorySummaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unit: StateFlow<WeightUnit> =
        settings.unit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUnit.KG)

    private val _launchedWorkoutId = MutableStateFlow<Long?>(null)
    val launchedWorkoutId: StateFlow<Long?> = _launchedWorkoutId.asStateFlow()

    fun startWorkout() {
        val defaultName = "Workout · " + java.text.SimpleDateFormat("MMM d").format(java.util.Date())
        viewModelScope.launch {
            _launchedWorkoutId.value = workouts.createWorkout(defaultName)
        }
    }

    fun consumeLaunch() {
        _launchedWorkoutId.value = null
    }
}
