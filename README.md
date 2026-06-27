# LAMPA — Lampac edition

Android / Android TV клиент [LAMPA](https://github.com/lampa-app/LAMPA), настроенный для работы с self-hosted сервером [Lampac](https://github.com/lampac-nextgen/lampac).

Приложение — это WebView-оболочка: открывает веб-интерфейс Lampa с вашего lampac и добавляет Android-функции (плееры, закладки на главном экране TV, голосовой поиск и т.д.).

**Пакет lampac-сборки:** `top.rootu.lampa.lampac`  
**Минимальная версия Android:** 4.1 (API 16)

---

## Быстрый старт

1. Запустите lampac и проверьте в браузере на ПК:
   ```
   http://ВАШ_СЕРВЕР:9118
   ```
2. Укажите URL в `gradle.properties` → `lampacUrl=...`
3. Соберите APK:
   ```bat
   gradlew.bat assembleLampacDebug
   ```
4. Установите `app\build\outputs\apk\lampac\debug\app-lampac-debug.apk` на TV.

---

## Что где настраивать

| Файл / место | Что задаёт | Когда менять |
|---|---|---|
| [`gradle.properties`](gradle.properties) → `lampacUrl` | **Главная настройка.** URL lampac, вшивается в APK как стартовая страница | Перед каждой сборкой под новый сервер |
| [`local.properties`](local.properties) → `sdk.dir` | Путь к Android SDK на вашем ПК | Один раз при первой сборке (файл локальный, в git не коммитится) |
| [`lampac-config/lampainit.js`](lampac-config/lampainit.js) | Override настроек Lampa **на сервере** lampac | Скопировать на сервер, если CUB/плагины мешают |
| [`app/build.gradle`](app/build.gradle) → `productFlavors.lampac` | Версия APK, `applicationId`, имя flavor | Редко — при смене версии или ID приложения |
| [`app/src/main/java/.../LampacHelper.kt`](app/src/main/java/top/rootu/lampa/helpers/LampacHelper.kt) | Патчи для lampac (отключение CUB store, версия клиента) | Только если нужна доработка логики |
| [`.github/workflows/lampac.yml`](.github/workflows/lampac.yml) | Сборка APK в GitHub Actions | При сборке через CI — URL передаётся в workflow |

### `gradle.properties` — главный конфиг

```properties
# IP, домен или полный URL. Порт lampac по умолчанию — 9118.
# НЕ используйте localhost / 127.0.0.1 — TV не видит ваш ПК по этим адресам.
lampacUrl=http://192.168.1.50:9118
```

Примеры:

```properties
lampacUrl=http://192.168.1.50:9118
lampacUrl=http://lampa.example.com:9118
lampacUrl=https://lampa.lab.example.ru
```

После изменения **обязательно пересоберите APK** — URL запекается в приложение при сборке.

### `local.properties` — Android SDK (только на ПК сборки)

Создайте в корне проекта (если нет Android Studio):

```properties
sdk.dir=C\:\\Android\\Sdk
```

Путь зависит от вашей системы. Файл уже в `.gitignore`.

### Сервер lampac — `lampac-config/lampainit.js`

Опционально, но рекомендуется. Копируется на сервер:

```
lampac/plugins/override/lampainit.js
```

Отключает CUB-магазин плагинов и лишние сервисы на стороне сервера. После копирования — перезапуск lampac.

Для Docker (из документации lampac):

```yaml
volumes:
  - ./lampac-docker/plugins/lampainit.js:/lampac/plugins/override/lampainit.js
```

---

## Сборка APK на Windows

### Требования

- **JDK 17**
- **Android SDK** (через Android Studio или command-line tools)
- Интернет при первой сборке (Gradle скачает зависимости)

### 1. Установка JDK 17

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
```

Переменные среды (Панель управления → Система → Переменные среды):

| Переменная | Значение |
|---|---|
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot` |
| `Path` | добавить `%JAVA_HOME%\bin` |

Откройте **новый** терминал и проверьте:

```powershell
java -version
```

### 2. Android SDK

**Вариант A — Android Studio** (проще): установите Studio, SDK появится в  
`%LOCALAPPDATA%\Android\Sdk`

**Вариант B — только command-line tools:** см. [developer.android.com](https://developer.android.com/studio#command-tools)

Создайте `local.properties`:

```properties
sdk.dir=C\:\\Android\\Sdk
```

Установите пакеты SDK:

```powershell
sdkmanager --sdk_root=C:\Android\Sdk --licenses
sdkmanager --sdk_root=C:\Android\Sdk "platform-tools" "platforms;android-34" "build-tools;34.0.0" "build-tools;30.0.3"
```

### 3. Настройка URL lampac

Отредактируйте `gradle.properties`:

```properties
lampacUrl=http://ВАШ_IP_ИЛИ_ДОМЕН:9118
```

### 4. Сборка

```powershell
cd D:\desktop\LAMPA-main
.\gradlew.bat assembleLampacDebug
```

Готовый APK:

```
app\build\outputs\apk\lampac\debug\app-lampac-debug.apk
```

**Release-сборка** (нужен keystore в `app/keystore/`):

```powershell
.\gradlew.bat assembleLampacRelease
```

Для личного использования достаточно **debug** — подписывается автоматически.

---

## Сборка через GitHub Actions

1. Залейте репозиторий на GitHub.
2. **Actions** → **Build Lampac APK** → **Run workflow**.
3. Введите `lampac_url` (тот же URL, что в `gradle.properties`).
4. Скачайте APK из **Artifacts** → `app-lampac-debug`.

Workflow: [`.github/workflows/lampac.yml`](.github/workflows/lampac.yml)

---

## Установка на Android TV

### Способ 1 — ADB с ПК

```powershell
adb connect IP_ТВ:5555
adb install -r app\build\outputs\apk\lampac\debug\app-lampac-debug.apk
```

### Способ 2 — USB / файловый менеджер

Скопируйте APK на флешку или в облако, откройте на TV через файловый менеджер и установите.

### После установки

- При первом запуске откроется URL из `lampacUrl`.
- Сменить URL вручную: удерживайте **Назад** или **Меню** → изменить адрес.
- Если меняли URL в конфиге — **пересоберите APK** или введите новый адрес в приложении.
- При проблемах: **Настройки → Приложения → LAMPA → Очистить данные**.

---

## Flavor `lampac` — отличия от обычной LAMPA

| Параметр | Значение |
|---|---|
| Package ID | `top.rootu.lampa.lampac` |
| Версия | `2.1.16` (versionCode `16`) |
| Стартовый URL | из `gradle.properties` → `lampacUrl` |
| User-Agent | `... lampa_client personal.lampa` |
| Автообновление APK | выключено |
| CUB plugin store | отключён патчем в `LampacHelper` |

Другие flavors (`lite`, `full`, `ruStore`) — из оригинального [lampa-app/LAMPA](https://github.com/lampa-app/LAMPA), для lampac не используются.

---

## Структура проекта (важное)

```
LAMPA-main/
├── gradle.properties          ← lampacUrl (главный конфиг)
├── local.properties           ← путь к Android SDK (локально)
├── lampac-config/
│   └── lampainit.js           ← override для сервера lampac
├── app/
│   ├── build.gradle           ← flavor lampac, версия, зависимости
│   └── src/main/java/top/rootu/lampa/
│       ├── MainActivity.kt    ← WebView, URL, меню
│       ├── AndroidJS.kt       ← мост JS ↔ Android (HTTP, плеер)
│       └── helpers/
│           └── LampacHelper.kt ← патчи для lampac
├── .github/workflows/
│   └── lampac.yml             ← CI-сборка
└── app/build/outputs/apk/lampac/debug/
    └── app-lampac-debug.apk   ← результат сборки
```

---

## Решение проблем

| Симптом | Что делать |
|---|---|
| `JAVA_HOME is not set` | Установите JDK 17, задайте `JAVA_HOME`, откройте новый терминал |
| `SDK location not found` | Создайте `local.properties` с `sdk.dir=...` |
| Чёрный / пустой экран | Проверьте lampac в браузере ПК; TV и сервер в одной сети; URL без `localhost` |
| Зелёная ошибка `reading 'find'` | Пересоберите APK; скопируйте `lampac-config/lampainit.js` на сервер; очистите данные приложения |
| «Обновите до версии 16» | Пересоберите APK — в flavor `lampac` versionCode = 16 |
| Старый URL после пересборки | Очистите данные приложения на TV |
| Release не собирается | Используйте `assembleLampacDebug` или добавьте keystore в `app/keystore/` |
| Тормозит / глючит WebView | Меню приложения → **Сменить браузер** → **Android System WebView** (на новых TV) |

### Проверка lampac перед сборкой APK

На ПК в браузере должно открываться без ошибок:

```
http://ВАШ_СЕРВЕР:9118
```

Тот же адрес (с тем же протоколом `http`/`https`) укажите в `lampacUrl`.

---

## Ссылки

- [lampa-app/LAMPA](https://github.com/lampa-app/LAMPA) — оригинальный Android-клиент
- [lampac-nextgen/lampac](https://github.com/lampac-nextgen/lampac) — backend-сервер
- [yumata/lampa](https://github.com/yumata/lampa) — веб-интерфейс Lampa