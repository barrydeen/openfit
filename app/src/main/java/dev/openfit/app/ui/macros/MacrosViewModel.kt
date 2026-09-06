package dev.openfit.app.ui.macros

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.data.macro.MealRepository
import dev.openfit.app.llm.Draft
import dev.openfit.app.llm.DraftJson
import dev.openfit.app.llm.LlmClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MacroUiState(
    val imagePath: String? = null,
    val draft: Draft? = null,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
)

class MacrosViewModel(
    private val appContext: Application,
    private val mealRepo: MealRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MacroUiState())
    val state: StateFlow<MacroUiState> = _state.asStateFlow()

    val macroSettings: StateFlow<MacroSettings> =
        settingsRepo.macroSettings.stateIn(viewModelScope, SharingStarted.Eagerly, MacroSettings())

    val meals: StateFlow<List<MealEntry>> =
        mealRepo.allMeals.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createImageUri(): Uri {
        val dir = File(appContext.filesDir, "images").apply { mkdirs() }
        val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
        _state.value = _state.value.copy(imagePath = file.absolutePath)
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
    }

    fun onPhotoCaptured(success: Boolean) {
        val st = _state.value
        val path = st.imagePath
        if (!success || path == null) return
        _state.value = _state.value.copy(draft = null, error = null)
        analyze(path)
    }

    fun analyze(imagePath: String) {
        viewModelScope.launch {
            val s = macroSettings.value
            if (s.apiKey.isBlank()) {
                _state.value = _state.value.copy(isAnalyzing = false, error = "Set your API key in Settings first.")
                return@launch
            }
            _state.value = _state.value.copy(isAnalyzing = true, error = null)
            try {
                val imageBytes = File(imagePath).readBytes()
                val history = mealRepo.recent(10)
                val client = LlmClient(s.baseUrl, s.apiKey, s.model)
                val draft = client.analyzeImage(imageBytes, "image/jpeg", history)
                _state.value = _state.value.copy(draft = draft, isAnalyzing = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isAnalyzing = false, error = e.message ?: "Analysis failed")
            }
        }
    }

    fun saveDraft(dish: String, calories: Double, protein: Double, carbs: Double, fat: Double) {
        viewModelScope.launch {
            val st = _state.value
            val entry = MealEntry(
                timestamp = System.currentTimeMillis(),
                imagePath = st.imagePath ?: "",
                dish = dish,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                foodsJson = st.draft?.let { DraftJson.foodsJson(it.foods) } ?: "",
                isAuto = st.draft != null,
            )
            mealRepo.add(entry)
            _state.value = MacroUiState()
        }
    }

    fun useRecent(entry: MealEntry) {
        _state.value = _state.value.copy(
            draft = Draft(
                dish = entry.dish,
                foods = emptyList(),
                calories = entry.calories,
                protein = entry.protein,
                carbs = entry.carbs,
                fat = entry.fat,
            )
        )
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch { mealRepo.delete(id) }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun todayTotals(meals: List<MealEntry>): DayTotals {
        val (start, end) = todayRange()
        val today = meals.filter { it.timestamp in start until end }
        return DayTotals(
            meals = today,
            calories = today.sumOf { it.calories },
            protein = today.sumOf { it.protein },
            carbs = today.sumOf { it.carbs },
            fat = today.sumOf { it.fat },
        )
    }

    fun formatTime(ts: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

    fun formatDate(ts: Long): String =
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(ts))

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return start to (start + 24 * 3600 * 1000L)
    }
}

data class DayTotals(
    val meals: List<MealEntry>,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)
