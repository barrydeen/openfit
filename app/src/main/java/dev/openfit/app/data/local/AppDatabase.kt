package dev.openfit.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.openfit.app.data.local.dao.ExerciseDao
import dev.openfit.app.data.local.dao.WorkoutDao
import dev.openfit.app.data.local.entity.ExerciseEntity
import dev.openfit.app.data.local.entity.WorkoutEntity
import dev.openfit.app.data.local.entity.WorkoutExerciseEntity
import dev.openfit.app.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    abstract fun workoutDao(): WorkoutDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "openfit_workout.db").build()

        fun inMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
