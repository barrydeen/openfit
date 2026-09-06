package dev.openfit.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.openfit.app.data.macro.MacroGoals
import dev.openfit.app.data.macro.MacroSettings
import dev.openfit.app.domain.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val UNIT = stringPreferencesKey("unit")
        val REST_SECONDS = longPreferencesKey("rest_seconds")
        val FAST_ADD_REST = longPreferencesKey("fast_add_rest")
        val BASE_URL = stringPreferencesKey("base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model")
        val GOAL_CAL = doublePreferencesKey("goal_calories")
        val GOAL_PROTEIN = doublePreferencesKey("goal_protein")
        val GOAL_CARBS = doublePreferencesKey("goal_carbs")
        val GOAL_FAT = doublePreferencesKey("goal_fat")
    }

    val unit: Flow<WeightUnit> = context.dataStore.data
        .map { prefs -> WeightUnit.from(prefs[Keys.UNIT]) }
        .distinctUntilChanged()

    val restSeconds: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[Keys.REST_SECONDS] ?: DEFAULT_REST_SECONDS }
        .distinctUntilChanged()

    val fastAddRest: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[Keys.FAST_ADD_REST] ?: DEFAULT_EXPRESS_REST }
        .distinctUntilChanged()

    suspend fun setUnit(unit: WeightUnit) {
        context.dataStore.edit { it[Keys.UNIT] = unit.name }
    }

    suspend fun setRestSeconds(seconds: Long) {
        context.dataStore.edit { it[Keys.REST_SECONDS] = seconds.coerceIn(5L, 600L) }
    }

    val macroSettings: Flow<MacroSettings> = context.dataStore.data
        .map { prefs ->
            MacroSettings(
                baseUrl = prefs[Keys.BASE_URL] ?: MacroSettings().baseUrl,
                apiKey = prefs[Keys.API_KEY] ?: "",
                model = prefs[Keys.MODEL] ?: MacroSettings().model,
                goals = MacroGoals(
                    calories = prefs[Keys.GOAL_CAL] ?: MacroGoals.DEFAULT.calories,
                    protein = prefs[Keys.GOAL_PROTEIN] ?: MacroGoals.DEFAULT.protein,
                    carbs = prefs[Keys.GOAL_CARBS] ?: MacroGoals.DEFAULT.carbs,
                    fat = prefs[Keys.GOAL_FAT] ?: MacroGoals.DEFAULT.fat,
                ),
            )
        }
        .distinctUntilChanged()

    suspend fun saveMacroSettings(settings: MacroSettings) {
        val goals = settings.goals
        context.dataStore.edit { it ->
            it[Keys.BASE_URL] = settings.baseUrl
            it[Keys.API_KEY] = settings.apiKey
            it[Keys.MODEL] = settings.model
            it[Keys.GOAL_CAL] = goals.calories
            it[Keys.GOAL_PROTEIN] = goals.protein
            it[Keys.GOAL_CARBS] = goals.carbs
            it[Keys.GOAL_FAT] = goals.fat
        }
    }

    companion object {
        const val DEFAULT_REST_SECONDS = 120L
        const val DEFAULT_EXPRESS_REST = 60L
    }
}
