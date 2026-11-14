# Class Rating

Электронный дневник (Kotlin, Jetpack Compose + Material 3, MVVM, Room). minSdk 24 (Android 7.0).

## Сборка APK
1. Android Studio Koala (2024.1) или новее → File → Open → папка ClassRating. Дождитесь Gradle Sync (нужен интернет, JDK 17 встроен).
2. Отладочный APK: Build → Build APK(s). Файл: app/build/outputs/apk/debug/app-debug.apk
3. Консоль: один раз `gradle wrapper --gradle-version 8.7`, затем `./gradlew assembleDebug`.

## Релизный APK
Build → Generate Signed Bundle / APK → APK → создайте keystore (сохраните пароль!) → release.
Файл: app/build/outputs/apk/release/app-release.apk

## Установка
Скопируйте APK на телефон, разрешите «Установку из неизвестных источников» и откройте файл. Или: `adb install app-debug.apk`.
