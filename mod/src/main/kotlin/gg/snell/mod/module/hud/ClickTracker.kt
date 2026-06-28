package gg.snell.mod.module.hud

/**
 * Rolling 1-second click counter for the CPS HUD. The click mixin records presses; the bridge
 * reads CPS each frame. Pure logic with an injectable `nowNanos` so it's unit-testable. Clicks
 * are recorded and read on the client thread only (mixin + HUD capture), so no synchronization.
 */
object ClickTracker {
    private const val WINDOW_NANOS = 1_000_000_000L
    private val left = ArrayDeque<Long>()
    private val right = ArrayDeque<Long>()

    fun onLeft(nowNanos: Long = System.nanoTime()) { record(left, nowNanos) }
    fun onRight(nowNanos: Long = System.nanoTime()) { record(right, nowNanos) }

    fun leftCps(nowNanos: Long = System.nanoTime()): Int = count(left, nowNanos)
    fun rightCps(nowNanos: Long = System.nanoTime()): Int = count(right, nowNanos)

    private fun record(q: ArrayDeque<Long>, now: Long) { q.addLast(now); prune(q, now) }
    private fun count(q: ArrayDeque<Long>, now: Long): Int { prune(q, now); return q.size }
    private fun prune(q: ArrayDeque<Long>, now: Long) {
        while (q.isNotEmpty() && now - q.first() > WINDOW_NANOS) q.removeFirst()
    }

    internal fun reset() { left.clear(); right.clear() }
}
