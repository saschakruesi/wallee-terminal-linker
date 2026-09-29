# wallee Terminal Linker

Android-App, um physische Zahlterminals mit den Payment-Terminals in Ihrem wallee-Space zu verbinden —
vor Ort, mit der Kamera, in unter einer Minute.

Für Techniker, Installationspartner und Support: Space wählen, Terminal in der Liste finden, Seriennummer
auf der Rückseite des Geräts scannen, fertig.

## Was die App kann

- **Terminals im Space sehen:** Liste aller Payment Terminals mit Status (gelinkt / ungelinkt), Suche nach
  Name, Terminal-ID oder Seriennummer, Filter und Sortierung («Ungelinkt zuerst»).
- **Gerät linken:** Ungelinktes Terminal öffnen → «Gerät linken» → Barcode/QR auf der Geräterückseite
  scannen → bestätigen. Ohne Kamera geht die Seriennummer auch von Hand.
- **Gerät ersetzen:** Bei einem defekten Gerät das alte trennen und das neue im selben Ablauf verknüpfen.
- **Gerät trennen:** Ein Terminal ohne Gerät freigeben.
- **Terminal pflegen:** Umbenennen, Zustand vom Gerät aktualisieren, Konfiguration an das Gerät schicken.
- **Mehrere Spaces:** Oben links zwischen allen Spaces wechseln, auf die Ihr Application User Zugriff hat.
  Nicht benötigte Spaces lassen sich in den Einstellungen aus der App entfernen (und wiederherstellen).
- **Schnellaktion:** Ein ungelinktes Terminal in der Liste lange drücken öffnet direkt den Scanner.
- **Einstellungen:** Sprache (System / Deutsch / English), stillgelegte Terminals anzeigen, Update-Hinweis,
  Lizenzen, alle lokalen Daten löschen.

Die App führt keine Zahlungen aus und löscht nie Terminals.

## Installation (APK)

1. Unter [Releases](https://github.com/saschakruesi/wallee-terminal-linker/releases) die neueste
   `wallee-terminal-linker-<version>.apk` auf das Android-Gerät laden (Android 8.0 oder neuer).
2. Beim Öffnen fragt Android, ob Apps aus dieser Quelle (Browser/Dateimanager) installiert werden dürfen —
   einmalig erlauben.
3. App öffnen und wallee verbinden (unten).

Updates: Die App prüft einmal täglich, ob es ein neueres Release gibt, und zeigt einen Hinweis über der
Terminalliste; das neue APK einfach über das alte installieren (Einstellungen bleiben erhalten). Die App
lädt nie selbst etwas herunter.

Prüfsumme: Jedes Release enthält `SHA256SUMS.txt`. Wer die Datei vor der Installation prüfen will:

```bash
sha256sum -c SHA256SUMS.txt
```

## Einrichtung in wallee (einmalig)

1. **Application User anlegen:** wallee-Backend → Account → Users → Application Users → «Create».
   **User ID** und **Authentication Key** notieren (der Key wird nur einmal angezeigt).
2. **Berechtigung:** Dem Application User in jedem Space, in dem Terminals gelinkt werden sollen, eine Rolle
   mit Rechten auf **Payment Terminals** (Lesen und Schreiben) geben.
3. In der App User ID und Key eingeben → «Verbindung testen & speichern» — oder auf das QR-Symbol tippen
   und den Zugangsdaten-QR-Code scannen (sobald das wallee-Portal ihn anbietet). Die App zeigt die gefundenen
   Spaces; fehlt einer, kann seine Space-ID manuell ergänzt werden.

Zugangsdaten werden ausschliesslich verschlüsselt auf dem Gerät gespeichert und nie an andere Server als
`app-wallee.com` gesendet.

## Häufige Fragen

- **Die Kamera erkennt den Code nicht.** Taschenlampe einschalten, Gerät 15–25 cm entfernt ruhig halten,
  Code in den Rahmen legen. Sonst «Seriennummer manuell eingeben».
- **«Zugangsdaten ungültig» trotz richtigem Key.** Uhrzeit des Telefons prüfen (automatische Zeit
  aktivieren) — die Anmeldung ist zeitbasiert.
- **«Keine Berechtigung in diesem Space».** Der Application User braucht im Space eine Rolle mit
  Payment-Terminal-Rechten.
- **wallee meldet, die Seriennummer sei unbekannt oder bereits verwendet.** Das Gerät ist entweder nicht für
  Ihren Account registriert oder noch mit einem anderen Terminal verknüpft — dort zuerst trennen.

- **Ein Space fehlt in der Liste.** wallee liefert nur die ersten zehn Spaces; weitere über «Space-ID
  manuell hinzufügen» ergänzen. Versehentlich entfernte Spaces stehen in den Einstellungen unter
  «Entfernte Spaces».

## Entwicklung

Kotlin, Jetpack Compose, CameraX, ML Kit. Spezifikation und Arbeitsanweisungen in `CLAUDE.md` und `docs/`.

```bash
./gradlew assembleDebug test ktlintCheck     # Build, Unit-Tests, Code-Style
./gradlew connectedDebugAndroidTest          # Compose-UI-Tests (Emulator oder Gerät)
```

CI (`.github/workflows/ci.yml`) führt bei jedem Push ktlint, Unit-Tests, den Debug-Build und die UI-Tests
auf einem Emulator aus.

### Release

Ein Git-Tag `vX.Y.Z` auf `main` löst `.github/workflows/release.yml` aus: signiertes APK, `SHA256SUMS.txt`
und R8-Mapping als GitHub Release; die Release-Notes kommen aus dem passenden Abschnitt in `CHANGELOG.md`.
`versionName` ist der Tag ohne `v`, `versionCode` die laufende Nummer des Workflow-Laufs.

Voraussetzung sind vier Repository-Secrets (`TL_KEYSTORE_BASE64`, `TL_KEYSTORE_PASSWORD`, `TL_KEY_ALIAS`,
`TL_KEY_PASSWORD`); den Keystore einmalig erzeugen wie in `docs/01-architektur.md` §Signing beschrieben.
Ohne die Secrets bricht der Workflow ab, ein Debug-signiertes APK wird nie veröffentlicht.

```bash
git tag -a v1.0.0 -m "v1.0.0" && git push origin v1.0.0
```

## Lizenz

MIT (Code). Roboto: Apache License 2.0. Google ML Kit: [ML Kit Terms](https://developers.google.com/ml-kit/terms).
Die vollständige Liste der Open-Source-Komponenten zeigt die App unter Einstellungen → Lizenzen; sie wird
beim Build aus den Abhängigkeiten erzeugt. wallee-Logo und Marke: © wallee Group AG, nur für diese App.
