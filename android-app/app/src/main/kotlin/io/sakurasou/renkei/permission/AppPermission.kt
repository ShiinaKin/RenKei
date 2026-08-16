package io.sakurasou.renkei.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

enum class AppPermission(
    val manifestName: String,
    val title: String,
    val description: String,
    val stage: String,
) {
    NOTIFICATIONS(
        manifestName = Manifest.permission.POST_NOTIFICATIONS,
        title = "显示通知",
        description = "显示转发状态、失败提醒，以及后续通话服务的常驻通知。",
        stage = "基础能力",
    ),
    RECEIVE_SMS(
        manifestName = Manifest.permission.RECEIVE_SMS,
        title = "接收短信",
        description = "在新短信到达时接收系统广播；不读取既有短信历史。",
        stage = "短信转发",
    ),
    PHONE_STATE(
        manifestName = Manifest.permission.READ_PHONE_STATE,
        title = "读取电话状态",
        description = "识别响铃、接通和挂断状态，并区分设备上的电话账户。",
        stage = "来电通知",
    ),
    CONTACTS_FOR_CALLS(
        manifestName = Manifest.permission.READ_CONTACTS,
        title = "覆盖联系人来电",
        description = "Android 只有在授予此权限后，才会把通讯录号码交给来电筛选服务；RenKei 不读取或上传通讯录。",
        stage = "来电通知",
    ),
    ;

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, manifestName) == PackageManager.PERMISSION_GRANTED
}

data class PermissionStatus(
    val permission: AppPermission,
    val granted: Boolean,
)

fun permissionStatuses(context: Context): List<PermissionStatus> =
    AppPermission.entries.map { permission ->
        PermissionStatus(permission = permission, granted = permission.isGranted(context))
    }
