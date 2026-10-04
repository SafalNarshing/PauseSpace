package com.pausespace.app.data

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import com.pausespace.app.service.PauseAccessibilityService

enum class Permission(val title: String, val why: String) {
    USAGE("Usage access", "Shows screen time and when each app was open."),
    OVERLAY("Display over apps", "Lets the breathing pause appear on top."),
    ACCESSIBILITY("Accessibility", "Notices the moment a paused app opens."),
}

object Permissions {
    fun granted(context: Context, p: Permission): Boolean = when (p) {
        Permission.USAGE -> usage(context)
        Permission.OVERLAY -> Settings.canDrawOverlays(context)
        Permission.ACCESSIBILITY -> accessibility(context)
    }

    fun all(context: Context) = Permission.entries.all { granted(context, it) }

    fun settingsIntent(context: Context, p: Permission): Intent = when (p) {
        Permission.USAGE -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        Permission.OVERLAY -> Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        Permission.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun usage(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun accessibility(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val me = ComponentName(context, PauseAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}
