package io.sakurasou.renkei.call

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.sakurasou.renkei.R
import io.sakurasou.renkei.consumer.EventDatabaseListener
import io.sakurasou.renkei.module.IoDispatcher
import io.sakurasou.renkei.module.dao.IncomingCallEventDAO
import io.sakurasou.renkei.module.entity.IncomingCallEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IncomingCallScreeningService : CallScreeningService() {
    @Inject
    lateinit var incomingCallEventDAO: IncomingCallEventDAO

    @Inject
    lateinit var eventDatabaseListener: EventDatabaseListener

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        respondToCall(
            callDetails,
            CallResponse
                .Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .build(),
        )

        val callerNumber = callDetails.callerNumber()
        val receivedAt = callDetails.creationTimeMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        val receipt =
            IncomingCallReceipt(
                receivedAtEpochMillis = receivedAt,
                callerNumber = callerNumber,
            )

        IncomingCallReceiptStore.record(applicationContext, receipt)
        persistIncomingCall(receipt)
        showDebugNotification(applicationContext, receipt)
    }

    private fun persistIncomingCall(receipt: IncomingCallReceipt) {
        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            runCatching {
                incomingCallEventDAO.save(
                    IncomingCallEvent(
                        number = receipt.callerNumber,
                        createdTime = receipt.receivedAtEpochMillis,
                    ),
                )
            }.onSuccess {
                eventDatabaseListener.notifyNewEvent()
            }.onFailure { error ->
                Log.e(TAG, "Failed to persist incoming call", error)
            }
        }
    }

    private fun showDebugNotification(
        context: Context,
        receipt: IncomingCallReceipt,
    ) {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                DEBUG_CHANNEL_ID,
                "来电监听调试",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "用于确认 RenKei 已收到系统来电事件"
            },
        )

        val notification =
            NotificationCompat
                .Builder(context, DEBUG_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("RenKei 检测到来电")
                .setContentText(receipt.callerNumber)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(receipt.receivedAtEpochMillis.hashCode(), notification)
    }

    private fun Call.Details.callerNumber(): String {
        if (handlePresentation != TelecomManager.PRESENTATION_ALLOWED) {
            return IncomingCallReceiptStore.UNKNOWN_CALLER
        }

        return handle
            ?.schemeSpecificPart
            ?.takeIf(String::isNotBlank)
            ?: IncomingCallReceiptStore.UNKNOWN_CALLER
    }

    private companion object {
        const val TAG = "IncomingCallService"
        const val DEBUG_CHANNEL_ID = "incoming_call_debug"
    }
}
