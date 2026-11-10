package com.security.chat.multiplatform.features.mesh.ui.screens.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshLog
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.logs_title
import kotlin.time.Instant

private val logTimeFormat = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

@Composable
public fun LogsScreen(onBackClicked: () -> Unit) {
    val logManager = koinInject<LogManager>()
    val logsFlow = remember(logManager) { logManager.getLogsFlow() }
    val logs by logsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    LogsScreenContent(
        logs = logs,
        timeZone = TimeZone.currentSystemDefault(),
        onBackClicked = onBackClicked,
    )
}

@Preview
@Composable
internal fun LogsScreenPreview() {
    AppTheme {
        LogsScreenContent(logs = emptyList(), timeZone = TimeZone.UTC, onBackClicked = {})
    }
}

@Preview
@Composable
internal fun LogsScreenWithEntriesPreview() {
    AppTheme {
        LogsScreenContent(
            logs = listOf(
                MeshLog(
                    id = 2,
                    recordedAtMillis = Instant.parse("2026-11-10T12:30:02.301Z")
                        .toEpochMilliseconds(),
                    message = "Mesh BLE peer discovered (RSSI: -62 dBm)",
                ),
                MeshLog(
                    id = 1,
                    recordedAtMillis = Instant.parse("2026-11-10T12:30:00.125Z")
                        .toEpochMilliseconds(),
                    message = "Android mesh BLE advertising started",
                ),
            ),
            timeZone = TimeZone.UTC,
            onBackClicked = {},
        )
    }
}

@Composable
private fun LogsScreenContent(
    logs: List<MeshLog>,
    timeZone: TimeZone,
    onBackClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundPrimary)
            .systemBarsPadding(),
    ) {
        ToolbarComponent(
            modifier = Modifier.fillMaxWidth(),
            startContent = SideContent.Button(
                icon = DrawableRes.ic_back,
                onClicked = onBackClicked,
            ),
            centerContent = CenterContent.Title(text = stringResource(StringRes.logs_title)),
            endContent = null,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(logs, key = { it.id }) { log ->
                val timestamp = remember(log.recordedAtMillis, timeZone) {
                    val localTime = Instant.fromEpochMilliseconds(log.recordedAtMillis)
                        .toLocalDateTime(timeZone)
                    logTimeFormat.format(localTime)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = timestamp,
                        color = AppTheme.colors.textSecondary,
                        style = AppTheme.typography.annotation,
                    )
                    Text(
                        text = log.message,
                        color = AppTheme.colors.textPrimary,
                        style = AppTheme.typography.body,
                    )
                }
            }
        }
    }
}
