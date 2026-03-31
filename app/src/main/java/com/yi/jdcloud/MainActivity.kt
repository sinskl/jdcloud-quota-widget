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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

object Screen {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun MainApp(
    loginLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    refreshKey: Int
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNav(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.HOME,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.HOME) {
                LoginScreen(
                    onNavigateToLogin = { context ->
                        val intent = Intent(context, LoginActivity::class.java)
                        loginLauncher.launch(intent)
                    },
                    refreshKey = refreshKey
                )
            }
            composable(Screen.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun BottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
            label = { Text("首页") },
            selected = currentRoute == Screen.HOME,
            onClick = {
                if (currentRoute != Screen.HOME) {
                    navController.navigate(Screen.HOME) {
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
            selected = currentRoute == Screen.SETTINGS,
            onClick = {
                if (currentRoute != Screen.SETTINGS) {
                    navController.navigate(Screen.SETTINGS) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}
