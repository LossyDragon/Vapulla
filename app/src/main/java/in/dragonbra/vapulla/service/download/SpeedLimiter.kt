package `in`.dragonbra.vapulla.service.download

/**
 * Holds a download to a speed. The downloader has no limit of its own, so the limit is
 * kept by making the thread that reports a chunk wait until that chunk is "paid for" at
 * the allowed rate. The chunks behind it queue up, and the network follows.
 *
 * @param bytesPerSecond the limit right now; 0 or less for none.
 * @param stopped true once the download is over and nothing should wait any longer.
 */
internal class SpeedLimiter(
    private val bytesPerSecond: () -> Long,
    private val stopped: () -> Boolean,
) {
    /** The moment, on the [System.nanoTime] clock, everything reported so far is paid for. */
    private var paidUntil = 0L

    /** Counts [bytes] as received and waits until the limit allows for more. */
    @Synchronized
    fun pace(bytes: Long) {
        val limit = bytesPerSecond()
        if (limit <= 0) return

        val now = System.nanoTime()
        val cost = (bytes.toDouble() / limit * NANOS_PER_SECOND).toLong()
        // Capped, so a long wait is not taken for a stalled download.
        paidUntil = (maxOf(paidUntil, now) + cost).coerceAtMost(now + MAX_WAIT_NANOS)

        while (!stopped() && bytesPerSecond() > 0) {
            val left = paidUntil - System.nanoTime()
            if (left <= 0) break
            Thread.sleep((left / NANOS_PER_MILLI).coerceIn(1, SLEEP_MILLIS))
        }
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val NANOS_PER_MILLI = 1_000_000L
        const val MAX_WAIT_NANOS = 30 * NANOS_PER_SECOND

        /** Short, so pausing or lifting the limit is noticed quickly. */
        const val SLEEP_MILLIS = 200L
    }
}
