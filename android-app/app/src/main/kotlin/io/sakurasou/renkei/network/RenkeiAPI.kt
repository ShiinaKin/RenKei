package io.sakurasou.renkei.network

import io.sakurasou.renkei.module.entity.IncomingCallEvent
import io.sakurasou.renkei.module.entity.SMSEvent

/**
 * @author Shiina Kin
 * 2026/8/16 15:56
 */
interface RenkeiAPI {
    suspend fun sendSmsEvent(request: SMSEvent)

    suspend fun sendIncomingCallEvent(request: IncomingCallEvent)
}
