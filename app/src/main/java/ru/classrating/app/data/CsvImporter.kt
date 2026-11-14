package ru.classrating.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Одна оценка из файла. quarter = номер четверти, если оценка четвертная, иначе null. */
data class ImportedMark(val subject: String, val value: Int, val day: LocalDate, val quarter: Int?)

data class ImportResult(val marks: List<ImportedMark>, val skippedLines: Int)

/**
 * Формат CSV, одна оценка в строке, разделитель ; или запятая:
 *   предмет;оценка;дата;четверть
 * Примеры:
 *   Математика;5;12.09.2026
 *   Русский язык;4;14.09.2026;1      <- четвертная оценка за 1 четверть
 * Четвёртая колонка необязательна. Строка-заголовок «предмет;...» пропускается.
 */
object CsvImporter {
    private val formats = listOf(
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    fun parse(text: String): ImportResult {
        val marks = mutableListOf<ImportedMark>()
        var skipped = 0
        text.trimStart('\uFEFF').lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val delim = if (line.count { it == ';' } >= line.count { it == ',' }) ';' else ','
                val cols = line.split(delim).map { it.trim().trim('"') }
                val mark = toMark(cols)
                when {
                    mark != null -> marks += mark
                    cols.firstOrNull()?.equals("предмет", ignoreCase = true) == true -> Unit
                    else -> skipped++
                }
            }
        return ImportResult(marks, skipped)
    }

    private fun toMark(c: List<String>): ImportedMark? {
        if (c.size < 3 || c[0].isBlank()) return null
        val value = c[1].toIntOrNull()?.takeIf { it in 2..5 } ?: return null
        val day = parseDate(c[2]) ?: return null
        val quarter = c.getOrNull(3)?.toIntOrNull()?.takeIf { it in 1..4 }
        return ImportedMark(c[0], value, day, quarter)
    }

    private fun parseDate(s: String): LocalDate? {
        for (f in formats) {
            try { return LocalDate.parse(s, f) } catch (_: Exception) { }
        }
        return null
    }
}
