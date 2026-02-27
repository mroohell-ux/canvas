# Wear Sticky Notes Importer

Wear OS app for Samsung Galaxy Watch / Wear OS that imports sticky notes **from your phone over local Wi-Fi** (no Wearable Data Layer / Play services).

## What it does

- Shows built-in sample notes at startup.
- Lets you tap **Import from phone**.
- Import flow states:
  - Searching
  - Device list
  - Requesting approval
  - Waiting
  - Downloading
  - Imported / Failed
- Supports manual fallback: enter `IP:port` if discovery fails.
- Notes are browsed with rotary bezel/crown; tap a note to flip front/back.

## Phone server protocol expected by watch

- NSD service type: `_timescape._tcp`
- `GET /meta` (optional)
- `POST /session/request` body:
  - `{ "clientId": "uuid", "clientName": "Wear <model>" }`
- `GET /session/status?sessionId=...` until `APPROVED` / `DENIED`
- `GET /export?token=...` returns sticky-notes JSON payload

## JSON format

Top-level `stickyNotes` array is imported with `Json { ignoreUnknownKeys = true }`.

```json
{
  "version": 1,
  "generatedAt": 1739700000000,
  "stickyNotes": [
    {
      "id": 101,
      "flowId": 1,
      "flowName": "Daily",
      "cardId": 11,
      "cardTitle": "Morning Plan",
      "color": "#FFF8A6",
      "rotation": -2.5,
      "front": { "label": "front", "text": "Drink water" },
      "back": { "label": "back", "text": "500ml before breakfast" }
    }
  ]
}
```

## Permissions / local network notes

- App declares `INTERNET` for HTTP import.
- Discovery uses Android NSD (`NsdManager`); availability/behavior depends on watch firmware and local network configuration.
- Watch and phone must be on the same LAN.

## Build

```bash
./gradlew assembleDebug
```
