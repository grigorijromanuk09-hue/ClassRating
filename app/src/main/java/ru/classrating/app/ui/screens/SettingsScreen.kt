@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ru.classrating.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.classrating.app.data.DEFAULT_API
import ru.classrating.app.data.remote.buildAuthUrl
import ru.classrating.app.ui.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val name by vm.prefs.studentName.collectAsStateWithLifecycle()
    val cls by vm.prefs.className.collectAsStateWithLifecycle()
    val theme by vm.prefs.themeMode.collectAsStateWithLifecycle()
    val token by vm.prefs.token.collectAsStateWithLifecycle()
    val clientSaved by vm.prefs.clientId.collectAsStateWithLifecycle()
    val baseSaved by vm.prefs.apiBase.collectAsStateWithLifecycle()
    val last by vm.prefs.lastSync.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()

    var n by remember(name) { mutableStateOf(name) }
    var c by remember(cls) { mutableStateOf(cls) }
    var client by remember(clientSaved) { mutableStateOf(clientSaved) }
    var base by remember(baseSaved) { mutableStateOf(baseSaved) }
    var manualToken by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    ScreenScaffold("Настройки", onBack = onBack) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppCard {
                SectionTitle("Профиль")
                OutlinedTextField(n, { n = it }, label = { Text("Имя ученика") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(c, { c = it }, label = { Text("Класс (например, 8Б)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button({ vm.saveProfile(n, c) }) { Text("Сохранить") }
            }
            AppCard {
                SectionTitle("Тема")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Системная", "Светлая", "Тёмная").forEachIndexed { i, t -> FilterChip(theme == i, { vm.setTheme(i) }, label = { Text(t) }) }
                }
            }
            AppCard {
                SectionTitle("Синхронизация с Дневник.ру")
                Text(
                    "Вход выполняется на официальной странице Дневник.ру (OAuth), приложение не видит и не хранит ваш пароль. " +
                        "Для этого нужен идентификатор приложения (client_id), выданный Дневник.ру при регистрации разработчика. " +
                        "Без него пользуйтесь ручным вводом данных.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(client, { client = it }, label = { Text("client_id") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(base, { base = it }, label = { Text("Адрес API") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton({ base = DEFAULT_API }) { Text("Адрес по умолчанию") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({
                        vm.saveConnection(client, base)
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildAuthUrl(client.trim()))))
                    }, enabled = client.isNotBlank()) { Text("Войти через Дневник.ру") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(manualToken, { manualToken = it }, label = { Text("Или вставьте токен доступа") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton({ vm.saveConnection(client, base); vm.saveToken(manualToken); manualToken = "" }, enabled = manualToken.isNotBlank()) { Text("Сохранить токен") }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(if (token.isBlank()) "Статус: не подключено" else "Статус: подключено", fontWeight = FontWeight.Medium)
                if (last > 0) Text("Последняя синхронизация: " + SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru")).format(Date(last)), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ vm.saveConnection(client, base); vm.sync() }, enabled = token.isNotBlank() && !syncing) {
                        if (syncing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Синхронизировать")
                    }
                    OutlinedButton({ vm.disconnect() }, enabled = token.isNotBlank()) { Text("Отключить") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Загружаются оценки и расписание; домашние задания и рейтинг класса вводятся вручную. Ручные данные не удаляются.", style = MaterialTheme.typography.bodySmall)
            }
            AppCard {
                SectionTitle("Данные")
                OutlinedButton({ confirmClear = true }) { Text("Удалить все данные") }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Удалить все данные?") },
        text = { Text("Предметы, оценки, расписание, задания и рейтинг будут удалены без возможности восстановления.") },
        confirmButton = { TextButton({ vm.clearAll(); confirmClear = false }) { Text("Удалить") } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Отмена") } }
    )
}
