package com.yi.jdcloud.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.cornerRadius
import androidx.compose.ui.graphics.Color
import com.yi.jdcloud.MainActivity
import com.yi.jdcloud.data.Preferences
import com.yi.jdcloud.domain.QuotaInfo as QuotaModel
import kotlinx.coroutines.flow.first
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yi.jdcloud.worker.QuotaRefreshWorker

class QuotaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val quota = try {
            Preferences(context).quotaInfo.first()
        } catch (e: Exception) {
            null
        }

        provideContent {
            GlanceTheme {
                WidgetContent(quota)
            }
        }
    }

    @Composable
    private fun WidgetContent(quota: QuotaModel?) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "京东云额度",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFE2231A)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = quota?.defaultModel ?: "—",
                        style = TextStyle(
                            color = ColorProvider(Color.Gray),
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(GlanceModifier.height(8.dp))

                if (quota == null) {
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = "点击登录",
                        style = TextStyle(
                            color = ColorProvider(Color.Gray),
                            fontSize = 12.sp
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                    Spacer(GlanceModifier.defaultWeight())
                } else {
                    QuotaRow("5小时", quota.h5Used, quota.h5Limit, quota.h5Percent)
                    Spacer(GlanceModifier.height(6.dp))
                    QuotaRow("7天", quota.d7Used, quota.d7Limit, quota.d7Percent)
                    Spacer(GlanceModifier.height(6.dp))
                    QuotaRow("本月", quota.monthUsed, quota.monthLimit, quota.monthPercent)
                }
            }
        }
    }

    @Composable
    private fun QuotaRow(label: String, used: Int, limit: Int, pct: Float) {
        val barColor = when {
            pct > 0.8f -> Color(0xFFE2231A)
            pct > 0.5f -> Color(0xFFFF9800)
            else -> Color(0xFF4CAF50)
        }

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        color = ColorProvider(Color.DarkGray),
                        fontSize = 11.sp
                    )
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = "$used / $limit",
                    style = TextStyle(
                        color = ColorProvider(Color.Black),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(GlanceModifier.height(3.dp))

            // Progress bar background
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFFEEEEEE))
            )

            // Progress bar fill — use a text-based approach since Glance Box doesn't support fillMaxWidth(fraction)
            val filledWidth = (pct.coerceIn(0f, 1f) * 100).toInt()
            if (filledWidth > 0) {
                Spacer(GlanceModifier.height((-4).dp))
                Box(
                    modifier = GlanceModifier
                        .width(filledWidth.dp)
                        .height(4.dp)
                        .background(barColor)
                ) {}
            }
        }
    }
}

class QuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuotaWidget()
}

object WidgetScheduler {
    fun schedule(context: Context, intervalHours: Int) {
        val workRequest = PeriodicWorkRequestBuilder<QuotaRefreshWorker>(
            intervalHours.toLong(),
            java.util.concurrent.TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            QuotaRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(QuotaRefreshWorker.WORK_NAME)
    }
}
