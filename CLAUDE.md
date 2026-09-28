# wallee Terminal Linker — Anweisungen für Claude Code

Dieses Repository enthält den **wallee Terminal Linker**: eine kleine native Android-App für Techniker,
Installateure und Support-Mitarbeitende, um physische Zahlterminals (PAX-Geräte) mit den in wallee
angelegten Payment-Terminals zu verbinden («linken»), zu ersetzen («replace») und zu trennen («unlink»).

Kernablauf: Space wählen → Terminal in der Liste finden → «Linken» → Kamera scannt die Seriennummer
(Barcode/QR auf der Rückseite des Geräts) → Bestätigen → wallee verknüpft Gerät und Terminal.

Zugangsdaten (Application User ID, Authentication Key, optional Space-IDs) werden von Hand eingegeben
**oder** per QR-Code gescannt (`wallee-app-user://v1…`, Format in `docs/02 §5`). Das Portal erzeugt diesen
QR-Code noch nicht — die App ist die Referenzimplementierung des Formats.

**Lies vor jeder Arbeit `docs/05-implementationsplan.md`** — dort steht, welche Phase aktuell ist und
welche Akzeptanzkriterien gelten. Die anderen Dokumente sind die verbindliche Spezifikation:

| Dokument | Inhalt |
|---|---|
| `docs/01-architektur.md` | Technologie-Entscheid, Module, Ordnerstruktur, Sicherheit, Build & Release (APK) |
| `docs/02-wallee-api.md` | JWT-Auth, verwendete Endpunkte (Spaces, Terminals, Link/Unlink), Fehlerbehandlung |
| `docs/03-ui-flows.md` | Screens, Navigation, Flows für Setup, Space-Wahl, Terminalliste, Link, Replace, Unlink |
| `docs/04-design-system.md` | wallee-Design für Android/Compose: Farben, Typografie, Logo, Komponenten |
| `docs/05-implementationsplan.md` | Phasen, Reihenfolge, Definition of Done, Testplan |

## Harte Regeln

- **Kotlin + Jetpack Compose, nativ.** Kein Flutter, kein WebView, kein React Native. Abhängigkeiten nur,
  wenn in `docs/01-architektur.md` genannt (CameraX, ML Kit Barcode Scanning, OkHttp, kotlinx.serialization,
  Navigation Compose, DataStore, Security-Crypto). Keine Hilt/Dagger-Einführung — manuelle DI über einen
  `AppContainer` reicht für diese App-Grösse.
- **Nur wallee API v2.0 mit JWT** (`https://app-wallee.com/api/v2.0`). Die alte API v1
  (`/api/...` mit HMAC-SHA512-`x-mac-*`-Headern) ist deprecated und wird nicht verwendet.
- **Credentials bleiben auf dem Gerät.** Application User ID und Authentication Key liegen ausschliesslich in
  `EncryptedSharedPreferences` (Android Keystore). Nie in Logs, nie in Crash-Reports, nie in URLs, nie im Repo.
  Der Authentication Key wird in der UI nach dem Speichern maskiert — auch im Bestätigungs-Sheet nach einem
  QR-Scan; der Rohtext eines Credential-QR wandert nie in Navigation-Args, Prefs oder Logs. Das Repo ist **public** — es darf nie
  ein echter Key, eine Space-ID eines Kunden oder ein Keystore eingecheckt werden (`.gitignore` prüfen).
- **Nichts Destruktives ohne Bestätigung.** `unlink` und `replace` (= unlink + link) verlangen einen
  Bestätigungsdialog, der den Terminalnamen und die aktuelle Geräte-Seriennummer nennt. `DELETE /payment/terminals/{id}`
  wird **nie** aufgerufen.
- **wallee-Design ist Pflicht** (`docs/04-design-system.md`): Roboto, Türkis `#11D9CC` als Fläche, Text
  schwarz/dunkelgrau, kein Türkis als Textfarbe auf Weiss, Wortmarke oben rechts, ruhiger Look, kein Dark Mode.
  Keine Material-3-Standardoptik durchscheinen lassen (keine lila Akzente, keine Tonal-Elevation-Verläufe,
  keine Standard-FAB-Formen).
- **Sprache:** UI-Texte über Android-Ressourcen (`values/strings.xml` = Deutsch, Schweizer Rechtschreibung
  «ss», nie «ß»; `values-en/strings.xml` = Englisch). Code, Kommentare, Commit-Messages auf Englisch.
- **Offline-tolerant, nicht offline-fähig.** Ohne Netz zeigt die App den Zustand klar an; sie cached nur die
  zuletzt geladene Terminalliste pro Space für die Anzeige, nie Credentials im Klartext.
- **Nichts erfinden:** Wenn ein Endpunkt oder Feld nicht in `docs/02-wallee-api.md` oder der OpenAPI-Spec
  `https://app-wallee.com/api/spec3.json` steht, nachfragen statt raten.

## Kommandos

```bash
./gradlew assembleDebug            # Debug-APK → app/build/outputs/apk/debug/
./gradlew assembleRelease          # signiertes Release-APK (braucht Keystore-Env, siehe docs/01)
./gradlew test                     # Unit-Tests (JWT, Query-Builder, Serial-Parser, Sortierung)
./gradlew connectedAndroidTest     # Instrumented Tests (optional, Emulator)
./gradlew ktlintCheck              # Code-Style
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Arbeitsweise

- Jede Phase aus dem Implementationsplan als eigener Branch/PR, kleine Commits, Conventional Commits.
- Vor dem Abschluss einer Phase: `./gradlew assembleDebug test ktlintCheck` sauber, und der manuelle Test aus
  dem Plan ist mit einem wallee **Test-Space** und einem echten Testterminal durchgespielt.
- Bei Unklarheiten in der API zuerst `docs/02-wallee-api.md`, dann die OpenAPI-Spec konsultieren.
- Die Kamera-/Scan-Logik ist gekapselt (`feature/scan`), damit sie ohne API testbar bleibt und der
  Link-Flow auch mit manueller Eingabe der Seriennummer funktioniert.
