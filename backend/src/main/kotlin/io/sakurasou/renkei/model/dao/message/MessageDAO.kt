package io.sakurasou.renkei.model.dao.message

import io.sakurasou.renkei.model.entity.message.Message

/**
 * @author Shiina Kin
 * 2026/9/2 00:30
 */
interface MessageDAO {
    suspend fun saveMessage(message: Message): Long

    suspend fun getMessageById(id: Long): Message?
}
