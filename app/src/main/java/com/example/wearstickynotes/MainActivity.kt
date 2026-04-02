package com.example.wearstickynotes

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val TAG = "RotaryDemo"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: starting rotary input demo")

        setContent {
            MaterialTheme {
                Surface {
                    RotaryListDemo()
                }
            }
        }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_SCROLL) {
            val verticalScroll = ev.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val horizontalScroll = ev.getAxisValue(MotionEvent.AXIS_HSCROLL)
            val source = ev.source
            val fromRotary = (source and InputDevice.SOURCE_ROTARY_ENCODER) == InputDevice.SOURCE_ROTARY_ENCODER
            Log.i(
                TAG,
                "Activity dispatchGenericMotionEvent ACTION_SCROLL: " +
                    "vScroll=$verticalScroll hScroll=$horizontalScroll source=$source fromRotary=$fromRotary"
            )
        }
        return super.dispatchGenericMotionEvent(ev)
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun RotaryListDemo() {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val items = remember { List(20) { index -> "Item ${index + 1}" } }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        Log.i(TAG, "Requested focus for list container")
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { state ->
                Log.i(
                    TAG,
                    "Focus changed: hasFocus=${state.hasFocus}, isFocused=${state.isFocused}, isCaptured=${state.isCaptured}"
                )
            }
            .onRotaryScrollEvent { event ->
                Log.i(
                    TAG,
                    "onRotaryScrollEvent: vertical=${event.verticalScrollPixels}, horizontal=${event.horizontalScrollPixels}, uptimeMillis=${event.uptimeMillis}"
                )
                // Keep behavior explicit for the feasibility test.
                scope.launch {
                    listState.scrollBy(-event.verticalScrollPixels)
                }
                true
            }
            .pointerInteropFilter { motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_SCROLL) {
                    Log.i(
                        TAG,
                        "pointerInteropFilter ACTION_SCROLL: " +
                            "vScroll=${motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL)} " +
                            "hScroll=${motionEvent.getAxisValue(MotionEvent.AXIS_HSCROLL)} source=${motionEvent.source}"
                    )
                } else {
                    Log.i(
                        TAG,
                        "pointerInteropFilter action=${motionEvent.actionMasked} x=${motionEvent.x} y=${motionEvent.y}"
                    )
                }
                false
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val first = event.changes.firstOrNull()
                        if (first != null) {
                            Log.i(
                                TAG,
                                "pointerInput type=${event.type} pressed=${first.pressed} position=${first.position}"
                            )
                        }
                    }
                }
            },
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items) { label ->
            Text(
                text = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}
