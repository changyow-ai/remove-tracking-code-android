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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.urlcleaner.R
import app.urlcleaner.clipboard.ClipboardWatchMode
import app.urlcleaner.clipboard.ClipboardWatcherService
import app.urlcleaner.data.ShareMode
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val status by viewModel.updateStatus.collectAsState()
    val ctx = LocalContext.current

    // Surface update status as a Toast; the screen itself stays scroll-only.
    LaunchedEffect(status) {
        val s = status ?: return@LaunchedEffect
        Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val s = settings
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (s == null) {
                Text(stringResource(R.string.loading))
                return@Column
            }

            SectionHeader(stringResource(R.string.section_share_behavior))
            ShareMode.entries.forEach { mode ->
                val label = when (mode) {
                    ShareMode.ClipboardOnly -> stringResource(R.string.mode_clipboard_only)
                    ShareMode.ReShare -> stringResource(R.string.mode_reshare)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (s.shareMode == mode),
                            onClick = { viewModel.setShareMode(mode) },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = (s.shareMode == mode), onClick = { viewModel.setShareMode(mode) })
                    Spacer(Modifier.width(8.dp))
                    Text(label)
                }
            }

            HorizontalDivider()

            SectionHeader(stringResource(R.string.section_privacy))
            SwitchRow(
                label = stringResource(R.string.toggle_history),
                subtitle = stringResource(R.string.toggle_history_sub),
                checked = s.historyEnabled,
                onCheckedChange = { viewModel.setHistoryEnabled(it) },
            )
            SwitchRow(
                label = stringResource(R.string.toggle_referral),
                subtitle = stringResource(R.string.toggle_referral_sub),
                checked = s.removeReferralMarketing,
                onCheckedChange = { viewModel.setRemoveReferralMarketing(it) },
            )

            HorizontalDivider()

            // ── Clipboard watch ──────────────────────────────────────────────
            var a11yEnabled by remember { mutableStateOf(ClipboardWatcherService.isEnabled(ctx)) }
            var overlayEnabled by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }

            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(lifecycle) {
                val obs = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        a11yEnabled = ClipboardWatcherService.isEnabled(ctx)
                        overlayEnabled = Settings.canDrawOverlays(ctx)
                    }
                }
                lifecycle.addObserver(obs)
                onDispose { lifecycle.removeObserver(obs) }
            }

            SectionHeader(stringResource(R.string.section_clipboard_watch))
            ClipboardWatchMode.entries.forEach { mode ->
                val (label, sub) = when (mode) {
                    ClipboardWatchMode.OFF -> Pair(
                        stringResource(R.string.watch_mode_off), null
                    )
                    ClipboardWatchMode.AUTO_CLEAN -> Pair(
                        stringResource(R.string.watch_mode_auto),
                        stringResource(R.string.watch_mode_auto_sub),
                    )
                    ClipboardWatchMode.ASK -> Pair(
                        stringResource(R.string.watch_mode_ask),
                        stringResource(R.string.watch_mode_ask_sub),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (s.clipboardWatchMode == mode),
                            onClick = { viewModel.setClipboardWatchMode(mode) },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    RadioButton(
                        selected = (s.clipboardWatchMode == mode),
                        onClick = { viewModel.setClipboardWatchMode(mode) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(label)
                        if (sub != null) {
                            Text(
                                sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Permission warnings
            if (s.clipboardWatchMode != ClipboardWatchMode.OFF) {
                if (!a11yEnabled) {
                    PermissionWarningCard(
                        message = stringResource(R.string.warn_a11y_disabled),
                        buttonLabel = stringResource(R.string.btn_open_a11y_settings),
                        onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    )
                }
                if (s.clipboardWatchMode == ClipboardWatchMode.ASK && !overlayEnabled) {
                    PermissionWarningCard(
                        message = stringResource(R.string.warn_overlay_disabled),
                        buttonLabel = stringResource(R.string.btn_open_overlay_settings),
                        onClick = {
                            ctx.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${ctx.packageName}"),
                                )
                            )
                        },
                    )
                }
            }

            HorizontalDivider()

            SectionHeader(stringResource(R.string.section_rules))
            Text(
                when {
                    s.rulesUpdatedAtEpochMs == 0L -> stringResource(R.string.rules_bundled)
                    else -> stringResource(
                        R.string.rules_updated_at,
                        DateFormat.getDateTimeInstance().format(Date(s.rulesUpdatedAtEpochMs)),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
            )
            s.rulesVersion?.let {
                Text(
                    stringResource(R.string.rules_version, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = { viewModel.updateRulesNow() }) {
                Text(stringResource(R.string.update_rules_now))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun PermissionWarningCard(message: String, buttonLabel: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
