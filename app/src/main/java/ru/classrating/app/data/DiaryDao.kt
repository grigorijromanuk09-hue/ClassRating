package ru.classrating.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM Subject ORDER BY name") fun subjects(): Flow<List<Subject>>
    @Query("SELECT * FROM Subject WHERE name = :name LIMIT 1") suspend fun subjectByName(name: String): Subject?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSubject(s: Subject): Long
    @Delete suspend fun deleteSubject(s: Subject)

    @Query("SELECT * FROM Grade ORDER BY epochDay DESC, id DESC") fun grades(): Flow<List<Grade>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertGrade(g: Grade): Long
    @Delete suspend fun deleteGrade(g: Grade)
    @Query("DELETE FROM Grade WHERE subjectId = :subjectId AND isQuarter = 1 AND quarter = :quarter")
    suspend fun deleteQuarter(subjectId: Long, quarter: Int)

    @Query("SELECT * FROM Lesson ORDER BY dayOfWeek, number") fun lessons(): Flow<List<Lesson>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLesson(l: Lesson): Long
    @Delete suspend fun deleteLesson(l: Lesson)

    @Query("SELECT * FROM Homework ORDER BY done, dueEpochDay") fun homework(): Flow<List<Homework>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertHomework(h: Homework): Long
    @Delete suspend fun deleteHomework(h: Homework)

    @Query("SELECT * FROM Classmate ORDER BY name") fun classmates(): Flow<List<Classmate>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertClassmate(c: Classmate): Long
    @Delete suspend fun deleteClassmate(c: Classmate)
    @Query("SELECT * FROM SubjectScore") fun scores(): Flow<List<SubjectScore>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertScore(s: SubjectScore)

    @Query("DELETE FROM Subject") suspend fun wipeSubjects()
    @Query("DELETE FROM Classmate") suspend fun wipeClassmates()
}
