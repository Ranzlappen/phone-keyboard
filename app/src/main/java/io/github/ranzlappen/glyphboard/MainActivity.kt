package io.github.ranzlappen.glyphboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import io.github.ranzlappen.glyphboard.ui.theme.GlyphBoardTheme
import kotlinx.coroutines.launch

/**
 * Companion screen: guides the user through enabling and selecting the
 * keyboard, offers a test field, and hosts the (deliberately small) settings.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlyphBoardTheme {
                Scaffold { padding ->
                    SetupScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as GlyphBoardApp
    val scope = rememberCoroutineScope()

    // Re-check enabled/selected state every time the user returns from
    // the system settings or the keyboard picker.
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
