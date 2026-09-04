package io.sakurasou.renkei.data

import io.sakurasou.renkei.call.IncomingCallReceipt
import io.sakurasou.renkei.module.dao.IncomingCallEventDAO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingCallRepository
@Inject
constructor(
    private val incomingCallEventDAO: IncomingCallEventDAO,
) {
    fun observeLatest(): Flow<IncomingCallReceipt?> =
        incomingCallEventDAO.observeLatest().map { event ->
            event?.let {
                IncomingCallReceipt(
                    receivedAtEpochMillis = it.createdTime,
                    callerNumber = it.number,
                )
            }
        }
}
