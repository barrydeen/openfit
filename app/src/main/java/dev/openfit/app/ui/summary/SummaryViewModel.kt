package dev.openfit.app.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.openfit.app.coach.PostWorkoutCoach
import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.local.entity.WorkoutWithExercises
import dev.openfit.app.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SummaryViewModel(
    private val workoutId: Long,
    private val workouts: WorkoutRepository,
    settings: SettingsRepository,
    private val coach: PostWorkoutCoach?,
) : ViewModel() {

    val workout: StateFlow<WorkoutWithExercises?> =
        workouts.observeWorkout(workoutId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val unit: StateFlow<WeightUnit> =
        settings.unit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUnit.KG)

    private val _coachFeedback = MutableStateFlow<String?>(null)
    val coachFeedback: StateFlow<String?> = _coachFeedback.asStateFlow()

    private val _coachLoading = MutableStateFlow(false)
    val coachLoading: StateFlow<Boolean> = _coachLoading.asStateFlow()

    private var coachRequested = false

    init {
        loadCoachFeedback()
    }

    fun loadCoachFeedback() {
        if (coachRequested || coach == null) return
        coachRequested = true
        viewModelScope.launch {
            _coachLoading.value = true
            _coachFeedback.value = coach.feedbackFor(workoutId)
            _coachLoading.value = false
        }
    }

    fun deleteWorkout(onDeleted: () -> Unit) {
        viewModelScope.launch {
            workouts.deleteWorkout(workoutId)
            onDeleted()
        }
    }
}
