package ru.classrating.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow

const val DEFAULT_API = "https://api.dnevnik.ru/v2/"

class Prefs(ctx: Context) {
    private val sp: SharedPreferences = ctx.getSharedPreferences("class_rating", Context.MODE_PRIVATE)

    // Токен доступа хранится в зашифрованном хранилище. Пароли приложение не запрашивает и не хранит.
    private val secure: SharedPreferences = try {
        val key = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            ctx, "class_rating_secure", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        ctx.getSharedPreferences("class_rating_secure_fallback", Context.MODE_PRIVATE)
    }

    val studentName = MutableStateFlow(sp.getString("name", "Ученик") ?: "Ученик")
    val className = MutableStateFlow(sp.getString("class", "") ?: "")
    val themeMode = MutableStateFlow(sp.getInt("theme", 0)) // 0 система, 1 светлая, 2 тёмная
    val clientId = MutableStateFlow(sp.getString("client_id", "") ?: "")
    val apiBase = MutableStateFlow(sp.getString("api_base", DEFAULT_API) ?: DEFAULT_API)
    val token = MutableStateFlow(secure.getString("token", "") ?: "")
    val lastSync = MutableStateFlow(sp.getLong("last_sync", 0L))

    fun setProfile(name: String, cls: String) {
        sp.edit().putString("name", name).putString("class", cls).apply()
        studentName.value = name; className.value = cls
    }
    fun setTheme(mode: Int) { sp.edit().putInt("theme", mode).apply(); themeMode.value = mode }
    fun setConnection(client: String, base: String) {
        sp.edit().putString("client_id", client).putString("api_base", base).apply()
        clientId.value = client; apiBase.value = base
    }
    fun setToken(t: String) { secure.edit().putString("token", t).apply(); token.value = t }
    fun setLastSync(ts: Long) { sp.edit().putLong("last_sync", ts).apply(); lastSync.value = ts }
}
