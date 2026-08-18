package io.sakurasou.renkei.data

import io.sakurasou.renkei.module.dao.SMSEventDAO
import io.sakurasou.renkei.sms.SmsReceipt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepository
@Inject
constructor(
    private val smsEventDao: SMSEventDAO,
) {
    fun observeLatest(): Flow<SmsReceipt?> =
        smsEventDao.observeLatest().map { event ->
            event?.let {
                SmsReceipt(
                    receivedAtEpochMillis = it.receivedAt,
                    content = it.message,
                )
            }
        }
}
