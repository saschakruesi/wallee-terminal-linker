# Changelog

Alle nennenswerten Änderungen dieser App. Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [SemVer](https://semver.org/lang/de/). Die Versionsnummer entspricht dem Git-Tag ohne `v`.

## [Unreleased]

## [1.0.1] – 2026-09-30

### Behoben
- Die Liste der Open-Source-Komponenten (Einstellungen → Lizenzen) fehlte im veröffentlichten APK von 1.0.0,
  weil sie bei einem sauberen Build nicht erzeugt wurde. Sie wird jetzt für jede Variante vor dem Packen erzeugt.

## [1.0.0] – 2026-09-29

Erste veröffentlichte Version.

### Hinzugefügt
- Einrichtung mit Application User ID und Authentication Key, Verbindungstest gegen wallee, Zugangsdaten
  verschlüsselt auf dem Gerät; optional per QR-Code (`wallee-app-user://v1…`).
- Space-Auswahl über den Chip oben links (zuletzt verwendet, alle, manuell per Space-ID); Spaces lassen sich
  aus der App entfernen und wiederherstellen.
- Terminalliste des aktiven Space mit Suche, Filter (Alle / Ungelinkt / Gelinkt), Sortierung und
  Pull-to-Refresh; Terminal-Detail mit Faktentabelle, Umbenennen, Aktualisieren, Konfiguration auslösen.
- Kamera-Scanner (CameraX + ML Kit) für die Seriennummer auf der Geräterückseite, mit Taschenlampe,
  Tap-to-Focus und manueller Eingabe als Fallback.
- Seriennummern-Plausibilität nach Länge: 10 Ziffern (PAX) oder 8 Zeichen (FEIG Device-ID) gelten als vertraut,
  alles andere zeigt im Bestätigungs-Sheet einen Hinweis; bei mehreren Codes im Sucher wird der vertraute bevorzugt.
- Gerät linken, ersetzen und trennen mit Bestätigung, Fortschritt, ehrlichem Teilfehler-Zustand und
  Ergebnis-Screen; Long-Press auf ein ungelinktes Terminal öffnet den Scanner direkt.
- Einstellungen: Zugangsdaten, Spaces, Sprache (System / Deutsch / English), stillgelegte Terminals anzeigen,
  Update-Hinweis via GitHub Releases, Lizenzen, alle lokalen Daten löschen.
- Deutsch (Schweiz) und Englisch.

### Sicherheit
- Nur wallee API v2.0 mit JWT (HS256) pro Request; der Authentication Key verlässt das Gerät nie.
- Keine Analytics, keine Crash-Reporter; Netzwerk nur zu `app-wallee.com` und `api.github.com`.

[Unreleased]: https://github.com/saschakruesi/wallee-terminal-linker/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/saschakruesi/wallee-terminal-linker/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/saschakruesi/wallee-terminal-linker/releases/tag/v1.0.0
