package `in`.dragonbra.vapulla.ui.screens.chat.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

/** The local calendar day an epoch-millisecond timestamp falls on. */
internal fun Long.toLocalDay(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

/** "Wednesday, November 11, 2026" for the day an epoch-millisecond timestamp falls on. */
internal fun Long.toDayHeader(): String = toLocalDay().format(DAY_FORMAT)

/**
 * Whether a message starts a new day in a chat list, and so gets a [DayHeader] above it.
 *
 * @param olderTimestamp the message just before this one, or null when this is the oldest
 *  one loaded.
 */
internal fun startsNewDay(timestamp: Long, olderTimestamp: Long?): Boolean =
    olderTimestamp == null || timestamp.toLocalDay() != olderTimestamp.toLocalDay()

/** The line between two days of a conversation, placed above a day's first message. */
@Composable
internal fun DayHeader(timestamp: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        DayPill(text = timestamp.toDayHeader())
    }
}

/**
 * The day of the messages at the top of the screen, floating over the list while it is being
 * scrolled. Together with the [DayHeader] lines this works as a sticky header: a chat list is
 * laid out bottom-up, where the list's own sticky headers would pin to the wrong edge.
 */
@Composable
internal fun FloatingDayHeader(timestamp: Long?, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible && timestamp != null,
        modifier = modifier.padding(top = 8.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // Keeps showing the last day while fading out after the timestamp has gone.
        if (timestamp != null) {
            DayPill(text = timestamp.toDayHeader(), elevated = true)
        }
    }
}

@Composable
private fun DayPill(text: String, elevated: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = if (elevated) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = if (elevated) 3.dp else 0.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingDayHeader(timestamp = 1_760_722_982_000, visible = true)
                DayHeader(timestamp = 1_760_722_982_000)
            }
        }
    }
}
