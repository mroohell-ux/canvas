package com.example.wearstickynotes

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    var showBack by remember { mutableStateOf(false) }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    var importState by remember { mutableStateOf<ImportState>(ImportState.Idle) }
    var services by remember { mutableStateOf(emptyList<DiscoveredService>()) }
    var manualAddress by remember { mutableStateOf("") }

    fun onImported(imported: List<StickyNote>) {
        notes.clear()
        notes.addAll(imported)
        selectedIndex = 0
        showBack = false
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
                notes = notes,
                selectedIndex = selectedIndex,
                showBack = showBack,
                rotaryAccumulator = rotaryAccumulator,
                onRotaryAccumulatorChange = { rotaryAccumulator = it },
                onSelectedIndexChange = {
                    selectedIndex = it
                    showBack = false
                },
                onFlip = { showBack = !showBack },
                onImportFromPhone = { startDiscovery() }
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
    onFlip: () -> Unit,
    onImportFromPhone: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onImportFromPhone) { Text("Import from phone") }
            }
            return@Box
        }

        val note = notes[selectedIndex]
        val text = if (showBack) note.back.text else note.front.text
        val label = if (showBack) note.back.label else note.front.label

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent {
                    var updated = rotaryAccumulator + it.verticalScrollPixels
                    when {
                        updated > 25f -> {
                            onSelectedIndexChange((selectedIndex + 1).coerceAtMost(notes.lastIndex))
                            updated = 0f
                        }

                        updated < -25f -> {
                            onSelectedIndexChange((selectedIndex - 1).coerceAtLeast(0))
                            updated = 0f
                        }
                    }
                    onRotaryAccumulatorChange(updated)
                    true
                }
                .padding(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(parseColor(note.color))
                .clickable { onFlip() },
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
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        Button(
            onClick = onImportFromPhone,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x88000000)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
        ) {
            Text("Phone")
        }
    }
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
        color = "#FFF8A6",
        rotation = -2.5,
        front = NoteSide(label = "front", text = "Drink water"),
        back = NoteSide(label = "back", text = "500ml before breakfast")
    ),
    StickyNote(
        id = 102,
        flowId = 1,
        flowName = "Daily",
        cardId = 11,
        cardTitle = "Morning Plan",
        color = "#D7E8FF",
        rotation = 1.0,
        front = NoteSide(label = "front", text = "Stretch 10 min"),
        back = NoteSide(label = "back", text = "Neck + hamstrings")
    )
)

private fun parseColor(value: String): Color {
    val normalized = if (value.length == 7) "#FF${value.removePrefix("#")}" else value
    return runCatching {
        Color(android.graphics.Color.parseColor(normalized))
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
