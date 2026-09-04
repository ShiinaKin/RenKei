package io.sakurasou.renkei.model.dao.relation

import io.sakurasou.renkei.db.DatabaseSingleton
import io.sakurasou.renkei.model.dao.device.Devices
import io.sakurasou.renkei.model.entity.device.DevicePlatform
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class SubcribeRelationDAOImpl : SubcribeRelationDAO {
    override suspend fun subscribe(
        providerDeviceID: String,
        subcriberDeviceID: String,
    ): Boolean =
        DatabaseSingleton.dbQuery {
            val exists =
                SubcribeRelation
                    .selectAll()
                    .where {
                        (SubcribeRelation.providerDeviceID eq providerDeviceID) and
                            (SubcribeRelation.subcriberDeviceID eq subcriberDeviceID)
                    }.limit(1)
                    .any()
            if (exists) return@dbQuery false

            SubcribeRelation
                .insert {
                    it[SubcribeRelation.providerDeviceID] = providerDeviceID
                    it[SubcribeRelation.subcriberDeviceID] = subcriberDeviceID
                }.insertedCount != 0
        }

    override suspend fun unsubscribe(
        providerDeviceID: String,
        subcriberDeviceID: String,
    ): Boolean =
        DatabaseSingleton.dbQuery {
            SubcribeRelation
                .deleteWhere {
                    SubcribeRelation.providerDeviceID eq providerDeviceID and
                        (SubcribeRelation.subcriberDeviceID eq subcriberDeviceID)
                } != 0
        }

    override suspend fun getSubcribers(providerDeviceID: String): List<Pair<String, DevicePlatform>> =
        DatabaseSingleton.dbQuery {
            SubcribeRelation
                .join(
                    Devices,
                    joinType = JoinType.INNER,
                    onColumn = SubcribeRelation.subcriberDeviceID,
                    otherColumn = Devices.uniqueID,
                ).select(SubcribeRelation.subcriberDeviceID, Devices.platform)
                .where { SubcribeRelation.providerDeviceID eq providerDeviceID }
                .map { it[SubcribeRelation.subcriberDeviceID] to DevicePlatform.valueOf(it[Devices.platform]) }
        }

    override suspend fun isSubscribed(
        providerDeviceID: String,
        subcriberDeviceID: String,
    ): Boolean =
        DatabaseSingleton.dbQuery {
            SubcribeRelation
                .selectAll()
                .where {
                    (SubcribeRelation.providerDeviceID eq providerDeviceID) and
                        (SubcribeRelation.subcriberDeviceID eq subcriberDeviceID)
                }.limit(1)
                .any()
        }
}
