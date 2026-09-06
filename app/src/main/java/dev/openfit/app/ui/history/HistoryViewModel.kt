package dev.openfit.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.local.entity.WorkoutSummaryRow
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.data.macro.MealRepository
import dev.openfit.app.domain.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val workouts: WorkoutRepository,
    private val mealsRepo: MealRepository,
    settings: SettingsRepository
) : ViewModel() {

    val history: StateFlow<List<WorkoutSummaryRow>> =
        workouts.observeHistorySummaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meals: StateFlow<List<MealEntry>> =
        mealsRepo.allMeals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unit: StateFlow<WeightUnit> =
        settings.unit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUnit.KG)

    fun deleteWorkout(id: Long) {
        viewModelScope.launch { workouts.deleteWorkout(id) }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch { mealsRepo.delete(id) }
    }
}
