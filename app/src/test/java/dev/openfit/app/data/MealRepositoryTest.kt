package dev.openfit.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.openfit.app.data.macro.MacroDatabase
import dev.openfit.app.data.macro.MealEntry
import dev.openfit.app.data.macro.MealRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MealRepositoryTest {

    private lateinit var db: MacroDatabase
    private lateinit var repo: MealRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = MacroDatabase.inMemory(context)
        repo = MealRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private var seq = 0L

    private fun meal(dish: String, cal: Double) = MealEntry(
        timestamp = System.currentTimeMillis() + (seq++),
        imagePath = "",
        dish = dish,
        calories = cal,
        protein = 10.0,
        carbs = 20.0,
        fat = 5.0,
    )

    @Test
    fun `add and list meals`() = runBlocking {
        repo.add(meal("Oats", 300.0))
        repo.add(meal("Chicken", 500.0))
        val all = repo.allMeals.first()
        assertEquals(2, all.size)
        // newest first
        assertTrue(all[0].dish == "Chicken")
    }

    @Test
    fun `recent returns limited newest entries`() = runBlocking {
        repo.add(meal("A", 1.0))
        repo.add(meal("B", 2.0))
        repo.add(meal("C", 3.0))
        val recent = repo.recent(2)
        assertEquals(2, recent.size)
        assertEquals("C", recent[0].dish)
    }

    @Test
    fun `delete removes meal`() = runBlocking {
        val id = repo.add(meal("Oats", 300.0))
        repo.delete(id)
        assertEquals(0, repo.allMeals.first().size)
    }
}
