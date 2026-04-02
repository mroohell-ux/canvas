package com.example.wearstickynotes

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val TAG = "RotaryDemo"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: starting rotary input demo")

        setContent {
            MaterialTheme {
                RotaryListDemo()
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

data class DemoChatItem(
    val name: String,
    val preview: String,
    val avatarColor: Color
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RotaryListDemo() {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val items = remember {
        List(20) { index ->
            DemoChatItem(
                name = if (index % 2 == 0) "Kim Smith" else "Jessica Moore",
                preview = "Message preview ${index + 1}...",
                avatarColor = if (index % 2 == 0) Color(0xFF76B5E8) else Color(0xFFE2A957)
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        Log.i(TAG, "Requested focus for list container")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD8CCE9)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD5C3F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◔", color = Color(0xFF4A1D79), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
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
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        ChatRow(item)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .width(170.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFEB9DEE))
                        .clickable { Log.i(TAG, "Add button tapped") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color(0xFF31103E),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRow(item: DemoChatItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(Color(0xFF5130A3))
            .border(width = 1.dp, color = Color(0xFF6D4BC9), shape = RoundedCornerShape(38.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(item.avatarColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = item.name,
                color = Color(0xFFE6DBFF),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.preview,
                color = Color(0xFFD8C8F7),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
