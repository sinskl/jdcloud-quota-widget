package com.yi.jdcloud.ui.login

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yi.jdcloud.data.CookieExtractor
import com.yi.jdcloud.ui.theme.JdCloudTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private var loginState by mutableStateOf<LoginUiState2>(LoginUiState2.Loading)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get desktop user agent
        val desktopUA = try {
            packageManager.getApplicationInfo(packageName, 0).let {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
            }
        } catch (e: Exception) {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
        }

        setContent {
            JdCloudTheme {
                when (val state = loginState) {
                    is LoginUiState2.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is LoginUiState2.WebViewLogin -> {
                        WebViewLoginScreen(
                            desktopUA = desktopUA,
                            onLoginDetected = {
                                loginState = LoginUiState2.LoginDetected
                            },
                            onManualExtract = {
                                performExtract()
                            }
                        )
                    }
                    is LoginUiState2.LoginDetected -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = "✅ 登录成功",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "点击下方按钮提取 Cookie 并返回",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = { performExtract() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text("提取 Cookie")
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { loginState = LoginUiState2.WebViewLogin },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("重新登录")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun performExtract() {
        loginState = LoginUiState2.Extracting
        try {
            CookieManager.getInstance().flush()
            val state = CookieExtractor.extract()
            if (state.isComplete()) {
                // Save via a simple approach - finish with result
                val intent = intent.apply {
                    putExtra(EXTRA_COOKIE_PIN, state.pin)
                    putExtra(EXTRA_COOKIE_THOR, state.thor)
                    putExtra(EXTRA_COOKIE_QID_UID, state.qidUid)
                    putExtra(EXTRA_COOKIE_QID_SID, state.qidSid)
                    putExtra(EXTRA_COOKIE_JDV, state.jdv)
                }
                setResult(RESULT_OK, intent)
                finish()
            } else {
                loginState = LoginUiState2.LoginDetected
            }
        } catch (e: Exception) {
            loginState = LoginUiState2.LoginDetected
        }
    }

    companion object {
        const val EXTRA_COOKIE_PIN = "cookie_pin"
        const val EXTRA_COOKIE_THOR = "cookie_thor"
        const val EXTRA_COOKIE_QID_UID = "cookie_qid_uid"
        const val EXTRA_COOKIE_QID_SID = "cookie_qid_sid"
        const val EXTRA_COOKIE_JDV = "cookie_jdv"
    }
}

sealed class LoginUiState2 {
    data object Loading : LoginUiState2()
    data object WebViewLogin : LoginUiState2()
    data object LoginDetected : LoginUiState2()
    data object Extracting : LoginUiState2()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewLoginScreen(
    desktopUA: String,
    onLoginDetected: () -> Unit,
    onManualExtract: () -> Unit
) {
    var webViewRef by androidx.compose.runtime.remember { mutableStateOf<WebView?>(null) }
    var hasNavigatedAway by androidx.compose.runtime.remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "京东云登录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "使用桌面模式登录，登录成功后点击「提取 Cookie」",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = desktopUA
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url ?: "")
                            // Only detect as "logged in" when URL moves away from /login
                            val currentUrl = url ?: ""
                            val onLoginPage = currentUrl.contains("/login") ||
                                    currentUrl.contains("/uc/login") ||
                                    currentUrl.contains("/static/login")
                            if (!onLoginPage && hasNavigatedAway && currentUrl.contains("joybuilder-console")) {
                                onLoginDetected()
                            }
                            // After first navigation away from login page, mark as having navigated
                            if (!onLoginPage && currentUrl.contains("joybuilder-console")) {
                                hasNavigatedAway = true
                            }
                        }
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    loadUrl("https://joybuilder-console.jdcloud.com/login")
                    webViewRef = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Bottom action bar
        Button(
            onClick = onManualExtract,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("提取 Cookie")
        }
    }
}
