package com.cedd.budgettracker.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cedd.budgettracker.R
import com.cedd.budgettracker.presentation.budget.BudgetScreen
import com.cedd.budgettracker.presentation.history.HistoryScreen
import com.cedd.budgettracker.presentation.settings.SettingsScreen
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    data object Budget : Screen("budget")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    var showSplash by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Budget.route
        ) {
            composable(Screen.Budget.route) {
                BudgetScreen(
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(600))
        ) {
            SplashOverlay()
        }
    }

    LaunchedEffect(Unit) {
        delay(2800)
        showSplash = false
    }
}

// ── Splash overlay ─────────────────────────────────────────────────────────────

@Composable
private fun SplashOverlay() {
    val DeepNavy = Color(0xFF0A2342)
    val Turquoise = Color(0xFF00B7B5)

    var logoVisible  by remember { mutableStateOf(false) }
    var line1Visible by remember { mutableStateOf(false) }
    var line2Visible by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.7f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "logo_scale"
    )
    val line1Alpha by animateFloatAsState(
        targetValue = if (line1Visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "line1_alpha"
    )
    val line1Offset by animateFloatAsState(
        targetValue = if (line1Visible) 0f else 28f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "line1_offset"
    )
    val line2Alpha by animateFloatAsState(
        targetValue = if (line2Visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "line2_alpha"
    )
    val line2Offset by animateFloatAsState(
        targetValue = if (line2Visible) 0f else 28f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "line2_offset"
    )

    LaunchedEffect(Unit) {
        delay(100);  logoVisible  = true
        delay(350);  line1Visible = true
        delay(200);  line2Visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App icon
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "CeddFlow logo",
                modifier = Modifier
                    .size(110.dp)
                    .alpha(logoAlpha)
                    .scale(logoScale)
            )

            Spacer(Modifier.height(28.dp))

            // "Own Your Budget."
            Text(
                text = "Own Your Budget.",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .alpha(line1Alpha)
                    .offset(y = line1Offset.dp)
            )

            Spacer(Modifier.height(8.dp))

            // "Stay on Track."
            Text(
                text = "Stay on Track.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Turquoise,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .alpha(line2Alpha)
                    .offset(y = line2Offset.dp)
            )
        }
    }
}
