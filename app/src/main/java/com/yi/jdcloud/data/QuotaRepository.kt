package com.yi.jdcloud.data

import com.yi.jdcloud.domain.LoginState
import com.yi.jdcloud.domain.QuotaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuotaRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val preferences: Preferences
) {
    companion object {
        private const val BASE_URL = "https://joybuilder-console.jdcloud.com"
        private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun fetchQuota(loginState: LoginState): Result<QuotaInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/openApi/modelservice/describeUserActivePlan?_t=${System.currentTimeMillis()}"
            val requestBody = "{}".toRequestBody(MEDIA_TYPE_JSON)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("cookie", loginState.toCookieString())
                .addHeader(
                    "user-agent",
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

            val apiResponse: ApiResponse<QuotaResponse> = try {
                com.google.gson.Gson().fromJson(body, ApiResponse::class.java) as ApiResponse<QuotaResponse>
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Parse error: $body"))
            }

            if (apiResponse.error != null) {
                return@withContext Result.failure(Exception(apiResponse.error.message))
            }

            val result = apiResponse.result ?: return@withContext Result.failure(Exception("No result"))

            // Parse limits from limits list (or use defaults)
            val limits = result.limits ?: emptyList()
            val usages = result.usages ?: emptyList()

            fun getLimit(period: String): Int = limits.find { it.period == period }?.limitValue ?: 0
            fun getUsage(type: String): Int = usages.find { it.type == type }?.count ?: 0

            val quota = QuotaInfo(
                planId = result.planId,
                planName = result.name,
                planType = result.planType,
                defaultModel = result.defaultModel,
                endTime = result.endTime,
                h5Limit = getLimit("5hours"),
                h5Used = getUsage("5hours"),
                d7Limit = getLimit("7days"),
                d7Used = getUsage("7days"),
                monthLimit = getLimit("month"),
                monthUsed = getUsage("month")
            )

            preferences.saveQuotaInfo(quota)
            Result.success(quota)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
