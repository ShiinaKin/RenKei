package io.sakurasou.renkei

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import io.sakurasou.renkei.consumer.EventDatabaseListener
import io.sakurasou.renkei.crypto.CryptoKeyRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class RenKeiApplication : Application() {
    @Inject
    lateinit var cryptoKeyRepository: CryptoKeyRepository

    @Inject
    lateinit var eventDatabaseListener: EventDatabaseListener

    @Suppress("InjectDispatcher")
    private val keyInitializationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        keyInitializationScope.launch {
            runCatching { cryptoKeyRepository.generateKeyPair() }
                .onFailure { error -> Log.e(TAG, "Unable to initialize crypto key pair", error) }
            eventDatabaseListener.start()
        }
    }

    private companion object {
        const val TAG = "RenKeiApplication"
    }
}
