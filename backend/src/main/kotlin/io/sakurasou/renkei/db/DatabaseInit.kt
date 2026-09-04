package io.sakurasou.renkei.db

import io.sakurasou.renkei.model.dao.device.Devices
import io.sakurasou.renkei.model.dao.message.Messages
import io.sakurasou.renkei.model.dao.message.MessageAccessTokens
import io.sakurasou.renkei.model.dao.notification.NotificationTargets
import io.sakurasou.renkei.model.dao.relation.SubcribeRelation
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

/**
 * @author Shiina Kin
 * 2026/9/2 00:11
 */
object DatabaseInit {
    fun init(database: Database) {
        transaction(database) {
            MigrationUtils
                .statementsRequiredForDatabaseMigration(
                    Devices,
                    Messages,
                    MessageAccessTokens,
                    SubcribeRelation,
                    NotificationTargets,
                ).forEach { statement -> exec(statement) }
        }
    }
}
