package io.sakurasou.renkei.model.dao.relation

import io.sakurasou.renkei.model.dao.device.Devices
import org.jetbrains.exposed.v1.core.Table

/**
 * @author Shiina Kin
 * 2026/9/2 02:23
 */
object SubcribeRelation : Table() {
    val providerDeviceID = varchar("provider_device_id", 255)
    val subcriberDeviceID = varchar("subcriber_device_id", 255)

    override val primaryKey = PrimaryKey(providerDeviceID, subcriberDeviceID)

    init {
        foreignKey(providerDeviceID to Devices.uniqueID)
        foreignKey(subcriberDeviceID to Devices.uniqueID)
    }
}
