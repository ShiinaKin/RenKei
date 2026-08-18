package io.sakurasou.renkei.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sakurasou.renkei.call.IncomingCallReceipt
import io.sakurasou.renkei.call.IncomingCallReceiptStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingCallRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    fun observeLatest(): Flow<IncomingCallReceipt?> =
        callbackFlow {
            trySend(IncomingCallReceiptStore.latest(context))
            val closeListener = IncomingCallReceiptStore.listen(context) { trySend(it) }
            awaitClose(closeListener)
        }.conflate()
}
