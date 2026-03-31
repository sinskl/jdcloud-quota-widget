package com.yi.jdcloud.ui.login

import android.webkit.WebView
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

    fun onWebViewLoginSuccess() {
        _uiState.value = _uiState.value.copy(isExtractingCookies = true)
        viewModelScope.launch {
            try {
                val state = CookieExtractor.extract()
                if (state.isComplete()) {
                    preferences.saveLoginState(state)
                    _uiState.value = _uiState.value.copy(isExtractingCookies = false)
                    // Auto-fetch quota after login
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
                    error = e.message ?: "登录失败"
                )
            }
        }
    }

    fun fetchQuota(state: LoginState? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val currentState = state ?: loginState.value
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
