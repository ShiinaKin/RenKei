package io.sakurasou.renkei.module.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import io.sakurasou.renkei.common.getUTCTimestamp
import kotlinx.serialization.Serializable

/**
 * @author Shiina Kin
 * 2026/8/16 15:32
 */
@Entity(tableName = "incoming_call_event")
@Serializable
data class IncomingCallEvent(
    @ColumnInfo(name = "number")
    val number: String,
    @PrimaryKey(autoGenerate = true)
    override val id: Long = 0,
    override val status: BaseEvent.Status = BaseEvent.Status.WAITING,
    @ColumnInfo(name = "created_time")
    override val createdTime: Long = getUTCTimestamp(),
    @ColumnInfo(name = "updated_time")
    override val updatedTime: Long? = null,
) : BaseEvent
