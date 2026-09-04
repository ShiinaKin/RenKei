package io.sakurasou.renkei.consumer

import android.util.Log
import io.sakurasou.renkei.module.IoDispatcher
import io.sakurasou.renkei.module.dao.IncomingCallEventDAO
import io.sakurasou.renkei.module.dao.SMSEventDAO
import io.sakurasou.renkei.module.entity.BaseEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class EventDatabaseListener
@Inject
constructor(
    private val smsEventDAO: SMSEventDAO,
    private val incomingCallEventDAO: IncomingCallEventDAO,
    private val eventConsumer: EventConsumer,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val trigger = Channel<Unit>(Channel.CONFLATED)
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        trigger.trySend(Unit)
        scope.launch { listen() }
    }

    fun notifyNewEvent() {
        trigger.trySend(Unit)
    }

    private suspend fun listen() {
        while (currentCoroutineContext().isActive) {
            withTimeoutOrNull(POLL_INTERVAL_MILLIS) { trigger.receive() }
            pollDatabases()
        }
    }

    private suspend fun pollDatabases() {
        runCatching { consumeSmsEvents() }
            .onFailure { error -> Log.e(TAG, "Failed to poll SMS events", error) }
        runCatching { consumeIncomingCallEvents() }
            .onFailure { error -> Log.e(TAG, "Failed to poll incoming call events", error) }
    }

    private suspend fun consumeSmsEvents() {
        smsEventDAO.findPending().forEach { event ->
            smsEventDAO.updateSMSEventStatus(event.id, BaseEvent.Status.SENDING)
            runCatching { eventConsumer.onSMSEvent(event) }
                .onSuccess {
                    smsEventDAO.updateSMSEventStatus(event.id, BaseEvent.Status.SENT)
                }.onFailure { error ->
                    smsEventDAO.updateSMSEventStatus(event.id, BaseEvent.Status.FAILED)
                    Log.e(TAG, "Failed to consume SMS event ${event.id}", error)
                }
        }
    }

    private suspend fun consumeIncomingCallEvents() {
        incomingCallEventDAO.findPending().forEach { event ->
            incomingCallEventDAO.updateStatus(event.id, BaseEvent.Status.SENDING)
            runCatching { eventConsumer.onIncomingCallEvent(event) }
                .onSuccess {
                    incomingCallEventDAO.updateStatus(event.id, BaseEvent.Status.SENT)
                }.onFailure { error ->
                    incomingCallEventDAO.updateStatus(event.id, BaseEvent.Status.FAILED)
                    Log.e(TAG, "Failed to consume incoming call event ${event.id}", error)
                }
        }
    }

    private companion object {
        const val TAG = "EventDatabaseListener"
        const val POLL_INTERVAL_MILLIS = 30_000L
    }
}
