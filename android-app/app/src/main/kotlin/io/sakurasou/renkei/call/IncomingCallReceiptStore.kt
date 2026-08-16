package io.sakurasou.renkei.call

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

data class IncomingCallReceipt(
    val receivedAtEpochMillis: Long,
    val callerNumber: String,
)

object IncomingCallReceiptStore {
    private const val PREFERENCES_NAME = "incoming_call_receipt"
    private const val KEY_RECEIVED_AT = "received_at"
    private const val KEY_CALLER_NUMBER = "caller_number"

    fun record(
        context: Context,
        receipt: IncomingCallReceipt,
    ) {
        preferences(context).edit {
            putLong(KEY_RECEIVED_AT, receipt.receivedAtEpochMillis)
            putString(KEY_CALLER_NUMBER, receipt.callerNumber)
        }
    }

    fun latest(context: Context): IncomingCallReceipt? {
        val preferences = preferences(context)
        val receivedAt = preferences.getLong(KEY_RECEIVED_AT, 0L)
        if (receivedAt == 0L) return null

        return IncomingCallReceipt(
            receivedAtEpochMillis = receivedAt,
            callerNumber = preferences.getString(KEY_CALLER_NUMBER, UNKNOWN_CALLER) ?: UNKNOWN_CALLER,
        )
    }

    fun listen(
        context: Context,
        onChanged: (IncomingCallReceipt?) -> Unit,
    ): () -> Unit {
        val preferences = preferences(context)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_RECEIVED_AT || key == KEY_CALLER_NUMBER) {
                    onChanged(latest(context))
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    const val UNKNOWN_CALLER = "未知号码"
}
