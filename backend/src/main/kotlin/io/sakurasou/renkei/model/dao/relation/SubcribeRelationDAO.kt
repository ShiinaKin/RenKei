package io.sakurasou.renkei.model.dao.relation

import io.sakurasou.renkei.model.entity.device.DevicePlatform

/**
 * @author Shiina Kin
 * 2026/9/2 02:22
 */
interface SubcribeRelationDAO {
    suspend fun subscribe(
        providerDeviceID: String,
        subcriberDeviceID: String,
    ): Boolean

    suspend fun unsubscribe(
        providerDeviceID: String,
        subcriberDeviceID: String,
    ): Boolean

    suspend fun getSubcribers(providerDeviceID: String): List<Pair<String, DevicePlatform>>

    suspend fun isSubscribed(
        providerDeviceID: String,
        subcriberDeviceID: String,
    ): Boolean
}
