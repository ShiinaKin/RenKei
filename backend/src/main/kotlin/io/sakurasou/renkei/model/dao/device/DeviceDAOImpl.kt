package io.sakurasou.renkei.model.dao.device

import io.sakurasou.renkei.db.DatabaseSingleton
import io.sakurasou.renkei.model.entity.device.Device
import io.sakurasou.renkei.model.entity.device.DevicePlatform
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class DeviceDAOImpl : DeviceDAO {
    override suspend fun saveDevice(device: Device): Long =
        DatabaseSingleton.dbQuery {
            Devices
                .insertAndGetId {
                    it[name] = device.name
                    it[uniqueID] = device.uniqueID
                    it[platform] = device.platform.name
                    it[publicKey] = device.publicKey
                }.value
        }

    override suspend fun deleteDeviceByUniqueID(uniqueID: String): Boolean =
        DatabaseSingleton.dbQuery {
            Devices.deleteWhere { Devices.uniqueID eq uniqueID } > 0
        }

    override suspend fun getDeviceByUniqueID(uniqueID: String): Device? =
        DatabaseSingleton.dbQuery {
            Devices
                .selectAll()
                .where { Devices.uniqueID eq uniqueID }
                .singleOrNull()
                ?.let { row ->
                    Device(
                        id = row[Devices.id].value,
                        name = row[Devices.name],
                        uniqueID = row[Devices.uniqueID],
                        platform = DevicePlatform.valueOf(row[Devices.platform]),
                        publicKey = row[Devices.publicKey],
                    )
                }
        }

    override suspend fun updatePublicKey(
        uniqueID: String,
        publicKey: String,
    ): Boolean =
        DatabaseSingleton.dbQuery {
            Devices.update({ Devices.uniqueID eq uniqueID }) {
                it[Devices.publicKey] = publicKey
            } > 0
        }
}
