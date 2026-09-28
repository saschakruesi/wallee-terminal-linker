# 05 — Implementationsplan

Sechs Phasen, jede mit «Definition of Done». Reihenfolge einhalten; nach Phase 4 ist die App im Feld
einsetzbar (Link/Replace/Unlink), Phase 5 bringt die Komfortfunktionen, Phase 6 den Release.

**Aktuelle Phase: 3 — Terminalliste und Detail.** Phase 1 und 2 sind abgenommen und auf `main` gemergt (Setup mit Verbindungstest, Space-Auswahl, Credential-QR-Parser; Kamera-Scanner folgt in Phase 4). Phase 3 entsteht auf Branch `phase-3-terminals`. Diese Zeile bei jedem Phasenabschluss aktualisieren.

Voraussetzungen: Android Studio (aktuell), JDK 17, ein wallee **Test-Space** mit Application User (Rolle mit
Payment-Terminal-Rechten in ≥ 2 Spaces), mindestens ein Payment Terminal im Zustand `ACTIVE` ohne Gerät und
ein physisches PAX-Testgerät. Credentials nur lokal in `local.properties` (gitignored) für
Integrationstests, nie im Repo.

---

## Phase 1 — Gerüst, Theme, Komponenten

**Ziel:** Ein installierbares Debug-APK, das eine leere, aber fertig gestylte App im wallee-Look zeigt.

1. Projekt anlegen: `app`-Modul, Kotlin DSL, Version Catalog, minSdk 26, targetSdk 35, Compose BOM,
   ktlint. `.gitignore` (inkl. `local.properties`, `*.jks`, `*.keystore`, `/app/release`).
2. `core/ui`: `WalleeTheme` (Farben, Typografie, Shapes, Tonal Elevation 0), Roboto-Fonts in `res/font`,
   Logos als VectorDrawables, Icon-Set als VectorDrawables, Adaptive App-Icon.
3. Komponenten gemäss `docs/04`: WPrimaryButton, WSecondaryButton, WTextButton, WInput (inkl. Passwort),
   WSegmented, WBadge, WListRow, WFactTable, Headline, WHeader, SpaceChip, WBottomSheet, WDialog, WToast,
   TurquoisePanel, Spinner.
4. Navigation Compose mit allen Routen als Platzhalter; `Styleguide`-Route mit allen Komponenten.
5. i18n: `values/strings.xml` (de-CH), `values-en/strings.xml`. Kein hartkodierter UI-Text.
6. GitHub Actions `ci.yml` (ktlint, test, assembleDebug, APK-Artefakt).

**DoD:** `./gradlew assembleDebug` läuft in CI; APK installiert; Styleguide zeigt alle Komponenten ohne
Material-Standardoptik (Review gegen `docs/04`: kein Lila, keine Schatten, Roboto sichtbar, Wortmarke
oben rechts); TalkBack liest alle Steuerelemente.

## Phase 2 — Setup, wallee-Client, Space-Auswahl

1. `core/api/Jwt.kt` mit Unit-Test gegen den bekannten Vektor (aus dem Virtual Terminal übernehmen).
2. `core/api/WalleeClient.kt` (OkHttp, Signierung, Query-Builder, `expand`, Fehlerklasse, 204-Handling),
   DTOs `Space`, `PaymentTerminal` + Teilobjekte, `SearchResponse<T>`. Tests mit MockWebServer
   (Pfad+Query im JWT korrekt, Fehlermapping, `space`-Header).
3. `core/auth/CredentialStore.kt` (EncryptedSharedPreferences), `core/prefs/UiPrefs.kt` (DataStore).
4. `feature/setup`: Formular, Verbindungstest (`GET /spaces`, Fallback manuell mit `GET /spaces/{id}`),
   Speichern, Maskierung, Fehlertexte; «Merken»-Checkbox.
5. `feature/spaces`: `SpaceRepository` (automatisch/manuell vereinheitlicht), Space-Chip im Header,
   Bottom Sheet mit Suche, «Zuletzt verwendet», manuelles Hinzufügen.
6. **Offenen Punkt klären:** Liefert `GET /spaces` für den Application User die Spaces mit Rolle?
   Ergebnis in `docs/02 §3.1` festhalten.
7. `core/auth/CredentialQr.kt` (Parser für `wallee-app-user://v1…` und JSON-Form) mit Unit-Tests; QR-Icon-Button
   im Setup und in den Einstellungen. Der Scanner-Screen selbst kommt in Phase 4 — bis dahin ist der Button
   mit einem Platzhalter verdrahtet, der den Parser über ein Textfeld (Debug-Build) testbar macht.
   Test-QR-Codes für den Test-Space lokal erzeugen (z.B. `qrencode`), **nicht** ins Repo.

**DoD:** Mit echten Test-Credentials zeigt der Verbindungstest die Spaces; falsche Credentials liefern
verständliche Fehler; App-Neustart behält Credentials und aktiven Space; Space-Wechsel im Sheet funktioniert;
Unit-Tests JWT/Client/Store grün.

## Phase 3 — Terminalliste und Detail

1. `TerminalRepository`: Laden aller Terminals eines Space (Pagination bis `hasMore = false`, Cap 1000),
   In-Memory-Cache je Space, `refreshAll()`, `get(id)`. Nötige `expand`-Parameter empirisch bestimmen
   (Standort-/Konfigurationsname) und in `docs/02 §3.2` nachtragen.
2. `feature/terminals/list`: Suche (lokal), Segmented-Filter mit Zählern, Sortierung (Ungelinkt zuerst,
   Name, ID, Standort, Aktiviert), Pull-to-Refresh, Leerzustände, Statuszeile. Sortier-/Filterlogik als reine
   Kotlin-Funktionen mit Tests.
3. `feature/terminals/detail`: Faktentabelle, Badges, Aktionsleiste (Buttons noch ohne Scan: Link führt zur
   manuellen Eingabe), Overflow mit «Aktualisieren», «Konfiguration auslösen», «Terminal-ID kopieren»,
   Umbenennen inline (`PATCH` mit `version`, 409-Handling).
4. Stillgelegte Terminals ausblenden (Schalter in Einstellungen).

**DoD:** Ein Space mit > 100 Terminals lädt vollständig; Suche und Filter reagieren sofort; Sortierung
«Ungelinkt zuerst» stimmt; Detail zeigt alle Felder; Umbenennen, Aktualisieren und Konfiguration auslösen
funktionieren im Test-Space; Unit-Tests für Filter/Sortierung grün.

## Phase 4 — Scanner, Link, Replace, Unlink (Kernstück)

1. `feature/scan`: CameraX-Preview, ML-Kit-Analyzer mit ROI, Formatliste, Stabilitätsprüfung (2 gleiche
   Treffer), Torch, Tap-to-Focus, Permission-Handling mit Rationale, manuelle Eingabe. Gekapselt als
   `ScannerScreen(mode, onResult: (String) -> Unit)` — ohne API-Abhängigkeit. Modus `CREDENTIALS`: nur
   `QR_CODE`, quadratischer Sucher, Ergebnis geht an `CredentialQr.parse` und das Bestätigungs-Sheet aus
   `docs/03` (Key nie im Klartext); Rohwert nie in Navigation-Args.
2. `core/serial/SerialNumber.kt` (`parse(raw): Result<String>`) mit Tests (Code128 pur, `SN:`-Präfix,
   QR mit mehreren Feldern, URL, Müll).
3. Bestätigungs-Sheet (S/N gross, Terminalname), Progress-Overlay, `LinkFlowViewModel` mit Zuständen
   `Idle → Scanning → Confirm(serial) → Working(step) → Done(outcome) | Failed(step, error, serial)`.
4. Aktionen: `link`, `unlink`, Replace als Sequenz mit Teilfehler-Handling gemäss `docs/03`; nach jeder
   Aktion `GET` nachladen; Bestätigungsdialoge für Unlink/Replace.
5. `Result`-Screen mit Türkis-Fläche, «Zurück zur Liste», «Nächstes Terminal linken».
6. Haptik, TalkBack-Ansage beim Treffer.
7. Mit realen Geräten: Barcode-Formate/-Inhalte auf den eingesetzten PAX-Modellen sammeln, als Testfälle
   hinterlegen; die Fehlermeldungen von `link` (422/409) im Test-Space provozieren und Klartexte ableiten.

**DoD:** Ein ungelinktes Terminal wird per Kamera-Scan in < 60 s gelinkt und die Liste zeigt es als
«Gelinkt» mit S/N; Replace tauscht ein Gerät aus (beide S/N im Ergebnis sichtbar); Unlink trennt; Teilfehler
bei Replace zeigt den ehrlichen Zustand mit «Erneut scannen»; manuelle Eingabe funktioniert ohne Kamera;
falsche S/N liefert die wallee-Meldung im Klartext; Kamera-Permission abgelehnt → manueller Pfad.

## Phase 5 — Komfort & Feinschliff

1. «Nächstes Terminal linken» springt in die Liste mit Filter «Ungelinkt»; Long-Press-Schnellaktion.
2. Einstellungen komplett (Sprache, Spaces-Verwaltung, Lizenzen, Daten löschen).
3. Update-Hinweis via GitHub Releases API (1×/Tag, dezenter Banner).
4. Zustandserhalt bei Prozess-Tod (`SavedStateHandle` für Filter/Suche, aktiver Space aus DataStore).
5. Netzwerk-Robustheit: Backoff bei 429 (GET), klare Offline-Panels, alle Aufrufe abbrechbar.
6. Compose-UI-Tests für Setup, Liste (Filter/Suche) und Link-Flow (mit gemocktem Scanner).

**DoD:** Alle Screens in DE und EN ohne abgeschnittene Texte (Schriftgrösse 1.3× getestet); Prozess-Tod im
Scanner/Detail verliert keinen Zustand ausser dem laufenden Scan; UI-Tests grün in CI.

## Phase 6 — Release

1. `release.yml`: Tag `v*` → Keystore aus Secrets → `assembleRelease` → APK + `SHA256SUMS.txt` als GitHub
   Release; `versionName` aus Tag, `versionCode` aus `github.run_number`.
2. `README.md` für Nutzer: Was die App kann, Installation des APK (unbekannte Quellen), Einrichtung des
   Application Users mit Rolle, Bedienung Link/Replace/Unlink, FAQ (Kamera erkennt nichts → Torch/manuell;
   401 → Uhrzeit; 403 → Rolle).
3. `CHANGELOG.md`, Lizenzhinweise (Roboto Apache 2.0, ML Kit Terms, OSS-Liste über Gradle-Plugin).
4. Abnahmetest T1–T14 (unten) mit Test-Space und Testgerät auf zwei Android-Versionen (26 und aktuell).

**DoD:** `v1.0.0`-Release auf GitHub mit signiertem APK; Installation auf einem frischen Gerät und
Durchlauf T1–T14 ohne Befund.

---

## Abnahmetest (manuell, Test-Space)

| # | Test | Erwartung |
|---|---|---|
| T1 | Frische Installation, falsche User-ID | Klartext-Fehler 401, keine Weiterleitung |
| T2 | Richtige Credentials | Spaces gefunden, Liste des ersten aktiven Space |
| T3 | Space wechseln im Sheet | Headline und Liste wechseln, Filter bleibt |
| T4 | Suche «WT-» | Trefferliste sofort, Zähler stimmen |
| T5 | Filter «Ungelinkt», Sortierung Name | Nur ungelinkte, alphabetisch |
| T6 | Detail eines ungelinkten Terminals → «Gerät linken» → Scan | S/N erkannt, Bestätigung, «Gelinkt» mit S/N |
| T7 | Gleiche S/N an zweitem Terminal linken | wallee-Fehler im Klartext, kein Zustandswechsel |
| T8 | Gelinktes Terminal → «Gerät ersetzen» → Scan neues Gerät | Zwei Schritte sichtbar, «Gerät ersetzt» mit alter und neuer S/N |
| T9 | Replace mit ungültiger neuer S/N | Teilfehler-Panel «Altes Gerät getrennt…», «Erneut scannen» führt zum Erfolg |
| T10 | «Gerät trennen» | Dialog, danach «Ungelinkt» in Liste und Detail |
| T11 | Flugmodus während Liste laden / während Link | Offline-Panel bzw. «Zustand unklar» + Nachladen nach Netzrückkehr |
| T12 | Kamera-Permission verweigern | Manuelle Eingabe, Link gelingt |
| T13 | Setup → QR-Icon → lokal erzeugten Credential-QR (mit Space-IDs) scannen | Bestätigungs-Sheet ohne Klartext-Key, Verbindungstest läuft, erster Space aus dem Code ist aktiv |
| T14 | Fremden QR-Code (z.B. Website-URL) im Credential-Modus scannen | «Kein wallee-Zugangsdaten-Code», nichts gespeichert |

## Offene Punkte für wallee (intern klären)

- ~~Verhalten von `GET /spaces` für Application Users (Phase 2).~~ Geklärt: liefert die Spaces, ist aber langsam (≈ 6 s für 10 Spaces, Timeout bei 100) — siehe `docs/02 §3.1`.
- Exakte Fehlercodes/-texte von `POST /payment/terminals/{id}/link` bei unbekannter oder bereits
  verwendeter Seriennummer (Phase 4).
- Ob nach `link` zusätzlich ein Aktivierungsschritt am Gerät nötig ist (Aktivierungscode, `activation-status`)
  — falls ja, Anzeige des Codes im Ergebnis-Screen (v1.1).
- Barcode-Inhalt/-Format je PAX-Modell (Phase 4, Sammlung realer Beispiele).
