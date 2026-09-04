package io.sakurasou.renkei.model.dao.device

import io.sakurasou.renkei.model.entity.device.Device

/**
 * @author Shiina Kin
 * 2026/9/2 00:29
 */
interface DeviceDAO {
    suspend fun saveDevice(device: Device): Long

    suspend fun deleteDeviceByUniqueID(uniqueID: String): Boolean

    suspend fun getDeviceByUniqueID(uniqueID: String): Device?

    suspend fun updatePublicKey(
        uniqueID: String,
        publicKey: String,
    ): Boolean
}
