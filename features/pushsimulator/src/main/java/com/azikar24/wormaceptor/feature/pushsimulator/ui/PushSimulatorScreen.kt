package com.azikar24.wormaceptor.feature.pushsimulator.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.azikar24.wormaceptor.common.presentation.BaseScreen
import com.azikar24.wormaceptor.core.ui.components.appbar.WormaCeptorTopBar
import com.azikar24.wormaceptor.core.ui.components.button.WormaCeptorIconButton
import com.azikar24.wormaceptor.core.ui.components.dialog.WormaCeptorAlertDialog
import com.azikar24.wormaceptor.core.ui.theme.WormaCeptorTheme
import com.azikar24.wormaceptor.core.ui.theme.WormaCeptorTokens
import com.azikar24.wormaceptor.domain.entities.NotificationTemplate
import com.azikar24.wormaceptor.feature.pushsimulator.R
import com.azikar24.wormaceptor.feature.pushsimulator.vm.PushSimulatorViewEffect
import com.azikar24.wormaceptor.feature.pushsimulator.vm.PushSimulatorViewEvent
import com.azikar24.wormaceptor.feature.pushsimulator.vm.PushSimulatorViewModel
import com.azikar24.wormaceptor.feature.pushsimulator.vm.PushSimulatorViewState
import kotlinx.coroutines.launch

@Composable
fun PushSimulatorScreen(
    viewModel: PushSimulatorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current

    // Saveable so a grant after rotating during the permission dialog still sends
    var pendingSend by rememberSaveable(stateSaver = PendingSendSaver) { mutableStateOf<PendingSend?>(null) }

    // Granted but not yet sent: after process death the grant can arrive before templates reload
    var grantedSend by rememberSaveable(stateSaver = PendingSendSaver) { mutableStateOf<PendingSend?>(null) }

    val notificationSentMessage = stringResource(R.string.pushsimulator_notification_sent)
    val templateSavedMessage = stringResource(R.string.pushsimulator_template_saved)
    val templateDeletedMessage = stringResource(R.string.pushsimulator_template_deleted)

    BaseScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is PushSimulatorViewEffect.NotificationSent -> {
                    scope.launch { snackBarHostState.showSnackbar(notificationSentMessage) }
                }
                is PushSimulatorViewEffect.TemplateSaved -> {
                    scope.launch { snackBarHostState.showSnackbar(templateSavedMessage) }
                }
                is PushSimulatorViewEffect.TemplateDeleted -> {
                    scope.launch { snackBarHostState.showSnackbar(templateDeletedMessage) }
                }
                is PushSimulatorViewEffect.TemplateLoaded -> {
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            resources.getString(R.string.pushsimulator_template_loaded, effect.name),
                        )
                    }
                }
                is PushSimulatorViewEffect.Error -> {
                    scope.launch { snackBarHostState.showSnackbar(effect.message) }
                }
            }
        },
    ) { state, onEvent ->
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            val pending = pendingSend
            pendingSend = null
            if (isGranted) {
                grantedSend = pending
            } else {
                val message = resources.getString(R.string.pushsimulator_notification_permission_denied)
                scope.launch { snackBarHostState.showSnackbar(message) }
            }
        }
        LaunchedEffect(grantedSend, state.templates) {
            val event = grantedSend?.toEvent(state.templates) ?: return@LaunchedEffect
            grantedSend = null
            onEvent(event)
        }
        val sendWithPermission: (PendingSend) -> Unit = { pending ->
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingSend = pending
                permissionLauncher.launch(permission)
            } else {
                pending.toEvent(state.templates)?.let(onEvent)
            }
        }
        PushSimulatorScreenContent(
            state = state,
            onEvent = onEvent,
            onBack = onBack,
            snackBarHostState = snackBarHostState,
            onSendClick = { sendWithPermission(PendingSend.Form) },
            onSendTemplate = { sendWithPermission(PendingSend.Template(it.id)) },
            modifier = modifier,
        )
    }
}

private sealed interface PendingSend {
    data object Form : PendingSend

    data class Template(val id: String) : PendingSend

    fun toEvent(templates: List<NotificationTemplate>): PushSimulatorViewEvent? = when (this) {
        Form -> PushSimulatorViewEvent.SendNotification
        is Template -> templates.firstOrNull { it.id == id }?.let(PushSimulatorViewEvent::SendFromTemplate)
    }
}

private const val PendingFormKey = "form"
private const val PendingTemplatePrefix = "template:"

private val PendingSendSaver = Saver<PendingSend?, String>(
    save = { pending ->
        when (pending) {
            PendingSend.Form -> PendingFormKey
            is PendingSend.Template -> PendingTemplatePrefix + pending.id
            null -> null
        }
    },
    restore = { saved ->
        if (saved == PendingFormKey) {
            PendingSend.Form
        } else {
            PendingSend.Template(saved.removePrefix(PendingTemplatePrefix))
        }
    },
)

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PushSimulatorScreenContent(
    state: PushSimulatorViewState,
    onEvent: (PushSimulatorViewEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSendClick: () -> Unit = { onEvent(PushSimulatorViewEvent.SendNotification) },
    onSendTemplate: (NotificationTemplate) -> Unit = { onEvent(PushSimulatorViewEvent.SendFromTemplate(it)) },
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = modifier.fillMaxSize(),
        topBar = {
            WormaCeptorTopBar(
                title = stringResource(R.string.pushsimulator_title),
                onBack = onBack,
                backContentDescription = stringResource(R.string.pushsimulator_back),
                actions = {
                    WormaCeptorIconButton(onClick = { onEvent(PushSimulatorViewEvent.ClearForm) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.pushsimulator_clear_form),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState, modifier = Modifier.navigationBarsPadding()) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(
                start = WormaCeptorTokens.Spacing.lg,
                top = WormaCeptorTokens.Spacing.lg,
                end = WormaCeptorTokens.Spacing.lg,
                bottom = WormaCeptorTokens.Spacing.lg +
                    WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(WormaCeptorTokens.Spacing.lg),
        ) {
            item {
                TemplatesRow(
                    templates = state.templates,
                    onLoad = { onEvent(PushSimulatorViewEvent.LoadTemplate(it)) },
                    onSend = onSendTemplate,
                    onDelete = { onEvent(PushSimulatorViewEvent.DeleteTemplate(it.id)) },
                )
            }

            item {
                NotificationFormCard(
                    state = state,
                    channels = state.channels,
                    previewTitlePlaceholder = stringResource(R.string.pushsimulator_preview_title_placeholder),
                    previewBodyPlaceholder = stringResource(R.string.pushsimulator_preview_body_placeholder),
                    onTitleChange = { onEvent(PushSimulatorViewEvent.UpdateTitle(it)) },
                    onBodyChange = { onEvent(PushSimulatorViewEvent.UpdateBody(it)) },
                    onChannelChange = { onEvent(PushSimulatorViewEvent.UpdateChannelId(it)) },
                    onPriorityChange = { onEvent(PushSimulatorViewEvent.UpdatePriority(it)) },
                    onNewActionTitleChange = { onEvent(PushSimulatorViewEvent.UpdateNewActionTitle(it)) },
                    onAddAction = { onEvent(PushSimulatorViewEvent.AddAction(it)) },
                    onRemoveAction = { onEvent(PushSimulatorViewEvent.RemoveAction(it)) },
                )
            }

            item {
                ActionButtonsRow(
                    onSendClick = onSendClick,
                    onSaveClick = { onEvent(PushSimulatorViewEvent.ShowSaveDialog) },
                    isTitleEmpty = state.title.isBlank(),
                )
            }
        }
    }

    if (state.showSaveDialog) {
        SaveTemplateDialog(
            onDismiss = { onEvent(PushSimulatorViewEvent.DismissSaveDialog) },
            onSave = { name -> onEvent(PushSimulatorViewEvent.SaveAsTemplate(name)) },
        )
    }

    if (state.showPermissionDialog) {
        WormaCeptorAlertDialog(
            title = stringResource(R.string.pushsimulator_permission_title),
            message = stringResource(R.string.pushsimulator_permission_message),
            confirmLabel = stringResource(R.string.pushsimulator_ok),
            onConfirm = { onEvent(PushSimulatorViewEvent.DismissPermissionDialog) },
            dismissLabel = stringResource(R.string.pushsimulator_dialog_cancel),
            onDismiss = { onEvent(PushSimulatorViewEvent.DismissPermissionDialog) },
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true)
@Composable
private fun PushSimulatorScreenContentPreview() {
    WormaCeptorTheme {
        PushSimulatorScreenContent(
            state = PushSimulatorViewState(title = "Test Notification", body = "This is a test"),
            onEvent = {},
            onBack = {},
        )
    }
}
