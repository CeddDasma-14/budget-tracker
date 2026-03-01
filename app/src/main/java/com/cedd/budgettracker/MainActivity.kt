package com.cedd.budgettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedd.budgettracker.data.preferences.PreferencesRepository
import com.cedd.budgettracker.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefsRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by prefsRepository.isDarkMode.collectAsStateWithLifecycle(initialValue = false)
            BudgetTrackerTheme(darkTheme = isDarkMode) {
                AppNavigation()
            }
        }
    }
}

// ── App theme ──────────────────────────────────────────────────────────────────
// Palette: Midnight Finance
//   Deep Navy #0A2342 | Royal Blue #1565C0 | Sky Blue #42A5F5
//   Teal Green #00BFA5 | Soft White #F5F7FA | Pure White #FFFFFF

private val LightColors = lightColorScheme(
    primary             = Color(0xFF1565C0),   // Royal Blue — buttons, focus borders
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFF0A2342),   // Deep Navy — header card background
    onPrimaryContainer  = Color(0xFFFFFFFF),   // White text on header
    secondary           = Color(0xFF42A5F5),   // Sky Blue
    onSecondary         = Color(0xFF003166),
    secondaryContainer  = Color(0xFFD0E8FF),
    onSecondaryContainer = Color(0xFF001D40),
    tertiary            = Color(0xFF00BFA5),   // Teal Green — healthy states
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF00352D),
    background          = Color(0xFFF5F7FA),   // Soft White app background
    onBackground        = Color(0xFF1A1C1E),
    surface             = Color(0xFFFFFFFF),   // Pure White cards
    onSurface           = Color(0xFF1A1C1E),
    surfaceVariant      = Color(0xFFE3E8F0),
    onSurfaceVariant    = Color(0xFF42474E),
    outline             = Color(0xFF90A4AE),   // Blue-Gray — checkbox borders
)

private val DarkColors = darkColorScheme(
    primary             = Color(0xFF90CAF9),   // Light Blue
    onPrimary           = Color(0xFF003166),
    primaryContainer    = Color(0xFF0D2B5E),   // Dark Navy header
    onPrimaryContainer  = Color(0xFFD0E8FF),
    secondary           = Color(0xFF64B5F6),
    onSecondary         = Color(0xFF003166),
    secondaryContainer  = Color(0xFF1A3A6B),
    onSecondaryContainer = Color(0xFFD0E8FF),
    tertiary            = Color(0xFF4DB6AC),   // Muted Teal
    onTertiary          = Color(0xFF00352D),
    tertiaryContainer   = Color(0xFF00574A),
    onTertiaryContainer = Color(0xFFB2DFDB),
    background          = Color(0xFF0D1117),   // GitHub-dark style
    onBackground        = Color(0xFFE1E8F0),
    surface             = Color(0xFF161B22),
    onSurface           = Color(0xFFE1E8F0),
    outline             = Color(0xFF546E7A),
)

@Composable
fun BudgetTrackerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
