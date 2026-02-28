package com.example.wearstickynotes

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StickyNotesApp(importer = PhoneImportClient(this))
            }
        }
    }
}

@Composable
private fun StickyNotesApp(importer: PhoneImportClient) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notes = remember {
        mutableStateListOf<StickyNote>().apply { addAll(defaultStickyNotes()) }
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    var importState by remember { mutableStateOf<ImportState>(ImportState.Idle) }
    var services by remember { mutableStateOf(emptyList<DiscoveredService>()) }
    var manualAddress by remember { mutableStateOf("") }
    var shuffleMode by remember { mutableStateOf(false) }
    var textScale by remember { mutableStateOf(TextScaleOption.Large) }
    var noteOrder by remember { mutableStateOf(notes.indices.toList()) }
    val noteSideState = remember { mutableStateMapOf<Long, Boolean>() }

    fun reorderNotes() {
        noteOrder = if (shuffleMode) notes.indices.shuffled() else notes.indices.toList()
        selectedIndex = 0
    }

    fun onImported(imported: List<StickyNote>) {
        notes.clear()
        notes.addAll(imported)
        noteSideState.clear()
        reorderNotes()
        importState = ImportState.Imported(imported.size)
    }

    fun startDiscovery() {
        scope.launch {
            importState = ImportState.Searching
            val found = importer.discoverServices(timeoutMs = 8_000)
            services = found
            importState = ImportState.DeviceList(found)
        }
    }

    fun runImport(target: ConnectionTarget) {
        scope.launch {
            importState = ImportState.RequestingApproval(target)
            val result = importer.importFromTarget(
                target = target,
                clientName = "Wear ${android.os.Build.MODEL}",
                onWaiting = { importState = ImportState.Waiting },
                onDownloading = { importState = ImportState.Downloading }
            )

            result.onSuccess { imported ->
                onImported(imported)
                Toast.makeText(context, "Imported ${imported.size} notes", Toast.LENGTH_SHORT).show()
            }.onFailure {
                importState = ImportState.Failed(it.message ?: "Unknown error")
            }
        }
    }

    val orderedNotes = noteOrder.mapNotNull { index -> notes.getOrNull(index) }
    val currentNote = orderedNotes.getOrNull(selectedIndex)
    val showBack = currentNote?.let { noteSideState[it.id] ?: false } ?: false

    when (val state = importState) {
        ImportState.Searching,
        is ImportState.DeviceList,
        is ImportState.RequestingApproval,
        ImportState.Waiting,
        ImportState.Downloading,
        is ImportState.Imported,
        is ImportState.Failed -> {
            ImportFlowScreen(
                state = state,
                services = services,
                manualAddress = manualAddress,
                onManualAddressChange = { manualAddress = it },
                onDiscover = { startDiscovery() },
                onSelectService = { runImport(ConnectionTarget(it.host, it.port)) },
                onManualConnect = {
                    val parsed = parseManualAddress(manualAddress)
                    if (parsed == null) {
                        importState = ImportState.Failed("Use format IP:port")
                    } else {
                        runImport(parsed)
                    }
                },
                onClose = { importState = ImportState.Idle }
            )
        }

        ImportState.Idle -> {
            NotesScreen(
                notes = orderedNotes,
                selectedIndex = selectedIndex,
                showBack = showBack,
                rotaryAccumulator = rotaryAccumulator,
                onRotaryAccumulatorChange = { rotaryAccumulator = it },
                onSelectedIndexChange = { selectedIndex = it },
                onFlip = { noteId ->
                    val current = noteSideState[noteId] ?: false
                    noteSideState[noteId] = !current
                },
                onImportFromPhone = { startDiscovery() },
                shuffleMode = shuffleMode,
                onToggleShuffle = {
                    shuffleMode = !shuffleMode
                    reorderNotes()
                },
                textScale = textScale,
                onTextScaleChange = { textScale = it }
            )
        }
    }
}

@Composable
private fun ImportFlowScreen(
    state: ImportState,
    services: List<DiscoveredService>,
    manualAddress: String,
    onManualAddressChange: (String) -> Unit,
    onDiscover: () -> Unit,
    onSelectService: (DiscoveredService) -> Unit,
    onManualConnect: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            when (state) {
                ImportState.Searching -> {
                    CircularProgressIndicator()
                    Text("Searching phones…")
                }

                is ImportState.DeviceList -> {
                    Text("Select a phone")
                    if (services.isEmpty()) {
                        Text("No devices found")
                    }
                    services.forEach { service ->
                        Button(onClick = { onSelectService(service) }) {
                            Text("${service.displayName} (${service.host}:${service.port})")
                        }
                    }
                }

                is ImportState.RequestingApproval -> {
                    CircularProgressIndicator()
                    Text("Requesting approval…")
                    Text("${state.target.host}:${state.target.port}")
                }

                ImportState.Waiting -> {
                    CircularProgressIndicator()
                    Text("Waiting for phone approval…")
                }

                ImportState.Downloading -> {
                    CircularProgressIndicator()
                    Text("Downloading sticky notes…")
                }

                is ImportState.Imported -> {
                    Text("Imported ${state.count} notes")
                    Button(onClick = onClose) { Text("Done") }
                }

                is ImportState.Failed -> {
                    Text("Failed: ${state.message}", color = Color(0xFFB00020), textAlign = TextAlign.Center)
                }

                ImportState.Idle -> Unit
            }

            if (state is ImportState.DeviceList || state is ImportState.Failed) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = manualAddress,
                        onValueChange = onManualAddressChange,
                        label = { Text("IP:port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
                Button(onClick = onManualConnect) { Text("Manual connect") }
                Button(onClick = onDiscover) { Text("Search again") }
                Button(onClick = onClose) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun NotesScreen(
    notes: List<StickyNote>,
    selectedIndex: Int,
    showBack: Boolean,
    rotaryAccumulator: Float,
    onRotaryAccumulatorChange: (Float) -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
    onFlip: (Long) -> Unit,
    onImportFromPhone: () -> Unit,
    shuffleMode: Boolean,
    onToggleShuffle: () -> Unit,
    textScale: TextScaleOption,
    onTextScaleChange: (TextScaleOption) -> Unit
) {
    var horizontalDragSum by remember { mutableFloatStateOf(0f) }
    var showTray by remember { mutableStateOf(false) }
    val noteScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val trayScrimAlpha by animateFloatAsState(
        targetValue = if (showTray) 0.30f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 480f),
        label = "trayScrimAlpha"
    )

    LaunchedEffect(notes.size, showTray) {
        if (!showTray) {
            focusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onImportFromPhone) { Text("Import from phone") }
            }
            return@Box
        }

        val safeIndex = selectedIndex.coerceIn(0, notes.lastIndex)
        val note = notes[safeIndex]
        val text = if (showBack) note.back.text else note.front.text
        val label = if (showBack) note.back.label else note.front.label

        LaunchedEffect(note.id, showBack, textScale) {
            noteScrollState.scrollTo(0)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onRotaryScrollEvent {
                    var updated = rotaryAccumulator + it.verticalScrollPixels
                    when {
                        updated > 25f -> {
                            onSelectedIndexChange((safeIndex + 1).coerceAtMost(notes.lastIndex))
                            updated = 0f
                        }

                        updated < -25f -> {
                            onSelectedIndexChange((safeIndex - 1).coerceAtLeast(0))
                            updated = 0f
                        }
                    }
                    onRotaryAccumulatorChange(updated)
                    true
                }
                .pointerInput(safeIndex, notes.size) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount -> horizontalDragSum += dragAmount },
                        onDragEnd = {
                            when {
                                horizontalDragSum > 36f -> onSelectedIndexChange((safeIndex - 1).coerceAtLeast(0))
                                horizontalDragSum < -36f -> onSelectedIndexChange((safeIndex + 1).coerceAtMost(notes.lastIndex))
                            }
                            horizontalDragSum = 0f
                        },
                        onDragCancel = { horizontalDragSum = 0f }
                    )
                }
                .pointerInput(note.id, showTray) {
                    detectTapGestures(
                        onDoubleTap = { showTray = !showTray },
                        onTap = {
                            if (!showTray) {
                                onFlip(note.id)
                            }
                        }
                    )
                }
                .padding(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(noteRadialGradient(note)),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 14.dp)
            ) {
                val density = LocalDensity.current
                val textMeasurer = rememberTextMeasurer()
                val horizontalPadding = 12.dp
                val headerReserved = 30.dp
                val baseFontSize = adaptiveFontSize(text) * textScale.factor

                val maxWidthPx = with(density) { (maxWidth - (horizontalPadding * 2)).toPx().roundToInt().coerceAtLeast(1) }
                val maxHeightPx = with(density) { (maxHeight - headerReserved).toPx().roundToInt().coerceAtLeast(1) }

                fun fitsOnSingleScreen(fontSize: TextUnit): Boolean {
                    val layout = textMeasurer.measure(
                        text = AnnotatedString(text),
                        style = TextStyle(fontSize = fontSize, lineHeight = fontSize * 1.2),
                        constraints = Constraints(maxWidth = maxWidthPx)
                    )
                    return layout.size.height <= maxHeightPx
                }

                val fitsAtSelectedSize = fitsOnSingleScreen(baseFontSize)
                val effectiveFontSizeRaw = if (fitsAtSelectedSize) {
                    listOf(1.30f, 1.22f, 1.16f, 1.10f, 1.06f, 1.0f)
                        .firstNotNullOfOrNull { factor ->
                            val candidate = baseFontSize * factor
                            if (fitsOnSingleScreen(candidate)) candidate else null
                        } ?: baseFontSize
                } else {
                    baseFontSize
                }
                val effectiveFontSize = clampToMaxNoteFont(effectiveFontSizeRaw)
                val effectiveLineHeight = effectiveFontSize * 1.2
                val useScrollableTopLayout = !fitsAtSelectedSize

                Text(
                    text = "${safeIndex + 1}/${notes.size} • ${label}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                )

                if (useScrollableTopLayout) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = headerReserved)
                            .verticalScroll(noteScrollState)
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = effectiveFontSize,
                            lineHeight = effectiveLineHeight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = horizontalPadding)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = effectiveFontSize,
                            lineHeight = effectiveLineHeight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = horizontalPadding)
                        )
                    }
                }
            }
        }

        if (trayScrimAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = trayScrimAlpha))
                    .clickable { showTray = false }
            )
        }

        AnimatedVisibility(
            visible = showTray,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xDD101418))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    showTray = false
                    onImportFromPhone()
                }) { Text("Import notes") }

                Button(onClick = onToggleShuffle) {
                    Text(if (shuffleMode) "Shuffle: On" else "Shuffle: Off")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextScaleOption.entries.forEach { option ->
                        Button(
                            onClick = { onTextScaleChange(option) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (option == textScale) Color(0xFF2D6EEA) else Color(0xFF2D2D2D)
                            )
                        ) {
                            Text(option.label)
                        }
                    }
                }
            }
        }
    }
}

private enum class TextScaleOption(val label: String, val factor: Float) {
    ExtraSmall("XS", 0.74f),
    Small("S", 0.86f),
    Large("L", 1.16f)
}

private sealed interface ImportState {
    data object Idle : ImportState
    data object Searching : ImportState
    data class DeviceList(val devices: List<DiscoveredService>) : ImportState
    data class RequestingApproval(val target: ConnectionTarget) : ImportState
    data object Waiting : ImportState
    data object Downloading : ImportState
    data class Imported(val count: Int) : ImportState
    data class Failed(val message: String) : ImportState
}

private data class DiscoveredService(
    val displayName: String,
    val host: String,
    val port: Int
)

private data class ConnectionTarget(val host: String, val port: Int)

private class PhoneImportClient(context: Context) {
    private val appContext = context.applicationContext
    private val nsdManager = appContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder().build()

    suspend fun discoverServices(timeoutMs: Long): List<DiscoveredService> = withContext(Dispatchers.IO) {
        val found = ConcurrentHashMap<String, DiscoveredService>()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != "_timescape._tcp.") return
                Thread {
                    runCatching {
                        kotlinx.coroutines.runBlocking {
                            resolve(serviceInfo)?.let { resolved ->
                                found["${resolved.host}:${resolved.port}"] = resolved
                            }
                        }
                    }
                }.start()
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
        }

        runCatching {
            nsdManager.discoverServices("_timescape._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
            delay(timeoutMs)
        }

        runCatching { nsdManager.stopServiceDiscovery(listener) }
        found.values.sortedBy { it.displayName }
    }

    private suspend fun resolve(info: NsdServiceInfo): DiscoveredService? =
        suspendCancellableCoroutine { cont ->
            val listener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    if (cont.isActive) cont.resume(null)
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    if (!cont.isActive) return
                    val host = serviceInfo.host?.hostAddress ?: return cont.resume(null)
                    cont.resume(
                        DiscoveredService(
                            displayName = serviceInfo.serviceName ?: host,
                            host = host,
                            port = serviceInfo.port
                        )
                    )
                }
            }
            nsdManager.resolveService(info, listener)
        }

    suspend fun importFromTarget(
        target: ConnectionTarget,
        clientName: String,
        onWaiting: () -> Unit,
        onDownloading: () -> Unit
    ): Result<List<StickyNote>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = "http://${target.host}:${target.port}"

            // Optional, ignore failures
            runCatching {
                val metaRequest = Request.Builder().url("$base/meta").get().build()
                client.newCall(metaRequest).execute().close()
            }

            val sessionId = requestSession(base, clientName)
            onWaiting()
            val token = pollSession(base, sessionId)
            onDownloading()
            val exportRequest = Request.Builder().url("$base/export?token=$token").get().build()
            val payload = client.newCall(exportRequest).execute().use { response ->
                if (!response.isSuccessful) error("Export failed (${response.code})")
                response.body?.string().orEmpty()
            }
            json.decodeFromString<StickyNotesFile>(payload).stickyNotes
        }
    }

    private fun requestSession(base: String, clientName: String): String {
        val body = json.encodeToString(
            SessionRequest(
                clientId = UUID.randomUUID().toString(),
                clientName = clientName
            )
        ).toRequestBody("application/json".toMediaType())

        val req = Request.Builder().url("$base/session/request").post(body).build()
        val res = client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) error("Session request failed (${response.code})")
            response.body?.string().orEmpty()
        }
        return json.decodeFromString<SessionRequestResponse>(res).sessionId
    }

    private suspend fun pollSession(base: String, sessionId: String): String {
        val timeoutMs = 30_000L
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val req = Request.Builder().url("$base/session/status?sessionId=$sessionId").get().build()
            val body = client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) error("Status failed (${response.code})")
                response.body?.string().orEmpty()
            }
            val status = json.decodeFromString<SessionStatusResponse>(body)
            when (status.status.uppercase()) {
                "APPROVED" -> return status.token ?: error("Missing token")
                "DENIED" -> error("Denied on phone")
            }
            delay(1000)
        }
        error("Approval timed out")
    }
}

private fun parseManualAddress(value: String): ConnectionTarget? {
    val trimmed = value.trim()
    if (!trimmed.contains(':')) return null
    val host = trimmed.substringBefore(':').trim()
    val port = trimmed.substringAfter(':').trim().toIntOrNull() ?: return null
    if (host.isEmpty() || port !in 1..65535) return null
    return ConnectionTarget(host, port)
}

private fun defaultStickyNotes(): List<StickyNote> = listOf(
    StickyNote(
        id = 101,
        flowId = 1,
        flowName = "Daily",
        cardId = 11,
        cardTitle = "Morning Plan",
        color = "#34C79A",
        rotation = -2.5,
        front = NoteSide(label = "front", text = "Drink water before breakfast"),
        back = NoteSide(label = "back", text = "Finish about 500ml water before your first coffee so your energy is steadier through the morning.")
    ),
    StickyNote(
        id = 102,
        flowId = 1,
        flowName = "Daily",
        cardId = 12,
        cardTitle = "Mobility",
        color = "#2F86FF",
        rotation = 1.0,
        front = NoteSide(label = "front", text = "10-minute stretch reset"),
        back = NoteSide(label = "back", text = "Slowly stretch your neck, shoulders, hamstrings, and calves for ten minutes to reduce stiffness after sitting.")
    ),
    StickyNote(
        id = 103,
        flowId = 2,
        flowName = "Focus",
        cardId = 21,
        cardTitle = "Deep Work",
        color = "#8A7CFF",
        rotation = -1.0,
        front = NoteSide(label = "front", text = "Start one focused block"),
        back = NoteSide(label = "back", text = "Put your phone away and do one uninterrupted 25-minute block. Tiny wins build momentum faster than waiting for motivation.")
    ),
    StickyNote(
        id = 104,
        flowId = 3,
        flowName = "Health",
        cardId = 31,
        cardTitle = "Bedtime",
        color = "#3EBE76",
        rotation = 0.5,
        front = NoteSide(label = "front", text = "Sleep window tonight"),
        back = NoteSide(label = "back", text = "Try to go to sleep between 10:30 PM and 11:00 PM. Keep lights dim 30 minutes before bed to help melatonin rise.")
    ),
    StickyNote(
        id = 105,
        flowId = 4,
        flowName = "Learning",
        cardId = 41,
        cardTitle = "Language",
        color = "#E07A2E",
        rotation = -0.3,
        front = NoteSide(label = "front", text = "Review 5 new phrases"),
        back = NoteSide(label = "back", text = "Read each phrase out loud twice, then use it in a short sentence. Active recall beats passive rereading.")
    ),
    StickyNote(
        id = 106,
        flowId = 5,
        flowName = "Career",
        cardId = 51,
        cardTitle = "Weekly Planning",
        color = "#4AA3A1",
        rotation = 0.8,
        front = NoteSide(label = "front", text = "Define top 3 outcomes"),
        back = NoteSide(label = "back", text = "Before Monday starts, define the top three outcomes for the week and translate each outcome into one concrete action you can finish in under one hour.")
    ),
    StickyNote(
        id = 107,
        flowId = 5,
        flowName = "Career",
        cardId = 52,
        cardTitle = "Communication",
        color = "#6A89FF",
        rotation = -0.7,
        front = NoteSide(label = "front", text = "Write clearer updates"),
        back = NoteSide(label = "back", text = "When posting an update, include context, current status, blockers, and the next step with an owner, so teammates can respond quickly without back-and-forth questions.")
    ),
    StickyNote(
        id = 108,
        flowId = 6,
        flowName = "Mindset",
        cardId = 61,
        cardTitle = "Evening Reflection",
        color = "#A06BE5",
        rotation = 0.4,
        front = NoteSide(label = "front", text = "Reflect in three lines"),
        back = NoteSide(label = "back", text = "At night, write three short lines: one win from today, one lesson you learned, and one tiny improvement for tomorrow. This keeps progress visible and sustainable.")
    )
)

private fun parseBaseColor(value: String): Color {
    val normalized = if (value.length == 7) "#FF${value.removePrefix("#")}" else value
    return runCatching {
        Color(android.graphics.Color.parseColor(normalized))
    }.getOrDefault(Color(0xFFD7E8FF))
}

private fun noteRadialGradient(note: StickyNote): Brush {
    val base = parseBaseColor(note.color)
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(base.toArgb(), hsv)

    val hueShift = ((note.id % 5).toInt() - 2) * 3f
    val hue = (hsv[0] + hueShift + 360f) % 360f

    // Calm system-card styling: broad soft center + deep edge vignette.
    val center = hsvColor(
        hue = hue,
        saturation = (hsv[1] * 0.68f + 0.10f).coerceIn(0.22f, 0.55f),
        value = (hsv[2] * 0.86f + 0.04f).coerceIn(0.62f, 0.86f)
    )
    val mid = hsvColor(
        hue = hue,
        saturation = (hsv[1] * 0.72f + 0.12f).coerceIn(0.26f, 0.58f),
        value = (hsv[2] * 0.58f).coerceIn(0.42f, 0.62f)
    )
    val edge = hsvColor(
        hue = hue,
        saturation = (hsv[1] * 0.80f + 0.14f).coerceIn(0.30f, 0.64f),
        value = (hsv[2] * 0.34f).coerceIn(0.24f, 0.42f)
    )

    return Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to center,
            0.62f to mid,
            1.0f to edge
        )
    )
}

private fun hsvColor(hue: Float, saturation: Float, value: Float): Color {
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
}

private fun clampToMaxNoteFont(fontSize: TextUnit): TextUnit {
    val maxFontSize = 24.sp
    return if (fontSize.value > maxFontSize.value) maxFontSize else fontSize
}

private fun adaptiveFontSize(text: String) = when {
    text.length <= 12 -> 42.sp
    text.length <= 28 -> 34.sp
    text.length <= 56 -> 28.sp
    text.length <= 92 -> 24.sp
    text.length <= 140 -> 20.sp
    else -> 17.sp
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

@Serializable
private data class SessionRequest(
    val clientId: String,
    val clientName: String
)

@Serializable
private data class SessionRequestResponse(
    val sessionId: String
)

@Serializable
private data class SessionStatusResponse(
    val status: String,
    val token: String? = null
)
