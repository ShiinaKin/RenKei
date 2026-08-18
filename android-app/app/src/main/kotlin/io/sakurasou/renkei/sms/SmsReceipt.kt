package io.sakurasou.renkei.sms

data class SmsReceipt(
    val receivedAtEpochMillis: Long,
    val content: String,
)
