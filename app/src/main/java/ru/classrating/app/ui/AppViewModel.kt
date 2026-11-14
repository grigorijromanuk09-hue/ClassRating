package ru.classrating.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import kotlinx.coroutines.launch
import ru.classrating.app.ClassRatingApp
import ru.classrating.app.data.*
import ru.classrating.app.data.CsvImporter
import ru.classrating.app.data.remote.SyncService
import ru.classrating.app.data.remote.friendlyError
import kotlin.coroutines.cancellation.CancellationException

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app as ClassRatingApp
    private val dao = c.db.dao()
    val prefs = c.prefs
    private val sync = SyncService(dao, prefs)

    private fun <T> Flow<T>.state(init: T) = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), init)

    val subjects = dao.subjects().state(emptyList())
    val diary = combine(dao.subjects(), dao.grades()) { s, g -> buildDiary(s, g) }.state(DiaryState())
    val lessons = dao.lessons().state(emptyList())
    val homework = dao.homework().state(emptyList())
    val classmates = dao.classmates().state(emptyList())
    val scores = dao.scores().state(emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()
    val syncing = MutableStateFlow(false)

    init {
        // Автосинхронизация при запуске, если приложение подключено и данные старше 6 часов
        val stale = System.currentTimeMillis() - prefs.lastSync.value > 6 * 3600_000L
        if (prefs.token.value.isNotBlank() && stale) sync(silent = true)
    }

    private fun safe(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e }
        catch (e: Exception) { _messages.tryEmit("Ошибка: ${e.localizedMessage ?: "не удалось сохранить"}") }
    }

    // ---- ручной ввод ----
    fun addSubject(name: String) = safe {
        val n = name.trim()
        if (n.isEmpty()) { _messages.tryEmit("Введите название предмета"); return@safe }
        if (dao.insertSubject(Subject(name = n)) < 0) _messages.tryEmit("Такой предмет уже есть")
    }
    fun deleteSubject(s: Subject) = safe { dao.deleteSubject(s) }

    fun addGrade(subjectId: Long, value: Int, day: Long, isQuarter: Boolean, quarter: Int, comment: String) = safe {
        if (isQuarter) dao.deleteQuarter(subjectId, quarter)
        dao.upsertGrade(Grade(subjectId = subjectId, value = value, epochDay = day,
            isQuarter = isQuarter, quarter = quarter, comment = comment.trim()))
    }
    fun deleteGrade(g: Grade) = safe { dao.deleteGrade(g) }

    fun addLesson(l: Lesson) = safe { dao.upsertLesson(l) }
    fun deleteLesson(l: Lesson) = safe { dao.deleteLesson(l) }

    fun addHomework(subjectId: Long, text: String, due: Long) = safe {
        if (text.isBlank()) { _messages.tryEmit("Опишите задание"); return@safe }
        dao.upsertHomework(Homework(subjectId = subjectId, text = text.trim(), dueEpochDay = due))
    }
    fun toggleHomework(h: Homework) = safe { dao.upsertHomework(h.copy(done = !h.done)) }
    fun deleteHomework(h: Homework) = safe { dao.deleteHomework(h) }

    fun addClassmate(name: String, avg: Double) = safe {
        if (name.isBlank()) { _messages.tryEmit("Введите имя ученика"); return@safe }
        dao.upsertClassmate(Classmate(name = name.trim(), avg = avg))
    }
    fun deleteClassmate(c: Classmate) = safe { dao.deleteClassmate(c) }
    fun setScore(classmateId: Long, subjectId: Long, avg: Double) = safe {
        dao.upsertScore(SubjectScore(classmateId, subjectId, avg))
    }

    // ---- настройки ----
    fun saveProfile(name: String, cls: String) { prefs.setProfile(name.trim(), cls.trim()); _messages.tryEmit("Профиль сохранён") }
    fun setTheme(mode: Int) = prefs.setTheme(mode)
    fun saveConnection(client: String, base: String) = prefs.setConnection(client.trim(), base.trim().ifBlank { DEFAULT_API })
    fun saveToken(t: String) { prefs.setToken(t.trim()); _messages.tryEmit("Токен сохранён") }
    fun disconnect() { prefs.setToken(""); _messages.tryEmit("Дневник.ру отключён") }
    fun clearAll() = safe { dao.wipeSubjects(); dao.wipeClassmates(); _messages.tryEmit("Данные удалены") }

    /** Обработка возврата из браузера: classrating://oauth#access_token=... */
    fun handleOAuthRedirect(uri: Uri?) {
        if (uri == null || uri.scheme != "classrating") return
        val token = uri.fragment?.let { Uri.parse("http://x/?$it").getQueryParameter("access_token") }
        if (token.isNullOrBlank()) { _messages.tryEmit("Авторизация не завершена"); return }
        prefs.setToken(token)
        _messages.tryEmit("Дневник.ру подключён")
        sync()
    }

    // ---- импорт оценок из файла (CSV) и «Поделиться» ----
    private suspend fun subjectIdFor(name: String): Long {
        dao.subjectByName(name)?.let { return it.id }
        dao.insertSubject(Subject(name = name))
        return dao.subjectByName(name)!!.id
    }

    private fun decode(bytes: ByteArray): String {
        val utf = String(bytes, Charsets.UTF_8)
        return if (utf.contains('\uFFFD')) String(bytes, Charset.forName("windows-1251")) else utf
    }

    /** Разбирает текст CSV и сохраняет оценки. Повторный импорт того же файла не дублирует оценки. */
    suspend fun importText(text: String) {
        try {
            val result = CsvImporter.parse(text)
            if (result.marks.isEmpty()) {
                _messages.tryEmit("В файле нет оценок. Сохраните выгрузку как CSV: предмет;оценка;дата")
                return
            }
            result.marks.forEach { m ->
                val sid = subjectIdFor(m.subject)
                dao.upsertGrade(Grade(
                    subjectId = sid,
                    value = m.value,
                    epochDay = m.day.toEpochDay(),
                    isQuarter = m.quarter != null,
                    quarter = m.quarter ?: 1,
                    externalId = "csv-${m.subject}|${m.day}|${m.value}|${m.quarter ?: 0}"
                ))
            }
            _messages.tryEmit("Импортировано оценок: ${result.marks.size}, пропущено строк: ${result.skippedLines}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _messages.tryEmit("Ошибка импорта: ${e.localizedMessage ?: "неизвестная"}")
        }
    }

    fun importFromUri(uri: Uri) = viewModelScope.launch {
        val bytes = try {
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (e: Exception) { null }
        if (bytes == null) { _messages.tryEmit("Не удалось открыть файл"); return@launch }
        importText(decode(bytes))
    }

    /** Открытие через «Поделиться» или из файлового менеджера. Схему classrating:// отдаём OAuth. */
    fun handleIncoming(intent: Intent?) {
        val data = intent?.data
        if (data?.scheme == "classrating") { handleOAuthRedirect(data); return }
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                when {
                    uri != null -> importFromUri(uri)
                    !text.isNullOrBlank() -> viewModelScope.launch { importText(text) }
                }
            }
            Intent.ACTION_VIEW -> data?.let { importFromUri(it) }
        }
    }

    fun sync(silent: Boolean = false) {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            val r = sync.run()
            syncing.value = false
            r.onSuccess { if (!silent) _messages.tryEmit(it) }
                .onFailure { _messages.tryEmit(friendlyError(it)) }
        }
    }
}
