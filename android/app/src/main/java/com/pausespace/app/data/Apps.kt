package com.pausespace.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(val pkg: String, val label: String) {
    val letter get() = label.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•"
}

object Apps {
    /** Apps paused by default on first run, if installed — the design's starting set. */
    val DEFAULT_PAUSED = listOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.reddit.frontpage",
        "com.twitter.android",
    )

    /** Tile hue/saturation from the design for well-known apps; everything else gets a stable hash hue. */
    private val KNOWN_TINT = mapOf(
        "com.instagram.android" to (335f to 70f),
        "com.google.android.youtube" to (4f to 75f),
        "com.zhiliaoapp.musically" to (210f to 10f),
        "com.ss.android.ugc.trill" to (210f to 10f),
        "com.reddit.frontpage" to (18f to 80f),
        "com.twitter.android" to (220f to 8f),
        "com.whatsapp" to (145f to 55f),
        "com.android.chrome" to (214f to 70f),
        "com.netflix.mediaclient" to (356f to 70f),
    )

    fun tintOf(pkg: String): Pair<Float, Float> =
        KNOWN_TINT[pkg] ?: (((pkg.hashCode() % 360) + 360) % 360).toFloat() to 55f

    @Volatile private var cache: List<AppInfo>? = null
    private val labels = HashMap<String, String>()

    fun launchable(context: Context, refresh: Boolean = false): List<AppInfo> {
        cache?.takeIf { !refresh }?.let { return it }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != context.packageName }
            .distinctBy { it.first }
            .map { AppInfo(it.first, it.second) }
            .sortedBy { it.label.lowercase() }
        synchronized(labels) { list.forEach { labels[it.pkg] = it.label } }
        cache = list
        return list
    }

    fun label(context: Context, pkg: String): String {
        synchronized(labels) { labels[pkg] }?.let { return it }
        val l = runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg.substringAfterLast('.'))
        synchronized(labels) { labels[pkg] = l }
        return l
    }

    fun isInstalled(context: Context, pkg: String) =
        runCatching { context.packageManager.getApplicationInfo(pkg, 0) }.isSuccess

    /** Rendered launcher icons, keyed by package. ~60 icons at 144px is a few MB. */
    private const val ICON_PX = 144
    private val icons = LruCache<String, ImageBitmap>(80)

    fun cachedIcon(pkg: String): ImageBitmap? = icons.get(pkg)

    /** The app's real launcher icon, or null if it can't be loaded. Does disk I/O; call off the main thread. */
    fun icon(context: Context, pkg: String): ImageBitmap? {
        icons.get(pkg)?.let { return it }
        val bmp = runCatching {
            context.packageManager.getApplicationIcon(pkg).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
        }.getOrNull() ?: return null
        icons.put(pkg, bmp)
        return bmp
    }
}
