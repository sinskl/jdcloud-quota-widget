package com.yi.jdcloud.ui.login

import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yi.jdcloud.data.CookieExtractor
import com.yi.jdcloud.data.Preferences
import com.yi.jdcloud.data.QuotaRepository
import com.yi.jdcloud.domain.LoginState
import com.yi.jdcloud.domain.QuotaInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isExtractingCookies: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val preferences: Preferences,
    private val quotaRepository: QuotaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val loginState: StateFlow<LoginState> = preferences.loginState
        .stateIn(viewModelScope, SharingStarted.Eagerly, LoginState())

    val quotaInfo: StateFlow<QuotaInfo?> = preferences.quotaInfo
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Try to load quota on startup if cookies exist
        viewModelScope.launch {
            val state = preferences.loginState.first()
            if (state.isLoggedIn && state.isComplete()) {
                fetchQuota(state)
            }
        }
    }

    fun onWebViewLoginSuccess() {
        _uiState.value = _uiState.value.copy(isExtractingCookies = true)

        // Flush WebView cookies to system cookie manager before extracting
        try {
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            // Ignore flush errors
        }

        viewModelScope.launch {
            try {
                // Small delay to let cookies settle
                kotlinx.coroutines.delay(500)

                val state = CookieExtractor.extract()
                if (state.isComplete()) {
                    preferences.saveLoginState(state)
                    _uiState.value = _uiState.value.copy(isExtractingCookies = false)
                    // Fetch quota immediately with the extracted state
                    fetchQuota(state)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isExtractingCookies = false,
                        error = "Cookie 提取不完整，请重试"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExtractingCookies = false,
                    error = "登录失败: ${e.message}"
                )
            }
        }
    }

    fun fetchQuota(state: LoginState? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentState = state ?: preferences.loginState.first()
            if (!currentState.isComplete()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "未登录或 Cookie 不完整"
                )
                return@launch
            }
            val result = quotaRepository.fetchQuota(currentState)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferences.clearAll()
            CookieExtractor.clear()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
