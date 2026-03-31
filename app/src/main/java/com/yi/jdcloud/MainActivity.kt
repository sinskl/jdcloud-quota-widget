package com.yi.jdcloud

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.yi.jdcloud.data.Preferences
import com.yi.jdcloud.domain.LoginState
import com.yi.jdcloud.ui.login.LoginActivity
import com.yi.jdcloud.ui.login.LoginScreen
import com.yi.jdcloud.ui.settings.SettingsScreen
import com.yi.jdcloud.ui.theme.JdCloudTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: Preferences

    private var refreshKey by mutableIntStateOf(0)

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val pin = data.getStringExtra(LoginActivity.EXTRA_COOKIE_PIN) ?: ""
                val thor = data.getStringExtra(LoginActivity.EXTRA_COOKIE_THOR) ?: ""
                val qidUid = data.getStringExtra(LoginActivity.EXTRA_COOKIE_QID_UID) ?: ""
                val qidSid = data.getStringExtra(LoginActivity.EXTRA_COOKIE_QID_SID) ?: ""
                val jdv = data.getStringExtra(LoginActivity.EXTRA_COOKIE_JDV) ?: ""

                if (pin.isNotBlank() && thor.isNotBlank()) {
                    lifecycleScope.launch {
                        preferences.saveLoginState(
                            LoginState(
                                isLoggedIn = true,
                                pin = pin,
                                thor = thor,
                                qidUid = qidUid,
                                qidSid = qidSid,
                                jdv = jdv
                            )
                        )
                        // Force recomposition with new key
                        refreshKey++
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JdCloudTheme {
                MainApp(loginLauncher = loginLauncher, refreshKey = refreshKey)
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
}

@Composable
fun MainApp(
    loginLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    refreshKey: Int
) {
    val navController = androidx.navigation.compose.rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNav(navController = navController)
        }
    ) { paddingValues ->
        androidx.navigation.compose.NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            androidx.navigation.compose.composable(Screen.Home.route) {
                LoginScreen(
                    onNavigateToLogin = { context ->
                        val intent = Intent(context, LoginActivity::class.java)
                        loginLauncher.launch(intent)
                    },
                    refreshKey = refreshKey
                )
            }
            androidx.navigation.compose.composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun BottomNav(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
            label = { Text("首页") },
            selected = currentRoute == Screen.Home.route,
            onClick = {
                if (currentRoute != Screen.Home.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
            label = { Text("设置") },
            selected = currentRoute == Screen.Settings.route,
            onClick = {
                if (currentRoute != Screen.Settings.route) {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}
