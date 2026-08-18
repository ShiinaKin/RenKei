package io.sakurasou.renkei.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sakurasou.renkei.call.IncomingCallReceipt
import io.sakurasou.renkei.data.IncomingCallRepository
import io.sakurasou.renkei.data.SmsRepository
import io.sakurasou.renkei.sms.SmsReceipt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val latestSms: SmsReceipt? = null,
    val latestIncomingCall: IncomingCallReceipt? = null,
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    smsRepository: SmsRepository,
    incomingCallRepository: IncomingCallRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(
            smsRepository.observeLatest(),
            incomingCallRepository.observeLatest(),
        ) { latestSms, latestIncomingCall ->
            HomeUiState(
                latestSms = latestSms,
                latestIncomingCall = latestIncomingCall,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
