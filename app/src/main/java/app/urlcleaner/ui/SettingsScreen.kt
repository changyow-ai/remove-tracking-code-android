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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import app.urlcleaner.R
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
