package app.urlcleaner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.urlcleaner.R
import app.urlcleaner.util.ShareActions
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(
                        onClick = onOpenHistory,
                        enabled = settings?.historyEnabled == true,
                    ) {
                        Icon(Icons.Default.History, contentDescription = stringResource(R.string.history))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.paste_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.paste_box_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    val original = lastResult?.original
                    SelectionContainer {
                        Text(
                            text = original?.takeIf { it.isNotEmpty() }
                                ?: stringResource(R.string.paste_box_placeholder),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val text = clipboard.getText()?.text.orEmpty()
                            if (text.isNotBlank()) viewModel.cleanPasted(text)
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.paste_button))
                        }
                        if (lastResult != null) {
                            OutlinedButton(onClick = { viewModel.clearLastResult() }) {
                                Text(stringResource(R.string.clear))
                            }
                        }
                    }
                }
            }

            lastResult?.let { result ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val status: String = when {
                            result.isBlocked -> stringResource(R.string.status_blocked)
                            !result.wasChanged -> stringResource(R.string.status_no_change)
                            else -> stringResource(R.string.status_cleaned, result.paramsRemoved)
                        }
                        Text(status, style = MaterialTheme.typography.titleSmall)

                        if (!result.isBlocked) {
                            Text(stringResource(R.string.label_cleaned), style = MaterialTheme.typography.labelMedium)
                            SelectionContainer {
                                Text(
                                    result.cleaned,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    ShareActions.copyToClipboard(ctx, result.cleaned)
                                    ShareActions.toastCopied(ctx, count = 1)
                                }) { Text(stringResource(R.string.copy)) }

                                OutlinedButton(onClick = {
                                    ShareActions.copyToClipboard(ctx, result.cleaned)
                                    ShareActions.reShare(ctx, result.cleaned)
                                }) { Text(stringResource(R.string.share)) }
                            }
                        }
                    }
                }

                // Auto-copy on paste per the plan: user doesn't have to press Copy again.
                androidx.compose.runtime.LaunchedEffect(result.original) {
                    if (result.isBlocked) {
                        ShareActions.toastAfterCleaning(ctx, 0, isBlocked = true, forceShow = true)
                    } else {
                        ShareActions.copyToClipboard(ctx, result.cleaned)
                        ShareActions.toastAfterCleaning(ctx, result.paramsRemoved, isBlocked = false)
                    }
                }
            }

            settings?.let { s ->
                if (s.rulesUpdatedAtEpochMs > 0) {
                    Text(
                        stringResource(
                            R.string.rules_updated_at,
                            DateFormat.getDateTimeInstance().format(Date(s.rulesUpdatedAtEpochMs)),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
