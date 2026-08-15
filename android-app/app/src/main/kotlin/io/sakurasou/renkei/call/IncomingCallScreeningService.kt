package io.sakurasou.renkei.call

import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * 目前只为申请系统的“来电识别和骚扰拦截”角色提供入口。
 * 来电事件上传会在后续步骤接入；此服务始终允许电话正常响铃。
 */
class IncomingCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .build(),
        )
    }
}
