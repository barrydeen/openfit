package dev.openfit.app.coach

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.openfit.app.data.ExerciseRepository
import dev.openfit.app.data.WorkoutRepository
import dev.openfit.app.data.local.AppDatabase
import dev.openfit.app.data.local.entity.ExerciseEntity
import dev.openfit.app.data.local.entity.Equipment
import dev.openfit.app.data.macro.MacroDatabase
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.data.macro.MealRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoachToolsTest {

    private lateinit var appDb: AppDatabase
    private lateinit var macroDb: MacroDatabase
    private lateinit var mealRepo: MealRepository
    private lateinit var workoutRepo: WorkoutRepository
    private lateinit var exerciseRepo: ExerciseRepository
    private var benchId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        appDb = AppDatabase.inMemory(context)
        macroDb = MacroDatabase.inMemory(context)
        mealRepo = MealRepository(macroDb)
        workoutRepo = WorkoutRepository(appDb.workoutDao(), appDb.exerciseDao())
        exerciseRepo = ExerciseRepository(appDb.exerciseDao())
        benchId = exerciseRepo.createCustom("Bench Press", "Chest", Equipment.BARBELL)
    }

    @After
    fun tearDown() {
        appDb.close()
        macroDb.close()
    }

    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        LocalDate.of(year, month, dayOfMonth).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `nutrition tool returns meals and totals for a range`() = runBlocking {
        mealRepo.add(MealEntry(timestamp = day(2026, 1, 12), imagePath = "", dish = "Oats", calories = 300.0, protein = 12.0, carbs = 50.0, fat = 6.0))
        mealRepo.add(MealEntry(timestamp = day(2026, 1, 20), imagePath = "", dish = "Chicken rice", calories = 600.0, protein = 40.0, carbs = 70.0, fat = 15.0))

        val result = NutritionTool(mealRepo).run(buildJsonObject {
            put("start", "2026-01-01")
            put("end", "2026-01-31")
        })
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals(2, json["mealCount"]!!.jsonPrimitive.int)
        assertEquals("900", (json["totals"]!!.jsonObject["calories"])!!.jsonPrimitive.content)
        assertEquals(2, json["meals"]!!.jsonArray.size)
    }

    @Test
    fun `nutrition tool defaults to today when no dates given`() = runBlocking {
        mealRepo.add(MealEntry(timestamp = day(LocalDate.now().year, LocalDate.now().monthValue, LocalDate.now().dayOfMonth), imagePath = "", dish = "Snack", calories = 100.0, protein = 2.0, carbs = 20.0, fat = 3.0))
        val result = NutritionTool(mealRepo).run(buildJsonObject { })
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals(1, json["mealCount"]!!.jsonPrimitive.int)
    }

    @Test
    fun `workout tool returns workouts with sets and volume`() = runBlocking {
        val id = workoutRepo.createWorkout("Push Day")
        val entry = workoutRepo.addExercise(id, benchId)
        // remove seeded placeholders so only the intended set counts
        appDb.workoutDao().setsFor(entry).forEach { appDb.workoutDao().deleteSet(it) }
        workoutRepo.addSet(entry, 100.0, 5)
        workoutRepo.addSet(entry, 40.0, 10, isWarmup = true)
        workoutRepo.finishWorkout(id)

        val result = WorkoutTool(workoutRepo).run(buildJsonObject {
            put("start", "2000-01-01")
            put("end", "2100-01-01")
        })
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals(1, json["workoutCount"]!!.jsonPrimitive.int)
        val workout = json["workouts"]!!.jsonArray.first().jsonObject
        assertEquals("Push Day", workout["name"]!!.jsonPrimitive.content)
        assertEquals("500", workout["volumeKg"]!!.jsonPrimitive.content)
        val sets = workout["exercises"]!!.jsonArray.first().jsonObject["sets"]!!.jsonArray
        assertEquals(2, sets.size)
    }

    @Test
    fun `exercise progress tool groups sets into e1rm sessions`() = runBlocking {
        repeat(2) { n ->
            val id = workoutRepo.createWorkout("W$n")
            val entry = workoutRepo.addExercise(id, benchId)
            appDb.workoutDao().setsFor(entry).forEach { appDb.workoutDao().deleteSet(it) }
            workoutRepo.addSet(entry, 100.0, 5)
            workoutRepo.finishWorkout(id)
        }
        val result = ExerciseProgressTool(workoutRepo, exerciseRepo).run(buildJsonObject {
            put("exerciseName", "Bench Press")
        })
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals("Bench Press", json["exercise"]!!.jsonPrimitive.content)
        assertEquals(2, json["sessions"]!!.jsonArray.size)
    }

    @Test
    fun `log meal tool adds a meal`() = runBlocking {
        val result = LogMealTool(mealRepo).run(buildJsonObject {
            put("dish", "Burrito")
            put("calories", 800.0)
            put("protein", 35.0)
            put("carbs", 90.0)
            put("fat", 25.0)
            put("date", "2026-02-01")
        })
        val json = Json.parseToJsonElement(result).jsonObject
        assertEquals(true, json["ok"]!!.jsonPrimitive.content.toBoolean())
        val saved = mealRepo.betweenOnce(day(2026, 2, 1), day(2026, 2, 2))
        assertEquals(1, saved.size)
        assertEquals("Burrito", saved[0].dish)
        assertEquals(800.0, saved[0].calories, 0.001)
    }

    @Test
    fun `add set tool creates workout and adds a completed set`() = runBlocking {
        val result = AddSetTool(workoutRepo, exerciseRepo).run(buildJsonObject {
            put("exerciseName", "Bench Press")
            put("weightKg", 85.0)
            put("reps", 6)
        })
        val json = Json.parseToJsonElement(result).jsonObject
        assertTrue(json["ok"]!!.jsonPrimitive.content.toBoolean())
        val active = workoutRepo.activeWorkout()
        assertTrue(active != null)
        val setCount = appDb.workoutDao().setsFor(workoutRepo.exercisesOf(active!!.id).first().id).size
        assertEquals(1, setCount)
    }
}
