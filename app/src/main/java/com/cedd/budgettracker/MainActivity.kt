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
// Palette: Deep Teal #005461 | Mid Teal #018790 | Bright Turquoise #00B7B5
//          Cloud White #F4F4F4 | Pure White #FFFFFF

private val LightColors = lightColorScheme(
    primary             = Color(0xFF005461),   // Deep Teal — buttons, focus borders
    onPrimary           = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFF005461),   // Header card background
    onPrimaryContainer  = Color(0xFFFFFFFF),   // White text on header
    secondary           = Color(0xFF018790),   // Mid Teal
    onSecondary         = Color(0xFFFFFFFF),
    secondaryContainer  = Color(0xFFB2EBED),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary            = Color(0xFF00B7B5),   // Bright Turquoise
    onTertiary          = Color(0xFFFFFFFF),
    tertiaryContainer   = Color(0xFFB3F5F4),
    onTertiaryContainer = Color(0xFF001F24),
    background          = Color(0xFFF4F4F4),   // Cloud White app background
    onBackground        = Color(0xFF191C1C),
    surface             = Color(0xFFFFFFFF),   // Pure White cards
    onSurface           = Color(0xFF191C1C),
    surfaceVariant      = Color(0xFFDCE4E5),
    onSurfaceVariant    = Color(0xFF3F484A),
    outline             = Color(0xFF018790),   // Mid Teal — checkbox borders
)

private val DarkColors = darkColorScheme(
    primary             = Color(0xFF80D5D8),
    onPrimary           = Color(0xFF002E31),
    primaryContainer    = Color(0xFF004A55),
    onPrimaryContainer  = Color(0xFFB3F0EF),
    secondary           = Color(0xFF80C8CC),
    onSecondary         = Color(0xFF002528),
    secondaryContainer  = Color(0xFF003B40),
    onSecondaryContainer = Color(0xFFB2EBED),
    tertiary            = Color(0xFF00D5D3),
    onTertiary          = Color(0xFF003B3A),
    tertiaryContainer   = Color(0xFF005251),
    onTertiaryContainer = Color(0xFFB3F5F4),
    background          = Color(0xFF0F1A1C),
    onBackground        = Color(0xFFE0E8E9),
    surface             = Color(0xFF1A2A2C),
    onSurface           = Color(0xFFE0E8E9),
    outline             = Color(0xFF80C8CC),
)

@Composable
fun BudgetTrackerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
