# Wear Rotary Input Demo

Tiny Wear OS Jetpack Compose demo app to validate whether Samsung digital bezel / rotary input reaches app code.

## What this app contains

- One screen only.
- A standard `LazyColumn` with 20 dummy chat rows styled like the requested mockup.
- A simple circular watch-like container and a bottom `+` button.
- Verbose logs for:
  - Focus changes on the list container.
  - `onRotaryScrollEvent` in Compose.
  - Generic motion `ACTION_SCROLL` in both Activity and Compose interop.
  - Pointer/touch events.

## How to run on a Samsung watch

1. Enable Developer options + ADB debugging on the watch.
2. Pair from Android Studio Device Manager (or `adb pair`).
3. Build + install:

```bash
./gradlew :app:installDebug
```

4. Open the app **Wear Rotary Input Demo** on the watch.
5. Open Logcat and filter by tag:

```text
RotaryDemo
```

## What success looks like

Rotate the bezel/crown and confirm:

- List moves up/down.
- Logcat contains at least one of:
  - `onRotaryScrollEvent: ...`
  - `Activity dispatchGenericMotionEvent ACTION_SCROLL: ... fromRotary=true`
  - `pointerInteropFilter ACTION_SCROLL: ...`

If this demo works but your real app does not, your app's custom focus/input wiring is likely the blocker.
