package io.sakurasou.renkei.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sakurasou.renkei.call.IncomingCallReceipt
import io.sakurasou.renkei.call.IncomingCallReceiptStore
import io.sakurasou.renkei.sms.SmsReceipt
import io.sakurasou.renkei.sms.SmsReceiptStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

@Singleton
class ReceiptRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val latestSms: Flow<SmsReceipt?> =
            callbackFlow {
                trySend(SmsReceiptStore.latest(context))
                val closeListener = SmsReceiptStore.listen(context) { trySend(it) }
                awaitClose(closeListener)
            }.conflate()

        val latestIncomingCall: Flow<IncomingCallReceipt?> =
            callbackFlow {
                trySend(IncomingCallReceiptStore.latest(context))
                val closeListener = IncomingCallReceiptStore.listen(context) { trySend(it) }
                awaitClose(closeListener)
            }.conflate()
    }
