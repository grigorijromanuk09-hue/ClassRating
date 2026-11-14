package ru.classrating.app

import android.app.Application
import androidx.room.Room
import ru.classrating.app.data.AppDatabase
import ru.classrating.app.data.Prefs

class ClassRatingApp : Application() {
    val db: AppDatabase by lazy { Room.databaseBuilder(this, AppDatabase::class.java, "class_rating.db").build() }
    val prefs: Prefs by lazy { Prefs(this) }
}
