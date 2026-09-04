package io.sakurasou.renkei.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import io.sakurasou.renkei.R
import io.sakurasou.renkei.consumer.EventDatabaseListener
import io.sakurasou.renkei.module.IoDispatcher
import io.sakurasou.renkei.module.dao.SMSEventDAO
import io.sakurasou.renkei.module.entity.SMSEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var smsEventDao: SMSEventDAO

    @Inject
    lateinit var eventDatabaseListener: EventDatabaseListener

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val sender = messages.first().displayOriginatingAddress ?: "未知号码"
        val receivedAt = System.currentTimeMillis()

        persistSms(sender, body, receivedAt)
        showDebugNotification(context, sender, body, receivedAt)
    }

    private fun persistSms(
        sender: String,
        body: String,
        receivedAt: Long,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            try {
                runCatching {
                    smsEventDao.saveSMSEvent(
                        SMSEvent(
                            number = sender,
                            message = body,
                            receivedAt = receivedAt,
                        ),
                    )
                }.onSuccess {
                    eventDatabaseListener.notifyNewEvent()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to persist received SMS", error)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showDebugNotification(
        context: Context,
        sender: String,
        body: String,
        receivedAt: Long,
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
                "短信接收调试",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "第一阶段用于确认 RenKei 已收到系统短信广播"
            },
        )

        val notification =
            NotificationCompat
                .Builder(context, DEBUG_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("RenKei 收到来自 $sender 的短信")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context).notify(receivedAt.hashCode(), notification)
    }

    private companion object {
        const val TAG = "SmsBroadcastReceiver"
        const val DEBUG_CHANNEL_ID = "sms_receipt_debug"
    }
}
