package com.cedd.budgettracker.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cedd.budgettracker.R
import com.cedd.budgettracker.presentation.budget.BudgetScreen
import com.cedd.budgettracker.presentation.goals.GoalsScreen
import com.cedd.budgettracker.presentation.history.HistoryScreen
import com.cedd.budgettracker.presentation.settings.SettingsScreen
import com.cedd.budgettracker.presentation.utils.glowEffect
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    data object Budget   : Screen("budget")
    data object History  : Screen("history")
    data object Goals    : Screen("goals")
    data object Settings : Screen("settings")
}

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem(Screen.Budget,   "Budget",   Icons.Default.Wallet),
    NavItem(Screen.History,  "History",  Icons.Default.BarChart),
    NavItem(Screen.Goals,    "Goals",    Icons.Default.TrackChanges),
    NavItem(Screen.Settings, "Settings", Icons.Default.Settings),
)

// ── Colors shared across nav ──────────────────────────────────────────────────
private val ElectricBlue = Color(0xFF38BDF8)
private val NavBackground = Color(0xFF080F20)
private val NavBorder     = Color(0xFF1B3A5C)

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    var showSplash by remember { mutableStateOf(true) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(modifier = Modifier.fillMaxSize()) {
        GlowingMeshBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                GlowingBottomNav(
                    items           = navItems,
                    currentDestination = currentDestination,
                    onItemClick     = { item ->
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavHost(
                navController      = navController,
                startDestination   = Screen.Budget.route,
                modifier           = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Budget.route)   { BudgetScreen() }
                composable(Screen.History.route)  { HistoryScreen() }
                composable(Screen.Goals.route)    { GoalsScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }
        }

        // Splash overlay on top of everything
        AnimatedVisibility(
            visible = showSplash,
            enter   = fadeIn(animationSpec = tween(300)),
            exit    = fadeOut(animationSpec = tween(600))
        ) {
            SplashOverlay()
        }
    }

    LaunchedEffect(Unit) {
        delay(2800)
        showSplash = false
    }
}

// ── Glowing bottom navigation bar ────────────────────────────────────────────

@Composable
private fun GlowingBottomNav(
    items: List<NavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onItemClick: (NavItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        // Subtle top glow line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ElectricBlue.copy(alpha = 0.4f),
                            ElectricBlue.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        NavigationBar(
            containerColor    = NavBackground,
            contentColor      = Color.White,
            tonalElevation    = 0.dp,
            modifier          = Modifier.fillMaxWidth()
        ) {
            items.forEach { item ->
                val selected = currentDestination?.hierarchy?.any {
                    it.route == item.screen.route
                } == true

                val iconAlpha by animateFloatAsState(
                    targetValue    = if (selected) 1f else 0.45f,
                    animationSpec  = tween(250),
                    label          = "icon_alpha_${item.label}"
                )
                val iconScale by animateFloatAsState(
                    targetValue    = if (selected) 1.1f else 1f,
                    animationSpec  = tween(250),
                    label          = "icon_scale_${item.label}"
                )

                NavigationBarItem(
                    selected  = selected,
                    onClick   = { onItemClick(item) },
                    icon      = {
                        Box(contentAlignment = Alignment.Center) {
                            // Glow behind active icon
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .glowEffect(ElectricBlue, glowRadius = 18.dp, cornerRadius = 21.dp)
                                )
                            }
                            Icon(
                                imageVector      = item.icon,
                                contentDescription = item.label,
                                modifier         = Modifier
                                    .size(22.dp)
                                    .alpha(iconAlpha)
                                    .scale(iconScale),
                                tint             = if (selected) ElectricBlue else Color(0xFF64748B)
                            )
                        }
                    },
                    label     = {
                        Text(
                            text       = item.label,
                            fontSize   = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (selected) ElectricBlue else Color(0xFF64748B)
                        )
                    },
                    colors    = NavigationBarItemDefaults.colors(
                        selectedIconColor   = ElectricBlue,
                        unselectedIconColor = Color(0xFF64748B),
                        selectedTextColor   = ElectricBlue,
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor      = Color(0xFF0EA5E9).copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}

// ── Glowing mesh gradient background ─────────────────────────────────────────

@Composable
private fun GlowingMeshBackground(modifier: Modifier = Modifier) {
    val baseColor   = Color(0xFF060D1A)
    val purpleColor = Color(0xFF9333EA)   // vivid violet-purple
    val crimsonColor = Color(0xFFBE185D)  // deep pink-red for purple-red blend
    val tealColor   = Color(0xFF0D9488)

    Canvas(modifier = modifier.fillMaxSize()) {
        // Base fill
        drawRect(color = baseColor)

        // Primary purple radial glow — upper-left
        val purpleCenter = Offset(size.width * 0.22f, size.height * 0.25f)
        val purpleRadius = size.minDimension * 1.05f
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(purpleColor.copy(alpha = 0.50f), Color.Transparent),
                center = purpleCenter,
                radius = purpleRadius
            ),
            radius = purpleRadius,
            center = purpleCenter
        )

        // Secondary crimson-pink glow — upper-center (creates purple-red blend)
        val crimsonCenter = Offset(size.width * 0.55f, size.height * 0.18f)
        val crimsonRadius = size.minDimension * 0.70f
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(crimsonColor.copy(alpha = 0.22f), Color.Transparent),
                center = crimsonCenter,
                radius = crimsonRadius
            ),
            radius = crimsonRadius,
            center = crimsonCenter
        )

        // Teal radial glow — right side
        val tealCenter = Offset(size.width * 0.92f, size.height * 0.55f)
        val tealRadius = size.minDimension * 0.85f
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(tealColor.copy(alpha = 0.30f), Color.Transparent),
                center = tealCenter,
                radius = tealRadius
            ),
            radius = tealRadius,
            center = tealCenter
        )

        // Graph paper grid overlay — more visible for the "graph paper" effect
        val gridColor   = Color.White.copy(alpha = 0.10f)
        val gridSpacing = 20.dp.toPx()

        var x = 0f
        while (x <= size.width) {
            drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
            x += gridSpacing
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
            y += gridSpacing
        }
    }
}

// ── Splash overlay ─────────────────────────────────────────────────────────────

@Composable
private fun SplashOverlay() {
    val NeonBlue  = Color(0xFF38BDF8)

    var logoVisible  by remember { mutableStateOf(false) }
    var line1Visible by remember { mutableStateOf(false) }
    var line2Visible by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue   = if (logoVisible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "logo_alpha"
    )
    val logoScale by animateFloatAsState(
        targetValue   = if (logoVisible) 1f else 0.7f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label         = "logo_scale"
    )
    val line1Alpha by animateFloatAsState(
        targetValue   = if (line1Visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "line1_alpha"
    )
    val line1Offset by animateFloatAsState(
        targetValue   = if (line1Visible) 0f else 28f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "line1_offset"
    )
    val line2Alpha by animateFloatAsState(
        targetValue   = if (line2Visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "line2_alpha"
    )
    val line2Offset by animateFloatAsState(
        targetValue   = if (line2Visible) 0f else 28f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "line2_offset"
    )

    LaunchedEffect(Unit) {
        delay(100);  logoVisible  = true
        delay(350);  line1Visible = true
        delay(200);  line2Visible = true
    }

    Box(
        modifier           = Modifier.fillMaxSize(),
        contentAlignment   = Alignment.Center
    ) {
        GlowingMeshBackground()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glow ring behind icon
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .alpha(logoAlpha * 0.6f)
                        .glowEffect(NeonBlue, glowRadius = 40.dp, cornerRadius = 65.dp)
                )
                Image(
                    painter            = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "CeddFlow logo",
                    modifier           = Modifier
                        .size(110.dp)
                        .alpha(logoAlpha)
                        .scale(logoScale)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text          = "Own Your Budget.",
                fontSize      = 28.sp,
                fontWeight    = FontWeight.ExtraBold,
                color         = Color.White,
                textAlign     = TextAlign.Center,
                letterSpacing = 0.5.sp,
                modifier      = Modifier
                    .alpha(line1Alpha)
                    .offset(y = line1Offset.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text          = "Stay on Track.",
                fontSize      = 18.sp,
                fontWeight    = FontWeight.Medium,
                color         = NeonBlue,
                textAlign     = TextAlign.Center,
                letterSpacing = 1.sp,
                modifier      = Modifier
                    .alpha(line2Alpha)
                    .offset(y = line2Offset.dp)
            )
        }
    }
}
