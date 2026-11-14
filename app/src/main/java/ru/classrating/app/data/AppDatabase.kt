package ru.classrating.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Subject::class, Grade::class, Lesson::class, Homework::class, Classmate::class, SubjectScore::class],
    version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() { abstract fun dao(): DiaryDao }
