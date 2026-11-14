package ru.classrating.app.data.remote

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.classrating.app.data.*
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class SyncException(message: String) : Exception(message)

const val REDIRECT_URI = "classrating://oauth"

/** Ссылка на официальную страницу авторизации Дневник.ру (OAuth). Пароль вводится только на сайте Дневник.ру. */
fun buildAuthUrl(clientId: String): String =
    "https://login.dnevnik.ru/oauth2?response_type=token&client_id=${Uri.encode(clientId)}" +
        "&scope=CommonInfo,EducationalInfo&redirect_uri=${Uri.encode(REDIRECT_URI)}"

fun friendlyError(e: Throwable): String = when (e) {
    is SyncException -> e.message ?: "Ошибка синхронизации"
    is HttpException -> when (e.code()) {
        401, 403 -> "Доступ закрыт или токен устарел. Подключите Дневник.ру заново"
        404 -> "Сервер не нашёл данные. Проверьте адрес API в настройках"
        else -> "Ошибка сервера (${e.code()})"
    }
    is IOException -> "Нет соединения с интернетом"
    is IllegalArgumentException -> "Некорректный адрес API или данные"
    else -> "Ошибка: ${e.localizedMessage ?: "неизвестная"}"
}

class SyncService(private val dao: DiaryDao, private val prefs: Prefs) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    private fun parseDay(s: String?): LocalDate? = runCatching { LocalDate.parse(s!!.take(10)) }.getOrNull()

    private suspend fun subjectId(name: String): Long {
        dao.subjectByName(name)?.let { return it.id }
        val id = dao.insertSubject(Subject(name = name))
        return if (id > 0) id else dao.subjectByName(name)!!.id
    }

    suspend fun run(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = prefs.token.value
            if (token.isBlank()) throw SyncException("Сначала подключите Дневник.ру в настройках")
            val base = prefs.apiBase.value.trim().let { if (it.endsWith("/")) it else "$it/" }
            val api = Retrofit.Builder().baseUrl(base).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build().create(DnevnikApi::class.java)

            val ctx = api.context(token)
            val person = ctx.personId ?: throw SyncException("Сервер не вернул профиль ученика")
            val groups = ctx.eduGroups?.mapNotNull { it.id }.orEmpty()
            if (groups.isEmpty()) throw SyncException("У ученика не найдено учебных групп")

            val today = LocalDate.now()
            val from = today.minusDays(120).toString()
            val to = today.plusDays(7).toString()
            val lessonSubject = HashMap<String, String>()
            var lessonCount = 0
            var markCount = 0

            for (g in groups) {
                val lessons = api.lessons(token, g, from, to)
                lessons.forEach { l ->
                    val name = l.subject?.name?.trim()
                    if (l.id != null && !name.isNullOrBlank()) lessonSubject[l.id] = name
                    val day = parseDay(l.date)
                    val num = l.number
                    if (day != null && num != null && name != null && name.isNotBlank() &&
                        !day.isBefore(today) && day.isBefore(today.plusDays(7))
                    ) {
                        val (s, e) = defaultTimes(num)
                        dao.upsertLesson(
                            Lesson(dayOfWeek = day.dayOfWeek.value, number = num, subjectId = subjectId(name),
                                startMin = s, endMin = e, room = l.place.orEmpty())
                        )
                        lessonCount++
                    }
                }
                api.marks(token, person, g, from, to).forEach { m ->
                    val value = m.value?.trim()?.toIntOrNull()?.takeIf { it in 1..5 }
                    val day = parseDay(m.date)
                    val subject = m.lesson?.let { lessonSubject[it] }
                    if (m.id != null && value != null && day != null && subject != null) {
                        dao.upsertGrade(Grade(subjectId = subjectId(subject), value = value,
                            epochDay = day.toEpochDay(), externalId = "dn-${m.id}"))
                        markCount++
                    }
                }
            }
            prefs.setLastSync(System.currentTimeMillis())
            Result.success("Синхронизировано: оценок $markCount, уроков в расписании $lessonCount")
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) prefs.setToken("")
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
