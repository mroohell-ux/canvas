# Wear Rotary Input Demo

Tiny Wear OS Jetpack Compose demo app to validate whether Samsung digital bezel / rotary input reaches app code.

## What this app contains

- One screen only.
- A standard `LazyColumn` with 20 dummy text rows (`Item 1` ... `Item 20`).
- No custom visuals.
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

You should rotate the bezel/crown and see at least one of these logs:

- `onRotaryScrollEvent: ...`
- `Activity dispatchGenericMotionEvent ACTION_SCROLL: ... fromRotary=true`
- `pointerInteropFilter ACTION_SCROLL: ...`

If these appear and the list scrolls, your watch is delivering usable rotary/bezel input to the app.
If this demo works but your real app does not, the blocker is likely your app's focus/input handling or custom UI structure.
