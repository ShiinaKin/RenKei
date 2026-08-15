package io.sakurasou.renkei.sms

import android.content.Context
import android.content.SharedPreferences

data class SmsReceipt(
    val receivedAtEpochMillis: Long,
    val characterCount: Int,
)

/**
 * 第一阶段只持久化验收所需的元数据，不保存短信发送者或正文。
 */
object SmsReceiptStore {
    private const val PREFERENCES_NAME = "sms_receipt"
    private const val KEY_RECEIVED_AT = "received_at"
    private const val KEY_CHARACTER_COUNT = "character_count"

    fun record(
        context: Context,
        receipt: SmsReceipt,
    ) {
        preferences(context)
            .edit()
            .putLong(KEY_RECEIVED_AT, receipt.receivedAtEpochMillis)
            .putInt(KEY_CHARACTER_COUNT, receipt.characterCount)
            .apply()
    }

    fun latest(context: Context): SmsReceipt? {
        val preferences = preferences(context)
        val receivedAt = preferences.getLong(KEY_RECEIVED_AT, 0L)
        if (receivedAt == 0L) return null

        return SmsReceipt(
            receivedAtEpochMillis = receivedAt,
            characterCount = preferences.getInt(KEY_CHARACTER_COUNT, 0),
        )
    }

    fun listen(
        context: Context,
        onChanged: (SmsReceipt?) -> Unit,
    ): () -> Unit {
        val preferences = preferences(context)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_RECEIVED_AT || key == KEY_CHARACTER_COUNT) {
                    onChanged(latest(context))
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
