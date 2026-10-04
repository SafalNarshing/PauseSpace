package com.pausespace.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pausespace.app.data.Apps
import com.pausespace.app.data.InterventionLog
import com.pausespace.app.data.Permission
import com.pausespace.app.data.Permissions
import com.pausespace.app.data.Stats
import com.pausespace.app.data.Store
import com.pausespace.app.data.ThemeMode
import com.pausespace.app.ui.components.Icon
import com.pausespace.app.ui.components.Icons
import com.pausespace.app.ui.components.NavPill
import com.pausespace.app.ui.components.Tab
import com.pausespace.app.ui.components.rememberShown
import com.pausespace.app.ui.components.tap
import com.pausespace.app.ui.screens.AboutScreen
import com.pausespace.app.ui.screens.AppsScreen
import com.pausespace.app.ui.screens.RitualScreen
import com.pausespace.app.ui.screens.RulesScreen
import com.pausespace.app.ui.screens.SetupScreen
import com.pausespace.app.ui.screens.TodayScreen
import com.pausespace.app.ui.screens.WordsScreen
import com.pausespace.app.ui.theme.LocalTokens
import com.pausespace.app.ui.theme.PauseTheme
import com.pausespace.app.ui.theme.isDarkTheme
import com.pausespace.app.ui.theme.ts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by Store.prefs.collectAsState()
            val dark = isDarkTheme(prefs.theme)
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                enableEdgeToEdge(bars, bars)
            }
            PauseTheme(dark) { AppRoot() }
        }
    }
}

private object Routes {
    const val SETUP = "setup"
    const val TODAY = "today"
    const val APPS = "apps"
    const val RITUAL = "ritual"
    const val WORDS = "words"
    const val ABOUT = "about"
    const val RULES = "rules/{pkg}"
    fun rules(pkg: String) = "rules/$pkg"
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val t = LocalTokens.current
    val nav = rememberNavController()
    val prefs by Store.prefs.collectAsState()
    val logVersion by InterventionLog.version.collectAsState()

    // Re-read permissions and stats whenever we come back from Settings or the overlay.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val granted = remember(resumes) { Permission.entries.associateWith { Permissions.granted(context, it) } }
    val start = remember { if (Store.current.onboarded && Permissions.all(context)) Routes.TODAY else Routes.SETUP }

    val entry by nav.currentBackStackEntryAsState()
    val tab = when (entry?.destination?.route) {
        Routes.TODAY -> Tab.TODAY
        Routes.APPS -> Tab.APPS
        Routes.RITUAL -> Tab.RITUAL
        Routes.ABOUT -> Tab.ABOUT
        else -> null
    }
    // Remember the last tab so the pill keeps its state while it fades out over detail pages.
    var lastTab by remember { mutableStateOf(Tab.TODAY) }
    if (tab != null) lastTab = tab

    Box(Modifier.fillMaxSize().background(t.bg)) {
        NavHost(
            nav,
            startDestination = start,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable(Routes.SETUP) {
                SetupScreen(
                    granted = granted,
                    onAllow = { context.startActivity(Permissions.settingsIntent(context, it)) },
                    onContinue = {
                        if (!Store.current.onboarded) {
                            val defaults = Apps.DEFAULT_PAUSED.filter { Apps.isInstalled(context, it) }
                            Store.edit { p ->
                                p.copy(
                                    onboarded = true,
                                    rules = p.rules + defaults.associateWith { p.rule(it).copy(paused = true) },
                                )
                            }
                        }
                        nav.navigate(Routes.APPS) { popUpTo(Routes.SETUP) { inclusive = true } }
                    },
                )
            }
            composable(Routes.TODAY) {
                val stats by produceState(null as com.pausespace.app.data.TodayStats?, resumes, logVersion, prefs.rules, prefs.ritual.skipsPerDay) {
                    value = withContext(Dispatchers.IO) { Stats.today(context, Store.current) }
                }
                TabPage {
                    if (granted.values.any { !it }) PermissionBanner { nav.navigate(Routes.SETUP) }
                    TodayScreen(
                        stats,
                        onToggleTheme = {
                            Store.edit { it.copy(theme = if (t.dark) ThemeMode.LIGHT else ThemeMode.DARK) }
                        },
                        onOpenApp = { nav.navigate(Routes.rules(it)) },
                        onChooseApps = { nav.tab(Tab.APPS) },
                    )
                }
            }
            composable(Routes.APPS) {
                val apps by produceState(null as List<com.pausespace.app.data.AppInfo>?, resumes) {
                    value = withContext(Dispatchers.IO) { Apps.launchable(context, refresh = true) }
                }
                val week by produceState(emptyMap<String, Long>(), resumes) {
                    value = withContext(Dispatchers.IO) { Stats.weekUsage(context) }
                }
                TabShell { padding ->
                    AppsScreen(
                        apps = apps,
                        weekMs = week,
                        paused = prefs.pausedPackages,
                        contentPadding = padding,
                        onToggle = { pkg, on -> Store.editRule(pkg) { it.copy(paused = on) } },
                        onOpen = { nav.navigate(Routes.rules(it)) },
                    )
                }
            }
            composable(Routes.RITUAL) {
                TabPage {
                    RitualScreen(prefs.ritual, prefs.words, onChange = Store::editRitual, onWords = { nav.navigate(Routes.WORDS) })
                }
            }
            composable(Routes.ABOUT) {
                TabPage { AboutScreen() }
            }
            composable(Routes.WORDS) {
                DetailPage { WordsScreen(prefs.words, onBack = { nav.popBackStack() }, onChange = Store::editWords) }
            }
            composable(Routes.RULES) { entry ->
                val pkg = entry.arguments?.getString("pkg").orEmpty()
                val label = remember(pkg) { Apps.label(context, pkg) }
                val day by produceState(null as com.pausespace.app.data.AppDay?, pkg, resumes, logVersion) {
                    value = withContext(Dispatchers.IO) { Stats.appDay(context, pkg) }
                }
                DetailPage {
                    RulesScreen(
                        pkg = pkg,
                        label = label,
                        rule = prefs.rule(pkg),
                        ritual = prefs.ritual,
                        day = day,
                        onBack = { nav.popBackStack() },
                        onRitual = { nav.tab(Tab.RITUAL) },
                        onChange = { change -> Store.editRule(pkg, change) },
                    )
                }
            }
        }

        val shown = rememberShown()
        AnimatedVisibility(
            visible = tab != null,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp),
            enter = fadeIn(tween(220)) + slideInVertically(tween(300)) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(250)) { it / 2 },
        ) {
            NavPill(lastTab, shown, onSelect = { nav.tab(it) })
        }
    }
}

private fun NavHostController.tab(tab: Tab) {
    val route = when (tab) {
        Tab.TODAY -> Routes.TODAY
        Tab.APPS -> Routes.APPS
        Tab.RITUAL -> Routes.RITUAL
        Tab.ABOUT -> Routes.ABOUT
    }
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Tab screen content area. The pill nav lives in [AppRoot] so it persists across tab switches. */
@Composable
private fun TabShell(content: @Composable (PaddingValues) -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        content(PaddingValues(start = 20.dp, end = 20.dp, top = top + 32.dp, bottom = bottom + 120.dp))
        StatusScrim(top)
    }
}

@Composable
private fun TabPage(content: @Composable () -> Unit) {
    TabShell { padding ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

@Composable
private fun DetailPage(content: @Composable () -> Unit) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = top + 28.dp, bottom = bottom + 36.dp),
        ) { content() }
        StatusScrim(top)
    }
}

/** Keeps scrolled content from running under the status bar icons. */
@Composable
private fun StatusScrim(height: androidx.compose.ui.unit.Dp) {
    val t = LocalTokens.current
    Box(Modifier.fillMaxWidth().height(height).background(t.bg.copy(alpha = 0.94f)))
}

@Composable
private fun PermissionBanner(onClick: () -> Unit) {
    val t = LocalTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(t.ink)
            .tap(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            "PauseSpace is missing a permission, so paused apps open without a breath.",
            Modifier.weight(1f),
            style = ts(14, 700, t.bg),
        )
        Icon(Icons.Chevron, t.bg, 16.dp)
    }
}
