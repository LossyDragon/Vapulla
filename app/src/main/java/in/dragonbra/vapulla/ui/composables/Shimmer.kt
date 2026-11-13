package `in`.dragonbra.vapulla.ui.composables

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

/** A plain block in the placeholder colour. The shimmer comes from a parent or [ShimmerBox]. */
@Composable
private fun PlaceholderBlock(modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, shape))
}

/** A shimmering block that stands in for an image or other content that is still loading. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
    PlaceholderBlock(modifier = modifier.shimmer(), shape = shape)
}

/**
 * Shimmering placeholder rows shaped like a grouped list. Shown while a list loads for the
 * first time, so the real rows replace something of the same shape instead of an empty screen.
 */
@Composable
fun SkeletonList(contentPadding: PaddingValues, modifier: Modifier = Modifier, rows: Int = 8) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .shimmer(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(rows) { index ->
            Surface(
                shape = groupedListShape(index, rows),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaceholderBlock(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.medium,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlaceholderBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(14.dp),
                            shape = CircleShape,
                        )
                        PlaceholderBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .height(12.dp),
                            shape = CircleShape,
                        )
                    }
                }
            }
        }
    }
}

/** Shimmering placeholder the size of an [InfoPill], [width] wide. */
@Composable
fun InfoPillSkeleton(modifier: Modifier = Modifier, width: Dp = 64.dp) {
    ShimmerBox(modifier = modifier.size(width = width, height = 22.dp), shape = CircleShape)
}

/** Shimmering placeholder for a [DetailList] of [rows] rows. */
@Composable
fun DetailListSkeleton(modifier: Modifier = Modifier, rows: Int = 3) {
    Column(
        modifier = modifier.shimmer(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(rows) { index ->
            PlaceholderBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = groupedListShape(index, rows),
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            SkeletonList(contentPadding = PaddingValues(16.dp), rows = 4)
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_DetailList() {
    VapullaTheme {
        Surface {
            DetailListSkeleton(modifier = Modifier.padding(16.dp))
        }
    }
}
