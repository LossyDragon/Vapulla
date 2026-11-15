package `in`.dragonbra.vapulla.util

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** [timeChunked] gathers the values of a flow into lists by count and by time. */
class FlowUtilsTest {

    @Test
    fun `a full batch is emitted at once and the rest when the flow ends`() = runBlocking {
        val batches = (1..5).asFlow().timeChunked(maxBatchSize = 2, duration = 1.minutes).toList()

        assertEquals(listOf(listOf(1, 2), listOf(3, 4), listOf(5)), batches)
    }

    @Test
    fun `a batch that does not fill up is emitted once its time is over`() = runBlocking {
        val values = flow {
            emit(1)
            delay(300.milliseconds)
            emit(2)
        }

        val batches = values.timeChunked(maxBatchSize = 10, duration = 50.milliseconds).toList()

        assertEquals(listOf(listOf(1), listOf(2)), batches)
    }

    @Test
    fun `a flow without values emits no batch`() = runBlocking {
        val batches = emptyFlow<Int>().timeChunked(maxBatchSize = 2).toList()

        assertEquals(emptyList<List<Int>>(), batches)
    }
}
