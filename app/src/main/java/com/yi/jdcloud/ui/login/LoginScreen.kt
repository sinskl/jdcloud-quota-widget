package com.yi.jdcloud.ui.login

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.yi.jdcloud.data.Preferences
import com.yi.jdcloud.data.QuotaRepository
import com.yi.jdcloud.domain.QuotaInfo

@Composable
fun LoginScreen(
    onNavigateToLogin: (Context) -> Unit,
    refreshKey: Int = 0,
    viewModel: LoginScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(refreshKey) {
        viewModel.tryAutoFetch()
    }

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

        when {
            uiState.isLoading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("加载中...")
                        }
                    }
                }
            }
            !uiState.isLoggedIn -> {
                NotLoggedInCard(onLoginClick = { onNavigateToLogin(context) })
            }
            uiState.quota != null -> {
                QuotaContent(
                    quota = uiState.quota!!,
                    onRefresh = { viewModel.fetchQuota() },
                    onLogout = { viewModel.logout() }
                )
            }
            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("已登录，点击刷新获取额度")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.fetchQuota() }) { Text("刷新") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.logout() }) { Text("退出登录") }
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NotLoggedInCard(onLoginClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔴", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "未登录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "点击下方按钮前往登录\n登录成功后自动返回",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                Text("前往登录")
            }
        }
    }
}

@Composable
private fun QuotaContent(
    quota: QuotaInfo,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("刷新") }
            OutlinedButton(onClick = onLogout, modifier = Modifier.weight(1f)) { Text("退出") }
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
                Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(text = "$used / $limit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = barColor,
                trackColor = Color(0xFFEEEEEE),
            )
        }
    }
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class LoginScreenUiState(
    val isLoggedIn: Boolean = false,
    val quota: QuotaInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val preferences: Preferences,
    private val quotaRepository: QuotaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginScreenUiState())
    val uiState: StateFlow<LoginScreenUiState> = _uiState

    init {
        viewModelScope.launch {
            preferences.loginState.collect { loginState ->
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = loginState.isLoggedIn && loginState.isComplete()
                )
                if (loginState.isLoggedIn && loginState.isComplete()) {
                    tryAutoFetch()
                }
            }
        }
        viewModelScope.launch {
            preferences.quotaInfo.collect { quota ->
                _uiState.value = _uiState.value.copy(quota = quota)
            }
        }
    }

    fun tryAutoFetch() {
        viewModelScope.launch {
            val state = preferences.loginState.first()
            if (state.isLoggedIn && state.isComplete()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val result = quotaRepository.fetchQuota(state)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun fetchQuota() {
        viewModelScope.launch {
            val state = preferences.loginState.first()
            if (!state.isComplete()) {
                _uiState.value = _uiState.value.copy(error = "未登录")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = quotaRepository.fetchQuota(state)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferences.clearAll()
        }
    }
}
