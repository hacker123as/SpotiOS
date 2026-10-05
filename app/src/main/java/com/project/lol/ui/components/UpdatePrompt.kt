package com.project.lol.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.project.lol.BuildConfig
import com.project.lol.R
import com.project.lol.update.ApkInstaller
import com.project.lol.update.AppUpdate
import com.project.lol.update.UpdateManager
import com.project.lol.update.UpdateManager.State
import com.project.lol.ui.screens.SettingTile
import com.project.lol.util.MarkdownText
import com.project.lol.util.UpdateChecker
import compose.icons.TablerIcons
import compose.icons.tablericons.AlertTriangle
import compose.icons.tablericons.CloudDownload
import compose.icons.tablericons.ExternalLink
import compose.icons.tablericons.Refresh

/** The update card over the app. Shown while [UpdateManager] has something to say. */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val state by UpdateManager.state.collectAsState()

    // Back from "Install unknown apps", or Android's install prompt is waiting for a screen.
    val confirm = (state as? State.Installing)?.confirm
    LifecycleResumeEffect(confirm) {
        UpdateManager.onResume(context)
        if (confirm != null) {
            UpdateManager.confirmShown()
            context.launch(confirm)
        }
        onPauseOrDispose { }
    }

    val current = state
    val update = current.update ?: return
    val busy = current is State.Downloading || current is State.Installing

    AlertDialog(
        onDismissRequest = {
            when (current) {
                is State.Available, is State.NeedsPermission, is State.Failed, is State.Reinstall -> UpdateManager.later(context)
                is State.Installing -> UpdateManager.close()
                else -> Unit
            }
        },
        properties = DialogProperties(dismissOnClickOutside = !busy),
        shape = RoundedCornerShape(28.dp),
        title = { UpdateTitle(current, update) },
        text = { UpdateBody(current, update) },
        confirmButton = {
            when (current) {
                is State.Available -> PrimaryButton(stringResource(R.string.update_update)) { UpdateManager.start(context) }
                is State.NeedsPermission -> PrimaryButton(stringResource(R.string.update_allow)) {
                    context.launch(ApkInstaller.unknownSourcesIntent(context))
                }
                is State.Failed -> PrimaryButton(stringResource(R.string.update_retry)) { UpdateManager.start(context) }
                is State.Reinstall -> PrimaryButton(stringResource(R.string.update_uninstall), destructive = true) {
                    if (!context.launch(ApkInstaller.uninstallIntent(context))) {
                        context.launch(ApkInstaller.appDetailsIntent(context))
                    }
                }
                else -> Unit
            }
        },
        dismissButton = {
            when (current) {
                is State.Available, is State.NeedsPermission, is State.Failed ->
                    SecondaryButton(stringResource(R.string.update_later)) { UpdateManager.later(context) }
                is State.Downloading -> SecondaryButton(stringResource(R.string.update_cancel)) { UpdateManager.close() }
                is State.Reinstall -> SecondaryButton(stringResource(R.string.update_close)) { UpdateManager.later(context) }
                else -> Unit
            }
        }
    )
}

@Composable
private fun UpdateTitle(state: State, update: AppUpdate) {
    val context = LocalContext.current
    val icon: ImageVector = when (state) {
        is State.Failed, is State.Reinstall -> TablerIcons.AlertTriangle
        is State.Installing -> TablerIcons.Refresh
        else -> TablerIcons.CloudDownload
    }
    val label = if (update.versionName == BuildConfig.VERSION_NAME && update.build > 0) {
        stringResource(R.string.update_version_with_build, update.versionName, update.build)
    } else {
        update.versionName
    }
    val details = listOfNotNull(
        update.build.takeIf { it > 0 }?.let { stringResource(R.string.update_build, it) },
        update.apkSize.takeIf { it > 0 }?.let { Formatter.formatShortFileSize(context, it) }
    ).joinToString(" · ")

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = if (state is State.Reinstall) stringResource(R.string.update_reinstall_title)
                else stringResource(R.string.update_available_title, label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UpdateBody(state: State, update: AppUpdate) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        if ((state is State.Available || state is State.Downloading) && update.notes.isNotBlank()) {
            MarkdownText(
                markdown = update.notes,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (configuration.screenHeightDp * 0.35f).dp)
                    .verticalScroll(rememberScrollState()),
                onLinkClick = { url -> context.launch(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            )
        }
        when (state) {
            is State.Downloading -> {
                if (update.notes.isNotBlank()) Spacer(Modifier.height(16.dp))
                val total = state.total
                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { (state.done.toFloat() / total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                BodyText(
                    if (total > 0) {
                        stringResource(
                            R.string.update_downloading,
                            Formatter.formatShortFileSize(context, state.done),
                            Formatter.formatShortFileSize(context, total)
                        )
                    } else {
                        stringResource(R.string.update_downloading_no_size, Formatter.formatShortFileSize(context, state.done))
                    }
                )
            }
            is State.NeedsPermission -> BodyText(stringResource(R.string.update_permission))
            is State.Installing -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                BodyText(stringResource(R.string.update_installing))
            }
            is State.Failed -> {
                Text(
                    text = stringResource(
                        when (state.problem) {
                            UpdateManager.Problem.DOWNLOAD -> R.string.update_failed_download
                            UpdateManager.Problem.DAMAGED -> R.string.update_failed_damaged
                            UpdateManager.Problem.NOT_SPOTIOS -> R.string.update_failed_not_spotios
                            UpdateManager.Problem.INSTALL -> R.string.update_failed_install
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                if (state.problem == UpdateManager.Problem.INSTALL && !state.detail.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                BrowserButton(update)
            }
            is State.Reinstall -> {
                BodyText(
                    if (state.savedAs != null) stringResource(R.string.update_reinstall_text, state.savedAs)
                    else stringResource(R.string.update_reinstall_text_not_saved)
                )
                if (state.savedAs == null) {
                    Spacer(Modifier.height(4.dp))
                    BrowserButton(update)
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun BrowserButton(update: AppUpdate) {
    val context = LocalContext.current
    TextButton(
        onClick = { UpdateManager.openInBrowser(context, update) },
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = TablerIcons.ExternalLink,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.update_in_browser), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrimaryButton(text: String, destructive: Boolean = false, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private enum class CheckStatus { IDLE, CHECKING, UP_TO_DATE, RATE_LIMITED, FAILED }

/** Settings > About: the running version and build, and a manual check that opens the update card. */
@Composable
fun CheckForUpdatesTile() {
    val context = LocalContext.current
    var status by remember { mutableStateOf(CheckStatus.IDLE) }
    val current = if (BuildConfig.CI_BUILD > 0) {
        stringResource(R.string.update_current_build, BuildConfig.VERSION_NAME, BuildConfig.CI_BUILD)
    } else {
        stringResource(R.string.update_current_local, BuildConfig.VERSION_NAME)
    }
    SettingTile(
        title = stringResource(R.string.update_check),
        subtitle = when (status) {
            CheckStatus.IDLE -> current
            CheckStatus.CHECKING -> stringResource(R.string.update_checking)
            CheckStatus.UP_TO_DATE -> stringResource(R.string.update_up_to_date) + " · " + current
            CheckStatus.RATE_LIMITED -> stringResource(R.string.update_rate_limited)
            CheckStatus.FAILED -> stringResource(R.string.update_check_failed)
        },
        icon = TablerIcons.Refresh,
        onClick = {
            if (status == CheckStatus.CHECKING) return@SettingTile
            status = CheckStatus.CHECKING
            UpdateChecker(context).checkNow { result ->
                status = when (result) {
                    is UpdateChecker.Result.Available -> {
                        UpdateManager.show(result.update)
                        CheckStatus.IDLE
                    }
                    UpdateChecker.Result.UpToDate -> CheckStatus.UP_TO_DATE
                    UpdateChecker.Result.RateLimited -> CheckStatus.RATE_LIMITED
                    UpdateChecker.Result.Failed -> CheckStatus.FAILED
                }
            }
        }
    )
}

/** Starts [intent] from this screen; false if nothing could open it. */
private fun Context.launch(intent: Intent): Boolean {
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { startActivity(intent) }.isSuccess
}
