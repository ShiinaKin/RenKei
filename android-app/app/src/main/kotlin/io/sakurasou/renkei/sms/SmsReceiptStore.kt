package io.sakurasou.renkei.sms

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

data class SmsReceipt(
    val receivedAtEpochMillis: Long,
    val content: String,
)

object SmsReceiptStore {
    private const val PREFERENCES_NAME = "sms_receipt"
    private const val KEY_RECEIVED_AT = "received_at"
    private const val KEY_CONTENT = "content"

    fun record(
        context: Context,
        receipt: SmsReceipt,
    ) {
        preferences(context)
            .edit {
                putLong(KEY_RECEIVED_AT, receipt.receivedAtEpochMillis)
                    .putString(KEY_CONTENT, receipt.content)
            }
    }

    fun latest(context: Context): SmsReceipt? {
        val preferences = preferences(context)
        val receivedAt = preferences.getLong(KEY_RECEIVED_AT, 0L)
        if (receivedAt == 0L) return null

        return SmsReceipt(
            receivedAtEpochMillis = receivedAt,
            content = preferences.getString(KEY_CONTENT, "")!!,
        )
    }

    fun listen(
        context: Context,
        onChanged: (SmsReceipt?) -> Unit,
    ): () -> Unit {
        val preferences = preferences(context)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_RECEIVED_AT || key == KEY_CONTENT) {
                    onChanged(latest(context))
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun preferences(context: Context): SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
