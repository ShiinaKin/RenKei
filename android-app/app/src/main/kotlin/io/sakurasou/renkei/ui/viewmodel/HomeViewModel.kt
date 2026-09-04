package io.sakurasou.renkei.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sakurasou.renkei.call.IncomingCallReceipt
import io.sakurasou.renkei.consumer.EventConsumer
import io.sakurasou.renkei.data.IncomingCallRepository
import io.sakurasou.renkei.data.SmsRepository
import io.sakurasou.renkei.sms.SmsReceipt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val latestSms: SmsReceipt? = null,
    val latestIncomingCall: IncomingCallReceipt? = null,
    val isSendingSimulatedNotification: Boolean = false,
    val simulatedNotificationMessage: String? = null,
    val isSimulatedNotificationError: Boolean = false,
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    smsRepository: SmsRepository,
    incomingCallRepository: IncomingCallRepository,
    private val eventConsumer: EventConsumer,
) : ViewModel() {
    private val simulatedNotificationState = MutableStateFlow(SimulatedNotificationState())

    val uiState: StateFlow<HomeUiState> =
        combine(
            smsRepository.observeLatest(),
            incomingCallRepository.observeLatest(),
            simulatedNotificationState,
        ) { latestSms, latestIncomingCall, simulatedNotification ->
            HomeUiState(
                latestSms = latestSms,
                latestIncomingCall = latestIncomingCall,
                isSendingSimulatedNotification = simulatedNotification.isSending,
                simulatedNotificationMessage = simulatedNotification.message,
                isSimulatedNotificationError = simulatedNotification.isError,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState(),
        )

    fun sendSimulatedNotification() {
        if (simulatedNotificationState.value.isSending) return
        viewModelScope.launch {
            simulatedNotificationState.update {
                SimulatedNotificationState(isSending = true)
            }
            runCatching { eventConsumer.sendSimulatedNotification() }
                .onSuccess {
                    simulatedNotificationState.update {
                        SimulatedNotificationState(message = "模拟通知已发送，请检查 Bark")
                    }
                }.onFailure { error ->
                    simulatedNotificationState.update {
                        SimulatedNotificationState(
                            message = error.message ?: "模拟通知发送失败",
                            isError = true,
                        )
                    }
                }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private data class SimulatedNotificationState(
    val isSending: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)
