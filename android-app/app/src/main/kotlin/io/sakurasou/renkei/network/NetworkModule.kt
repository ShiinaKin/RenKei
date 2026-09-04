package io.sakurasou.renkei.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/**
 * @author Shiina Kin
 * 2026/8/16 15:43
 */

val JSON =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .build()
}
