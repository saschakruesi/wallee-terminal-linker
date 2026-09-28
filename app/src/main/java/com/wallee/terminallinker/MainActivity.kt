package com.wallee.terminallinker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.wallee.terminallinker.core.ui.WalleeColors
import com.wallee.terminallinker.core.ui.WalleeTheme
import com.wallee.terminallinker.navigation.AppNavGraph
import com.wallee.terminallinker.navigation.SetupRoute
import com.wallee.terminallinker.navigation.TerminalsRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Light status bar with dark icons, white navigation bar — always, regardless of system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(WalleeColors.Bg.toArgb(), WalleeColors.Bg.toArgb()),
        )
        super.onCreate(savedInstanceState)
        val container = (application as TerminalLinkerApp).container
        val start: Any = if (container.credentialStore.load() != null) TerminalsRoute else SetupRoute
        setContent {
            WalleeTheme {
                AppNavGraph(startDestination = start)
            }
        }
    }
}
