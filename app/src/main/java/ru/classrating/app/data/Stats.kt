package ru.classrating.app.data

import java.util.Locale

data class SubjectStat(val subject: Subject, val grades: List<Grade>) { // grades: новые сверху
    val current: List<Grade> = grades.filter { !it.isQuarter }
    val avg: Double? = current.takeIf { it.isNotEmpty() }?.map { it.value }?.average()
    fun quarter(q: Int): Int? = grades.filter { it.isQuarter && it.quarter == q }.maxByOrNull { it.epochDay }?.value
}

data class DiaryState(
    val stats: List<SubjectStat> = emptyList(),
    val recent: List<Pair<Grade, Subject>> = emptyList(),
    val distribution: Map<Int, Int> = emptyMap(),
    val trend: List<Float> = emptyList()
) {
    val overall: Double? = stats.mapNotNull { it.avg }.takeIf { it.isNotEmpty() }?.average()
}

fun buildDiary(subjects: List<Subject>, grades: List<Grade>): DiaryState {
    val byId = subjects.associateBy { it.id }
    val stats = subjects.map { s -> SubjectStat(s, grades.filter { it.subjectId == s.id }) }
    val current = grades.filter { !it.isQuarter }
    val recent = current.take(8).mapNotNull { g -> byId[g.subjectId]?.let { g to it } }
    val dist = (2..5).associateWith { v -> current.count { it.value == v } }
    var sum = 0.0
    val trend = current.sortedWith(compareBy({ it.epochDay }, { it.id })).mapIndexed { i, g ->
        sum += g.value; (sum / (i + 1)).toFloat()
    }.takeLast(40)
    return DiaryState(stats, recent, dist, trend)
}

data class RatingRow(val name: String, val avg: Double, val isMe: Boolean, val classmateId: Long? = null)

fun buildRating(
    myName: String, diary: DiaryState, classmates: List<Classmate>,
    scores: List<SubjectScore>, subjectId: Long?
): List<RatingRow> {
    val rows = mutableListOf<RatingRow>()
    val mine = if (subjectId == null) diary.overall else diary.stats.firstOrNull { it.subject.id == subjectId }?.avg
    if (mine != null) rows += RatingRow(myName.ifBlank { "Я" }, mine, true)
    classmates.forEach { c ->
        val a = if (subjectId == null) c.avg
        else scores.firstOrNull { it.classmateId == c.id && it.subjectId == subjectId }?.avg
        if (a != null) rows += RatingRow(c.name, a, false, c.id)
    }
    return rows.sortedByDescending { it.avg }
}

fun fmt(v: Double?): String = v?.let { String.format(Locale("ru"), "%.2f", it) } ?: "—"

fun defaultTimes(number: Int): Pair<Int, Int> {
    val start = 8 * 60 + 30 + (number - 1) * 55
    return start to start + 40
}
