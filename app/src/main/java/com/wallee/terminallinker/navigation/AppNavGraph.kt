package com.wallee.terminallinker.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wallee.terminallinker.BuildConfig
import com.wallee.terminallinker.di.appContainer
import com.wallee.terminallinker.feature.link.ResultScreen
import com.wallee.terminallinker.feature.scan.ScanScreen
import com.wallee.terminallinker.feature.settings.SettingsScreen
import com.wallee.terminallinker.feature.setup.SetupScreen
import com.wallee.terminallinker.feature.styleguide.StyleguideScreen
import com.wallee.terminallinker.feature.terminals.TerminalDetailScreen
import com.wallee.terminallinker.feature.terminals.TerminalFilter
import com.wallee.terminallinker.feature.terminals.TerminalsScreen
import kotlinx.coroutines.launch

private const val TRANSITION_MS = 200

/**
 * All routes from docs/03 wired with fade transitions ≤ 200 ms. [startDestination] is Terminals when
 * credentials are stored, otherwise Setup (decided in MainActivity).
 */
@Composable
fun AppNavGraph(startDestination: Any, navController: NavHostController = rememberNavController()) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(TRANSITION_MS)) },
        exitTransition = { fadeOut(tween(TRANSITION_MS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MS)) },
        popExitTransition = { fadeOut(tween(TRANSITION_MS)) },
    ) {
        composable<SetupRoute> {
            SetupScreen(
                onContinue = {
                    navController.navigate(TerminalsRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onScanCredentials = { navController.navigate(ScanRoute(mode = ScanMode.CREDENTIALS)) },
            )
        }
        composable<TerminalsRoute> {
            TerminalsScreen(
                onOpenTerminal = { id -> navController.navigate(TerminalDetailRoute(id)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onScanSpaces = { navController.navigate(ScanRoute(mode = ScanMode.CREDENTIALS, spacesOnly = true)) },
            )
        }
        composable<TerminalDetailRoute> { entry ->
            val route = entry.toRoute<TerminalDetailRoute>()
            TerminalDetailScreen(
                terminalId = route.terminalId,
                onBack = { navController.popBackStack() },
                onLink = { navController.navigate(ScanRoute(mode = ScanMode.LINK, terminalId = route.terminalId)) },
                onReplace = {
                    navController.navigate(ScanRoute(mode = ScanMode.REPLACE, terminalId = route.terminalId))
                },
                onUnlinked = { serial, confirmed ->
                    navController.navigate(
                        ResultRoute(
                            route.terminalId,
                            ResultOutcome.UNLINKED,
                            previousSerial = serial,
                            confirmed = confirmed,
                        ),
                    )
                },
            )
        }
        composable<ScanRoute> { entry ->
            val route = entry.toRoute<ScanRoute>()
            ScanScreen(
                mode = route.mode,
                terminalId = route.terminalId,
                onClose = { navController.popBackStack() },
                onLinked = { terminalId, outcome, serial, previousSerial, confirmed ->
                    navController.navigate(ResultRoute(terminalId, outcome, serial, previousSerial, confirmed)) {
                        popUpTo<TerminalDetailRoute> { inclusive = false }
                    }
                },
                onCredentialsScanned = { navController.popBackStack() },
                onToList = {
                    navController.navigate(TerminalsRoute) {
                        popUpTo<TerminalsRoute> { inclusive = true }
                    }
                },
                spacesOnly = route.spacesOnly,
            )
        }
        composable<ResultRoute> { entry ->
            val route = entry.toRoute<ResultRoute>()
            ResultScreen(
                route = route,
                onBackToList = {
                    navController.navigate(TerminalsRoute) {
                        popUpTo<TerminalsRoute> { inclusive = true }
                    }
                },
                onLinkNext = {
                    // «Nächstes Terminal linken»: list filtered to unlinked (docs/03 §Ergebnis).
                    scope.launch { container.uiPrefs.setTerminalFilter(TerminalFilter.UNLINKED) }
                    navController.navigate(TerminalsRoute) {
                        popUpTo<TerminalsRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onEditCredentials = { navController.navigate(SetupRoute) },
                onScanSpaces = { navController.navigate(ScanRoute(mode = ScanMode.CREDENTIALS, spacesOnly = true)) },
                onOpenStyleguide = if (BuildConfig.DEBUG) {
                    { navController.navigate(StyleguideRoute) }
                } else {
                    null
                },
            )
        }
        if (BuildConfig.DEBUG) {
            composable<StyleguideRoute> {
                StyleguideScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
