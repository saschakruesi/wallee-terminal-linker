# 04 — Design-System (wallee Corporate Design, Android/Compose)

Verbindlich, übernommen aus dem wallee Virtual Terminal (`docs/04-design-system.md` dort) und auf Android
übersetzt. Der Look ist **leise**: dünne Schriftschnitte, kleine Headlines, Wirkung durch Türkis-Flächen und
Weissraum — nicht durch Fettdruck, Schatten oder Verläufe. Claim: **#homeofpayment** (immer klein).

Material 3 ist nur die technische Basis (Compose-Komponenten, Ripple, Bottom Sheet). Das Farbschema wird
vollständig überschrieben, Tonal Elevation auf 0 gesetzt, Formen und Typografie ersetzt. Ziel: Man sieht
nicht, dass es Material ist.

## Farben (`core/ui/WalleeColors.kt`)

| Token | Wert | Verwendung |
|---|---|---|
| `Turquoise` | `#11D9CC` | Markenfarbe, **flächig**: Setup-Fläche, Ergebnisfläche, aktives Segment-Underline, Fokusrahmen, Scanner-Ecken |
| `Turquoise80` | `#41E1D6` | Pressed auf Türkis-Fläche |
| `Turquoise40` | `#9CEDE7` | Zeilen-Pressed auf Türkis |
| `Turquoise20` | `#CFF7F4` | Sanfte Flächen (Info-Boxen), Zeilen-Pressed auf Weiss, Badge «Gelinkt» |
| `TurquoiseText` | `#0B8F87` | **Einzige** türkis wirkende Textfarbe auf Weiss (Links, Sekundäraktionen, Badge-Text «Gelinkt») |
| `TurquoiseDeep` | `#0E6B66` | Hervorhebung innerhalb einer Türkis-Fläche |
| `Orange` | `#FF4D00` | Ein Call-out pro Screen: destruktive Aktion (Text), Fehlermarker, Fehlerrahmen. Nie flächig |
| `OrangeSoft` / `OrangeText` | `#FFE4D9` / `#B33600` | Badge «Fehler»/Warnpanel |
| `Black` | `#000000` | Headline Zeile 2, Primärbuttons, Text auf Türkis |
| `Text` | `#333333` | Fliesstext |
| `TextMuted` | `#808080` | Rubrikzeile, Labels, Fussnoten, inaktive Segmente |
| `Line` | `#D9D9D9` | Haarlinien, Rahmen von Inputs, Listentrenner |
| `Grid` | `#E6E6E6` | Sehr dezente Flächen-Trenner |
| `Bg` | `#FFFFFF` | Standardhintergrund |
| `BgSoft` | `#F7F7F7` | Hintergrund hinter Karten (sparsam), Sekundärbutton pressed |
| `BadgeNeutral` | `#F0F0F0` | Badge «Ungelinkt», «Inaktiv» |

**Kontrastregeln (nicht verhandelbar):**
- Text auf Türkis ist **Schwarz** (`#000`/`#333`). Weiss auf Türkis nur für die Wortmarke und Display-Zahlen ≥ 40 sp.
- Türkis `#11D9CC` ist **keine Textfarbe auf Weiss** (1.8:1). Dafür `TurquoiseText`.
- Orange nie als Fliesstext, nie als Fläche, nie neben Türkis als gleichwertige zweite Fläche.
- Kein Farbverlauf, keine Schlagschatten auf Farbflächen. Karten/Zeilen werden durch Haarlinien getrennt.
- Kein Dark Mode: `isSystemInDarkTheme()` wird ignoriert, Theme ist immer hell.

Material-Mapping (`WalleeTheme`): `primary = Black`, `onPrimary = White`, `secondary = TurquoiseText`,
`surface = Bg`, `background = Bg`, `onSurface = Text`, `onSurfaceVariant = TextMuted`, `outline = Line`,
`error = Orange`, `primaryContainer = Turquoise`, `onPrimaryContainer = Black`. `LocalTonalElevation`-Effekte
über `surfaceColorAtElevation` vermeiden: Komponenten mit `tonalElevation = 0.dp` und `shadowElevation = 0.dp`.

## Typografie (`core/ui/WalleeType.kt`)

- **Roboto** für alles, als `res/font/`: `roboto_light` (300), `roboto_regular` (400), `roboto_medium` (500),
  `roboto_bold` (700, nur Display-Zahlen). Kein Downloadable-Fonts-Provider.
- Grundgrösse 15 sp, Zeilenhöhe 1.5. Links-bündig. Keine Kursive, keine Versalien-Labels mit Letterspacing
  (auch nicht Material-Buttons: `letterSpacing = 0.sp`, keine Uppercase-Transformation).
- **Headline-Pattern** (jede Seite oben links):
  ```
  Terminals                 ← Zeile 1: Roboto Light 24 sp, #808080
  Hotel Muster – Rezeption  ← Zeile 2: Roboto Medium 24 sp, #000000
  ```

| Rolle | Gewicht | Grösse | Farbe |
|---|---|---|---|
| Headline Zeile 1 / 2 | Light / Medium | 24 sp | `#808080` / `#000` |
| Abschnittstitel | Medium | 17 sp | `#000` |
| Fliesstext, Formulare, Listenzeile Titel | Regular (Titel: Medium) | 15 sp | `#333` (Titel `#000`) |
| Labels, Listenzeile 2, Tabellenkopf | Regular | 13 sp | `#808080` |
| Fussnoten, Statuszeile | Light | 12 sp | `#808080` |
| Seriennummer (Bestätigung) | Medium | 32 sp | `#000`, `FontFeature("tnum")` |
| Statement auf Türkis-Fläche | Regular | 24 sp | `#000` |

Schriftgrössen respektieren die System-Skalierung (sp), Layouts müssen bis 1.3× nicht brechen.

## Logo

- Assets in `assets/brand/` → als VectorDrawable nach `res/drawable/`: `wallee_logo_turquoise` (auf Weiss),
  `wallee_logo_white` (auf Türkis), `wallee_logo_black`.
- Platzierung: **oben rechts im Header**, Höhe 22 dp, Schutzraum rundum ≥ 22 dp. Nicht verzerren, nicht
  umfärben. Die Wortmarke wird nie als Text gesetzt — immer das Drawable.
- Setup- und Ergebnisflächen zeigen die weisse Wortmarke gross (36 dp) auf Voll-Türkis.
- **App-Icon:** «w»-Monogramm türkis auf Weiss (aus `assets/icon/icon.svg`), als Adaptive Icon:
  Hintergrund Weiss, Vordergrund Monogramm mit Sicherheitszone; monochrome Variante schwarz für Android 13+.
  App-Name im Launcher: «Terminal Linker».

## Abstände & Formen

- Basis 8 dp: `S1 = 8, S2 = 16, S3 = 24, S4 = 32, S5 = 40`. Seitenrand 20 dp.
- Radius: 4 dp für Inputs/Buttons, 8 dp für Karten/Sheets (Bottom Sheet oben 8 dp), Badges 12 dp.
  Nichts Pillenförmiges ausser Badges. Material-`Shapes` entsprechend überschreiben (`small = 4, medium = 8,
  large = 8, extraLarge = 8`).
- Haarlinie 1 dp `#D9D9D9` als Trenner zwischen Listenzeilen und Sektionen — statt Kartenrahmen/Schatten.
- Header 56 dp, Listenzeile 64 dp, Control 44 dp, grosse Aktion 52 dp.

## Komponenten (`core/ui/components/`)

- **`WPrimaryButton`:** Schwarz `#000`, weisser Text, 44 dp (gross: 52 dp, volle Breite), Radius 4 dp,
  Roboto Medium 15 sp, Ripple weiss 20 %. Pressed `#333`. Auf Türkis-Fläche: Weiss mit schwarzem Text.
  Ladezustand: Spinner (weiss, 20 dp) ersetzt den Text, Button bleibt gleich gross, disabled.
- **`WSecondaryButton`:** transparent, 1 dp `#D9D9D9`, Text `#000`; pressed `#F7F7F7`.
  Auf Schwarz/Kamera: weisser Rahmen 1 dp, weisser Text.
- **`WTextButton`:** `TurquoiseText`, keine Unterstreichung. Destruktiv: `Orange`.
- **`WInput`:** 44 dp, 1 dp `#D9D9D9`, Radius 4, Fokus: 2 dp Türkis-Rahmen, Label 13 sp grau **über** dem
  Feld (kein Floating-Label). Fehler: Rahmen Orange + Text 13 sp Orange darunter. Passwortfeld mit Auge.
- **`WSegmented`:** Text-Segmente 44 dp; aktiv: schwarz Medium + 2 dp türkiser Unterstrich; inaktiv: `#808080`.
  Zähler als 13 sp grau hinter dem Label.
- **`WBadge`:** 12 dp Radius, 12 sp Medium, Padding 2×8 dp. Varianten: `Linked` (`#CFF7F4`/`#0B8F87`),
  `Unlinked` (`#F0F0F0`/`#333`), `Inactive` (`#F0F0F0`/`#808080`), `Error` (`#FFE4D9`/`#B33600`).
- **`WListRow`:** 64 dp, Titel/Zeile 2/Badge/Chevron, Haarlinie unten, Pressed `#CFF7F4`.
- **`WFactTable`:** Label-Wert-Zeilen 48 dp mit Haarlinien; Werte mit `tnum`.
- **`Headline`:** zweizeilig wie oben.
- **`WHeader`:** 56 dp, weiss, Haarlinie unten; Slot links (Space-Chip oder Zurück), Wortmarke rechts.
- **`SpaceChip`:** 36 dp, transparent, 1 dp `#D9D9D9`, Radius 4; Name (15 sp Medium, ellipsized) + Chevron.
- **`WBottomSheet`:** Weiss, Radius 8 dp oben, Griff 32×4 dp `#D9D9D9`, kein Scrim-Blur, Scrim schwarz 40 %.
- **`WDialog`:** Weiss, Radius 8, Titel 17 sp Medium, Text 15 sp, Buttons rechts (Sekundär + Primär bzw.
  destruktiv als Textbutton Orange).
- **`WToast`/Snackbar:** unten, Weiss, Haarlinie, links 3 dp Türkis-Balken (Erfolg) oder Orange (Fehler),
  Text `#333`. Keine Material-Snackbar in Dunkelgrau.
- **`TurquoisePanel`:** Fläche `#11D9CC`, Innenabstand 40 dp, Text Schwarz, weisse Wortmarke nur wenn das
  Panel den Screen dominiert (Setup, Ergebnis).
- **Scanner-Overlay:** Abdunkelung schwarz 60 %, Sucher-Ecken 3 dp Türkis (24 dp lang), Text weiss.
- **Icons:** minimal, 1.5 dp Linien, monochrom (schwarz/grau/weiss): Suche, Schliessen, Zurück, Chevron
  rechts/unten, Haken, Warnung, Taschenlampe, Stift, Kopieren, Aktualisieren, Zahnrad, Plus, Auge/Auge-aus,
  Kamera, QR-Scan (`ic_qr_scan`: Sucher-Ecken mit QR-Andeutung). Eigene VectorDrawables (`res/drawable/ic_*.xml`), keine Material-Icons-Extended-Abhängigkeit,
  keine Emojis.
- **Spinner:** `CircularProgressIndicator` mit Strichstärke 2 dp, Farbe Schwarz (auf Türkis) bzw.
  `TurquoiseText` (auf Weiss), kein Track.

## Bewegung

Übergänge ≤ 200 ms, nur Fade/leichtes Slide für Sheets. Keine Bounces, keine Shared-Element-Animationen.
Haptik nur beim Scan-Treffer (`HapticFeedbackType.LongPress`) und beim erfolgreichen Link (`Confirm`).

## Was ausdrücklich nicht wallee ist

Gradient-Buttons, Glassmorphism, Neon-Schatten, lila/blaue Akzente (Material-Standard-Purple!), Dark Mode,
runde Pillen-Buttons, FABs, fette Versalien-Headlines, Emojis in der UI, dekorative Illustrationen,
Material-Tonal-Flächen, Bewegungseffekte über 200 ms.

## Styleguide-Screen

Versteckte Route `Styleguide` (nur im Debug-Build über Einstellungen → 7× auf die Version tippen), die alle
Komponenten in allen Zuständen zeigt. Dient als Abnahme-Referenz in Phase 1.
