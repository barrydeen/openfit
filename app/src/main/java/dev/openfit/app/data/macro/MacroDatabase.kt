package dev.openfit.app.data.macro

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MealEntry::class], version = 1, exportSchema = false)
abstract class MacroDatabase : RoomDatabase() {

    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile private var instance: MacroDatabase? = null

        fun build(context: Context): MacroDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MacroDatabase::class.java,
                    "openfit_macros.db"
                ).build().also { instance = it }
            }

        fun inMemory(context: Context): MacroDatabase =
            Room.inMemoryDatabaseBuilder(context, MacroDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
