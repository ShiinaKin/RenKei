package io.sakurasou.renkei.module.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import io.sakurasou.renkei.common.getUTCTimestamp
import io.sakurasou.renkei.module.entity.BaseEvent
import io.sakurasou.renkei.module.entity.IncomingCallEvent
import kotlinx.coroutines.flow.Flow

/**
 * @author Shiina Kin
 * 2026/8/18 21:58
 */
@Dao
interface IncomingCallEventDAO {
    @Query("SELECT * FROM incoming_call_event WHERE status != 'SENT' ORDER BY created_time, id")
    suspend fun findPending(): List<IncomingCallEvent>

    @Query("SELECT * FROM incoming_call_event ORDER BY created_time DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<IncomingCallEvent?>

    @Query(
        """
        UPDATE incoming_call_event
        SET status = :status, updated_time = :updatedTime
        WHERE id = :id
        """,
    )
    suspend fun updateStatus(
        id: Long,
        status: BaseEvent.Status,
        updatedTime: Long = getUTCTimestamp(),
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun save(event: IncomingCallEvent): Long
}
