package io.sakurasou.renkei.module.entity

import kotlinx.serialization.Serializable

/**
 * @author Shiina Kin
 * 2026/8/18 21:22
 */
@Serializable
sealed interface BaseEvent {
    val id: Long
    val status: Status
    val createdTime: Long
    val updatedTime: Long?

    enum class Status {
        SENDING,
        SENT,
        WAITING,
        FAILED,
    }
}
