@file:Suppress("ktlint:standard:function-naming")

package io.sakurasou.renkei.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.sakurasou.renkei.sms.SmsReceipt
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    latestReceipt: SmsReceipt?,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SmsReceiptCard(latestReceipt)
        }
    }
}

@Composable
private fun SmsReceiptCard(latestReceipt: SmsReceipt?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("短信接收测试", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(latestReceipt.description())
        }
    }
}

private fun SmsReceipt?.description(): String =
    if (this == null) {
        "尚未收到测试短信。授权后，请用另一台设备给这台 Android 手机发一条普通 SMS。"
    } else {
        val time = DateFormat.getDateTimeInstance().format(Date(receivedAtEpochMillis))
        "最近在 $time 收到一条短信。正文：$content"
    }
