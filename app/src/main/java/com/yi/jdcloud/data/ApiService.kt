package com.yi.jdcloud.data

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

interface JdCloudApiService {

    @FormUrlEncoded
    @POST("openApi/modelservice/describeUserActivePlan")
    suspend fun getUserActivePlan(
        @Query("_t") timestamp: String,
        @Field("") body: String = "{}"
    ): ApiResponse<QuotaResponse>
}
