package com.pausespace.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

data class Session(val start: Long, val end: Long) {
    val ms get() = end - start
}

/** Reads foreground time from Android's usage stats. Requires Usage access. */
object Usage {
    /** Gaps shorter than this (e.g. a quick trip to the share sheet) don't split a session. */
    private const val MERGE_GAP_MS = 60_000L

    fun startOfDay(ts: Long = System.currentTimeMillis(), daysAgo: Int = 0): Long =
        Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }.timeInMillis

    fun minuteOfDay(ts: Long = System.currentTimeMillis()): Int =
        Calendar.getInstance().run {
            timeInMillis = ts
            get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
        }

    /** Foreground sessions per package between [from] and [to]. Empty without Usage access. */
    fun sessions(context: Context, from: Long, to: Long = System.currentTimeMillis()): Map<String, List<Session>> {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        // Start a little early so an app already open at [from] is caught; clipped below.
        val events = runCatching { usm.queryEvents(from - 3 * 3_600_000L, to) }.getOrNull() ?: return emptyMap()
        val open = HashMap<String, Long>()
        val raw = HashMap<String, MutableList<Session>>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> open.putIfAbsent(pkg, e.timeStamp)
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                -> open.remove(pkg)?.let { raw.getOrPut(pkg) { mutableListOf() } += Session(it, e.timeStamp) }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    open.forEach { (p, s) -> raw.getOrPut(p) { mutableListOf() } += Session(s, e.timeStamp) }
                    open.clear()
                }
            }
        }
        // Still in the foreground right now.
        open.forEach { (p, s) -> raw.getOrPut(p) { mutableListOf() } += Session(s, to) }

        return raw.mapValues { (_, list) -> merge(list.mapNotNull { clip(it, from, to) }) }
            .filterValues { it.isNotEmpty() }
    }

    private fun clip(s: Session, from: Long, to: Long): Session? {
        val a = maxOf(s.start, from)
        val b = minOf(s.end, to)
        return if (b > a) Session(a, b) else null
    }

    private fun merge(list: List<Session>): List<Session> {
        if (list.isEmpty()) return list
        val sorted = list.sortedBy { it.start }
        val out = mutableListOf(sorted.first())
        for (s in sorted.drop(1)) {
            val last = out.last()
            if (s.start - last.end <= MERGE_GAP_MS) out[out.lastIndex] = Session(last.start, maxOf(last.end, s.end))
            else out += s
        }
        return out
    }
}
