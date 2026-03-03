package com.cedd.budgettracker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cedd.budgettracker.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setOnExitAnimationListener { provider ->
            val scaleX = ObjectAnimator.ofFloat(provider.iconView, View.SCALE_X, 1f, 1.8f)
            val scaleY = ObjectAnimator.ofFloat(provider.iconView, View.SCALE_Y, 1f, 1.8f)
            val fade  = ObjectAnimator.ofFloat(provider.view, View.ALPHA, 1f, 0f)
            AnimatorSet().apply {
                interpolator = AnticipateInterpolator()
                duration = 500L
                playTogether(scaleX, scaleY, fade)
                doOnEnd { provider.remove() }
                start()
            }
        }
        setContent {
            BudgetTrackerTheme {
                AppNavigation()
            }
        }
    }
}

// ── App theme ──────────────────────────────────────────────────────────────────
// Palette: Neon Finance (Dark-only)
//   Night Base  #060D1A | Night Card #0C1829 | Electric Blue #0EA5E9
//   Indigo      #818CF8 | Emerald    #34D399  | Amber         #FBBF24

private val DarkColors = darkColorScheme(
    primary              = Color(0xFF38BDF8),   // Electric Sky Blue — glow color
    onPrimary            = Color(0xFF001829),
    primaryContainer     = Color(0xFF0C1829),   // Dark card
    onPrimaryContainer   = Color(0xFFF0F9FF),
    secondary            = Color(0xFF818CF8),   // Soft Indigo
    onSecondary          = Color(0xFF0D0D25),
    secondaryContainer   = Color(0xFF1E1B4B),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary             = Color(0xFF34D399),   // Emerald — positive/healthy
    onTertiary           = Color(0xFF001A0D),
    tertiaryContainer    = Color(0xFF064E3B),
    onTertiaryContainer  = Color(0xFFD1FAE5),
    background           = Color(0xFF060D1A),   // Ultra dark navy
    onBackground         = Color(0xFFF0F9FF),
    surface              = Color(0xFF0C1829),   // Dark card surface
    onSurface            = Color(0xFFF0F9FF),
    surfaceVariant       = Color(0xFF0F2035),   // Slightly elevated card
    onSurfaceVariant     = Color(0xFF94A3B8),
    outline              = Color(0xFF1B3A5C),   // Subtle border
    error                = Color(0xFFF87171),
    onError              = Color(0xFF2A0000),
    errorContainer       = Color(0xFF7F1D1D),
    onErrorContainer     = Color(0xFFFECACA),
)

@Composable
fun BudgetTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
