package io.github.ranzlappen.glyphboard.ui.keyboard

import android.content.ClipboardManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * In-keyboard clipboard panel, opened by the 📋 function key. Shows the
 * current system clip (read ONLY while this panel is open — never in the
 * background, preserving the privacy promise) plus a locally-persisted
 * pinned list. Tap inserts; long-press a pinned entry to unpin.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardPanel(
    pinnedClips: List<String>,
    onCommit: (String) -> Unit,
    onPinClip: (String) -> Unit,
    onUnpinClip: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Read once per panel opening.
    val currentClips = remember {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = clipboard?.primaryClip
        buildList {
            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    val text = clip.getItemAt(i).coerceToText(context)?.toString()
                    if (!text.isNullOrBlank()) add(text)
                }
            }
        }.distinct()
    }
    val colors = MaterialTheme.colorScheme

    Column(modifier.fillMaxWidth().height(316.dp)) {
        Row(
            Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp, 36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceVariant)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("⌨", color = colors.onSurface, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Clipboard",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            item {
                SectionHeader("Current — tap to insert")
            }
            if (currentClips.isEmpty()) {
                item {
                    Text(
                        "Clipboard is empty",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            items(currentClips.size) { i ->
                val text = currentClips[i]
                ClipRow(
                    text = text,
                    trailing = {
                        if (text !in pinnedClips) {
                            TextButton(onClick = { onPinClip(text) }) { Text("Pin") }
                        }
                    },
                    onClick = { onCommit(text) },
                )
            }
            item { SectionHeader("Pinned — tap to insert, long-press to unpin") }
            if (pinnedClips.isEmpty()) {
                item {
                    Text(
                        "Nothing pinned yet",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            items(pinnedClips.size) { i ->
                val text = pinnedClips[i]
                ClipRow(
                    text = text,
                    trailing = {},
                    onClick = { onCommit(text) },
                    onLongClick = { onUnpinClip(text) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 2.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipRow(
    text: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text.replace('\n', ' '),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}
