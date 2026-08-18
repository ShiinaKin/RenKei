package io.sakurasou.renkei.module.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import io.sakurasou.renkei.common.getUTCTimestamp
import io.sakurasou.renkei.module.entity.BaseEvent
import io.sakurasou.renkei.module.entity.SMSEvent
import kotlinx.coroutines.flow.Flow

/**
 * @author Shiina Kin
 * 2026/8/18 21:58
 */
@Dao
interface SMSEventDAO {
    @Query("SELECT * FROM sms_event ORDER BY created_time DESC, id DESC LIMIT 1")
    suspend fun findLatest(): SMSEvent?

    @Query("SELECT * FROM sms_event ORDER BY created_time DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<SMSEvent?>

    @Query(
        """
        SELECT * FROM sms_event
        WHERE number = :number
        ORDER BY created_time DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun findSMSByPhoneNumber(number: String): SMSEvent?

    @Query("SELECT * FROM sms_event WHERE id = :id")
    suspend fun findSMSByID(id: Long): SMSEvent?

    @Query(
        """
        UPDATE sms_event
        SET status = :status, updated_time = :updatedTime
        WHERE id = :id
        """,
    )
    suspend fun updateSMSEventStatus(
        id: Long,
        status: BaseEvent.Status,
        updatedTime: Long = getUTCTimestamp(),
    ): Int

    @Update
    suspend fun updateSMSEvent(event: SMSEvent): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun saveSMSEvent(event: SMSEvent): Long

    @Query("DELETE FROM sms_event WHERE id = :id")
    suspend fun deleteSMSEventByID(id: Long): Int
}
