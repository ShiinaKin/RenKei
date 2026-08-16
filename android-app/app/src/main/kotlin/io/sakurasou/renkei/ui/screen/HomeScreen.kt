@file:Suppress("ktlint:standard:function-naming")

package io.sakurasou.renkei.ui.screen

import android.annotation.SuppressLint
import android.system.Os.link
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.sakurasou.renkei.R
import io.sakurasou.renkei.sms.SmsReceipt
import java.text.DateFormat
import java.util.Date
import java.util.Properties

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    latestReceipt: SmsReceipt?,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SmsMoniterStatusCard(latestReceipt)
            IncomingCallMoniterStatusCard(latestReceipt)
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline,
            )
            RightAndAbout()
        }
    }
}

@Composable
private fun SmsMoniterStatusCard(latestReceipt: SmsReceipt?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "短信接收测试",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = latestReceipt.description(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun IncomingCallMoniterStatusCard(latestReceipt: SmsReceipt?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "来电监听测试",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = latestReceipt.description(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun RightAndAbout() {
    val context = LocalContext.current
    val buildRecord =
        remember(context) {
            context.resources.openRawResource(R.raw.build_record).bufferedReader().use { reader ->
                val properties = Properties().apply { load(reader) }
                BuildRecord(
                    version = properties.getProperty("version", "unknown"),
                    commitId = properties.getProperty("commitId", "unknown"),
                    buildTime = properties.getProperty("buildTime", "unknown"),
                )
            }
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            text =
                buildAnnotatedString {
                    append("RenKei ${buildRecord.version}")
                    append(" | ")
                    withLink(
                        link =
                            LinkAnnotation.Url(
                                url = "https://github.com/ShiinaKin/RenKei",
                                styles =
                                    TextLinkStyles(
                                        style =
                                            SpanStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                textDecoration = TextDecoration.None,
                                            ),
                                    ),
                            ),
                    ) {
                        append("Github")
                    }
                    append(" | ")
                    append("Shiina Kin")
                },
        )
        Text(
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            text =
                buildAnnotatedString {
                    withLink(
                        link =
                            LinkAnnotation.Url(
                                url = "https://github.com/ShiinaKin/RenKei/commit/${buildRecord.commitId}",
                                styles =
                                    TextLinkStyles(
                                        style =
                                            SpanStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                textDecoration = TextDecoration.None,
                                            ),
                                    ),
                            ),
                    ) {
                        append(buildRecord.commitId)
                    }
                    append(" | ")
                    append(buildRecord.buildTime)
                },
        )
    }
}

private data class BuildRecord(
    val version: String,
    val commitId: String,
    val buildTime: String,
)

private fun SmsReceipt?.description(): String =
    if (this == null) {
        "尚未收到测试短信。授权后，请发送一条测试短信到本机。"
    } else {
        val time = DateFormat.getDateTimeInstance().format(Date(receivedAtEpochMillis))
        "$time\n$content"
    }

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        latestReceipt =
            SmsReceipt(
                receivedAtEpochMillis = System.currentTimeMillis(),
                content = "测试短信内容",
            ),
    )
}
