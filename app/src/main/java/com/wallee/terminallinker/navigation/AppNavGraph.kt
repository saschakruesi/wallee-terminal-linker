package com.wallee.terminallinker.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wallee.terminallinker.BuildConfig
import com.wallee.terminallinker.feature.link.ResultScreen
import com.wallee.terminallinker.feature.scan.ScanScreen
import com.wallee.terminallinker.feature.settings.SettingsScreen
import com.wallee.terminallinker.feature.setup.SetupScreen
import com.wallee.terminallinker.feature.styleguide.StyleguideScreen
import com.wallee.terminallinker.feature.terminals.TerminalDetailScreen
import com.wallee.terminallinker.feature.terminals.TerminalsScreen

private const val TRANSITION_MS = 200

/**
 * All routes from docs/03 wired with fade transitions ≤ 200 ms. Phase 1 starts at Setup because there
 * is no credential store yet; phase 2 chooses Setup or Terminals from the stored state.
 */
@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = SetupRoute,
        enterTransition = { fadeIn(tween(TRANSITION_MS)) },
        exitTransition = { fadeOut(tween(TRANSITION_MS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MS)) },
        popExitTransition = { fadeOut(tween(TRANSITION_MS)) },
    ) {
        composable<SetupRoute> {
            SetupScreen(
                onContinue = {
                    navController.navigate(TerminalsRoute) {
                        popUpTo<SetupRoute> { inclusive = true }
                    }
                },
                onScanCredentials = { navController.navigate(ScanRoute(mode = ScanMode.CREDENTIALS)) },
            )
        }
        composable<TerminalsRoute> {
            TerminalsScreen(
                onOpenTerminal = { id -> navController.navigate(TerminalDetailRoute(id)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
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
                onUnlinked = { serial ->
                    navController.navigate(
                        ResultRoute(route.terminalId, ResultOutcome.UNLINKED, previousSerial = serial),
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
                onLinked = { terminalId, serial, previousSerial ->
                    val outcome = if (route.mode == ScanMode.REPLACE) ResultOutcome.REPLACED else ResultOutcome.LINKED
                    navController.navigate(ResultRoute(terminalId, outcome, serial, previousSerial)) {
                        popUpTo<TerminalDetailRoute> { inclusive = false }
                    }
                },
                onCredentialsScanned = { navController.popBackStack() },
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
                    navController.navigate(TerminalsRoute) {
                        popUpTo<TerminalsRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
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
