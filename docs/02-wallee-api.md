# 02 — wallee API (v2.0, JWT)

Quelle: `https://app-wallee.com/doc/api/web-service` und die OpenAPI-Spezifikation
`https://app-wallee.com/api/spec3.json` (OpenAPI 3.0.1, Version 2.0). Alle Endpunkte unten sind gegen die
Spec geprüft (Stand 28.09.2026). **Nur v2.0 verwenden.** v1 (HMAC-SHA512 in `x-mac-*`-Headern) ist
deprecated und nicht Teil dieses Projekts.

Base-URL: `https://app-wallee.com/api/v2.0`

## 1. Authentifizierung

Jeder Request trägt `Authorization: Bearer <JWT>`. Das JWT wird **pro Request** neu erzeugt und ist an Pfad
und Methode gebunden.

```
Header   { "alg": "HS256", "typ": "JWT", "ver": 1 }
Payload  { "sub": "<applicationUserId>",              // String!
           "iat": <unix seconds>,                     // jetzt, in Sekunden
           "requestPath": "/api/v2.0/payment/terminals/search?limit=100&query=...",  // Pfad inkl. Query, ohne Host
           "requestMethod": "POST" }                  // Grossbuchstaben
Signatur HMAC-SHA256(base64url(header) + "." + base64url(payload), key)
key      = Base64-DECODE(authenticationKey)           // der Key aus dem Backend ist base64-kodiert
```

Implementierung (`core/api/Jwt.kt`):

- `requestPath` muss **exakt** dem Pfad entsprechen, den wallee sieht: `/api/v2.0/...` inkl. Query-String in
  derselben Reihenfolge und Kodierung wie gesendet. Der Client baut die URL mit `HttpUrl.Builder`, nimmt
  `encodedPath + "?" + encodedQuery`, signiert, und sendet genau diese URL.
- `iat` in **Sekunden**. Beim Verbindungstest bei `401` einmalig mit Millisekunden probieren und den
  funktionierenden Modus in den Prefs merken (`iatUnit`). Uhrzeit-Abweichung > wenige Minuten → 401 →
  Meldung «Uhrzeit des Telefons prüfen (automatische Zeit aktivieren)».
- Base64url ohne Padding (`Base64.URL_SAFE or NO_PADDING or NO_WRAP`). Payload-JSON ohne Whitespace, Reihenfolge
  der Felder egal.
- Unit-Test gegen einen bekannten Vektor (Token mit Beispiel-Key auf jwt.io nachrechnen). Der Test ist
  derselbe wie im Virtual Terminal (`web/src/api/jwt.test.ts`) — Vektor übernehmen.

Credentials entstehen im wallee-Backend unter **Account → Users → Application Users**: `userId` (numerisch)
und `authenticationKey` (nur einmal sichtbar). Der Application User braucht in jedem Ziel-Space eine Rolle mit
Rechten auf **Payment Terminals (Lesen + Schreiben)** und **Space (Lesen)**. Ein Application User kann in
mehreren Spaces berechtigt sein — das ist die Grundlage für die Space-Auswahl in der App.

Zusätzlich bei allen Terminal-Endpunkten: Header `space: <spaceId>` (Pflicht).

## 2. Client-Konventionen (`core/api/WalleeClient.kt`)

```kotlin
suspend fun <T> request(method: String, path: String, query: Map<String, String> = emptyMap(),
                        body: Any? = null, spaceId: Long? = null, expand: List<String> = emptyList()): T
```

- Baut `path` + Query (`expand` als wiederholten Query-Parameter `expand=a&expand=b`, so wie die Spec ihn
  als Array definiert), signiert, sendet mit `Accept: application/json`, `Content-Type: application/json`
  (nur bei Body), `space: <id>` wenn gesetzt.
- `204 No Content` → `Unit`.
- Fehler: Status ≠ 2xx → `WalleeApiException(status, code?, message, errors, errorId?)`; Body-Schema
  `RestApiErrorResponse` laut Spec: `code` (maschinenlesbar), `message` (Klartext), `errors` (Map Feld → Meldung),
  `id`, `date`. Mapping für die UI:

| Status | Anzeige |
|---|---|
| 401 | «Zugangsdaten ungültig — User-ID und Authentication Key prüfen» (bzw. Uhrzeit, siehe oben) |
| 403 | «Der Application User hat in diesem Space keine Berechtigung für Terminals» |
| 404 | «Terminal nicht (mehr) gefunden» → Liste neu laden |
| 409 | «Das Terminal wurde inzwischen verändert» → Detail neu laden (optimistic locking, `version`) |
| 422 | Meldung aus dem Body im Klartext (z.B. Seriennummer unbekannt / Gerät bereits anderswo gelinkt) |
| 429 | «Zu viele Anfragen» → Backoff 2 s, 4 s, 8 s (nur GET automatisch) |
| Netzwerk | «Keine Verbindung zu app-wallee.com» + «Erneut versuchen» |

- Timeout 20 s (Connect 10 s), Call-Timeout 30 s. Keine automatischen Retries bei POST/PATCH — Link/Unlink sind nicht
  idempotent aus Sicht der UI (Nutzer muss den Zustand sehen).
- Alle Aufrufe mit `Dispatchers.IO`, abbrechbar über den Coroutine-Scope des Screens.

## 3. Verwendete Endpunkte

### 3.1 Spaces (Space-Auswahl im Header)

| Zweck | Call |
|---|---|
| Alle Spaces, auf die der Application User Zugriff hat | `GET /spaces?limit=10` → `{ data: Space[], hasMore, limit }` — **ohne** `space`-Header, **ohne** `expand`, ein einziger Request mit `limit=10` (die Liste ist serverseitig langsam für Nutzer mit vielen Spaces; weitere Spaces per ID, `hasMore` wird als Hinweis angezeigt). Die API paginiert per Cursor (`after`/`before`), kein `offset` |
| Einzelnen Space prüfen (Fallback / Verbindungstest) | `GET /spaces/{id}` → `Space` |

`Space`-Felder für die UI: `id`, `name`, `state` (`ACTIVE`, `INACTIVE`, `DELETING`, `DELETED`),
`account.name` (mit `expand=account`, optional für Gruppierung), `primaryCurrency`.

**Geklärt (28.09.2026, Test-Space):** `GET /spaces` liefert für einen Application User die Spaces mit Rolle
(HTTP 200, `hasMore = true` bei mehr als `limit`). Die Antwort ist aber **langsam**: mit `limit=100` und
`expand=account` lief die Anfrage in den 20-s-Lese-Timeout, mit `limit=10` ohne `expand` antwortete der Server
in ≈ 5,7 s. Deshalb lädt die App nur die ersten 10 Spaces und bietet den Rest per ID an. Die Spec verlangt
keinen `account`-Header. Verhalten der App:

1. Verbindungstest ruft `GET /spaces` auf. Liefert er ≥ 1 Space → Dropdown wird daraus gefüllt.
2. Liefert er 0 Spaces oder 403 → die App wechselt in den **manuellen Modus**: Der Nutzer erfasst
   Space-IDs von Hand, jede wird mit `GET /spaces/{id}` verifiziert (Name wird angezeigt) und lokal gemerkt.
   Beide Modi münden in dieselbe Space-Liste (`SpaceRepository.spaces: Flow<List<SpaceRef>>`).

Nur Spaces mit `state = ACTIVE` sind wählbar; andere werden ausgegraut angezeigt.

### 3.2 Terminals lesen

| Zweck | Call |
|---|---|
| Liste / Suche | `GET /payment/terminals/search?limit=100&offset=0&order=name:ASC&query=<q>` → `{ data: PaymentTerminal[], hasMore, limit, offset }` |
| Einzelnes Terminal (Detail, nach Aktion neu laden) | `GET /payment/terminals/{id}` → `PaymentTerminal` |

Query-Syntax (Doku §«Search»): Feldvergleiche mit `:` (gleich), `:~` (enthält), Verknüpfung `AND`/`OR`,
Klammern. Beispiele:

```
state:ACTIVE
state:ACTIVE AND deviceSerialNumber:null                      // ungelinkt (Feldwert leer) — Syntax für «leer» in Phase 3 prüfen
(name:~"kasse" OR identifier:~"kasse" OR deviceSerialNumber:~"kasse")   // Freitextsuche
```

Die App lädt pro Space **alle** Terminals mit `state` in `ACTIVE`, `PREPARING`, `INACTIVE` (paginiert à 100,
bis `hasMore = false`, max. 1000) und filtert/sortiert **lokal** — die Freitextsuche mit `:~` ist nur der
Fallback, falls ein Space mehr als 1000 Terminals hat (dann Server-Suche nach dem Tippen, Debounce 400 ms).
Grund: Lokale Filterung ist sofort, funktioniert bei schlechtem Netz und erlaubt das Sortieren nach
«gelinkt/ungelinkt», das die Server-Query nicht sauber abbildet.

`PaymentTerminal`-Felder für die UI:

| Feld | Verwendung |
|---|---|
| `id` | Pfad-Parameter für Aktionen |
| `name` | Titel in Liste und Detail |
| `identifier` | Terminal-ID, wie sie auf dem Gerät angezeigt wird (z.B. `WT-…`); zweite Zeile in der Liste, Suchfeld |
| `state` | `CREATE`, `PREPARING`, `ACTIVE`, `INACTIVE`, `DECOMMISSIONING`, `DECOMMISSIONED` → Badge |
| `deviceSerialNumber` | **leer = ungelinkt, gesetzt = gelinkt** (zentrales Kriterium der App) |
| `deviceName` | Gerätemodell des gelinkten Geräts (z.B. «PAX A77») |
| `type.name` (lokalisierte Map) | Terminaltyp |
| `locationVersion.location.name` | Standort (mit `expand=locationVersion.location`; ohne Expand ggf. nur IDs — in Phase 3 prüfen, welche Expands nötig sind) |
| `configurationVersion.configuration.name` | Konfiguration (mit `expand=configurationVersion.configuration`) |
| `defaultCurrency` | Detail |
| `activatedOn`, `deactivatedOn`, `decommissionedOn` | Detail (Datum) |
| `version` | Für `PATCH` (Umbenennen) mitgeben |

Anzeigelogik «Link-Status»:

```
linked   = deviceSerialNumber != null && deviceSerialNumber.isNotBlank()
```

### 3.3 Link / Unlink / Replace

| Aktion | Call | Antwort |
|---|---|---|
| **Link** (Gerät mit Terminal verknüpfen) | `POST /payment/terminals/{id}/link?serialNumber=<S/N>` | `204` |
| **Unlink** (Gerät vom Terminal trennen) | `POST /payment/terminals/{id}/unlink` | `204` |
| **Replace** (gelinktes Terminal auf neues Gerät umziehen) | **kein eigener Endpunkt** — Sequenz: `unlink` → `link?serialNumber=<neue S/N>` | 2× `204` |

Regeln:

- `serialNumber` ist ein Query-Parameter, URL-kodiert; er wird exakt so in `requestPath` des JWT signiert.
- Nach `204` **immer** `GET /payment/terminals/{id}` nachladen und erst dann «Gelinkt» anzeigen — die
  Wahrheit ist der Serverzustand (`deviceSerialNumber`), nicht der 204.
- Fehlerfälle von `link` (aus 422/409-Body anzeigen, im Test-Space empirisch sammeln — Phase 4):
  Seriennummer unbekannt/nicht registriert; Gerät bereits mit einem anderen Terminal gelinkt (dann Hinweis
  «Zuerst am anderen Terminal trennen»); Terminal nicht im Zustand `ACTIVE`/`PREPARING`.
- **Replace ist zweistufig und nicht atomar.** Schlägt `link` nach erfolgreichem `unlink` fehl, ist das
  Terminal ungelinkt. Die App zeigt das ehrlich: «Altes Gerät getrennt, neues Gerät konnte nicht verknüpft
  werden» mit Button «Erneut scannen» (wiederholt nur `link`). Kein automatisches Zurück-Linken des alten
  Geräts (das alte Gerät ist ja in der Regel defekt/abgebaut); optional als Textlink «Altes Gerät wieder
  verknüpfen» wenn die alte S/N bekannt ist.
- Vor `unlink`/`replace` Bestätigungsdialog mit Terminalname und aktueller S/N.

### 3.4 Weitere Aktionen im Terminal-Detail

| Aktion | Call | Antwort | UI |
|---|---|---|---|
| Zustand vom Gerät aktualisieren | `POST /payment/terminals/{id}/refresh` | `200 PaymentTerminal` | «Aktualisieren» — Ergebnis ersetzt den Detail-State |
| Konfiguration ans Gerät schicken | `POST /payment/terminals/{id}/trigger-configuration` | `200` | «Konfiguration auslösen» — Toast «Konfiguration wurde ausgelöst» |
| Umbenennen | `PATCH /payment/terminals/{id}` Body `{ "name": "...", "version": <aktuelle version> }` | `200 PaymentTerminal` | Inline-Edit des Namens; 409 → neu laden |
| Tagesabschluss auslösen | `POST /payment/terminals/{id}/trigger-final-balance` | `200` | **Nicht in v1** (Finanzwirkung; erst nach Rücksprache) |

**Nie aufrufen:** `DELETE /payment/terminals/{id}`, `POST /payment/terminals` (Anlegen), `perform-transaction`,
`trigger-reversal`.

### 3.5 Optional (v2): Aktivierungsstatus

`GET /payment/terminals/activation-status?limit=100` liefert pro Terminal `activationCode`, `activationCodeActive`,
`environment` (TEST/PRODUCTION), `activatedOn`. Könnte im Detail den Aktivierungscode anzeigen, falls ein
Gerät nach dem Linken noch aktiviert werden muss. Nicht Teil von v1; Feld `activationCode` aus
`PaymentTerminal` reicht dort, falls gesetzt.

## 4. Seriennummer aus dem Scan

Der Barcode auf der Rückseite eines PAX-Geräts (A35, A77, A920, A80 …) enthält in der Regel **nur die
Seriennummer** (Code128, z.B. `2290012345`), teilweise als QR mit mehreren Feldern (`SN:…`, `PN:…`) oder
als DataMatrix. Normalisierung in `core/serial/SerialNumber.kt`:

1. Rohwert trimmen, Whitespace innen entfernen.
2. Enthält der Wert `SN:` / `S/N:` / `SERIAL` (case-insensitive), den Teil danach bis zum nächsten
   Trennzeichen (`;`, `,`, Leerzeichen, Zeilenumbruch) nehmen.
3. Ist der Wert eine URL, den letzten Pfad-/Query-Teil nehmen, der wie eine S/N aussieht (`[A-Z0-9]{8,20}`).
4. Ergebnis muss `^[A-Za-z0-9-]{6,32}$` erfüllen, sonst «Kein gültiger Seriennummern-Code — bitte manuell eingeben».
5. Der Nutzer sieht die erkannte Nummer **vor** dem Senden gross und bestätigt sie (Fehlscans sind billig,
   ein Link auf das falsche Gerät nicht).

Welche Codeformate die eingesetzten Terminals tatsächlich tragen, wird in Phase 4 mit realen Geräten
gesammelt und als Testfälle in `SerialNumberTest.kt` hinterlegt.

## 5. Credential-QR-Code (Zugangsdaten per Scan)

Das wallee-Portal soll künftig beim Application User einen QR-Code anzeigen, der die Zugangsdaten für
diese App enthält. Das Portal-Feature existiert **noch nicht**; die App implementiert den Parser jetzt schon
und ist damit Referenz für das Format. Änderungen am Format brauchen eine neue `v`.

### Format (v1)

Der QR-Code enthält eine URI mit eigenem Schema (kompakt, kein JSON-Escaping, von jedem QR-Generator lesbar):

```
wallee-app-user://v1?u=<applicationUserId>&k=<authenticationKey>[&s=<spaceId>[,<spaceId>…]][&n=<label>][&e=<env>]
```

| Parameter | Pflicht | Inhalt |
|---|---|---|
| `u` | ja | Application User ID (Ganzzahl) |
| `k` | ja | Authentication Key, exakt wie im Backend angezeigt (base64; im Query URL-kodiert, `+`/`/`/`=` → `%2B`/`%2F`/`%3D`) |
| `s` | nein | Eine oder mehrere Space-IDs, kommagetrennt; die erste wird aktiver Space |
| `n` | nein | Bezeichnung (URL-kodiert, max. 60 Zeichen), z.B. Kundenname — nur Anzeige |
| `e` | nein | `prod` (Standard) oder `test` — reserviert; die App nutzt heute nur `app-wallee.com` |

Beispiel:

```
wallee-app-user://v1?u=12345&k=AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA%3D&s=67890,67891&n=Hotel%20Muster
```

Zusätzlich akzeptiert der Parser dieselben Felder als **JSON-Objekt** (falls das Portal das bevorzugt):

```json
{ "type": "wallee-app-user", "v": 1, "userId": 12345, "key": "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=",
  "spaceIds": [67890, 67891], "label": "Hotel Muster" }
```

### Parser (`core/auth/CredentialQr.kt`)

```kotlin
data class ScannedCredentials(val userId: Long, val key: String, val spaceIds: List<Long>, val label: String?)
fun parseCredentialQr(raw: String): Result<ScannedCredentials>
```

Regeln:
- Erkennung: Präfix `wallee-app-user://` **oder** JSON mit `"type":"wallee-app-user"`. Alles andere →
  `Failure(NotAWalleeCode)` (der Terminal-S/N-Scanner und der Credential-Scanner sind getrennte Modi; ein
  Credential-QR im S/N-Modus wird als ungültige S/N abgewiesen und umgekehrt).
- `v` ≠ 1 → `Failure(UnsupportedVersion)` mit Hinweis «App aktualisieren».
- `u` muss Ganzzahl > 0 sein; `k` muss nach URL-Dekodierung gültiges Base64 sein (Länge ≥ 16 Bytes; das
  Beispiel oben ist ein 32-Byte-Testschlüssel) — sonst `Failure(Malformed)`. Beim Dekodieren von `k` wird `+`
  **nicht** als Leerzeichen interpretiert (nur Prozent-Escapes), damit auch nicht-kodierte Keys funktionieren.
- Der Key wird **nie** geloggt, nie im Klartext angezeigt und sofort nach dem Speichern in
  `EncryptedSharedPreferences` aus dem Speicher verworfen. Der Rohtext des Scans wird nicht in Prefs oder
  Navigation-Args gehalten (Übergabe über den ViewModel-Scope, nicht über die Route).
- Unit-Tests: URI mit/ohne Spaces, URL-kodierter Key, JSON-Form, fremder QR, falsche Version, kaputtes Base64.

Sicherheitshinweis für das Portal-Feature: Der QR-Code enthält einen gültigen Authentication Key. Das Portal
sollte ihn nur nach Re-Authentifizierung anzeigen, zeitlich begrenzt, und idealerweise für einen frisch
erzeugten Key (der dann nur auf diesem Gerät existiert). Für die App ist das transparent.

## 6. Testen mit einem Test-Space

- Application User im Test-Account anlegen, Rolle mit Payment-Terminal-Rechten in ≥ 2 Spaces (damit das
  Dropdown getestet wird).
- Test-Space mit mindestens einem Payment Terminal im Zustand `ACTIVE` ohne gelinktes Gerät und ein
  physisches Testgerät (PAX) mit bekannter Seriennummer.
- Wenn kein Gerät verfügbar ist: manuelle Eingabe einer bekannten S/N nutzen; die Fehlerpfade (falsche S/N)
  lassen sich mit erfundenen Nummern provozieren.
