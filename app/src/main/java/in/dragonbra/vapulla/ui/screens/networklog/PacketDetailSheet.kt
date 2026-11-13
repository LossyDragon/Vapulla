package `in`.dragonbra.vapulla.ui.screens.networklog

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.vapulla.service.NetworkLogEntry
import `in`.dragonbra.vapulla.service.PacketDetail
import `in`.dragonbra.vapulla.service.PacketInspector
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import `in`.dragonbra.vapulla.util.Utils.toTimeString

/** Bytes of a body shown as hex; the rest is only counted. */
private const val HEX_SHOWN = 2048

/**
 * Everything known about one packet: its header, and when the body was kept, the body read
 * as protobuf and as a hex dump.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketDetailSheet(entry: NetworkLogEntry, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PacketDetailContent(entry)
    }
}

@Composable
private fun PacketDetailContent(entry: NetworkLogEntry) {
    val detail = entry.detail
    val body = detail?.body
    val decoded = remember(entry.id) { body?.let(PacketInspector::decodeProtobuf) }
    val hex = remember(entry.id) {
        body?.let { PacketInspector.hexDump(it.copyOfRange(0, minOf(it.size, HEX_SHOWN))) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Section(title = if (entry.incoming) "Received" else "Sent") {
            DetailList(
                listOf(
                    "Message" to entry.msg.name,
                    "Method" to detail?.method.orEmpty(),
                    "Time" to entry.timestamp.toTimeString(),
                    "Size" to entry.size.toLong().toFileSize(),
                ),
            )
        }

        if (detail == null) {
            Note("This packet's header could not be read.")
            return@Column
        }

        Section(title = "Header") {
            DetailList(
                listOf(
                    "Format" to detail.format,
                    "Source job" to detail.sourceJob?.toULong()?.toString().orEmpty(),
                    "Target job" to detail.targetJob?.toULong()?.toString().orEmpty(),
                    "Result" to detail.result.orEmpty(),
                    "Steam ID" to detail.steamId?.toULong()?.toString().orEmpty(),
                    "Session ID" to detail.sessionId?.toUInt()?.toString().orEmpty(),
                    "Header size" to "${detail.headerSize} bytes",
                    "Body size" to "${detail.bodySize} bytes",
                ),
            )
        }

        detail.bodyNote?.let { Note(it) }

        if (decoded != null) {
            Section(title = "Body, read as protobuf") {
                Note("Field numbers and values. The names are not part of the packet.")
                CodeBlock(decoded)
            }
        }

        if (hex != null && body != null) {
            Section(title = "Body, as bytes") {
                if (body.size > HEX_SHOWN) {
                    Note("The first $HEX_SHOWN of ${body.size} bytes.")
                }
                CodeBlock(hex)
            }
        }
    }
}

/** Monospaced, selectable text that scrolls sideways rather than wrapping. */
@Composable
private fun CodeBlock(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SelectionContainer {
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    // Field 1 = 440 (varint), field 2 = "hello".
    val body = byteArrayOf(0x08, 0xB8.toByte(), 0x03, 0x12, 0x05, 0x68, 0x65, 0x6C, 0x6C, 0x6F)

    VapullaTheme {
        Surface {
            PacketDetailContent(
                NetworkLogEntry(
                    id = 0,
                    timestamp = 1_760_722_982_000,
                    incoming = false,
                    msg = EMsg.ServiceMethodCallFromClient,
                    size = 96,
                    detail = PacketDetail(
                        format = "Protobuf",
                        method = "Player.GetOwnedGames#1",
                        sourceJob = 12,
                        targetJob = null,
                        steamId = 76561197960287930,
                        sessionId = 1234567,
                        result = null,
                        headerSize = 86,
                        bodySize = body.size,
                        body = body,
                        bodyNote = null,
                    ),
                ),
            )
        }
    }
}
