package com.yi.jdcloud.ui.login

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.yi.jdcloud.data.LoginUrlChecker
import com.yi.jdcloud.domain.QuotaInfo

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val quotaInfo by viewModel.quotaInfo.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "京东云 JoyBuilder",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        if (loginState.isLoggedIn && loginState.isComplete()) {
            // Logged in — show quota
            QuotaContent(
                quota = quotaInfo,
                isLoading = uiState.isLoading,
                onRefresh = { viewModel.fetchQuota() },
                onLogout = { viewModel.logout() }
            )
        } else {
            // Not logged in — show WebView login
            LoginWebViewContent(
                viewModel = viewModel,
                isExtractingCookies = uiState.isExtractingCookies
            )
        }

        // Error message
        uiState.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LoginWebViewContent(
    viewModel: LoginViewModel,
    isExtractingCookies: Boolean
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loginDetected by remember { mutableStateOf(false) }

    Text(
        text = "登录京东云控制台",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "在下方登录后，点击「提取 Cookie 并查询额度」",
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))

    when {
        isExtractingCookies -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("正在提取 Cookie...")
                }
            }
        }
        loginDetected -> {
            // Login detected — show success state with manual trigger button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "登录成功！",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "点击下方按钮提取 Cookie 并查询额度",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF555555)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.onWebViewLoginSuccess() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("提取 Cookie 并查询额度")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { loginDetected = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("重新登录")
                    }
                }
            }
        }
        else -> {
            // Show WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString =
                            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url ?: "")
                                val currentUrl = url ?: ""
                                // Detect successful login: on console page, not on login/auth pages
                                if (LoginUrlChecker.isLoginSuccess(currentUrl)) {
                                    loginDetected = true
                                }
                            }
                        }
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                        }
                        loadUrl("https://joybuilder-console.jdcloud.com/login")
                        webViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        webViewRef?.loadUrl("https://joybuilder-console.jdcloud.com/login")
                        loginDetected = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("刷新登录页")
                }
                OutlinedButton(
                    onClick = {
                        webViewRef?.let { wv ->
                            // Force extract cookies from whatever is currently in WebView
                            CookieManager.getInstance().flush()
                            viewModel.onWebViewLoginSuccess()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("手动提取")
                }
            }
        }
    }
}

@Composable
private fun QuotaContent(
    quota: QuotaInfo?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (quota != null) {
            QuotaCard(
                title = quota.planName,
                subtitle = "默认模型: ${quota.defaultModel}",
                footer = "到期: ${quota.endTime}"
            )
            Spacer(Modifier.height(16.dp))
            QuotaProgressCard("5小时", quota.h5Used, quota.h5Limit, quota.h5Percent)
            Spacer(Modifier.height(8.dp))
            QuotaProgressCard("7天", quota.d7Used, quota.d7Limit, quota.d7Percent)
            Spacer(Modifier.height(8.dp))
            QuotaProgressCard("本月", quota.monthUsed, quota.monthLimit, quota.monthPercent)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("加载中...")
                    } else {
                        Text(
                            text = "点击刷新获取额度",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("刷新")
            }
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.weight(1f)
            ) {
                Text("退出登录")
            }
        }
    }
}

@Composable
private fun QuotaCard(title: String, subtitle: String, footer: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun QuotaProgressCard(label: String, used: Int, limit: Int, pct: Float) {
    val barColor = when {
        pct > 0.8f -> MaterialTheme.colorScheme.error
        pct > 0.5f -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$used / $limit",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = barColor,
                trackColor = Color(0xFFEEEEEE),
            )
        }
    }
}
