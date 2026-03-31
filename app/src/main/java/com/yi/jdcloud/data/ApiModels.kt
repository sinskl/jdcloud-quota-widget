package com.yi.jdcloud.data

import com.google.gson.annotations.SerializedName

// --- API Response Models ---

data class ApiResponse<T>(
    val error: ApiError?,
    val result: T?
)

data class ApiError(
    val code: Int,
    val message: String,
    val status: String?
)

data class QuotaResponse(
    @SerializedName("plan_id") val planId: String,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("plan_type") val planType: String,
    @SerializedName("default_model") val defaultModel: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("limits") val limits: List<LimitItem>?,
    @SerializedName("usages") val usages: List<UsageItem>?
)

data class LimitItem(
    @SerializedName("metric_type") val metricType: String,
    @SerializedName("limit_value") val limitValue: Int,
    @SerializedName("unit") val unit: String,
    @SerializedName("period") val period: String
)

data class UsageItem(
    @SerializedName("type") val type: String,
    @SerializedName("count") val count: Int
)
