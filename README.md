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

Die App führt keine Zahlungen aus und löscht nie Terminals.

## Installation (APK)

1. Unter **Releases** die neueste `wallee-terminal-linker-<version>.apk` auf das Android-Gerät laden
   (Android 8.0 oder neuer).
2. Beim Öffnen fragt Android, ob Apps aus dieser Quelle (Browser/Dateimanager) installiert werden dürfen —
   einmalig erlauben.
3. App öffnen und wallee verbinden (unten).

Updates: Die App weist auf neue Versionen hin; das neue APK einfach über das alte installieren
(Einstellungen bleiben erhalten).

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

## Entwicklung

Kotlin, Jetpack Compose, CameraX, ML Kit. Spezifikation und Arbeitsanweisungen in `CLAUDE.md` und `docs/`.

```bash
./gradlew assembleDebug test
```

Release-Builds werden von GitHub Actions bei einem Tag `v*` signiert und als Release veröffentlicht
(siehe `docs/01-architektur.md`).

## Lizenz

MIT (Code). Roboto: Apache License 2.0. wallee-Logo und Marke: © wallee Group AG, nur für diese App.
