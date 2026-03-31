package com.yi.jdcloud.data

import android.webkit.CookieManager
import android.webkit.WebView
import com.yi.jdcloud.domain.LoginState

/**
 * 从 WebView 的 CookieManager 中提取京东云登录所需的 Cookie 字段。
 *
 * 需要的字段：pin, thor, qid_uid, qid_sid, jdv
 *
 * 使用方式：
 * 1. 用 WebView 加载 https://joybuilder-console.jdcloud.com/ 并完成登录
 * 2. 登录成功后，调用 extract() 获取 LoginState
 */
object CookieExtractor {

    private const val DOMAIN = "joybuilder-console.jdcloud.com"

    /**
     * 从 CookieManager 中提取登录态。
     * 调用前请确保 WebView 已加载登录页并完成登录。
     */
    fun extract(): LoginState {
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()

        val allCookies = cookieManager.getCookie(DOMAIN) ?: return LoginState()

        val map = parseCookieString(allCookies)

        return LoginState(
            isLoggedIn = map.containsKey("thor") && map["thor"]?.isNotBlank() == true,
            pin = map["pin"] ?: "",
            thor = map["thor"] ?: "",
            qidUid = map["qid_uid"] ?: "",
            qidSid = map["qid_sid"] ?: "",
            jdv = map["jdv"] ?: ""
        )
    }

    /**
     * 清空京东云相关的 Cookie。
     */
    fun clear() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    private fun parseCookieString(cookieString: String): Map<String, String> {
        return cookieString.split(";").mapNotNull { part ->
            val trimmed = part.trim()
            val eqIndex = trimmed.indexOf('=')
            if (eqIndex > 0) {
                trimmed.substring(0, eqIndex).trim() to trimmed.substring(eqIndex + 1).trim()
            } else null
        }.toMap()
    }
}

/**
 * 登录状态回调接口。
 * 在 WebView 页面加载完成后检查 URL，判断是否登录成功。
 */
object LoginUrlChecker {
    private const val LOGIN_SUCCESS_URL = "joybuilder-console.jdcloud.com/system"

    fun isLoginSuccess(url: String): Boolean {
        return url.contains(LOGIN_SUCCESS_URL) && !url.contains("/login")
    }
}
