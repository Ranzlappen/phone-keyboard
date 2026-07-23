package io.github.ranzlappen.glyphboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import io.github.ranzlappen.glyphboard.data.layouts.DefaultLayouts
import io.github.ranzlappen.glyphboard.data.layouts.LayoutConfig
import io.github.ranzlappen.glyphboard.ui.app.LayoutEditorScreen
import io.github.ranzlappen.glyphboard.ui.app.LayoutsListScreen
import io.github.ranzlappen.glyphboard.ui.app.SimilarityScreen
import io.github.ranzlappen.glyphboard.ui.theme.GlyphBoardTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Companion app: setup flow, test field, settings, and the editors for
 * custom layouts and the similarity database.
 */
class MainActivity : ComponentActivity() {

    companion object {
        /** Set by [io.github.ranzlappen.glyphboard.ime.KeyboardSwitchService] on pre-R devices. */
        const val EXTRA_SHOW_PICKER = "io.github.ranzlappen.glyphboard.SHOW_PICKER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlyphBoardTheme {
                AppRoot()
            }
        }
        maybeShowPicker(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        maybeShowPicker(intent)
    }

    private fun maybeShowPicker(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_SHOW_PICKER, false) != true) return
        intent.removeExtra(EXTRA_SHOW_PICKER)
        lifecycleScope.launch {
            // The picker is ignored for unfocused apps: RESUMED alone is not
            // enough (focus lands a few frames later), so wait for it too.
            lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {}
            var waited = 0L
            while (!window.decorView.hasWindowFocus() && waited < 2000L) {
                delay(50L)
                waited += 50L
            }
            getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
        }
    }
}

// Navigation stack encoded as a '|'-joined route string so it survives
// rotation via rememberSaveable: "home", "layouts", "edit:<id>", "similarity".
private const val ROUTE_HOME = "home"
private const val ROUTE_LAYOUTS = "layouts"
private const val ROUTE_SIMILARITY = "similarity"
private const val ROUTE_EDIT_PREFIX = "edit:"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as GlyphBoardApp
    val scope = rememberCoroutineScope()

    var route by rememberSaveable { mutableStateOf(ROUTE_HOME) }
    val stack = route.split('|')
    fun pop() {
        route = route.substringBeforeLast('|')
    }
    BackHandler(enabled = stack.size > 1) { pop() }
    fun push(screen: String) {
        route = "$route|$screen"
    }

    // Optimistic layout config: edits show instantly instead of waiting for
    // the DataStore write -> flow round trip (which would make "+ key" and
    // "Add layout" target a stale config and silently bounce).
    val storedConfig by app.layouts.config.collectAsState(initial = DefaultLayouts.config())
    var pendingConfig by remember { mutableStateOf<LayoutConfig?>(null) }
    val layoutConfig = pendingConfig ?: storedConfig
    LaunchedEffect(storedConfig) {
        if (storedConfig == pendingConfig) pendingConfig = null
    }
    val saveConfig: (LayoutConfig) -> Unit = { config ->
        pendingConfig = config
        scope.launch { app.layouts.save(config) }
    }

    val similarityMap by app.similarity.map.collectAsState(initial = emptyMap())

    val screen = stack.last()
    val title = when {
        screen == ROUTE_LAYOUTS -> "Keyboard layouts"
        screen.startsWith(ROUTE_EDIT_PREFIX) ->
            layoutConfig.layouts.firstOrNull { it.id == screen.removePrefix(ROUTE_EDIT_PREFIX) }?.name
                ?: "Edit layout"
        screen == ROUTE_SIMILARITY -> "Similarity database"
        else -> null
    }

    Scaffold(
        topBar = {
            if (title != null) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        TextButton(onClick = ::pop) { Text("←", fontSize = 20.sp) }
                    },
                )
            }
        },
    ) { padding ->
        // Cap content width so large screens/tablets don't stretch forms
        // across the whole display.
        Box(
            Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val content = Modifier.widthIn(max = 640.dp).fillMaxSize()
            when {
                screen == ROUTE_LAYOUTS -> LayoutsListScreen(
                    config = layoutConfig,
                    onSave = saveConfig,
                    onOpenEditor = { id -> push(ROUTE_EDIT_PREFIX + id) },
                    modifier = content,
                )
                screen.startsWith(ROUTE_EDIT_PREFIX) -> LayoutEditorScreen(
                    config = layoutConfig,
                    layoutId = screen.removePrefix(ROUTE_EDIT_PREFIX),
                    onSave = saveConfig,
                    onBack = ::pop,
                    modifier = content,
                )
                screen == ROUTE_SIMILARITY -> SimilarityScreen(
                    map = similarityMap,
                    onSetEntry = { base, variants ->
                        scope.launch { app.similarity.setEntry(base, variants) }
                    },
                    onReset = { scope.launch { app.similarity.resetToDefaults() } },
                    modifier = content,
                )
                else -> HomeScreen(
                    modifier = content,
                    onOpenLayouts = { push(ROUTE_LAYOUTS) },
                    onOpenSimilarity = { push(ROUTE_SIMILARITY) },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenLayouts: () -> Unit,
    onOpenSimilarity: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as GlyphBoardApp
    val scope = rememberCoroutineScope()

    // Re-check system state every time the user returns from settings.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val imm = remember { context.getSystemService(InputMethodManager::class.java) }
    val enabled = remember(refresh) {
        imm?.enabledInputMethodList.orEmpty().any { it.packageName == context.packageName }
    }
    val selected = remember(refresh) {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.startsWith(context.packageName + "/") == true
    }
    val quickSwitchEnabled = remember(refresh) {
        // Exact component-prefix match: a bare substring check would let the
        // .debug variant's service light this up for the release app.
        Settings.Secure.getString(
            context.contentResolver,
            "enabled_accessibility_services",
        )?.split(':')?.any { it.startsWith("${context.packageName}/") } == true
    }

    val haptics by app.settings.hapticsEnabled.collectAsState(initial = true)
    val hideUnsupported by app.settings.hideUnsupported.collectAsState(initial = true)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("Ω GlyphBoard", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Every Unicode character, one keyboard.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Setup", fontWeight = FontWeight.SemiBold)
                SetupStep(
                    done = enabled,
                    label = "1. Enable GlyphBoard in system settings",
                    buttonText = "Open keyboard settings",
                ) {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                }
                SetupStep(
                    done = selected,
                    label = "2. Switch to GlyphBoard (you can swap back to your other keyboard any time with the 🌐 key)",
                    buttonText = "Choose keyboard",
                ) {
                    imm?.showInputMethodPicker()
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Customize", fontWeight = FontWeight.SemiBold)
                Text(
                    "Any number of layouts, cycled by swiping the space bar. Every key's " +
                        "hold popup is editable, including lookalike characters and the " +
                        "zalgo slider.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onOpenLayouts) { Text("Layouts") }
                    OutlinedButton(onClick = onOpenSimilarity) { Text("Similarity database") }
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick switch button", fontWeight = FontWeight.SemiBold)
                Text(
                    "Optional: enable the GlyphBoard quick-switch accessibility service " +
                        "to get a floating button that swaps keyboards from anywhere — one " +
                        "tap to GlyphBoard, tap again to hop back to your previous " +
                        "keyboard. The service cannot read the screen or your input.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SetupStep(
                    done = quickSwitchEnabled,
                    label = "1. On Android 13+, sideloaded apps must first be unblocked: " +
                        "App info → ⋮ menu (top right) → “Allow restricted settings”. " +
                        "Skip if the toggle isn't blocked.",
                    buttonText = "Open app info",
                ) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    )
                }
                SetupStep(
                    done = quickSwitchEnabled,
                    label = if (quickSwitchEnabled) {
                        "2. Quick switch is on"
                    } else {
                        "2. Enable “GlyphBoard quick switch” under installed services / " +
                            "downloaded apps, and turn on its shortcut button"
                    },
                    buttonText = "Open accessibility settings",
                ) {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Try it", fontWeight = FontWeight.SemiBold)
                var testText by rememberSaveable { mutableStateOf("") }
                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tap here, then press Ω for the character map") },
                    minLines = 2,
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Settings", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                SettingRow(
                    title = "Key press vibration",
                    checked = haptics,
                ) { value -> scope.launch { app.settings.setHapticsEnabled(value) } }
                HorizontalDivider()
                SettingRow(
                    title = "Hide characters this device can't display",
                    subtitle = "Characters without a font glyph are skipped in the browser",
                    checked = hideUnsupported,
                ) { value -> scope.launch { app.settings.setHideUnsupported(value) } }
            }
        }

        Text(
            text = "GlyphBoard needs no permissions, never connects to the internet, " +
                "and does not log what you type.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetupStep(done: Boolean, label: String, buttonText: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (done) "✅" else "⬜", fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(label, modifier = Modifier.weight(1f))
        }
        if (done) {
            OutlinedButton(onClick = onClick) { Text(buttonText) }
        } else {
            Button(onClick = onClick) { Text(buttonText) }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            subtitle?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
