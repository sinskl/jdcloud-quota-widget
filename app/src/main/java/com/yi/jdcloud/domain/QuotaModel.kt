package com.yi.jdcloud.domain

data class QuotaInfo(
    val planId: String,
    val planName: String,
    val planType: String,
    val defaultModel: String,
    val endTime: String,
    val h5Limit: Int,
    val h5Used: Int,
    val d7Limit: Int,
    val d7Used: Int,
    val monthLimit: Int,
    val monthUsed: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val h5Percent: Float get() = (h5Used.toFloat() / h5Limit).coerceIn(0f, 1f)
    val d7Percent: Float get() = (d7Used.toFloat() / d7Limit).coerceIn(0f, 1f)
    val monthPercent: Float get() = (monthUsed.toFloat() / monthLimit).coerceIn(0f, 1f)
}

data class LoginState(
    val isLoggedIn: Boolean = false,
    val pin: String = "",
    val thor: String = "",
    val qidUid: String = "",
    val qidSid: String = "",
    val jdv: String = ""
) {
    fun toCookieString(): String =
        "pin=$pin; thor=$thor; qid_uid=$qidUid; qid_sid=$qidSid; jdv=$jdv"

    fun isComplete(): Boolean =
        pin.isNotBlank() && thor.isNotBlank() &&
        qidUid.isNotBlank() && qidSid.isNotBlank() && jdv.isNotBlank()
}
