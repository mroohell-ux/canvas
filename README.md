# Wear Sticky Notes Importer

A Wear OS app for Samsung Galaxy Watch / Wear OS devices that:

- Starts with a few built-in sample sticky notes so you can preview the UI immediately.
- Lets you pick a JSON file from storage.
- Imports all sticky notes in that file.
- Shows the **front** side first.
- Flips to the **back** side when tapped.
- Uses the rotating bezel / rotary input to move between notes.

## JSON format

The importer expects a structure with a top-level `stickyNotes` array (like your sample):

- `color` is applied as note background color.
- `front.text` and `back.text` are the visible sides.

Unknown fields are ignored.

## Build

```bash
./gradlew assembleDebug
```

## Use on watch

1. Install the app on your Wear OS watch.
2. Open the app and tap **Import JSON**.
3. Pick your file from device storage.
4. Rotate bezel/crown to browse notes.
5. Tap note to flip between front/back.
