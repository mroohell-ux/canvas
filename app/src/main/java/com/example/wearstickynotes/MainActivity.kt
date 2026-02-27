package com.example.wearstickynotes

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StickyNotesApp(
                    onOpenUri = { uri -> importNotes(uri) }
                )
            }
        }
    }

    private fun importNotes(uri: Uri): List<StickyNote> {
        return contentResolver.openInputStream(uri)?.use { input ->
            val json = input.bufferedReader().readText()
            Json { ignoreUnknownKeys = true }.decodeFromString<StickyNotesFile>(json).stickyNotes
        } ?: emptyList()
    }
}

@Composable
private fun StickyNotesApp(onOpenUri: (Uri) -> List<StickyNote>) {
    val context = LocalContext.current
    val notes = remember {
        mutableStateListOf<StickyNote>().apply {
            addAll(defaultStickyNotes())
        }
    }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showBack by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }

    val handlePickedUri: (Uri) -> Unit = { uri ->
        loading = true
        runCatching {
            onOpenUri(uri)
        }.onSuccess { imported ->
            notes.clear()
            notes.addAll(imported)
            selectedIndex = 0
            showBack = false
            Toast.makeText(context, "Imported ${imported.size} sticky notes", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Import failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
        loading = false
    }

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            handlePickedUri(uri)
        }
    }

    val openContent = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            handlePickedUri(uri)
        }
    }

    fun launchJsonPicker() {
        try {
            openDocument.launch(arrayOf("application/json", "text/plain", "*/*"))
        } catch (_: ActivityNotFoundException) {
            runCatching {
                openContent.launch("*/*")
            }.onFailure {
                Toast.makeText(
                    context,
                    "No file picker available on this watch. Install My Files or import from phone.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    when {
        loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        notes.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { launchJsonPicker() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
                ) {
                    Text("Import JSON")
                }
            }
        }

        else -> {
            val note = notes[selectedIndex]
            val text = if (showBack) note.back.text else note.front.text
            val label = if (showBack) note.back.label else note.front.label

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onRotaryScrollEvent {
                        rotaryAccumulator += it.verticalScrollPixels
                        when {
                            rotaryAccumulator > 25f -> {
                                selectedIndex = (selectedIndex + 1).coerceAtMost(notes.lastIndex)
                                showBack = false
                                rotaryAccumulator = 0f
                            }

                            rotaryAccumulator < -25f -> {
                                selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                showBack = false
                                rotaryAccumulator = 0f
                            }
                        }
                        true
                    }
                    .padding(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(parseColor(note.color))
                    .clickable { showBack = !showBack },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${selectedIndex + 1}/${notes.size} • ${label}",
                        fontSize = 12.sp,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = text,
                        color = Color.Black,
                        fontSize = adaptiveFontSize(text),
                        lineHeight = adaptiveFontSize(text) * 1.2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                    )
                }

                Button(
                    onClick = { launchJsonPicker() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x66000000)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text("Import JSON")
                }
            }
        }
    }
}

private fun defaultStickyNotes(): List<StickyNote> = listOf(
    StickyNote(
        id = 101,
        flowId = 1,
        flowName = "Daily Phrases",
        cardId = 11,
        cardTitle = "Greetings",
        color = "#FFFFF8A6",
        rotation = -2.5,
        front = NoteSide(label = "front", text = "你好"),
        back = NoteSide(label = "back", text = "Hello")
    ),
    StickyNote(
        id = 102,
        flowId = 1,
        flowName = "Daily Phrases",
        cardId = 12,
        cardTitle = "Thanks",
        color = "#FFD7E8FF",
        rotation = 1.0,
        front = NoteSide(label = "front", text = "谢谢"),
        back = NoteSide(label = "back", text = "Thank you")
    ),
    StickyNote(
        id = 103,
        flowId = 2,
        flowName = "Travel",
        cardId = 21,
        cardTitle = "Where",
        color = "#FFFFDAD7",
        rotation = 0.0,
        front = NoteSide(label = "front", text = "厕所在哪里？"),
        back = NoteSide(label = "back", text = "Where is the restroom?")
    )
)

private fun parseColor(value: String): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(value))
    }.getOrDefault(Color(0xFFFFF8A6))
}

private fun adaptiveFontSize(text: String) = when {
    text.length <= 6 -> 42.sp
    text.length <= 16 -> 34.sp
    text.length <= 32 -> 28.sp
    text.length <= 56 -> 22.sp
    else -> 18.sp
}

@Serializable
private data class StickyNotesFile(
    val version: Int,
    val generatedAt: Long,
    @SerialName("stickyNotes") val stickyNotes: List<StickyNote> = emptyList(),
    val totalStickyNotes: Int = 0,
    val totalFlows: Int = 0
)

@Serializable
private data class StickyNote(
    val id: Long,
    val flowId: Long,
    val flowName: String,
    val cardId: Long,
    val cardTitle: String,
    val color: String,
    val rotation: Double,
    val front: NoteSide,
    val back: NoteSide
)

@Serializable
private data class NoteSide(
    val label: String,
    val text: String
)
