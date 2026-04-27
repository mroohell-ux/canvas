package com.example.wearstickynotes

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.InputDevice
import android.view.MotionEvent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.coroutines.resume
import kotlin.random.Random

private const val DEBUG_TAG = "WearStickyNotes"
private const val SWIPE_MIN_FLING_VELOCITY_PX = 650f
private const val PREVIEW_MIN_FLING_VELOCITY_PX = 45f
private const val SWIPE_ACCEL_VELOCITY_2_PAGES = 2800f
private const val SWIPE_ACCEL_VELOCITY_3_PAGES = 4000f
private const val SWIPE_ACCEL_VELOCITY_4_PAGES = 5600f
private const val SWIPE_MAX_PAGES_PER_FLING = 3
private const val GENERIC_SCROLL_PAGE_THRESHOLD = 1f

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

    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences("sticky_prefs", Context.MODE_PRIVATE)
    }
    val storageJson = remember { Json { ignoreUnknownKeys = true } }

    val initialNotes = remember(prefs, storageJson) {
        runCatching {
            prefs.getString("notes_payload", null)
                ?.takeIf { it.isNotBlank() }
                ?.let { storageJson.decodeFromString<List<StickyNote>>(it) }
        }.getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: defaultStickyNotes()
    }

    val notes = remember {
        mutableStateListOf<StickyNote>().apply { addAll(initialNotes) }
    }

    val initialAppScreen = remember(prefs) {
        if (prefs.getBoolean("last_screen_notes", false)) AppScreen.Notes else AppScreen.CardFlows
    }
    val initialSelectedFlowId = remember(prefs) {
        if (prefs.contains("last_selected_flow_id")) prefs.getLong("last_selected_flow_id", Long.MIN_VALUE) else null
    }

    var appScreen by rememberSaveable { mutableStateOf(initialAppScreen) }
    var selectedFlowIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingRestoreFlowId by remember { mutableStateOf(initialSelectedFlowId) }
    val initialFlowLastOpened = remember(prefs, storageJson) {
        runCatching {
            prefs.getString("flow_last_opened_note_index", null)
                ?.takeIf { it.isNotBlank() }
                ?.let { storageJson.decodeFromString<Map<String, Int>>(it) }
        }.getOrNull().orEmpty()
            .mapNotNull { (key, value) -> key.toLongOrNull()?.let { id -> id to value } }
            .toMap()
    }
    val flowLastOpenedNoteIndex = remember {
        mutableStateMapOf<Long, Int>().apply { putAll(initialFlowLastOpened) }
    }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    var importState by remember { mutableStateOf<ImportState>(ImportState.Idle) }
    var services by remember { mutableStateOf(emptyList<DiscoveredService>()) }
    var manualAddress by remember { mutableStateOf("") }

    val initialCollectionIds = remember(prefs, storageJson) {
        runCatching {
            prefs.getString("collection_note_ids", null)
                ?.takeIf { it.isNotBlank() }
                ?.let { storageJson.decodeFromString<Set<String>>(it) }
        }.getOrNull().orEmpty()
    }
    val initialShuffleMode = remember(prefs) {
        prefs.getBoolean("shuffle_mode", false)
    }
    val initialTextScale = remember(prefs) {
        TextScaleOption.fromStorage(
            prefs.getString("text_scale", TextScaleOption.Large.storageKey)
                ?: TextScaleOption.Large.storageKey
        )
    }

    var shuffleMode by remember { mutableStateOf(initialShuffleMode) }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var textScale by remember { mutableStateOf(initialTextScale) }
    val noteSideState = remember { mutableStateMapOf<String, Boolean>() }
    val collectionNoteState = remember {
        mutableStateMapOf<String, Boolean>().apply {
            initialCollectionIds.forEach { id -> this[id] = true }
        }
    }

    fun stableShuffledNotes(list: List<StickyNote>, seed: Long): List<StickyNote> {
        val sorted = list.sortedBy { it.id }
        return if (shuffleMode) sorted.shuffled(Random(seed)) else sorted
    }

    val groupedFlows = notes.groupBy { "${it.flowId}|${it.flowName}" }
        .map { (_, flowNotes) ->
            CardFlow(
                id = flowNotes.first().flowId,
                name = flowNotes.first().flowName,
                notes = stableShuffledNotes(flowNotes, shuffleSeed.toLong() + flowNotes.first().flowId)
            )
        }
        .sortedBy { it.name }

    val allNotesFlow = CardFlow(
        id = Long.MIN_VALUE,
        name = "All Notes",
        notes = stableShuffledNotes(notes, shuffleSeed.toLong() + Long.MIN_VALUE)
    )
    val collectionNotes = notes
        .filter { collectionNoteState[it.id] == true }
        .let { filtered ->
            val sorted = filtered.sortedWith(compareBy<StickyNote> { it.cardTitle }.thenBy { it.id })
            if (shuffleMode) sorted.shuffled(Random(shuffleSeed.toLong() + (Long.MIN_VALUE + 1))) else sorted
        }

    val collectionsFlow = CardFlow(
        id = Long.MIN_VALUE + 1,
        name = "Collections",
        notes = collectionNotes
    )

    val flowBuckets = buildList {
        add(allNotesFlow)
        add(collectionsFlow)
        addAll(groupedFlows)
    }

    BackHandler {
        when {
            importState !is ImportState.Idle -> {
                importState = ImportState.Idle
            }
            appScreen == AppScreen.Notes -> {
                appScreen = AppScreen.CardFlows
            }
            else -> {
                (context as? Activity)?.let { activity ->
                    Log.d(DEBUG_TAG, "Back: exiting task from flow level")
                    activity.finishAffinity()
                    activity.finish()
                }
            }
        }
    }

    LaunchedEffect(flowBuckets, pendingRestoreFlowId) {
        val restoreFlowId = pendingRestoreFlowId ?: return@LaunchedEffect
        val restoredIndex = flowBuckets.indexOfFirst { it.id == restoreFlowId }
        if (restoredIndex >= 0) {
            selectedFlowIndex = restoredIndex
        }
        pendingRestoreFlowId = null
    }

    val safeFlowIndex = selectedFlowIndex.coerceIn(0, (flowBuckets.lastIndex).coerceAtLeast(0))
    val activeFlow = flowBuckets.getOrNull(safeFlowIndex)
    val flowNotes = activeFlow?.notes.orEmpty()
    val rememberedNoteIndex = activeFlow?.let { flowLastOpenedNoteIndex[it.id] ?: 0 } ?: 0
    val safeNoteIndex = rememberedNoteIndex.coerceIn(0, (flowNotes.lastIndex).coerceAtLeast(0))

    LaunchedEffect(shuffleMode) {
        prefs.edit().putBoolean("shuffle_mode", shuffleMode).apply()
    }

    LaunchedEffect(textScale) {
        prefs.edit().putString("text_scale", textScale.storageKey).apply()
    }

    LaunchedEffect(notes.toList()) {
        prefs.edit()
            .putString("notes_payload", storageJson.encodeToString(notes.toList()))
            .apply()
    }

    LaunchedEffect(notes.size, collectionNoteState.toMap()) {
        val validIds = notes.asSequence().map { it.id }.toSet()
        val selectedCollectionIds = collectionNoteState
            .asSequence()
            .filter { it.value && it.key in validIds }
            .map { it.key }
            .toSet()

        prefs.edit()
            .putString("collection_note_ids", storageJson.encodeToString(selectedCollectionIds))
            .apply()
    }

    LaunchedEffect(flowLastOpenedNoteIndex.toMap()) {
        val asStorageMap = flowLastOpenedNoteIndex.mapKeys { it.key.toString() }
        prefs.edit()
            .putString("flow_last_opened_note_index", storageJson.encodeToString(asStorageMap))
            .apply()
    }

    LaunchedEffect(appScreen) {
        prefs.edit()
            .putBoolean("last_screen_notes", appScreen == AppScreen.Notes)
            .apply()
    }

    LaunchedEffect(selectedFlowIndex, flowBuckets) {
        flowBuckets.getOrNull(selectedFlowIndex)?.let { flow ->
            prefs.edit()
                .putLong("last_selected_flow_id", flow.id)
                .apply()
        }
    }

    fun onImported(imported: List<StickyNote>) {
        Log.d(DEBUG_TAG, "Import: onImported received ${imported.size} notes")
        val organized = organizeImportedNotes(imported)
        Log.d(DEBUG_TAG, "Import: organized into ${organized.size} notes across ${organized.groupBy { it.flowId }.size} flows")
        notes.clear()
        notes.addAll(organized)
        noteSideState.clear()
        collectionNoteState.keys.retainAll(organized.map { it.id }.toSet())
        selectedFlowIndex = 0
        flowLastOpenedNoteIndex.clear()
        appScreen = AppScreen.CardFlows
        importState = ImportState.Imported(organized.size)
    }

    fun startDiscovery() {
        scope.launch {
            Log.d(DEBUG_TAG, "Import: starting service discovery")
            importState = ImportState.Searching
            val found = importer.discoverServices(timeoutMs = 8_000)
            Log.d(DEBUG_TAG, "Import: discovery completed with ${found.size} services")
            services = found
            importState = ImportState.DeviceList(found)
        }
    }

    fun runImport(target: ConnectionTarget) {
        scope.launch {
            Log.d(DEBUG_TAG, "Import: starting import from ${target.host}:${target.port}")
            importState = ImportState.RequestingApproval(target)
            val result = importer.importFromTarget(
                target = target,
                clientName = "Wear ${android.os.Build.MODEL}",
                onWaiting = {
                    Log.d(DEBUG_TAG, "Import: waiting for phone approval")
                    importState = ImportState.Waiting
                },
                onDownloading = {
                    Log.d(DEBUG_TAG, "Import: approval granted, downloading export payload")
                    importState = ImportState.Downloading
                }
            )

            result.onSuccess { imported ->
                Log.d(DEBUG_TAG, "Import: import completed successfully with ${imported.size} notes")
                onImported(imported)
                Toast.makeText(context, "Imported ${imported.size} notes", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Log.e(DEBUG_TAG, "Import: import failed from ${target.host}:${target.port}", it)
                val message = it.message.orEmpty()
                importState = if (message.contains("CLEARTEXT communication", ignoreCase = true)) {
                    ImportState.Failed("Cleartext HTTP blocked; verify app cleartext setting and retry import.")
                } else if (it is SerializationException) {
                    ImportState.Failed("Import payload format is invalid. Please update phone/watch app versions and retry.")
                } else {
                    ImportState.Failed(message.ifBlank { "Unknown error" })
                }
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
                        Log.w(DEBUG_TAG, "Import: manual address parse failed for input='$manualAddress'")
                        importState = ImportState.Failed("Use format IP:port")
                    } else {
                        Log.d(DEBUG_TAG, "Import: manual address parsed host=${parsed.host} port=${parsed.port}")
                        runImport(parsed)
                    }
                },
                onClose = { importState = ImportState.Idle }
            )
        }

        ImportState.Idle -> {
            AnimatedContent(
                targetState = appScreen,
                transitionSpec = {
                    if (targetState == AppScreen.Notes) {
                        slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { it / 2 }
                        ) + fadeIn() togetherWith slideOutHorizontally(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            targetOffsetX = { -it / 4 }
                        ) + fadeOut()
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { -it / 3 }
                        ) + fadeIn() togetherWith slideOutHorizontally(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            targetOffsetX = { it / 2 }
                        ) + fadeOut()
                    }
                },
                label = "screenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.CardFlows -> CardFlowsScreen(
                        flows = flowBuckets,
                        selectedIndex = safeFlowIndex,
                        onSelectedIndexChange = { selectedFlowIndex = it },
                        onOpenSelectedFlow = {
                            if (flowBuckets.isNotEmpty()) {
                                val activeId = flowBuckets[safeFlowIndex].id
                                flowLastOpenedNoteIndex.putIfAbsent(activeId, 0)
                                appScreen = AppScreen.Notes
                            }
                        },
                        onImportFromPhone = { startDiscovery() },
                        shuffleMode = shuffleMode,
                        onToggleShuffle = {
                            if (!shuffleMode) {
                                shuffleSeed = Random.nextInt()
                            }
                            shuffleMode = !shuffleMode
                        },
                        textScale = textScale,
                        onTextScaleChange = { option -> textScale = option }
                    )

                    AppScreen.Notes -> NotesScreen(
                        flowName = activeFlow?.name ?: "Flow",
                        notes = flowNotes,
                        selectedIndex = safeNoteIndex,
                        rotaryAccumulator = rotaryAccumulator,
                        onRotaryAccumulatorChange = { rotaryAccumulator = it },
                        onSelectedIndexChange = { index ->
                            activeFlow?.let { flow -> flowLastOpenedNoteIndex[flow.id] = index }
                        },
                        isNoteBackVisible = { noteId -> noteSideState[noteId] ?: false },
                        onFlip = { noteId ->
                            val current = noteSideState[noteId] ?: false
                            noteSideState[noteId] = !current
                        },
                        isCollectionsFlow = activeFlow?.id == (Long.MIN_VALUE + 1),
                        isNoteInCollection = { noteId -> collectionNoteState[noteId] == true },
                        onToggleCollection = { noteId ->
                            val current = collectionNoteState[noteId] == true
                            collectionNoteState[noteId] = !current
                        },
                        onImportFromPhone = { startDiscovery() },
                        shuffleMode = shuffleMode,
                        onToggleShuffle = {
                            if (!shuffleMode) {
                                shuffleSeed = Random.nextInt()
                            }
                            shuffleMode = !shuffleMode
                        },
                        textScale = textScale,
                        onTextScaleChange = { textScale = it }
                    )
                }
            }
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CardFlowsScreen(
    flows: List<CardFlow>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onOpenSelectedFlow: () -> Unit,
    onImportFromPhone: () -> Unit,
    shuffleMode: Boolean,
    onToggleShuffle: () -> Unit,
    textScale: TextScaleOption,
    onTextScaleChange: (TextScaleOption) -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    var genericScrollAccumulator by remember { mutableFloatStateOf(0f) }
    var showTray by remember { mutableStateOf(false) }
    var lastHapticFlowIndex by remember { mutableIntStateOf(selectedIndex) }
    var dragHapticAnchorIndex by remember { mutableIntStateOf(selectedIndex) }
    val focusRequester = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val minScreenDp = minOf(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
    val spacingPx = with(density) { (minScreenDp * 0.32f).coerceIn(70.dp, 110.dp).toPx() }
    val bottomTrayEdgePx = with(density) { 56.dp.toPx() }
    val swipeOpenThresholdPx = with(density) { 24.dp.toPx() }
    val trayScrimAlpha by animateFloatAsState(
        targetValue = if (showTray) 0.30f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 480f),
        label = "flowTrayScrimAlpha"
    )

    LaunchedEffect(flows.size) {
        if (flows.isNotEmpty()) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != lastHapticFlowIndex) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticFlowIndex = selectedIndex
        }
        dragHapticAnchorIndex = selectedIndex
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent {
                var updated = rotaryAccumulator + it.verticalScrollPixels
                when {
                    updated > 25f -> {
                        onSelectedIndexChange((selectedIndex + 1).coerceAtMost(flows.lastIndex.coerceAtLeast(0)))
                        updated = 0f
                    }

                    updated < -25f -> {
                        onSelectedIndexChange((selectedIndex - 1).coerceAtLeast(0))
                        updated = 0f
                    }
                }
                rotaryAccumulator = updated
                true
            }
            .pointerInteropFilter { motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_SCROLL) {
                    val vertical = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    val horizontal = motionEvent.getAxisValue(MotionEvent.AXIS_HSCROLL)
                    val dominant = if (kotlin.math.abs(vertical) >= kotlin.math.abs(horizontal)) vertical else horizontal
                    val sourceHasRotary = motionEvent.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)

                    Log.d(
                        DEBUG_TAG,
                        "Input signal: flow genericMotion action=SCROLL sourceRotary=$sourceHasRotary v=$vertical h=$horizontal index=$selectedIndex"
                    )

                    var updated = genericScrollAccumulator + dominant
                    when {
                        updated >= GENERIC_SCROLL_PAGE_THRESHOLD -> {
                            onSelectedIndexChange((selectedIndex - 1).coerceAtLeast(0))
                            updated = 0f
                        }

                        updated <= -GENERIC_SCROLL_PAGE_THRESHOLD -> {
                            onSelectedIndexChange((selectedIndex + 1).coerceAtMost(flows.lastIndex.coerceAtLeast(0)))
                            updated = 0f
                        }
                    }
                    genericScrollAccumulator = updated
                    return@pointerInteropFilter true
                }
                false
            }
            .pointerInput(flows.size, selectedIndex) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragOffset = 0f
                        dragHapticAnchorIndex = selectedIndex
                    },
                    onHorizontalDrag = { _, amount ->
                        dragOffset += amount
                        val dragSteps = (dragOffset / spacingPx).roundToInt()
                        val previewIndex = (selectedIndex - dragSteps).coerceIn(0, flows.lastIndex.coerceAtLeast(0))
                        if (previewIndex != dragHapticAnchorIndex) {
                            val direction = if (previewIndex > dragHapticAnchorIndex) 1 else -1
                            var stepIndex = dragHapticAnchorIndex
                            while (stepIndex != previewIndex) {
                                stepIndex += direction
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            lastHapticFlowIndex = previewIndex
                            dragHapticAnchorIndex = previewIndex
                        }
                    },
                    onDragEnd = {
                        val dragSteps = (dragOffset / spacingPx).roundToInt()
                        val targetIndex = (selectedIndex - dragSteps).coerceIn(0, flows.lastIndex.coerceAtLeast(0))
                        if (targetIndex != selectedIndex) {
                            onSelectedIndexChange(targetIndex)
                        }
                        dragHapticAnchorIndex = targetIndex
                        dragOffset = 0f
                    },
                    onDragCancel = {
                        dragHapticAnchorIndex = selectedIndex
                        dragOffset = 0f
                    }
                )
            }
            .pointerInput(showTray) {
                if (!showTray) {
                    var startedFromBottom = false
                    var cumulativeDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            startedFromBottom = offset.y >= (size.height - bottomTrayEdgePx)
                            cumulativeDrag = 0f
                        },
                        onVerticalDrag = { _, dragAmount ->
                            if (!startedFromBottom) return@detectVerticalDragGestures
                            cumulativeDrag += dragAmount
                            if (cumulativeDrag <= -swipeOpenThresholdPx) {
                                showTray = true
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (flows.isEmpty()) {
            Text("No card flows available")
            return@Box
        }

        fun centerProgress(offset: Float): Float {
            // Keep scale interpolation linear across one "step" of movement so
            // growth/shrink remains perceptible for the full transition.
            return (1f - (abs(offset) / spacingPx)).coerceIn(0f, 1f)
        }

        fun scaleFor(offset: Float): Float = 0.80f + (centerProgress(offset) * 0.28f)
        fun alphaFor(offset: Float): Float = 0.28f + (centerProgress(offset) * 0.72f)

        BoxWithConstraints {
            val minScreenSize = minOf(maxWidth, maxHeight)
            val selectedCircleSize = (minScreenSize * 0.45f).coerceIn(110.dp, 146.dp)
            val sideCircleSize = (selectedCircleSize * 0.84f).coerceIn(88.dp, 124.dp)
            val railHeight = (selectedCircleSize * 1.28f).coerceIn(150.dp, 208.dp)
            val adaptiveSpacingPx = with(density) { (selectedCircleSize * 0.86f).toPx() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Flow ${selectedIndex + 1}/${flows.size}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Box(modifier = Modifier.fillMaxWidth().height(railHeight), contentAlignment = Alignment.Center) {
                    flows.forEachIndexed { index, flow ->
                        val targetOffset by animateFloatAsState(
                            targetValue = ((index - selectedIndex) * adaptiveSpacingPx) + dragOffset,
                            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                            label = "flowOffset$index"
                        )
                        val emphasisScale = scaleFor(targetOffset)
                        val emphasisAlpha = alphaFor(targetOffset)
                        FlowCircle(
                            flow = flow,
                            selected = index == selectedIndex,
                            circleSize = if (index == selectedIndex) selectedCircleSize else sideCircleSize,
                            onClick = {
                                if (index == selectedIndex) onOpenSelectedFlow() else onSelectedIndexChange(index)
                            },
                            emphasisScale = emphasisScale,
                            emphasisAlpha = emphasisAlpha,
                            modifier = Modifier
                                .zIndex(emphasisScale)
                                .offset { IntOffset(targetOffset.roundToInt(), 0) }
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
                .padding(start = 8.dp, end = 8.dp, bottom = 20.dp)
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
                            colors = if (textScale == option) ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2C6E49)
                            ) else ButtonDefaults.buttonColors()
                        ) { Text(option.label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowCircle(
    flow: CardFlow,
    selected: Boolean,
    circleSize: Dp,
    onClick: () -> Unit,
    emphasisScale: Float,
    emphasisAlpha: Float,
    modifier: Modifier = Modifier
) {
    val size by animateDpAsState(
        targetValue = circleSize,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 240f),
        label = "flowCircleSize"
    )
    val smoothedScale by animateFloatAsState(
        targetValue = emphasisScale,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 240f),
        label = "flowCircleScale"
    )
    val circleAlpha by animateFloatAsState(
        targetValue = emphasisAlpha,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = 260f),
        label = "flowCircleAlpha"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = smoothedScale
                scaleY = smoothedScale
                alpha = circleAlpha
            }
            .size(size)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF2A3744))
            .clickable(onClick = onClick)
            .padding(if (selected) 12.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF3A4B5C))
                .padding(if (selected) 10.dp else 8.dp)
        ) {
            Text(
                text = "${flow.name} (${flow.notes.size})",
                textAlign = TextAlign.Center,
                fontSize = if (selected) 11.sp else 9.sp,
                lineHeight = if (selected) 13.sp else 11.sp,
                modifier = Modifier
                    .padding(horizontal = if (selected) 8.dp else 5.dp, vertical = if (selected) 12.dp else 8.dp),
                color = Color.White.copy(alpha = if (selected) 1f else 0.92f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun NotesScreen(
    flowName: String,
    notes: List<StickyNote>,
    selectedIndex: Int,
    rotaryAccumulator: Float,
    onRotaryAccumulatorChange: (Float) -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
    isNoteBackVisible: (String) -> Boolean,
    onFlip: (String) -> Unit,
    isCollectionsFlow: Boolean,
    isNoteInCollection: (String) -> Boolean,
    onToggleCollection: (String) -> Unit,
    onImportFromPhone: () -> Unit,
    shuffleMode: Boolean,
    onToggleShuffle: () -> Unit,
    textScale: TextScaleOption,
    onTextScaleChange: (TextScaleOption) -> Unit
) {
    data class BubbleAnchor(val x: Float, val y: Float, val radiusScale: Float)
    val context = LocalContext.current

    fun wrappedNoteIndex(page: Int): Int {
        if (notes.isEmpty()) return 0
        val size = notes.size
        return ((page % size) + size) % size
    }

    fun nearestVirtualPage(currentPage: Int, targetIndex: Int): Int {
        if (notes.isEmpty()) return 0
        val size = notes.size
        val base = currentPage - wrappedNoteIndex(currentPage)
        val candidates = listOf(base + targetIndex, base + targetIndex + size, base + targetIndex - size)
        return candidates.minBy { kotlin.math.abs(it - currentPage) }
    }

    var showTray by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var isBubbleMode by remember { mutableStateOf(false) }
    var bubblePan by remember { mutableStateOf(Offset.Zero) }
    var bubbleShuffleSeed by remember { mutableIntStateOf(0) }
    var genericScrollAccumulator by remember { mutableFloatStateOf(0f) }
    var lastHapticNoteIndex by remember { mutableIntStateOf(selectedIndex) }
    val noteScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val initialVirtualPage = remember(notes.size, selectedIndex) {
        if (notes.isEmpty()) {
            0
        } else {
            val half = Int.MAX_VALUE / 2
            val centered = half - wrappedNoteIndex(half)
            centered + selectedIndex.coerceIn(0, notes.lastIndex)
        }
    }
    val pagerState = rememberPagerState(
        initialPage = initialVirtualPage,
        pageCount = { if (notes.isEmpty()) 0 else Int.MAX_VALUE }
    )
    val configuration = LocalConfiguration.current
    val minScreenDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val noteCount = notes.size.coerceAtLeast(1)
    val noteCountDensity = ((noteCount - 1).toFloat() / 99f).coerceIn(0f, 1f)
    val densityCurve = kotlin.math.sqrt(noteCountDensity)
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }
    val previewCircleSize = (minScreenDp * 0.40f).coerceIn(72f, 108f).dp
    val previewPageWidthPx = with(LocalDensity.current) { previewCircleSize.toPx() }
    val bubbleSpaceWidthPx = screenWidthPx * (0.98f + (densityCurve * 1.62f))
    val bubbleSpaceHeightPx = screenHeightPx * (1.02f + (densityCurve * 1.86f))
    val bubblePanLimitX = ((bubbleSpaceWidthPx - screenWidthPx) / 2f).coerceAtLeast(0f)
    val bubblePanLimitY = ((bubbleSpaceHeightPx - screenHeightPx) / 2f).coerceAtLeast(0f)
    val bubblePanSpeed = 9.8f
    val bubbleDiameterScale = (1.14f - (densityCurve * 0.34f)).coerceIn(0.64f, 1.18f)
    val bubbleItemSize = previewCircleSize * bubbleDiameterScale
    val bubbleItemSizePx = with(LocalDensity.current) { bubbleItemSize.toPx() }
    val bubbleAnchors = remember(notes.map { it.id }, bubbleSpaceWidthPx, bubbleSpaceHeightPx, bubbleShuffleSeed) {
        val safeCount = noteCount
        val minSpacing = (bubbleItemSizePx * (0.60f + (densityCurve * 0.24f))).coerceAtLeast(14f)
        val placedAnchors = mutableListOf<BubbleAnchor>()
        val arrangedNoteIndices = notes.indices.shuffled(Random(bubbleShuffleSeed.toLong()))
        notes.mapIndexed { index, note ->
            val arrangedIndex = arrangedNoteIndices[index]
            val seed = note.id.hashCode().toLong() xor bubbleShuffleSeed.toLong()
            val random = Random(seed)
            val normalizedIndex = (arrangedIndex + 0.5f) / safeCount.toFloat()
            var bestCandidate = BubbleAnchor(0f, 0f, random.nextFloat())
            var bestDistance = Float.NEGATIVE_INFINITY
            repeat(26) { attempt ->
                val theta = (arrangedIndex * 2.3999632f) + (attempt * 0.58f) + (random.nextFloat() * 0.24f)
                val radial = kotlin.math.sqrt(normalizedIndex) + (attempt * 0.012f)
                val ellipseX = (bubbleSpaceWidthPx * 0.38f) * radial.coerceAtMost(1.22f)
                val ellipseY = (bubbleSpaceHeightPx * 0.38f) * radial.coerceAtMost(1.22f)
                val candidate = BubbleAnchor(
                    x = (cos(theta) * ellipseX) + ((random.nextFloat() - 0.5f) * bubbleItemSizePx * 0.06f),
                    y = (sin(theta) * ellipseY) + ((random.nextFloat() - 0.5f) * bubbleItemSizePx * 0.06f),
                    radiusScale = random.nextFloat()
                )
                val nearestDistance = placedAnchors.minOfOrNull { placed ->
                    hypot(candidate.x - placed.x, candidate.y - placed.y)
                } ?: Float.MAX_VALUE
                if (nearestDistance > bestDistance) {
                    bestDistance = nearestDistance
                    bestCandidate = candidate
                }
                if (nearestDistance >= minSpacing) return@repeat
            }
            placedAnchors += bestCandidate
            bestCandidate
        }
    }
    fun updateBubblePan(deltaX: Float, deltaY: Float) {
        bubblePan = Offset(
            x = (bubblePan.x + (deltaX * bubblePanSpeed)).coerceIn(-bubblePanLimitX, bubblePanLimitX),
            y = (bubblePan.y + (deltaY * bubblePanSpeed)).coerceIn(-bubblePanLimitY, bubblePanLimitY)
        )
    }

    val swipeAccelerationConnection = remember(pagerState, notes.size, isPreviewMode, previewPageWidthPx) {
        object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (notes.isEmpty()) return Velocity.Zero

                val velocityX = available.x
                val absoluteVelocity = kotlin.math.abs(velocityX)

                if (isPreviewMode) {
                    val offsetFraction = pagerState.currentPageOffsetFraction
                    val dragDirection = when {
                        offsetFraction > 0.04f -> 1
                        offsetFraction < -0.04f -> -1
                        else -> 0
                    }
                    val velocityDirection = when {
                        velocityX < -PREVIEW_MIN_FLING_VELOCITY_PX -> 1
                        velocityX > PREVIEW_MIN_FLING_VELOCITY_PX -> -1
                        else -> 0
                    }
                    val travelDirection = if (velocityDirection != 0) velocityDirection else dragDirection

                    if (travelDirection != 0) {
                        val momentumEnergy = absoluteVelocity + (abs(offsetFraction) * previewPageWidthPx * 2.4f)
                        val momentumDistancePx = ((momentumEnergy * 0.34f) + (previewPageWidthPx * 0.52f))
                            .coerceIn(previewPageWidthPx * 0.55f, previewPageWidthPx * (SWIPE_MAX_PAGES_PER_FLING + 0.8f))
                        val carryPages = (momentumDistancePx / previewPageWidthPx)
                            .coerceIn(1f, SWIPE_MAX_PAGES_PER_FLING + 0.8f)
                        val baseTargetPage = pagerState.targetPage
                        val targetPage = baseTargetPage + (kotlin.math.ceil(carryPages).toInt() * travelDirection)
                        val animationDurationMs = (610f - (momentumEnergy / 14f))
                            .coerceIn(230f, 520f)
                            .toInt()

                        Log.d(
                            DEBUG_TAG,
                            "Input signal: preview momentum fling velocityX=$velocityX offsetFraction=$offsetFraction carryPages=$carryPages target=$targetPage base=$baseTargetPage durationMs=$animationDurationMs"
                        )

                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = targetPage,
                                animationSpec = tween(durationMillis = animationDurationMs, easing = LinearOutSlowInEasing)
                            )
                        }
                        return available
                    }
                }

                if (absoluteVelocity < SWIPE_MIN_FLING_VELOCITY_PX) {
                    // Tiny/accidental fling: let pager handle normal settle behavior.
                    return Velocity.Zero
                }

                // Preserve the page user already dragged toward and only add
                // EXTRA pages for stronger flicks, so release result matches
                // what user saw right before lifting finger.
                val baseTargetPage = pagerState.targetPage
                val extraPagesByVelocity = when {
                    absoluteVelocity >= SWIPE_ACCEL_VELOCITY_4_PAGES -> 3
                    absoluteVelocity >= SWIPE_ACCEL_VELOCITY_3_PAGES -> 2
                    absoluteVelocity >= SWIPE_ACCEL_VELOCITY_2_PAGES -> 1
                    else -> 0
                }

                val direction = if (velocityX < 0f) 1 else -1
                val targetPage = baseTargetPage + (extraPagesByVelocity * direction)
                val pagesSkipped = kotlin.math.abs(targetPage - pagerState.currentPage)
                val animationDurationMs = (420f - (absoluteVelocity / 18f))
                    .coerceIn(120f, if (isPreviewMode) 360f else 420f)
                    .toInt()

                if (targetPage != baseTargetPage && pagesSkipped <= SWIPE_MAX_PAGES_PER_FLING) {
                    Log.d(
                        DEBUG_TAG,
                        "Input signal: fling velocityX=$velocityX base=$baseTargetPage extra=$extraPagesByVelocity target=$targetPage from=${pagerState.currentPage}"
                    )
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = targetPage,
                            animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing)
                        )
                    }
                    return available
                }

                return Velocity.Zero
            }
        }
    }
    val starFontSize = (minScreenDp * 0.075f).coerceIn(12f, 18f).sp
    val starBottomPadding = (minScreenDp * 0.045f).coerceIn(8f, 16f).dp
    val trayScrimAlpha by animateFloatAsState(
        targetValue = if (showTray) 0.30f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 480f),
        label = "trayScrimAlpha"
    )
    val previewTransitionProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    DisposableEffect(isBubbleMode, notes.size) {
        if (!isBubbleMode || notes.size < 2 || sensorManager == null) {
            onDispose { }
        } else {
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometer == null) {
                onDispose { }
            } else {
                var gravityX = 0f
                var gravityY = 0f
                var gravityZ = 0f
                var lastShuffleAt = 0L
                val shakeThreshold = 12.5f
                val cooldownMs = 1_100L
                val alpha = 0.82f
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        gravityX = (alpha * gravityX) + ((1f - alpha) * event.values[0])
                        gravityY = (alpha * gravityY) + ((1f - alpha) * event.values[1])
                        gravityZ = (alpha * gravityZ) + ((1f - alpha) * event.values[2])

                        val linearX = event.values[0] - gravityX
                        val linearY = event.values[1] - gravityY
                        val linearZ = event.values[2] - gravityZ
                        val acceleration = hypot(hypot(linearX, linearY), linearZ)
                        val now = SystemClock.elapsedRealtime()
                        if (acceleration > shakeThreshold && now - lastShuffleAt > cooldownMs) {
                            lastShuffleAt = now
                            bubbleShuffleSeed += 1
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }

                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                onDispose { sensorManager.unregisterListener(listener) }
            }
        }
    }

    LaunchedEffect(isPreviewMode, isBubbleMode) {
        if (isBubbleMode) {
            val selectedAnchor = if (bubbleAnchors.isNotEmpty()) {
                bubbleAnchors.getOrNull(selectedIndex.coerceIn(0, bubbleAnchors.lastIndex))
            } else {
                null
            }
            bubblePan = if (selectedAnchor != null) {
                Offset(
                    x = (-selectedAnchor.x).coerceIn(-bubblePanLimitX, bubblePanLimitX),
                    y = (-selectedAnchor.y).coerceIn(-bubblePanLimitY, bubblePanLimitY)
                )
            } else {
                Offset.Zero
            }
        }
        if (!isPreviewMode) {
            isBubbleMode = false
            bubblePan = Offset.Zero
        }
        previewTransitionProgress.animateTo(
            targetValue = if (isPreviewMode) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.92f, stiffness = 180f)
        )
    }
    val screenWidthDp = configuration.screenWidthDp.dp
    val previewHorizontalPadding = ((screenWidthDp - previewCircleSize) / 2f).coerceAtLeast(0.dp)
    LaunchedEffect(notes.size, showTray) {
        if (!showTray && notes.isNotEmpty()) {
            // Request focus only when the focusable note container is in composition.
            focusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isCollectionsFlow) {
                    Text(
                        text = "Collections is empty",
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Button(onClick = onImportFromPhone) { Text("Import from phone") }
                }
            }
            return@Box
        }

        LaunchedEffect(notes, selectedIndex) {
            if (notes.isNotEmpty()) {
                val targetPage = nearestVirtualPage(
                    currentPage = pagerState.currentPage,
                    targetIndex = selectedIndex.coerceIn(0, notes.lastIndex)
                )
                if (targetPage != pagerState.currentPage) {
                    pagerState.scrollToPage(targetPage)
                }
            }
        }

        LaunchedEffect(pagerState, notes.size) {
            snapshotFlow { pagerState.currentPage }
                .collect { page ->
                    if (notes.isNotEmpty()) {
                        val wrappedIndex = wrappedNoteIndex(page)
                        if (wrappedIndex != lastHapticNoteIndex) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticNoteIndex = wrappedIndex
                        }
                    }
                }
        }

        LaunchedEffect(pagerState, notes.size) {
            // Use settledPage so parent-selected note updates only after user
            // releases and pager settles, preventing mid-drag content/color jumps.
            snapshotFlow { pagerState.settledPage }
                .collect { page ->
                    if (notes.isNotEmpty()) {
                        val wrappedIndex = wrappedNoteIndex(page)
                        Log.d(DEBUG_TAG, "Notes pager settled page changed to $page (wrapped=$wrappedIndex total=${notes.size})")
                        onSelectedIndexChange(wrappedIndex)
                    }
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .pointerInput(showTray, notes.size, isPreviewMode, isBubbleMode) {
                    if (!showTray && notes.isNotEmpty() && isPreviewMode && !isBubbleMode) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (abs(zoom - 1f) > 0.04f) {
                                isBubbleMode = true
                            }
                        }
                    }
                }
                .pointerInput(showTray, notes.size) {
                    if (!showTray && notes.isNotEmpty()) {
                        detectTapGestures(
                            onLongPress = {
                                if (!isPreviewMode) {
                                    isBubbleMode = false
                                    isPreviewMode = true
                                }
                            }
                        )
                    }
                }
                .pointerInteropFilter { motionEvent ->
                    if (motionEvent.action == MotionEvent.ACTION_SCROLL) {
                        val sourceHasRotary = motionEvent.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)
                        val vertical = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL)
                        val horizontal = motionEvent.getAxisValue(MotionEvent.AXIS_HSCROLL)
                        val dominant = if (kotlin.math.abs(vertical) >= kotlin.math.abs(horizontal)) vertical else horizontal

                        Log.d(
                            DEBUG_TAG,
                            "Input signal: genericMotion action=SCROLL sourceRotary=$sourceHasRotary v=$vertical h=$horizontal page=${pagerState.currentPage}"
                        )

                        var updated = genericScrollAccumulator + dominant
                        if (updated >= GENERIC_SCROLL_PAGE_THRESHOLD) {
                            val previous = pagerState.currentPage - 1
                            scope.launch { pagerState.animateScrollToPage(previous) }
                            onRotaryAccumulatorChange(0f)
                            updated = 0f
                            genericScrollAccumulator = updated
                            return@pointerInteropFilter true
                        }

                        if (updated <= -GENERIC_SCROLL_PAGE_THRESHOLD) {
                            val next = pagerState.currentPage + 1
                            scope.launch { pagerState.animateScrollToPage(next) }
                            onRotaryAccumulatorChange(0f)
                            updated = 0f
                            genericScrollAccumulator = updated
                            return@pointerInteropFilter true
                        }
                        genericScrollAccumulator = updated
                        return@pointerInteropFilter true
                    }
                    false
                }
                .onRotaryScrollEvent {
                    Log.d(
                        DEBUG_TAG,
                        "Input signal: rotary delta=${it.verticalScrollPixels}, page=${pagerState.currentPage}"
                    )
                    var updated = rotaryAccumulator + it.verticalScrollPixels
                    when {
                        updated > 25f -> {
                            val next = pagerState.currentPage + 1
                            scope.launch { pagerState.animateScrollToPage(next) }
                            updated = 0f
                        }

                        updated < -25f -> {
                            val previous = pagerState.currentPage - 1
                            scope.launch { pagerState.animateScrollToPage(previous) }
                            updated = 0f
                        }
                    }
                    onRotaryAccumulatorChange(updated)
                    true
                },
            contentAlignment = Alignment.Center
        ) {
            if (isBubbleMode) {
                val bubbleFontSize = when {
                    noteCount >= 90 -> 8.sp
                    noteCount >= 50 -> 9.sp
                    else -> 10.sp
                }
                val bubbleLineHeight = when {
                    noteCount >= 90 -> 10.sp
                    noteCount >= 50 -> 11.sp
                    else -> 12.sp
                }
                val bubbleSnippetLimit = when {
                    noteCount >= 90 -> 16
                    noteCount >= 50 -> 20
                    else -> 28
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(showTray, notes.size, isBubbleMode) {
                            if (!showTray && notes.isNotEmpty() && isBubbleMode) {
                                detectDragGestures { _, dragAmount ->
                                    updateBubblePan(dragAmount.x, dragAmount.y)
                                }
                            }
                        }
                        .pointerInput(showTray, notes.size, bubblePan, isBubbleMode) {
                            if (!showTray && notes.isNotEmpty()) {
                                detectTransformGestures { _, pan, _, _ ->
                                    updateBubblePan(pan.x, pan.y)
                                }
                            }
                        }
                ) {
                    notes.forEachIndexed { index, note ->
                        val anchor = bubbleAnchors.getOrNull(index) ?: BubbleAnchor(0f, 0f, 0.5f)
                        val distanceFromCenter = hypot(anchor.x, anchor.y)
                        val depthFade = (1f - (distanceFromCenter / (bubbleSpaceWidthPx * 0.9f))).coerceIn(0.58f, 1f)
                        val targetScale = (0.86f + anchor.radiusScale * 0.24f) * depthFade
                        val bubblePulse = rememberInfiniteTransition(label = "bubblePulse$index")
                        val driftProgress by bubblePulse.animateFloat(
                            initialValue = 0f,
                            targetValue = (2f * PI).toFloat(),
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 4600 + ((anchor.radiusScale * 2200f).roundToInt()),
                                    easing = LinearEasing
                                ),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "bubbleDrift$index"
                        )
                        val driftAmplitudeScale = when {
                            noteCount >= 90 -> 0.45f
                            noteCount >= 50 -> 0.65f
                            else -> 1f
                        }
                        val driftX = (
                            cos(driftProgress + (anchor.radiusScale * PI).toFloat()) *
                                (8f + anchor.radiusScale * 12f) *
                                driftAmplitudeScale
                            ).toFloat()
                        val driftY = (
                            sin(driftProgress + (index * 0.55f)) *
                                (6f + anchor.radiusScale * 10f) *
                                driftAmplitudeScale
                            ).toFloat()
                        val animatedScale by animateFloatAsState(
                            targetValue = targetScale,
                            animationSpec = spring(dampingRatio = 0.84f, stiffness = 140f),
                            label = "bubbleScale$index"
                        )
                        val animatedX by animateFloatAsState(
                            targetValue = anchor.x + bubblePan.x + driftX,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 120f),
                            label = "bubbleX$index"
                        )
                        val animatedY by animateFloatAsState(
                            targetValue = anchor.y + bubblePan.y + driftY,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 120f),
                            label = "bubbleY$index"
                        )
                        val bubbleAlpha by animateFloatAsState(
                            targetValue = (0.72f + (anchor.radiusScale * 0.26f)).coerceIn(0.7f, 0.98f),
                            animationSpec = tween(durationMillis = 420),
                            label = "bubbleAlpha$index"
                        )
                        val snippetSource = note.cardTitle.ifBlank { note.front.text }
                        val snippet = snippetSource.replace("\n", " ").trim()

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset {
                                    IntOffset(
                                        x = animatedX.roundToInt(),
                                        y = animatedY.roundToInt()
                                    )
                                }
                                .size(bubbleItemSize)
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    alpha = bubbleAlpha
                                }
                                .clip(RoundedCornerShape(999.dp))
                                .background(noteRadialGradient(note))
                                .clickable {
                                    onSelectedIndexChange(index)
                                    scope.launch { pagerState.scrollToPage(nearestVirtualPage(pagerState.currentPage, index)) }
                                    isBubbleMode = false
                                    isPreviewMode = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = snippet.take(bubbleSnippetLimit) + if (snippet.length > bubbleSnippetLimit) "…" else "",
                                color = Color(0xFFF2F6FB),
                                fontSize = bubbleFontSize,
                                lineHeight = bubbleLineHeight,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(swipeAccelerationConnection),
                    pageSize = if (isPreviewMode) PageSize.Fixed(previewCircleSize) else PageSize.Fill,
                    pageSpacing = if (isPreviewMode) (-6).dp else 0.dp,
                    contentPadding = if (isPreviewMode) {
                        PaddingValues(horizontal = previewHorizontalPadding)
                    } else {
                        PaddingValues(0.dp)
                    }
                ) { page ->
                val pageNoteIndex = wrappedNoteIndex(page)
                val note = notes[pageNoteIndex]
                val pageDistance = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val previewScaleWhenActive = (0.98f - (pageDistance * 0.10f)).coerceIn(0.86f, 0.98f)
                val previewAlphaWhenActive = (1f - (pageDistance * 0.14f)).coerceIn(0.68f, 1f)
                val previewScale = lerp(1f, previewScaleWhenActive, previewTransitionProgress.value)
                val smoothedPreviewScale by animateFloatAsState(
                    targetValue = previewScale,
                    animationSpec = spring(dampingRatio = 0.94f, stiffness = 220f),
                    label = "smoothedPreviewScale"
                )
                val previewAlpha = lerp(1f, previewAlphaWhenActive, previewTransitionProgress.value)
                val smoothedPreviewAlpha by animateFloatAsState(
                    targetValue = previewAlpha,
                    animationSpec = spring(dampingRatio = 0.96f, stiffness = 240f),
                    label = "smoothedPreviewAlpha"
                )
                val showBack = isNoteBackVisible(note.id)
                val density = LocalDensity.current
                val flipRotation by animateFloatAsState(
                    targetValue = if (showBack) 180f else 0f,
                    animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing),
                    label = "noteFlipRotation"
                )
                val showingBackFace = flipRotation > 90f
                val visibleSide = if (showingBackFace) note.back else note.front
                val text = visibleSide.text
                val label = visibleSide.label
                val cardCameraDistancePx = with(density) { 28.dp.toPx() }

                LaunchedEffect(note.id, showBack, textScale) {
                    noteScrollState.scrollTo(0)
                }

                Box(
                    modifier = Modifier
                        .then(
                            if (isPreviewMode) {
                                Modifier
                                    .size(previewCircleSize)
                            } else {
                                Modifier.fillMaxSize()
                            }
                        )
                        .graphicsLayer {
                            rotationY = flipRotation
                            cameraDistance = cardCameraDistancePx
                            scaleX = smoothedPreviewScale
                            scaleY = smoothedPreviewScale
                            alpha = smoothedPreviewAlpha
                        }
                        .clip(RoundedCornerShape(999.dp))
                        .background(noteRadialGradient(note))
                        .pointerInput(note.id, showTray) {
                            detectTapGestures(
                                onLongPress = {
                                    if (!showTray && !isPreviewMode) {
                                        isBubbleMode = false
                                        isPreviewMode = true
                                    }
                                },
                                onTap = {
                                    Log.d(DEBUG_TAG, "Input signal: tap noteId=${note.id}, trayOpen=$showTray")
                                    if (!showTray && isPreviewMode) {
                                        scope.launch {
                                            pagerState.scrollToPage(page)
                                            onSelectedIndexChange(pageNoteIndex)
                                        }
                                        isBubbleMode = false
                                        isPreviewMode = false
                                    } else if (!showTray) {
                                        onFlip(note.id)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = if (showingBackFace) 180f else 0f
                            }
                            .padding(vertical = if (isPreviewMode) 8.dp else 14.dp)
                    ) {
                        val textMeasurer = rememberTextMeasurer()
                        val horizontalPadding = if (isPreviewMode) 12.dp else 22.dp
                        val headerReserved = 30.dp
                        val baseFontSize = adaptiveFontSize(text) * textScale.factor

                        val outerVerticalPadding = 14.dp
                        val contentTopPadding = 26.dp
                        val contentBottomPadding = 34.dp

                        val maxWidthPx = with(density) { (maxWidth - (horizontalPadding * 2)).toPx().roundToInt().coerceAtLeast(1) }
                        val maxHeightPx = with(density) {
                            (maxHeight
                                    - outerVerticalPadding * 2
                                    - headerReserved
                                    - contentTopPadding
                                    - contentBottomPadding
                                    ).toPx().roundToInt().coerceAtLeast(1)
                        }
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
                        val needsScroll = !fitsOnSingleScreen(effectiveFontSize)
                        val effectiveLineHeight = effectiveFontSize * 1.2
                        val useScrollableTopLayout = needsScroll
                        val noteHeaderTextColor = Color(0xFFEAF2FF)
                        val noteBodyTextColor = Color(0xFFF2F6FB)
                        if (!isPreviewMode) {
                            Text(
                                text = "$flowName • ${pageNoteIndex + 1}/${notes.size} • ${label}",
                                fontSize = 12.sp,
                                color = noteHeaderTextColor.copy(alpha = 0.78f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 2.dp)
                            )
                        }

                        if (isPreviewMode) {
                            Text(
                                text = (text.take(34) + if (text.length > 34) "…" else ""),
                                color = noteBodyTextColor,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = horizontalPadding)
                            )
                        } else if (useScrollableTopLayout) {
                            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Top,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = headerReserved, bottom = 16.dp)
                                        .verticalScroll(noteScrollState)
                                ) {
                                    Text(
                                        text = text,
                                        color = noteBodyTextColor,
                                        fontSize = effectiveFontSize,
                                        lineHeight = effectiveLineHeight,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = horizontalPadding)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 20.dp, bottom = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = text,
                                    color = noteBodyTextColor,
                                    fontSize = effectiveFontSize,
                                    lineHeight = effectiveLineHeight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = horizontalPadding)
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        if (!isPreviewMode) {
            val currentNoteId = notes[wrappedNoteIndex(pagerState.currentPage)].id
            val isInCollection = isNoteInCollection(currentNoteId)
            Text(
                text = if (isInCollection) "★" else "☆",
                color = if (isInCollection) Color(0xFFFFD54F) else Color.White,
                fontSize = starFontSize,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = starBottomPadding)
                    .clickable { onToggleCollection(currentNoteId) }
            )
        } else {
            Text(
                text = "${wrappedNoteIndex(pagerState.currentPage) + 1}/${notes.size}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            )
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
                .padding(start = 8.dp, end = 8.dp, bottom = 20.dp)
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

private enum class AppScreen {
    CardFlows,
    Notes
}

private data class CardFlow(
    val id: Long,
    val name: String,
    val notes: List<StickyNote>
)

private enum class TextScaleOption(
    val label: String,
    val factor: Float,
    val storageKey: String
) {
    ExtraSmall("XS", 0.74f, "xs"),
    Small("S", 0.86f, "s"),
    Large("L", 1.16f, "l");

    companion object {
        fun fromStorage(value: String): TextScaleOption {
            return entries.firstOrNull { it.storageKey == value } ?: Large
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
        Log.d(DEBUG_TAG, "Import: discoverServices called timeoutMs=$timeoutMs")
        val found = ConcurrentHashMap<String, DiscoveredService>()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(DEBUG_TAG, "Import: NSD discovery started type=$serviceType")
            }
            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(DEBUG_TAG, "Import: NSD discovery stopped type=$serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(DEBUG_TAG, "Import: NSD service found name=${serviceInfo.serviceName} type=${serviceInfo.serviceType}")
                if (serviceInfo.serviceType != "_timescape._tcp.") return
                Thread {
                    runCatching {
                        kotlinx.coroutines.runBlocking {
                            resolve(serviceInfo)?.let { resolved ->
                                Log.d(DEBUG_TAG, "Import: NSD service resolved host=${resolved.host} port=${resolved.port}")
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
        Log.d(DEBUG_TAG, "Import: discoverServices returning ${found.size} services")
        found.values.sortedBy { it.displayName }
    }

    private suspend fun resolve(info: NsdServiceInfo): DiscoveredService? =
        suspendCancellableCoroutine { cont ->
            val listener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(DEBUG_TAG, "Import: NSD resolve failed name=${serviceInfo.serviceName} code=$errorCode")
                    if (cont.isActive) cont.resume(null)
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    if (!cont.isActive) return
                    val host = serviceInfo.host?.hostAddress ?: return cont.resume(null)
                    Log.d(DEBUG_TAG, "Import: NSD resolve success name=${serviceInfo.serviceName} host=$host port=${serviceInfo.port}")
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
            Log.d(DEBUG_TAG, "Import: importFromTarget base=$base clientName=$clientName")

            // Optional, ignore failures
            runCatching {
                val metaRequest = Request.Builder().url("$base/meta").get().build()
                Log.d(DEBUG_TAG, "Import: requesting $base/meta")
                client.newCall(metaRequest).execute().close()
            }

            val sessionId = requestSession(base, clientName)
            Log.d(DEBUG_TAG, "Import: session request completed sessionId=$sessionId")
            onWaiting()
            val token = pollSession(base, sessionId)
            Log.d(DEBUG_TAG, "Import: session approved tokenReceived=${token.isNotBlank()}")
            onDownloading()
            val exportRequest = Request.Builder().url("$base/export?token=$token").get().build()
            Log.d(DEBUG_TAG, "Import: downloading export from $base/export")
            val payload = client.newCall(exportRequest).execute().use { response ->
                if (!response.isSuccessful) error("Export failed (${response.code})")
                response.body?.string().orEmpty()
            }
            Log.d(DEBUG_TAG, "Import: export payload size=${payload.length}")
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
        Log.d(DEBUG_TAG, "Import: POST $base/session/request")
        val res = client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) error("Session request failed (${response.code})")
            response.body?.string().orEmpty()
        }
        Log.d(DEBUG_TAG, "Import: session request response bytes=${res.length}")
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
            Log.d(DEBUG_TAG, "Import: pollSession status=${status.status} sessionId=$sessionId")
            when (status.status.uppercase()) {
                "APPROVED" -> return status.token ?: error("Missing token")
                "DENIED" -> error("Denied on phone")
            }
            delay(1000)
        }
        error("Approval timed out")
    }
}

private fun organizeImportedNotes(imported: List<StickyNote>): List<StickyNote> {
    return imported
        .groupBy { "${it.flowId}|${it.flowName}" }
        .toSortedMap(compareBy<String> { it.substringAfter("|") }.thenBy { it.substringBefore("|").toLongOrNull() ?: 0L })
        .values
        .flatMap { flowNotes ->
            flowNotes.sortedWith(
                compareBy<StickyNote> { it.id.toLongOrNull() ?: Long.MAX_VALUE }
                    .thenBy { it.id }
            )
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

private fun defaultStickyNotes(): List<StickyNote> {
    val flows = listOf(
        1L to "Daily",
        2L to "Focus",
        3L to "Health",
        4L to "Learning",
        5L to "Career",
        6L to "Mindset",
        7L to "Finance",
        8L to "Home",
        9L to "Travel",
        10L to "Creativity"
    )
    val titlePool = listOf(
        "Morning Reset", "Deep Work Sprint", "Stretch Break", "Hydration Check", "Inbox Cleanup",
        "Weekly Plan", "Language Drill", "Budget Snapshot", "Declutter Desk", "Idea Capture"
    )
    val actionPool = listOf(
        "Take one concrete step before opening distractions.",
        "Do this for 10 focused minutes and record progress.",
        "Keep this short and repeat it daily for consistency.",
        "Share a quick update with a teammate or friend.",
        "Write one sentence about what improved after finishing."
    )
    val palette = listOf(
        "#34C79A", "#2F86FF", "#8A7CFF", "#3EBE76", "#E07A2E",
        "#4AA3A1", "#6A89FF", "#A06BE5", "#3EA8D8", "#B46E3F"
    )
    return (1..100).map { index ->
        val flow = flows[(index - 1) % flows.size]
        val title = titlePool[(index - 1) % titlePool.size]
        val action = actionPool[(index - 1) % actionPool.size]
        StickyNote(
            id = (1000 + index).toString(),
            flowId = flow.first,
            flowName = flow.second,
            cardId = (flow.first * 100) + index,
            cardTitle = "$title #$index",
            color = palette[(index - 1) % palette.size],
            rotation = (((index % 7) - 3) * 0.35),
            front = NoteSide(label = "front", text = "$title #$index"),
            back = NoteSide(label = "back", text = "Test note $index. $action")
        )
    }
}

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

    val hueShift = (((abs(note.id.hashCode()) % 5) - 2) * 3f)
    val hue = (hsv[0] + hueShift + 360f) % 360f

    // Calm system-card styling: broad soft center + deep edge vignette.
    val centerValue = (hsv[2] * 0.50f).coerceIn(0.30f, 0.52f)
    val center = hsvColor(
        hue = hue,
        saturation = (hsv[1] * 0.90f + 0.18f).coerceIn(0.35f, 0.72f),
        value = centerValue
    )
    val mid = hsvColor(
        hue = hue,
        saturation = (hsv[1] * 0.96f + 0.20f).coerceIn(0.40f, 0.78f),
        value = centerValue
    )
    val edge = hsvColor(
        hue = hue,
        saturation = (hsv[1] * 1.02f + 0.24f).coerceIn(0.46f, 0.84f),
        value = (hsv[2] * 0.28f).coerceIn(0.16f, 0.34f)
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
    @Serializable(with = StringOrLongSerializer::class)
    val id: String,
    val flowId: Long,
    val flowName: String,
    val cardId: Long,
    val cardTitle: String,
    val color: String,
    val rotation: Double,
    val front: NoteSide,
    val back: NoteSide
)

private object StringOrLongSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrLong", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        if (decoder is JsonDecoder) {
            return when (val element = decoder.decodeJsonElement()) {
                is JsonPrimitive -> {
                    if (element.isString) {
                        element.content
                    } else {
                        element.content.toLongOrNull()?.toString() ?: element.content
                    }
                }

                else -> element.toString()
            }
        }
        return decoder.decodeString()
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

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
