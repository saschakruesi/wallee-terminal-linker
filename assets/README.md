# Assets

Übernommen aus dem wallee Virtual Terminal (gleiches Corporate Design).

- `brand/` — Wortmarke als SVG/PNG (türkis auf Weiss, weiss auf Türkis, schwarz). In Phase 1 als
  VectorDrawables nach `app/src/main/res/drawable/` konvertieren (Android Studio: New → Vector Asset → Local file).
- `icon/icon.svg` — «w»-Monogramm für das Adaptive App-Icon (Vordergrund; Hintergrund Weiss).
- `fonts/` — Roboto 300/400/500/700 als woff2 (Referenz). Für Android werden die **TTF**-Dateien gebraucht:
  von https://fonts.google.com/specimen/Roboto herunterladen (Apache 2.0) und als
  `app/src/main/res/font/roboto_light.ttf`, `roboto_regular.ttf`, `roboto_medium.ttf`, `roboto_bold.ttf` ablegen.
