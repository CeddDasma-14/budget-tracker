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

private val GreenPrimary      = Color(0xFF2E7D32)
private val GreenOnPrimary    = Color(0xFFFFFFFF)
private val GreenContainer    = Color(0xFFB8F5BB)
private val GreenOnContainer  = Color(0xFF002107)
private val GreenSecondary    = Color(0xFF52634F)
private val GreenTertiary     = Color(0xFF38656A)

private val LightColors = lightColorScheme(
    primary          = GreenPrimary,
    onPrimary        = GreenOnPrimary,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenOnContainer,
    secondary        = GreenSecondary,
    tertiary         = GreenTertiary
)

private val DarkColors = darkColorScheme(
    primary          = Color(0xFF9DD49E),
    onPrimary        = Color(0xFF003910),
    primaryContainer = Color(0xFF1A5E1F),
    onPrimaryContainer = Color(0xFFB8F5BB),
    secondary        = Color(0xFFB8CCB4),
    tertiary         = Color(0xFF86CBCE)
)

@Composable
fun BudgetTrackerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
