package com.yi.jdcloud.di

import android.content.Context
import com.yi.jdcloud.data.Preferences
import com.yi.jdcloud.data.QuotaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): Preferences {
        return Preferences(context)
    }

    @Provides
    @Singleton
    fun provideQuotaRepository(
        okHttpClient: OkHttpClient,
        preferences: Preferences
    ): QuotaRepository {
        return QuotaRepository(okHttpClient, preferences)
    }
}
