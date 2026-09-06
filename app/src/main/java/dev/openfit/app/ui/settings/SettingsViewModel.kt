package dev.openfit.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.openfit.app.data.BackupManager
import dev.openfit.app.data.SettingsRepository
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.domain.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val backup: BackupManager
) : ViewModel() {

    val unit: StateFlow<WeightUnit> =
        settings.unit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeightUnit.KG)

    val restSeconds: StateFlow<Long> =
        settings.restSeconds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_REST_SECONDS)

    val macroSettings: StateFlow<MacroSettings> =
        settings.macroSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MacroSettings())

    fun setUnit(unit: WeightUnit) {
        viewModelScope.launch { settings.setUnit(unit) }
    }

    fun setRestSeconds(seconds: Long) {
        viewModelScope.launch { settings.setRestSeconds(seconds) }
    }

    fun saveMacroSettings(macroSettings: MacroSettings) {
        viewModelScope.launch { settings.saveMacroSettings(macroSettings) }
    }

    fun export(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = backup.exportTo(uri)
                onResult(true, "Exported ${data.workouts.size} workouts and ${data.meals.size} meals")
            } catch (t: Throwable) {
                onResult(false, t.message ?: "Export failed")
            }
        }
    }

    fun import(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = backup.importFrom(uri)
                onResult(true, "Imported ${data.workouts.size} workouts and ${data.meals.size} meals")
            } catch (t: Throwable) {
                onResult(false, t.message ?: "Import failed")
            }
        }
    }
}
