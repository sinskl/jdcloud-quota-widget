package com.yi.jdcloud.data

import android.webkit.CookieManager
import android.webkit.ValueCallback
import com.yi.jdcloud.domain.LoginState

/**
 * 从 WebView 的 CookieManager 中提取京东云登录所需的 Cookie 字段。
 *
 * 需要的字段：pin, thor, qid_uid, qid_sid, jdv
 *
 * 注意：登录后 cookie 可能在 joybuilder-console.jdcloud.com 或其父域 .jdcloud.com
 */
object CookieExtractor {

    private val DOMAINS = listOf(
        "joybuilder-console.jdcloud.com",
        ".joybuilder-console.jdcloud.com",
        ".jdcloud.com",
        "jdcloud.com"
    )

    /**
     * 从 CookieManager 中提取登录态。
     * 调用前请确保 WebView 已加载登录页并完成登录。
     */
    fun extract(): LoginState {
        val cookieManager = CookieManager.getInstance()

        val allCookies = buildMap {
            for (domain in DOMAINS) {
                try {
                    val cookie = cookieManager.getCookie(domain)
                    if (!cookie.isNullOrBlank()) {
                        parseCookieString(cookie).forEach { (k, v) -> putIfAbsent(k, v) }
                    }
                } catch (e: Exception) {
                    // Try next domain
                }
            }
        }

        val pin = allCookies["pin"] ?: ""
        val thor = allCookies["thor"] ?: ""

        return LoginState(
            isLoggedIn = thor.isNotBlank(),
            pin = pin,
            thor = thor,
            qidUid = allCookies["qid_uid"] ?: "",
            qidSid = allCookies["qid_sid"] ?: "",
            jdv = allCookies["jdv"] ?: ""
        )
    }

    /**
     * 清空京东云相关的 Cookie。
     */
    fun clear() {
        try {
            val cookieManager = CookieManager.getInstance()
            for (domain in DOMAINS) {
                cookieManager.setCookie(domain, "")
            }
            cookieManager.flush()
        } catch (e: Exception) {
            // Ignore
        }
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
    private const val LOGIN_SUCCESS_URL = "joybuilder-console.jdcloud.com"

    fun isLoginSuccess(url: String): Boolean {
        val hasSuccessDomain = url.contains(LOGIN_SUCCESS_URL)
        val notLoginPage = !url.contains("/login")
        val notAuthPage = !url.contains("/uc/login")
        return hasSuccessDomain && notLoginPage && notAuthPage
    }
}
