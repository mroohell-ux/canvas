# Wear Sticky Notes Importer

Wear OS app for Samsung Galaxy Watch / Wear OS that imports sticky notes **from your phone over local Wi-Fi** (no Wearable Data Layer / Play services).

## What it does

- Shows built-in sample notes at startup (including longer entries for scrolling/typography testing).
- On note detail, **double-tap** any note to open/close the actions tray with smooth slide/fade animation; tap outside to dismiss.
- Import flow states:
  - Searching
  - Device list
  - Requesting approval
  - Waiting
  - Downloading
  - Imported / Failed
- Supports manual fallback: enter `IP:port` if discovery fails.
- Notes are browsed with rotary bezel/crown or swipe left/right; single tap flips front/back.
- Swipe up/down on the note now scrolls long text content.
- Note text is centered when content is short; long content switches to top-aligned scroll mode.
- Note position/side info (e.g., `1/8 • front`) is always pinned at the top.
- Rotary/bezel input now requests focus on the note card; on emulator use the Wear crown/bezel controls (or swipe fallback).
- Tray actions: Import notes, Shuffle mode toggle, and text size (XS/S/L).
- Font auto-fit: starts from your chosen XS/S/L size; if that already fits one screen, it enlarges text to the largest size that still fits without scrolling.
- Default text scale starts at `L` to better fill the round screen, while XS/S/L remain available in tray.
- Note backgrounds use calm, premium radial gradients with soft centers and deep vignette edges derived from each note base color.

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
