package io.sakurasou.renkei.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.sakurasou.renkei.R

class SmsBroadcastReceiver : BroadcastReceiver() {
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

        SmsReceiptStore.record(
            context,
            SmsReceipt(
                receivedAtEpochMillis = receivedAt,
                characterCount = body.length,
            ),
        )
        showDebugNotification(context, sender, body, receivedAt)
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
            NotificationCompat.Builder(context, DEBUG_CHANNEL_ID)
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
        const val DEBUG_CHANNEL_ID = "sms_receipt_debug"
    }
}
