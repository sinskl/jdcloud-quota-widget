package com.yi.jdcloud.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yi.jdcloud.data.Preferences
import com.yi.jdcloud.data.QuotaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class QuotaRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val quotaRepository: QuotaRepository,
    private val preferences: Preferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val loginState = preferences.loginState.first()
        if (!loginState.isLoggedIn || !loginState.isComplete()) {
            return Result.failure()
        }

        return try {
            val result = quotaRepository.fetchQuota(loginState)
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "quota_refresh"
    }
}
