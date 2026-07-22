package io.github.ranzlappen.glyphboard.ui.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.ranzlappen.glyphboard.data.layouts.VariantParser

/**
 * Editor for the similarity database: base character → lookalikes shown in
 * hold popups of keys that enable the "similar" checkbox.
 */
@Composable
fun SimilarityScreen(
    map: Map<String, List<String>>,
    onSetEntry: (String, List<String>) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingBase by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val sorted = remember(map) { map.entries.sortedBy { it.key } }

    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Similarity database", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Keys with the \"similar characters\" checkbox pull their extra " +
                "hold-popup options from this table. Tap an entry to edit it.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { adding = true }) { Text("Add entry") }
            OutlinedButton(onClick = { confirmReset = true }) { Text("Reset to defaults") }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(sorted.size) { i ->
                val (base, variants) = sorted[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editingBase = base }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(base, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        variants.joinToString(" "),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
        }
    }

    if (adding || editingBase != null) {
        val base = editingBase
        SimilarityEntryDialog(
            initialBase = base ?: "",
            initialVariants = base?.let { map[it] } ?: emptyList(),
            baseEditable = base == null,
            onDelete = base?.let { { onSetEntry(it, emptyList()); editingBase = null } },
            onSave = { newBase, variants ->
                onSetEntry(newBase, variants)
                adding = false
                editingBase = null
            },
            onDismiss = {
                adding = false
                editingBase = null
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset similarity database?") },
            text = { Text("All custom entries are replaced by the built-in table.") },
            confirmButton = {
                TextButton(onClick = {
                    onReset()
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SimilarityEntryDialog(
    initialBase: String,
    initialVariants: List<String>,
    baseEditable: Boolean,
    onDelete: (() -> Unit)?,
    onSave: (String, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var base by rememberSaveable { mutableStateOf(initialBase) }
    var variantsText by rememberSaveable { mutableStateOf(VariantParser.format(initialVariants)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (baseEditable) "Add entry" else "Edit entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = base,
                    onValueChange = { base = it },
                    label = { Text("Base character") },
                    singleLine = true,
                    enabled = baseEditable,
                )
                OutlinedTextField(
                    value = variantsText,
                    onValueChange = { variantsText = it },
                    label = { Text("Similar characters") },
                    supportingText = {
                        Text("Each character is one option; separate with spaces for multi-character options.")
                    },
                )
                onDelete?.let {
                    TextButton(onClick = it) { Text("Delete entry") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = base.isNotBlank() && VariantParser.parse(variantsText).isNotEmpty(),
                onClick = { onSave(base.trim(), VariantParser.parse(variantsText)) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
