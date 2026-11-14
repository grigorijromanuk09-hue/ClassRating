package ru.classrating.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.classrating.app.ClassRatingApp
import ru.classrating.app.data.*
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
