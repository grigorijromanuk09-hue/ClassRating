package ru.classrating.app.data

import androidx.room.*

@Entity(indices = [Index(value = ["name"], unique = true)])
data class Subject(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("subjectId"), Index(value = ["externalId"], unique = true)]
)
data class Grade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val value: Int,
    val epochDay: Long,
    val isQuarter: Boolean = false,
    val quarter: Int = 1,
    val comment: String = "",
    val externalId: String? = null
)

@Entity(
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("subjectId"), Index(value = ["dayOfWeek", "number"], unique = true)]
)
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,      // 1 = Пн … 7 = Вс
    val number: Int,
    val subjectId: Long,
    val startMin: Int,
    val endMin: Int,
    val room: String = "",
    val teacher: String = ""
)

@Entity(
    foreignKeys = [ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("subjectId")]
)
data class Homework(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val text: String,
    val dueEpochDay: Long,
    val done: Boolean = false
)

@Entity
data class Classmate(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val avg: Double)

@Entity(
    primaryKeys = ["classmateId", "subjectId"],
    foreignKeys = [
        ForeignKey(Classmate::class, ["id"], ["classmateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Subject::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("subjectId")]
)
data class SubjectScore(val classmateId: Long, val subjectId: Long, val avg: Double)
