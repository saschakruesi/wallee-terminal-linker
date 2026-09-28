# 01 — Architektur

## Zielbild

Eine schlanke Android-App (ein Modul, ein APK), mit der eine Person vor Ort ein physisches Terminal in
unter einer Minute mit einem wallee-Payment-Terminal verknüpft. Zielgruppe: Techniker, Installationspartner,
wallee-Support. Kein Kassensystem, keine Zahlungen — ausschliesslich Terminal-Verwaltung.

Nutzungssituation: Handy in einer Hand, Terminal in der anderen, oft im Laden mit wenig Licht, teilweise
schlechtes Mobilnetz. Daraus folgt: grosse Touchflächen, Kamera-Scan als Hauptpfad mit manueller Eingabe
als Fallback, klare Fehlermeldungen, alle Netzoperationen abbrechbar.

## Technologie-Entscheid

| Bereich | Entscheid | Begründung |
|---|---|---|
| Sprache / UI | **Kotlin 2.x, Jetpack Compose (Material 3 als Basis, vollständig überstyled)** | Nativ, beste Kamera-Integration, kleinste APK, kein zweites Ökosystem |
| Min SDK | **API 26 (Android 8.0)**, Target SDK 35 | Abdeckung > 97 %, EncryptedSharedPreferences und CameraX laufen ab 23/21, ML Kit ab 21 |
| Navigation | Navigation Compose (type-safe routes) | Standard, keine Zusatzlibrary |
| Netzwerk | OkHttp 4 + kotlinx.serialization (JSON) | Kein Retrofit nötig — 8 Endpunkte, ein generischer `request()`-Client wie im Virtual Terminal |
| JWT | Eigene Implementierung (`javax.crypto.Mac` HmacSHA256, `android.util.Base64` URL_SAFE/NO_PADDING/NO_WRAP) | Keine JWT-Library; Token ist pro Request neu und trivial |
| Kamera / Scan | **CameraX 1.4 + ML Kit Barcode Scanning (bundled)** | Erkennt Code128/Code39/QR/DataMatrix; bundled-Variante braucht keine Play-Services-Installation zur Laufzeit |
| Speicherung | DataStore (Preferences) für UI-Zustand; **EncryptedSharedPreferences** (androidx.security-crypto) für Credentials | Keystore-gesichert, kein eigenes Crypto |
| DI | Manuell: `AppContainer` in `Application` | App ist klein; Hilt lohnt sich nicht |
| Async | Kotlin Coroutines + Flow, `viewModelScope` | Standard |
| Bilder/Fonts | Roboto 300/400/500/700 als `res/font/*.ttf`, Logos als VectorDrawable | Offline, kein Google-Fonts-Provider |
| Tests | JUnit 5 + kotlinx-coroutines-test + MockWebServer; Compose UI-Tests für Kernscreens | |
| Build | Gradle (Kotlin DSL), Version Catalog (`gradle/libs.versions.toml`), AGP 8.x | |
| CI/CD | GitHub Actions: Build + Test auf PR; **Release-APK bei Git-Tag `v*`** als GitHub Release | Siehe unten |

Kein Dark Mode (Marke ist weiss/türkis), keine Tablet-Sonderlayouts (funktioniert, aber nicht optimiert),
nur Hochformat für Scanner-Screen (Rest frei).

## Module und Schichten

Ein Gradle-Modul `app` mit klarer Paketstruktur (Feature-first):

```
app/src/main/java/com/wallee/terminallinker/
├── TerminalLinkerApp.kt          # Application: AppContainer, Locale
├── di/AppContainer.kt            # baut Client, Repos, Stores (manuelle DI)
├── core/
│   ├── api/
│   │   ├── WalleeClient.kt       # request<T>(method, path, query, body), Fehlermapping
│   │   ├── Jwt.kt                # HS256-Token pro Request
│   │   ├── ApiError.kt           # WalleeApiException(status, code, message, fieldErrors)
│   │   └── dto/                  # Space, PaymentTerminal, SearchResponse …
│   ├── auth/CredentialStore.kt   # EncryptedSharedPreferences: userId, authKey
│   ├── prefs/UiPrefs.kt          # DataStore: activeSpaceId, Sortierung, Sprache, letzte Spaces
│   ├── serial/SerialNumber.kt    # Parser/Normalizer für gescannte Werte (siehe 03)
│   └── ui/                       # Theme, Tokens, Basis-Komponenten (WButton, WInput, Headline …)
├── feature/
│   ├── setup/                    # Credentials erfassen, Verbindungstest
│   ├── spaces/                   # Space-Liste laden, Space-Dropdown im Header
│   ├── terminals/                # Liste, Suche, Sortierung, Detail
│   ├── link/                     # Link / Replace / Unlink Flows inkl. Bestätigung und Ergebnis
│   └── scan/                     # CameraX-Preview, ML-Kit-Analyzer, Torch, manuelle Eingabe
└── navigation/AppNavGraph.kt
```

Schichtregel: Composables → ViewModel (StateFlow `UiState`) → Repository (`TerminalRepository`,
`SpaceRepository`) → `WalleeClient`. ViewModels kennen keine Android-Views, Repositories keine UI.

## Datenhaltung

| Was | Wo | Hinweis |
|---|---|---|
| Application User ID, Authentication Key | `EncryptedSharedPreferences` (`tl.credentials`) | Ein Credential-Satz pro App-Installation (ein Application User hat typischerweise Zugriff auf mehrere Spaces). Wechsel = neu einrichten. |
| Aktiver Space, Sprache, Sortierung/Filter der Liste, «zuletzt genutzte Spaces» | DataStore `tl.ui` | Unkritisch |
| Letzte Terminalliste je Space | In-Memory im Repository (+ optional DataStore-JSON für Schnellstart) | Nur Anzeige-Cache; Aktionen laden immer frisch |
| Manuell erfasste Space-IDs (Fallback, siehe 02 §3) | DataStore `tl.ui.manualSpaces` | Nur wenn `GET /spaces` für den Application User nicht funktioniert |

Kein Room, keine SQLite.

## Sicherheit

- Der Authentication Key verlässt das Gerät nie; er dient nur zum Signieren des JWT. Signieren passiert im
  Prozess der App, nie in einem Backend von uns.
- `android:allowBackup="false"`, `android:usesCleartextTraffic="false"`, Network Security Config nur für
  `app-wallee.com` (kein User-CA-Vertrauen im Release-Build).
- Logging: OkHttp-Interceptor nur im Debug-Build, mit Redaction des `Authorization`-Headers.
- Keine Analytics, keine Crash-Reporter mit Netzwerkzugang in v1.
- Der Scanner liest nur Text aus dem Barcode; er öffnet keine URLs und führt nichts aus.

## Berechtigungen (Manifest)

- `android.permission.INTERNET`
- `android.permission.CAMERA` (Runtime-Permission; Rationale-Dialog im Scan-Screen; Fallback manuelle Eingabe)
- `<uses-feature android:name="android.hardware.camera.any" android:required="false"/>`

## Build & Release

### Lokal

```bash
./gradlew assembleDebug                  # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Signing (Release)

Der Release-Keystore liegt **nicht** im Repo. `app/build.gradle.kts` liest ihn aus Umgebungsvariablen:

```
TL_KEYSTORE_BASE64   # Keystore-Datei base64-kodiert
TL_KEYSTORE_PASSWORD
TL_KEY_ALIAS
TL_KEY_PASSWORD
```

Fehlen sie, wird `assembleRelease` mit dem Debug-Key signiert und im Build-Log als «unsigned/debug-signed»
markiert (für lokale Tests). Keystore einmalig erzeugen:

```bash
keytool -genkeypair -v -keystore terminal-linker.jks -alias terminallinker \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -i terminal-linker.jks | pbcopy   # → GitHub Secret TL_KEYSTORE_BASE64
```

Den Keystore an einem sicheren Ort (Passwortmanager) aufbewahren — ohne ihn kann eine installierte App
nicht mehr per Update ersetzt werden.

### GitHub Actions

- `.github/workflows/ci.yml`: bei Push/PR → `./gradlew ktlintCheck test assembleDebug`, Debug-APK als
  Workflow-Artefakt (7 Tage).
- `.github/workflows/release.yml`: bei Tag `v*` → Keystore aus Secrets entpacken, `assembleRelease`,
  `versionName` aus dem Tag, `versionCode` = laufende Nummer (Anzahl Tags oder `github.run_number`),
  APK `wallee-terminal-linker-<version>.apk` + `SHA256SUMS.txt` als GitHub Release anhängen.
- Installation beim Nutzer: APK aus dem Release herunterladen, «Installation aus unbekannten Quellen»
  für den Browser erlauben, öffnen. Kein Play Store in v1. Beschreibung in `README.md`.

### In-App-Update-Hinweis

Beim Start (max. 1× pro Tag) `GET https://api.github.com/repos/<owner>/<repo>/releases/latest` (ohne Auth,
Rate-Limit 60/h reicht). Ist `tag_name` neuer als `versionName` → dezenter Banner «Version x.y verfügbar» mit
Link auf die Release-Seite. Kein automatischer Download.

## Versionierung

SemVer; `versionName` = Tag ohne `v`. Changelog in `CHANGELOG.md` (Keep-a-Changelog).
