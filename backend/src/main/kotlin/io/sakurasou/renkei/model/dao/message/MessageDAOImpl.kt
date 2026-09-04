package io.sakurasou.renkei.model.dao.message

import io.sakurasou.renkei.db.DatabaseSingleton
import io.sakurasou.renkei.model.dao.device.Devices
import io.sakurasou.renkei.model.entity.message.Message
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll

class MessageDAOImpl : MessageDAO {
    override suspend fun saveMessage(message: Message): Long =
        DatabaseSingleton.dbQuery {
            val deviceID =
                Devices
                    .selectAll()
                    .where { Devices.uniqueID eq message.deviceUniqueID }
                    .singleOrNull()
                    ?.get(Devices.id)
                    ?: error("Unknown device: ${message.deviceUniqueID}")

            Messages
                .insertAndGetId {
                    it[Messages.deviceID] = deviceID
                    it[title] = message.title
                    it[previewContent] = message.previewContent
                    it[content] = message.content
                    it[timestamp] = message.timestamp
                }.value
        }

    override suspend fun getMessageById(id: Long): Message? =
        DatabaseSingleton.dbQuery {
            Messages
                .innerJoin(Devices)
                .selectAll()
                .where { Messages.id eq id }
                .singleOrNull()
                ?.let { row ->
                    Message(
                        id = row[Messages.id].value,
                        deviceUniqueID = row[Devices.uniqueID],
                        title = row[Messages.title],
                        previewContent = row[Messages.previewContent],
                        content = row[Messages.content],
                        timestamp = row[Messages.timestamp],
                    )
                }
        }
}
