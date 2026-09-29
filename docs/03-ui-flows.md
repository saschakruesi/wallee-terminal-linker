# 03 — UI & Flows

Leitidee: **Vom App-Start bis «Gelinkt» in unter 60 Sekunden, mit einer Hand.** Der Scan ist der Hauptpfad,
die manuelle Eingabe immer einen Tipp entfernt. Jede Aktion mit Wirkung auf wallee wird vorher benannt und
nachher am Serverzustand bestätigt. Zielgruppe: Techniker und Support — Leute, die wissen, was ein Terminal
ist, aber keine Zeit für Menüs haben.

## Layout (alle Screens ausser Scanner und Setup)

```
┌──────────────────────────────────────────────┐
│ [Hotel Muster – Rezeption ▾]         [wallee] │  ← Header 56 dp, weiss, Haarlinie unten
├──────────────────────────────────────────────┤
│  Terminals                                    │  ← Headline Zeile 1: Light, grau
│  Hotel Muster – Rezeption                     │  ← Headline Zeile 2: Medium, schwarz
│                                               │
│  Inhalt …                                     │
│                                               │
└──────────────────────────────────────────────┘
```

- **Header:** links der **Space-Chip** (aktiver Space-Name, Chevron), rechts die Wortmarke
  (`wallee_logo_turquoise`, Höhe 22 dp, Schutzraum ≥ 22 dp). Auf Unterseiten (Detail, Einstellungen)
  ersetzt ein Zurück-Pfeil links den Space-Chip; der Space-Name steht dann in der Headline.
- **Headline-Pattern** (wallee-Signatur) auf jeder Seite oben links, zweizeilig, gleich gross, Hierarchie
  über Gewicht/Farbe.
- Seitenrand 20 dp, Basisraster 8 dp. Keine Bottom-Navigation (die App hat einen Hauptscreen), kein
  Hamburger-Menü. Einstellungen über das Zahnrad-Symbol im Space-Sheet oder im Overflow der Liste.
- System-Statusleiste hell (dunkle Icons), Navigationsleiste weiss.

## Navigation (Navigation Compose, type-safe)

| Route | Screen | Bedingung |
|---|---|---|
| `Setup` | Einrichtung (Credentials) | Start, wenn keine Credentials; sonst über Einstellungen |
| `Terminals` | Terminalliste des aktiven Space (Home) | Credentials + aktiver Space |
| `TerminalDetail(id)` | Detail mit Aktionen | |
| `Scan(mode: LINK \| REPLACE \| CREDENTIALS, terminalId?)` | Kamera-Scanner + Bestätigung (Terminal-S/N oder Credential-QR) | |
| `Result(terminalId, outcome)` | Ergebnisfläche nach Link/Replace/Unlink | |
| `Settings` | Einstellungen | |

Zurück-Verhalten: Vom Result «Zurück zur Liste» leert den Stack bis `Terminals`. System-Back im Scanner
bricht den Scan ab (kein API-Call passiert vor der Bestätigung).

## Screen: Einrichtung (`Setup`)

Kein Header. Oberes Drittel **Voll-Türkis-Fläche** mit weisser Wortmarke gross (Höhe 36 dp) und darunter
`#homeofpayment` in Roboto Light, schwarz. Darunter auf Weiss:

Headline: «Einrichtung» / «wallee verbinden».

Über dem Formular eine Zeile mit Abschnittstitel «Zugangsdaten» links und rechts einem **QR-Icon-Button**
(44 dp, Sekundär-Look, Icon `ic_qr_scan` + Label «QR scannen»). Zwei gleichwertige Wege, dieselben Daten:

**Weg A — von Hand:**

1. **Application User ID** (Zahl, `imeOptions=next`, numerische Tastatur)
2. **Authentication Key** (Passwortfeld mit Auge-Toggle, Einfügen aus Zwischenablage erlaubt; nach dem
   Speichern maskiert `••••••••` mit Textlink «ändern»)
3. Kurzer Hilfetext (13 dp grau): «Application User im wallee-Backend unter Account → Users → Application
   Users anlegen. Er braucht in jedem Space eine Rolle mit Rechten auf Payment Terminals.» + Link «Anleitung».
4. Primärbutton **«Verbindung testen & speichern»** (52 dp, volle Breite).

**Weg B — Credential-QR scannen** (Icon-Button oben rechts oder Textlink «Zugangsdaten per QR-Code
übernehmen» unter dem Formular):

1. Öffnet den Scanner (`Scan(mode=CREDENTIALS)`, gleicher Screen wie beim Terminal-Scan, Sucher quadratisch
   240×240 dp, nur Format `QR_CODE`, Hinweistext «QR-Code mit den Zugangsdaten aus dem wallee-Portal scannen»).
2. Treffer → Payload parsen (`docs/02 §5`). Ungültig/fremder QR → Sheet «Kein wallee-Zugangsdaten-Code» mit
   «Erneut scannen» / «Abbrechen».
3. Gültig → Bestätigungs-Sheet: «Zugangsdaten erkannt» — User-ID `12345`, Key `••••••••` (nie im Klartext),
   optional Space «Hotel Muster (67890)», Bezeichnung. Buttons **«Übernehmen & Verbindung testen»** /
   «Abbrechen». Übernehmen füllt das Formular (Key maskiert) und startet direkt den Verbindungstest; enthielt
   der Code eine Space-ID, wird dieser Space nach dem Test als aktiver Space gesetzt (bzw. im manuellen Modus
   hinzugefügt).
4. Waren bereits Zugangsdaten gespeichert: Dialog «Bestehende Zugangsdaten ersetzen?» vor dem Übernehmen.

Der QR-Code kann ausser dem Setup auch aus den Einstellungen (Zugangsdaten → «ändern» → QR-Icon) und aus dem
Space-Sheet («+ Space-ID manuell hinzufügen» → QR-Icon; dort werden nur Space-IDs aus dem Code übernommen,
Credentials bleiben unverändert) gescannt werden.

Hinweis im UI (12 dp grau unter dem QR-Button): «Der QR-Code wird im wallee-Portal beim Application User
erzeugt.» — das Portal-Feature ist geplant; bis dahin bleibt Weg A der Normalfall, die App ist aber bereits
vorbereitet.

Verbindungstest (siehe `02 §3.1`): Spinner im Button → bei Erfolg Liste der gefundenen Spaces kurz
eingeblendet («3 Spaces gefunden») → weiter zu `Terminals` mit dem ersten aktiven Space (bzw. dem zuletzt
genutzten). Bei 0 Spaces → Inline-Panel «Keine Spaces gefunden» mit Feld **«Space-ID manuell eingeben»** +
«Prüfen» (`GET /spaces/{id}`; Name erscheint, «Hinzufügen»). Fehler im Klartext unter dem Button (401/403/Netz).

Checkbox **«Zugangsdaten auf diesem Gerät merken»** (Standard an; aus = nur bis App-Ende im Speicher).

## Space-Auswahl (Header-Chip → Bottom Sheet)

Tipp auf den Space-Chip öffnet ein **Bottom Sheet** (nicht ein Dropdown-Menü — grössere Ziele, einhändig
erreichbar):

```
┌──────────────────────────────────────────────┐
│  Space wählen                                 │
│  [🔍 Space suchen…]        (nur bei > 8 Spaces)│
│  ZULETZT VERWENDET                            │
│  ● Hotel Muster – Rezeption     12345   ✓     │
│  ○ Hotel Muster – Bar           12346         │
│  ALLE SPACES (A–Z)                            │
│  ○ Café Seeblick                23456         │
│  ○ Praxis Dr. Beispiel          34567  inaktiv│
│  ──────────────────────────────────────────   │
│  + Space-ID manuell hinzufügen                │
│  ⚙ Einstellungen                              │
└──────────────────────────────────────────────┘
```

- Zeile: Space-Name (15 dp), Space-ID (13 dp grau), Häkchen beim aktiven Space, `inaktiv`-Label grau bei
  `state ≠ ACTIVE` (nicht wählbar).
- «Zuletzt verwendet»: max. 3, aus DataStore. Ein Space, kein Abschnitt «Zuletzt verwendet».
- Wechsel → Sheet schliesst, Headline und Liste laden neu, Suchfeld/Filter bleiben erhalten.
- «Space-ID manuell hinzufügen» → kleiner Dialog (ID, «Prüfen») — auch im automatischen Modus verfügbar
  (z.B. für einen Space, der in `GET /spaces` fehlt).

## Screen: Terminalliste (`Terminals`)

Headline: «Terminals» / «<Space-Name>».

```
┌──────────────────────────────────────────────┐
│ [🔍 Name, Terminal-ID oder Seriennummer]      │  ← Suchfeld 44 dp, Autofokus nein
│ [ Alle 12 ] [ Ungelinkt 3 ] [ Gelinkt 9 ]     │  ← Segmented (Filter), Zähler live
│ Sortierung: Ungelinkt zuerst ▾                │  ← Textbutton, öffnet kleines Menü
├──────────────────────────────────────────────┤
│ Kasse 1                         [Ungelinkt]  ›│
│ WT-8F3K2 · PAX A77 · Filiale Winterthur       │
├──────────────────────────────────────────────┤
│ Kasse 2                           [Gelinkt]  ›│
│ WT-8F3K3 · S/N 2290012345 · Filiale Winterthur│
├──────────────────────────────────────────────┤
│ Bar Terminal                     [Inaktiv]   ›│
│ WT-8F3K9 · Filiale Zürich                     │
└──────────────────────────────────────────────┘
```

- **Suche:** lokal, sofort beim Tippen, über `name`, `identifier`, `deviceSerialNumber`, Standortname;
  case-insensitive, «enthält». Leeren per ✕. Ab > 1000 Terminals Server-Suche (siehe `02 §3.2`).
- **Filter (Segmented):** Alle / Ungelinkt / Gelinkt. Der zuletzt gewählte Filter wird gemerkt.
  Terminals mit `state` `DECOMMISSIONING`/`DECOMMISSIONED` sind standardmässig ausgeblendet (Schalter in
  den Einstellungen «Stillgelegte anzeigen»).
- **Sortierung** (Menü): *Ungelinkt zuerst* (Standard; innerhalb Gruppe nach Name), *Name A–Z*,
  *Terminal-ID*, *Standort*, *Zuletzt aktiviert*. Wird gemerkt.
- **Zeile:** 64 dp, Name (15 dp Medium), Badge rechts, zweite Zeile 13 dp grau: `identifier` · Gerät
  (`deviceName`) bzw. S/N wenn gelinkt · Standort. Chevron rechts. Ganze Zeile tippbar → Detail.
- **Badges:** Ungelinkt = `#F0F0F0`/`#333`; Gelinkt = `#CFF7F4`/`#0B8F87`; Inaktiv/Vorbereitung
  (`INACTIVE`, `PREPARING`, `CREATE`) = `#F0F0F0`/`#808080`; Stillgelegt = `#F0F0F0`/`#808080` durchgestrichen nein,
  sondern Text «Stillgelegt».
- **Pull-to-refresh** lädt neu; Statuszeile unter der Liste: «12 Terminals · aktualisiert 14:32» (12 dp grau).
- **Leerzustände:** kein Terminal im Space → «In diesem Space sind keine Terminals angelegt. Terminals werden
  im wallee-Backend unter Terminals → Payment Terminals erstellt.»; Suche ohne Treffer → «Kein Terminal
  passt zu ‹xyz›» + «Suche leeren»; Netzfehler → Panel mit «Erneut versuchen».
- **Schnellaktion (Phase 5):** Long-Press auf eine ungelinkte, aktive Zeile → Haptik, Detail + Scanner werden
  geöffnet (Zurück aus dem Scanner landet im Detail).

## Screen: Terminal-Detail (`TerminalDetail`)

Header: Zurück-Pfeil, Wortmarke. Headline: «<identifier>» / «<name>» (Stift-Symbol neben dem Namen →
Umbenennen inline: Textfeld, «Speichern» → `PATCH`).

Darunter eine Faktentabelle (Label 13 dp grau links, Wert 15 dp rechts, Haarlinien):

| Label | Wert |
|---|---|
| Status | Badge (Ungelinkt / Gelinkt / …) |
| Gerät | `deviceName` oder «—» |
| Seriennummer | `deviceSerialNumber` (monospace-artig via `tabular-nums`, kopierbar per Tipp) oder «—» |
| Typ | `type.name[de]` |
| Standort | `locationVersion.location.name` |
| Konfiguration | `configurationVersion.configuration.name` |
| Währung | `defaultCurrency` |
| Aktiviert am | Datum |
| Terminal-ID (intern) | `id` |

Aktionen unten, fixiert (Bottom-Bar auf Weiss mit Haarlinie oben):

**Ungelinkt:**
- Primärbutton 52 dp volle Breite: **«Gerät linken»** → `Scan(mode=LINK)`

**Gelinkt:**
- Primärbutton: **«Gerät ersetzen»** → Bestätigungsdialog «Gerät <S/N> wird vom Terminal <name> getrennt
  und durch ein neues ersetzt. Halten Sie das neue Gerät bereit.» [Abbrechen] [Weiter zum Scan] →
  `Scan(mode=REPLACE)`
- Textbutton orange: **«Gerät trennen»** → Bestätigungsdialog «Gerät <S/N> vom Terminal <name> trennen? Das
  Terminal kann danach keine Zahlungen mehr annehmen, bis ein Gerät verknüpft ist.» [Abbrechen] [Trennen]
  → `unlink` → Nachladen → `Result(UNLINKED)`

**Immer (Overflow-Menü ⋮ im Header oder Sekundärzeile über den Buttons):**
- «Aktualisieren» (`refresh`) — ersetzt den Detail-State, Toast «Aktualisiert».
- «Konfiguration auslösen» (`trigger-configuration`) — Bestätigung «Die Konfiguration wird jetzt an das Gerät
  gesendet.» → Toast «Konfiguration ausgelöst».
- «Terminal-ID kopieren».

Bei `state` in `DECOMMISSIONING`/`DECOMMISSIONED`: keine Aktionen, Hinweistext «Dieses Terminal ist
stillgelegt.»

## Screen: Scanner (`Scan`)

Vollbild, Hochformat erzwungen, schwarzer Hintergrund mit Kamerabild.

```
┌──────────────────────────────────────────────┐
│ ✕                                  🔦         │  ← Schliessen links, Taschenlampe rechts (weiss, 44 dp)
│                                               │
│         ┌───────────────────────┐             │
│         │                       │             │  ← Sucher: 280×160 dp (Barcode-Format), Ecken türkis 3 dp,
│         │      (Kamerabild)     │             │     Rest abgedunkelt 60 %
│         └───────────────────────┘             │
│                                               │
│   Seriennummer auf der Rückseite des Geräts   │  ← 15 dp weiss, zentriert
│   scannen                                     │
│                                               │
│   Terminal: Kasse 1 · WT-8F3K2                │  ← 13 dp, weiss 70 %
│                                               │
│         [ Seriennummer manuell eingeben ]     │  ← Sekundärbutton weiss-transparent, 44 dp
└──────────────────────────────────────────────┘
```

- Kamera-Permission: beim ersten Öffnen Rationale («Die Kamera wird nur zum Lesen des Seriennummern-Codes
  verwendet.») → Systemdialog. Abgelehnt → Screen zeigt statt Kamera die manuelle Eingabe mit Hinweis und
  Link «In den Einstellungen erlauben».
- ML Kit: Formate `CODE_128, CODE_39, CODE_93, QR_CODE, DATA_MATRIX, EAN_13` (Rest aus). Analyse nur innerhalb
  des Suchers (ROI). Erster stabiler Treffer (2× gleicher Wert in Folge) → Haptik (kurz), Kamera einfrieren,
  **Bestätigungs-Sheet** von unten:

```
┌──────────────────────────────────────────────┐
│  Seriennummer erkannt                         │
│                                               │
│  2290012345                                   │  ← 32 dp Medium, schwarz, tabular-nums
│  Gerät: PAX (aus Code, falls vorhanden)       │
│                                               │
│  Mit Terminal «Kasse 1» (WT-8F3K2) verknüpfen │
│                                               │
│  [        Linken        ]  (Primär, 52 dp)    │
│  [ Erneut scannen ]                           │
└──────────────────────────────────────────────┘
```

- Rohwert wird durch `SerialNumber.parse()` normalisiert (`02 §4`). Ungültig → Sheet mit Rohwert und
  Meldung «Kein gültiger Seriennummern-Code» + «Manuell eingeben» / «Erneut scannen».
- **Manuelle Eingabe:** Sheet mit Textfeld (Grossbuchstaben/Ziffern, `imeOptions=done`), gleiche Bestätigung.
- Torch-Zustand wird nicht gemerkt (Batterie). Autofokus kontinuierlich; Tipp auf Bild → Fokus an Stelle.
- **Mode LINK:** «Linken» → Progress-Overlay «Verknüpfen…» → `link` → `GET` nachladen → `Result(LINKED)`.
- **Mode REPLACE:** «Ersetzen» → Progress-Overlay mit zwei Schritten («1 Altes Gerät trennen ✓ · 2 Neues
  Gerät verknüpfen …») → `unlink` → `link` → `GET` → `Result(REPLACED)`. Teilfehler siehe unten.

## Screen: Ergebnis (`Result`)

Voll-Türkis-Fläche (oberes 60 %) mit grossem Haken-Icon (schwarz, 1.5 dp Linie, 64 dp) und Statement in
Roboto Regular 24 dp schwarz:

- `LINKED`: «Gelinkt» / darunter «Kasse 1 · WT-8F3K2» / «S/N 2290012345»
- `REPLACED`: «Gerät ersetzt» / «Kasse 1» / «Neu: S/N 2290067890 · Alt: S/N 2290012345»
- `UNLINKED`: «Gerät getrennt» / «Kasse 1 · WT-8F3K2»

Unter der Fläche auf Weiss zwei Buttons: **«Zurück zur Liste»** (primär) und **«Nächstes Terminal linken»**
(sekundär; springt zur Liste mit Filter «Ungelinkt»). Wenn die Server-Nachladung nach dem 204 **nicht**
`linked` bestätigt: keine Türkis-Fläche, sondern Weiss mit Warn-Icon und «wallee hat die Verknüpfung
bestätigt, zeigt aber noch keine Seriennummer. Bitte in 10 Sekunden aktualisieren.» + «Aktualisieren».

### Fehler- und Teilfehlerzustände

| Situation | Anzeige |
|---|---|
| `link` 422 (S/N unbekannt / bereits gelinkt) | Sheet bleibt, Meldung aus Body in Orange 13 dp unter der S/N, Buttons «Erneut scannen» / «Manuell eingeben» |
| `unlink` fehlgeschlagen (Replace Schritt 1) | Overlay → Fehlerpanel «Altes Gerät konnte nicht getrennt werden» + Meldung + «Erneut versuchen» / «Abbrechen» (nichts verändert) |
| `link` fehlgeschlagen nach erfolgreichem `unlink` (Replace Schritt 2) | Fehlerpanel **auf Weiss** mit Warnsymbol: «Altes Gerät getrennt — neues Gerät konnte nicht verknüpft werden. Das Terminal ist jetzt ohne Gerät.» Buttons «Erneut scannen» (nur Schritt 2), Textlink «Altes Gerät wieder verknüpfen» (link mit alter S/N), «Zur Liste» |
| Netzfehler während Aktion | «Keine Verbindung. Der Zustand des Terminals ist unklar — bitte aktualisieren.» → `GET` nachladen, Zustand anzeigen |
| Kamera nicht verfügbar | Manuelle Eingabe |

## Screen: Einstellungen (`Settings`)

Headline: «Einstellungen» / «Terminal Linker».

- **Zugangsdaten:** User-ID, Key maskiert, «ändern» → Setup-Formular.
- **Spaces:** Modus (automatisch / manuell), Liste **aller** Spaces (aus wallee und manuell erfasst) je mit
  «Entfernen» (Bestätigungsdialog; wirkt nur in der App, nie in wallee). Entfernte Spaces aus wallee
  erscheinen darunter unter «Entfernte Spaces» mit «Wiederherstellen». «Space-ID hinzufügen».
- **Anzeige:** Sprache (System / Deutsch / English, Segmented; wechselt sofort), «Stillgelegte Terminals
  anzeigen».
- **Über:** Version, «Nach Updates suchen» (prüft sofort gegen GitHub Releases; bei neuer Version öffnet ein
  Tipp die Release-Seite), «Release-Seite öffnen», Lizenzen (Roboto, ML Kit, AndroidX, OkHttp, kotlinx).
- **Alle lokalen Daten löschen** (Textbutton orange, Bestätigung) → zurück zu Setup.

Update-Hinweis (docs/01): dezenter grauer Streifen unter dem Header der Terminalliste «Version x.y verfügbar»
mit «Ansehen» und ✕ (blendet genau diese Version aus).

## Barrierefreiheit & Bedienung

- Alle interaktiven Elemente ≥ 44 dp, `contentDescription` für Icons, Badges auch als Text lesbar.
- Farben nie alleiniges Merkmal (Badge trägt immer Text).
- Scanner-Screen kündigt Treffer per TalkBack an («Seriennummer erkannt: …»).
- Tastatur: numerisch bei IDs, `imeOptions` so, dass Enter das Formular abschickt.
