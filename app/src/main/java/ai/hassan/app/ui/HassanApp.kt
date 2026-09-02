package ai.hassan.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import ai.hassan.app.conversation.HassanTts
import ai.hassan.app.conversation.LocalHassanChat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import ai.hassan.app.BuildConfig
import ai.hassan.app.data.BridgeRequestEntity
import ai.hassan.app.data.BridgeStatuses
import ai.hassan.app.data.ConversationEntity
import ai.hassan.app.data.DecisionEntity
import ai.hassan.app.data.DecisionStatuses
import ai.hassan.app.data.ExecutionPlanEntity
import ai.hassan.app.data.MessageEntity
import ai.hassan.app.data.MessageRoles
import ai.hassan.app.data.ProjectEntity
import ai.hassan.app.data.RadarFindingEntity
import ai.hassan.app.data.RadarStatuses
import ai.hassan.app.data.ResourceLedgerEntity
import ai.hassan.app.data.TaskEntity
import ai.hassan.app.data.TaskStatuses
import ai.hassan.app.diagnostics.DiagnosticsCollector
import ai.hassan.app.execution.ExecutionState
import ai.hassan.app.conversation.AttachmentCodec
import ai.hassan.app.conversation.AttachmentKind
import ai.hassan.app.conversation.ConversationSettings
import ai.hassan.app.conversation.ConversationUiState
import ai.hassan.app.conversation.PendingAttachment
import ai.hassan.app.providers.CodexReasoningEffort
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class HassanScreen(val title: String, val icon: ImageVector) {
    Home("المحادثة", Icons.Rounded.Forum),
    Projects("المشاريع", Icons.Rounded.Workspaces),
    Tasks("المهام", Icons.Rounded.TaskAlt),
    Decisions("صندوق القرارات", Icons.Rounded.Inbox),
    Providers("الموارد", Icons.Rounded.Cloud),
    Radar("الرادار", Icons.Rounded.Radar),
    Diagnostics("Diagnostics", Icons.Rounded.HealthAndSafety),
    Settings("الإعدادات", Icons.Rounded.Settings),
}

private data class LeadOption(val id: String, val label: String)

private val leadOptions = listOf(
    LeadOption("auto", "Frishta Auto"),
    LeadOption("chatgpt", "ChatGPT"),
    LeadOption("gemini", "Gemini"),
    LeadOption("claude", "Claude"),
    LeadOption("deepseek", "DeepSeek"),
)

private fun responderDisplayName(providerId: String?): String = when (providerId?.lowercase()) {
    null, "", "auto", "frishta", "hassan", "hassan-local", "local" -> "Frishta AI"
    "chatgpt" -> "ChatGPT"
    "gemini" -> "Gemini"
    "claude" -> "Claude"
    "deepseek" -> "DeepSeek"
    else -> providerId
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HassanApp(
    viewModel: MainViewModel,
    diagnosticsCollector: DiagnosticsCollector,
    onApproveWithBiometric: (String) -> Unit,
    onLaunchBridge: (String, String) -> Unit,
    onCopyDiagnostics: (String) -> Unit,
    onShareDiagnostics: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(HassanScreen.Home) }
    var showConversationHistory by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = screen != HassanScreen.Home && drawerState.isClosed) {
        screen = HassanScreen.Home
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerHeader()
                    NavigationDrawerItem(
                        label = { Text("محادثة جديدة") },
                        selected = false,
                        onClick = {
                            viewModel.createNewChat()
                            screen = HassanScreen.Home
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("new_chat"),
                    )
                    NavigationDrawerItem(
                        label = { Text("المحادثات") },
                        selected = false,
                        onClick = {
                            showConversationHistory = true
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("conversation_history"),
                    )
                    Spacer(Modifier.height(8.dp))
                    HassanScreen.entries.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            selected = screen == item,
                            onClick = {
                                screen = item
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .testTag("nav_${item.name}"),
                        )
                    }
                }
            },
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            if (screen == HassanScreen.Home) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Frishta AI", fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(8.dp))
                                    ProviderStatusDot(state.conversationUi)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        when {
                                            state.conversationUi.isSending -> "يرسل…"
                                            state.conversationUi.providerConfigured -> "متصل"
                                            else -> "غير مُعد"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("connection_status_label"),
                                    )
                                }
                            } else {
                                Text(screen.title, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.Menu, contentDescription = "القائمة")
                            }
                        },
                        actions = {
                            if (screen == HassanScreen.Home) {
                                TopBarProviderSelector(
                                    selectedId = state.conversationSettings.chatProvider,
                                    onSelect = { id ->
                                        viewModel.updateConversationSettings(
                                            state.conversationSettings.copy(chatProvider = id),
                                        )
                                    },
                                )
                                IconButton(
                                    onClick = { screen = HassanScreen.Settings },
                                    modifier = Modifier.testTag("home_settings"),
                                ) {
                                    Icon(Icons.Rounded.Settings, contentDescription = "الإعدادات")
                                }
                            }
                        },
                    )
                },
            ) { contentPadding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .then(
                            if (screen == HassanScreen.Home) Modifier
                            else Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                        ),
                ) {
                    when (screen) {
                        HassanScreen.Home -> ChatHomeScreen(
                            state = state,
                            onSend = viewModel::sendMessage,
                            onLaunchBridge = onLaunchBridge,
                            onDownloadArtifact = viewModel::downloadArtifact,
                            onOpenProjects = { screen = HassanScreen.Projects },
                        )
                        HassanScreen.Projects -> ProjectsScreen(
                            projects = state.projects,
                            cloudJobs = state.cloudJobs,
                            artifacts = state.artifacts,
                            conversations = state.conversations,
                            workspaceFilesByProject = state.workspaceFilesByProject,
                            workspaceFilePreview = state.workspaceFilePreview,
                            onSync = viewModel::syncCloudJobs,
                            onOpenWorkspaceFile = viewModel::openWorkspaceFile,
                            onClearWorkspacePreview = viewModel::clearWorkspaceFilePreview,
                            onRefreshWorkspace = { viewModel.refreshWorkspaceFiles() },
                        )
                        HassanScreen.Tasks -> TasksScreen(
                            tasks = state.tasks,
                            cloudJobs = state.cloudJobs,
                            artifacts = state.artifacts,
                            onSync = viewModel::syncCloudJobs,
                            onDownloadArtifact = viewModel::downloadArtifact,
                            onCancelCloudJob = viewModel::cancelCloudJob,
                        )
                        HassanScreen.Decisions -> DecisionsScreen(
                            decisions = state.decisions,
                            onApprove = onApproveWithBiometric,
                            onReject = viewModel::rejectDecision,
                        )
                        HassanScreen.Providers -> ProvidersScreen(state.resources)
                        HassanScreen.Radar -> RadarScreen(
                            findings = state.radarFindings.filter {
                                it.userDecision != ai.hassan.app.data.RadarUserDecisions.REJECT
                            },
                            onRunRadar = viewModel::runRadar,
                            onRadarDecision = viewModel::updateRadarDecision,
                        )
                        HassanScreen.Diagnostics -> DiagnosticsScreen(
                            collector = diagnosticsCollector,
                            state = state,
                            onCopy = onCopyDiagnostics,
                            onShare = onShareDiagnostics,
                        )
                        HassanScreen.Settings -> SettingsScreen(
                            conversation = state.conversations.firstOrNull { it.id == state.activeConversationId }
                                ?: state.conversations.firstOrNull(),
                            conversationUi = state.conversationUi,
                            conversationSettings = state.conversationSettings,
                            hasApkBackup = viewModel.hasApkBackup(),
                            apkBackupLabel = viewModel.apkBackupLabel(),
                            onSelectLead = viewModel::selectLeadBrain,
                            onSelectCodexEffort = viewModel::selectCodexReasoningEffort,
                            onUpdateConversationSettings = viewModel::updateConversationSettings,
                            onBackupApk = viewModel::backupApkNow,
                            onRollbackApk = viewModel::rollbackApk,
                            onCheckUpdate = viewModel::checkAppUpdate,
                        )
                    }
                }
            }
        }
        if (showConversationHistory) {
            ConversationHistorySheet(
                conversations = state.conversations,
                activeConversationId = state.activeConversationId,
                onSelect = {
                    viewModel.selectConversation(it)
                    showConversationHistory = false
                },
                onRename = viewModel::renameConversation,
                onDelete = viewModel::deleteConversation,
                onDismiss = { showConversationHistory = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationHistorySheet(
    conversations: List<ConversationEntity>,
    activeConversationId: String?,
    onSelect: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("conversation_history_sheet"),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("المحادثات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            items(conversations, key = { it.id }) { conversation ->
                val selected = conversation.id == activeConversationId
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(conversation.id) }
                        .testTag("conversation_item_${conversation.id}"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (renamingId == conversation.id) {
                            OutlinedTextField(
                                value = renameText,
                                onValueChange = { renameText = it },
                                modifier = Modifier.fillMaxWidth().testTag("rename_conversation_input"),
                                singleLine = true,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onRename(conversation.id, renameText)
                                        renamingId = null
                                    },
                                    modifier = Modifier.testTag("rename_conversation_save"),
                                ) { Text("حفظ") }
                                OutlinedButton(onClick = { renamingId = null }) { Text("إلغاء") }
                            }
                        } else {
                            Text(conversation.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(shortDate(conversation.updatedAt), style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        renamingId = conversation.id
                                        renameText = conversation.title
                                    },
                                    modifier = Modifier.testTag("rename_conversation_${conversation.id}"),
                                ) { Text("إعادة تسمية") }
                                if (conversations.size > 1) {
                                    TextButton(
                                        onClick = { onDelete(conversation.id) },
                                        modifier = Modifier.testTag("delete_conversation_${conversation.id}"),
                                    ) { Text("حذف") }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChatHomeScreen(
    state: HassanUiState,
    onSend: (String, List<PendingAttachment>) -> Unit,
    onLaunchBridge: (String, String) -> Unit,
    onDownloadArtifact: (ai.hassan.app.data.ArtifactEntity) -> Unit,
    onOpenProjects: () -> Unit,
) {
    val conversation = state.conversations.firstOrNull { it.id == state.activeConversationId }
        ?: state.conversations.firstOrNull()
    val messages = state.messages.filter { it.conversationId == conversation?.id }
    val plan = activeExecutionPlan(state.plans, conversation)
    val bridge = state.bridgeRequests.firstOrNull {
        it.conversationId == conversation?.id && it.status == BridgeStatuses.PENDING
    }
    var composer by remember { mutableStateOf("") }
    var pendingAttachments by remember { mutableStateOf<List<PendingAttachment>>(emptyList()) }
    var speakNextReply by remember { mutableStateOf(false) }
    val messageListState = rememberLazyListState()
    val conversationUi = state.conversationUi
    val showStatusBanner = shouldShowConversationStatusBanner(conversationUi)
    val context = LocalContext.current
    val projectContext = state.projects.firstOrNull { it.id == conversation?.projectId }
    val selectedProviderId = state.conversationSettings.chatProvider
    val selectedProviderName = LocalHassanChat.displayName(selectedProviderId)
    val tts = remember(context) { HassanTts(context) }
    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    LaunchedEffect(messages.lastOrNull()?.id, conversationUi.isSending, speakNextReply) {
        if (speakNextReply && !conversationUi.isSending) {
            val last = messages.lastOrNull()
            if (last != null && last.role != MessageRoles.USER && last.content.isNotBlank()) {
                tts.speak(last.content)
                speakNextReply = false
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spoken.isNotBlank()) {
            speakNextReply = true
            onSend(spoken, pendingAttachments)
            composer = ""
            pendingAttachments = emptyList()
        }
    }

    fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث مع $selectedProviderName")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { speechLauncher.launch(intent) }
            .onFailure {
                Toast.makeText(context, "التعرّف على الصوت غير متاح على هذا الجهاز", Toast.LENGTH_SHORT).show()
            }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer()
        } else {
            Toast.makeText(context, "يلزم إذن الميكروفون للتكلم مع الذكاء", Toast.LENGTH_SHORT).show()
        }
    }

    fun onMicClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchSpeechRecognizer() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val mime = context.contentResolver.getType(uri).orEmpty()
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
        val kind = if (mime.startsWith("image/")) AttachmentKind.IMAGE else AttachmentKind.FILE
        pendingAttachments = pendingAttachments + PendingAttachment(
            uri = uri.toString(),
            displayName = name,
            mimeType = mime.ifBlank { "application/octet-stream" },
            kind = kind,
        )
    }

    val tailItemCount = messages.size +
        (if (plan != null) 1 else 0) +
        (if (bridge != null) 1 else 0) +
        (if (showStatusBanner) 1 else 0)
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(
        messages.lastOrNull()?.id,
        plan?.id,
        bridge?.id,
        conversationUi.statusMessage,
        conversationUi.isSending,
        imeVisible,
        state.cloudJobs.map { "${it.id}:${it.state}" },
    ) {
        if (tailItemCount > 0) {
            // Wait a frame so IME/layout height settles, then keep last message above composer.
            kotlinx.coroutines.delay(50)
            messageListState.animateScrollToItem(tailItemCount - 1)
        }
    }

    // Single column + imePadding: list shrinks with keyboard so last message stays visible.
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .testTag("chat_home"),
    ) {
        if (projectContext != null) {
            AssistChip(
                onClick = onOpenProjects,
                label = { Text(projectContext.name, maxLines = 1) },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("project_context_chip"),
            )
        }
        LazyColumn(
            state = messageListState,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("conversation_list"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    cloudJobs = state.cloudJobs,
                    artifacts = state.artifacts,
                    onDownloadArtifact = onDownloadArtifact,
                    onOpenProjects = onOpenProjects,
                )
            }
            if (showStatusBanner) {
                item(key = "conversation_status") {
                    ConversationStatusBanner(conversationUi)
                }
            }
            if (plan != null) item(key = "plan:${plan.id}") { PlanCard(plan) }
            if (bridge != null) {
                item(key = "bridge:${bridge.id}") {
                    HumanBridgeCard(bridge, onLaunchBridge)
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            if (pendingAttachments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("pending_attachments"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(pendingAttachments, key = { it.uri }) { attachment ->
                        AssistChip(
                            onClick = {},
                            label = { Text(attachment.displayName, maxLines = 1) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        pendingAttachments = pendingAttachments.filterNot { it.uri == attachment.uri }
                                    },
                                    modifier = Modifier.size(18.dp),
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "إزالة", modifier = Modifier.size(14.dp))
                                }
                            },
                        )
                    }
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.testTag("attach_button"),
                ) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = "إرفاق ملف")
                }
                IconButton(
                    onClick = { onMicClick() },
                    enabled = !conversationUi.isSending,
                    modifier = Modifier.testTag("voice_input"),
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = "تكلم مع الذكاء")
                }
                OutlinedTextField(
                    value = composer,
                    onValueChange = { composer = it },
                    modifier = Modifier.weight(1f).testTag("composer"),
                    placeholder = { Text("اكتب أو تكلم…") },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(
                    onClick = {
                        onSend(composer, pendingAttachments)
                        composer = ""
                        pendingAttachments = emptyList()
                    },
                    enabled = (composer.isNotBlank() || pendingAttachments.isNotEmpty()) && !conversationUi.isSending,
                    modifier = Modifier.testTag("send_message"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "إرسال")
                }
            }
        }
    }
}

private fun activeExecutionPlan(
    plans: List<ExecutionPlanEntity>,
    conversation: ConversationEntity?,
): ExecutionPlanEntity? {
    val conv = conversation ?: return null
    if (conv.state == ExecutionState.DISCUSSING.name) return null
    val plan = plans.firstOrNull { it.conversationId == conv.id } ?: return null
    if (plan.status in INACTIVE_PLAN_STATUSES) return null
    return plan
}

private val INACTIVE_PLAN_STATUSES = setOf(
    ExecutionState.REJECTED.name,
    ExecutionState.COMPLETED.name,
    ExecutionState.DISCUSSING.name,
)

private fun shouldShowConversationStatusBanner(ui: ConversationUiState): Boolean =
    ui.isSending || !ui.providerConfigured

@Composable
private fun ConversationStatusBanner(ui: ConversationUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("conversation_status_banner"),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (ui.isSending) "Frishta يكتب…" else "NOT_CONFIGURED",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    ui.isSending -> "جارٍ تجهيز الرد…"
                    ui.statusMessage.isNotBlank() -> ui.statusMessage
                    else -> "لم يتم إعداد مزود المحادثة بعد. افتح الإعدادات."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProviderStatusDot(ui: ConversationUiState) {
    val color = when {
        ui.isSending -> Color(0xFFE6A700)
        ui.providerConfigured -> Color(0xFF17824B)
        else -> Color(0xFFC62828)
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
            .testTag("provider_status_dot"),
    )
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    cloudJobs: List<ai.hassan.app.data.CloudJobEntity> = emptyList(),
    artifacts: List<ai.hassan.app.data.ArtifactEntity> = emptyList(),
    onDownloadArtifact: (ai.hassan.app.data.ArtifactEntity) -> Unit = {},
    onOpenProjects: () -> Unit = {},
) {
    val isUser = message.role == MessageRoles.USER
    val tag = if (!isUser) Modifier.testTag("hassan_message") else Modifier
    val linkedJob = message.taskId?.let { id -> cloudJobs.firstOrNull { it.id == id } }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .then(tag)
                .background(
                    if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 4.dp else 16.dp,
                        bottomEnd = if (isUser) 16.dp else 4.dp,
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!isUser) {
                Text(
                    responderDisplayName(
                        when (message.role) {
                            MessageRoles.PROVIDER -> message.providerId
                            else -> message.providerId ?: "frishta"
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
            }
            if (message.content.isNotBlank()) {
                Text(message.content, style = MaterialTheme.typography.bodyMedium)
            }
            val attachments = AttachmentCodec.decode(message.attachmentRefs)
            if (attachments.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                attachments.forEach { attachment ->
                    Text(
                        buildString {
                            append("📎 ${attachment.displayName}")
                            attachment.cloudArtifactId?.let { append(" · cloud") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (linkedJob != null) {
                Spacer(Modifier.height(8.dp))
                ChatTaskCard(
                    job = linkedJob,
                    artifacts = artifacts.filter { it.jobId == linkedJob.id },
                    onDownloadArtifact = onDownloadArtifact,
                    onOpenProjects = onOpenProjects,
                )
            }
        }
    }
}

@Composable
private fun ChatTaskCard(
    job: ai.hassan.app.data.CloudJobEntity,
    artifacts: List<ai.hassan.app.data.ArtifactEntity>,
    onDownloadArtifact: (ai.hassan.app.data.ArtifactEntity) -> Unit,
    onOpenProjects: () -> Unit,
) {
    val done = job.state.equals("COMPLETED", ignoreCase = true)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${job.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        done -> "مكتمل"
                        job.state.equals("FAILED", true) -> "فشل"
                        job.state.equals("CANCELLED", true) -> "ملغى"
                        else -> "جاري التنفيذ"
                    },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(job.state)
            }
            Text(
                job.goal.take(80),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("task_card_goal"),
            )
            if (!job.resultSummary.isNullOrBlank() && done) {
                Text(
                    job.resultSummary.orEmpty().take(160),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (done && artifacts.isNotEmpty()) {
                Text("الملفات", fontWeight = FontWeight.Medium)
                artifacts.take(6).forEach { art ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().testTag("task_artifact_${art.id}"),
                    ) {
                        Text(
                            art.name,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = { onDownloadArtifact(art) },
                            modifier = Modifier.testTag("task_download_${art.id}"),
                        ) { Text("تنزيل") }
                    }
                }
                TextButton(
                    onClick = onOpenProjects,
                    modifier = Modifier.testTag("task_open_project"),
                ) { Text("فتح المشروع") }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: ExecutionPlanEntity) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().testTag("plan_card")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("ExecutionPlan", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusChip(plan.status)
            }
            Text(plan.summary)
            Text("المكونات", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(plan.components)
            Text("المخاطر: ${plan.risks}", style = MaterialTheme.typography.bodySmall)
            Text("التحقق: ${plan.verification}", style = MaterialTheme.typography.bodySmall)
            Text("الرجوع: ${plan.rollback}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HumanBridgeCard(bridge: BridgeRequestEntity, onLaunch: (String, String) -> Unit) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("مطلوب جسر بشري آمن", fontWeight = FontWeight.Bold)
            Text("سيرسل Android الـTaskPack إلى التطبيق المختار. لا Accessibility ولا scraping ولا API مدفوعة.")
            Button(
                onClick = { onLaunch(bridge.providerId, bridge.taskPackText) },
                modifier = Modifier.fillMaxWidth().testTag("bridge_send"),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("إرسال TaskPack إلى ${bridge.providerId}")
            }
        }
    }
}

@Composable
private fun TopBarProviderSelector(
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = leadOptions.firstOrNull { it.id == selectedId } ?: leadOptions.first()
    Box {
        AssistChip(
            onClick = { open = true },
            label = {
                Text(
                    selected.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            modifier = Modifier.testTag("provider_selector"),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            leadOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option.id)
                        open = false
                    },
                    modifier = Modifier.testTag("provider_option_${option.id}"),
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Text("F", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Frishta AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Personal AI Command Center", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsScreen(
    projects: List<ProjectEntity>,
    cloudJobs: List<ai.hassan.app.data.CloudJobEntity>,
    artifacts: List<ai.hassan.app.data.ArtifactEntity>,
    conversations: List<ConversationEntity>,
    workspaceFilesByProject: Map<String, List<ai.hassan.app.cloud.CloudWorkspaceFileDto>>,
    workspaceFilePreview: WorkspaceFilePreview?,
    onSync: () -> Unit,
    onOpenWorkspaceFile: (String, String) -> Unit,
    onClearWorkspacePreview: () -> Unit,
    onRefreshWorkspace: () -> Unit,
) {
    LaunchedEffect(projects.map { it.id }, cloudJobs.map { it.cloudProjectId }) {
        onRefreshWorkspace()
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedButton(
                onClick = onSync,
                modifier = Modifier.fillMaxWidth().testTag("sync_cloud_projects"),
            ) {
                Text("مزامنة المشاريع وWorkspace")
            }
        }
        if (projects.isEmpty() && cloudJobs.isEmpty()) {
            item { EmptyStateContent("لا توجد مشاريع بعد", "أنشئ مشروعاً عبر Hassan Cloud أو من المحادثة.") }
        }
        items(projects, key = { it.id }) { project ->
            val projectJobs = cloudJobs.filter { it.cloudProjectId == project.id }
            val projectArtifacts = artifacts.filter { it.projectId == project.id }
            val projectConversations = conversations.filter { it.projectId == project.id }
            val workspaceFiles = workspaceFilesByProject[project.id].orEmpty()
            ElevatedCard(modifier = Modifier.testTag("project_${project.id}")) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(project.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(project.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "محادثات: ${projectConversations.size} · مهام: ${projectJobs.size} · ملفات: ${projectArtifacts.size} · Workspace: ${workspaceFiles.size}",
                    )
                    if (workspaceFiles.isNotEmpty()) {
                        Text("ملفات Workspace", fontWeight = FontWeight.SemiBold)
                        workspaceFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenWorkspaceFile(project.id, file.path) }
                                    .testTag("workspace_file_${project.id}_${file.path}")
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "📄 ${file.path}",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text("${file.size_bytes} B", style = MaterialTheme.typography.labelSmall)
                                TextButton(onClick = { onOpenWorkspaceFile(project.id, file.path) }) {
                                    Text("فتح")
                                }
                            }
                        }
                    }
                    projectJobs.take(3).forEach { job ->
                        Text("• ${job.goal.take(40)} — ${job.state}", style = MaterialTheme.typography.bodySmall)
                    }
                    projectArtifacts.take(3).forEach { art ->
                        Text("📎 ${art.name} (${art.sizeBytes} B)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (workspaceFilePreview != null) {
        ModalBottomSheet(
            onDismissRequest = onClearWorkspacePreview,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(workspaceFilePreview.path, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("workspace_preview_path"))
                Text(
                    workspaceFilePreview.content.take(8000),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("workspace_preview_content"),
                )
                TextButton(onClick = onClearWorkspacePreview, modifier = Modifier.align(Alignment.End)) {
                    Text("إغلاق")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TasksScreen(
    tasks: List<TaskEntity>,
    cloudJobs: List<ai.hassan.app.data.CloudJobEntity>,
    artifacts: List<ai.hassan.app.data.ArtifactEntity>,
    onSync: () -> Unit,
    onDownloadArtifact: (ai.hassan.app.data.ArtifactEntity) -> Unit,
    onCancelCloudJob: (String) -> Unit,
) {
    val context = LocalContext.current
    if (tasks.isEmpty() && cloudJobs.isEmpty()) {
        EmptyState("لا توجد مهام بعد", "تنشأ المهام من المحادثة أو Hassan Cloud.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth().testTag("sync_cloud_jobs")) {
                Text("مزامنة Hassan Cloud")
            }
        }
        if (cloudJobs.isNotEmpty()) {
            item { Text("مهام سحابية", fontWeight = FontWeight.Bold) }
            items(cloudJobs, key = { "cloud_${it.id}" }) { job ->
                val jobArtifacts = artifacts.filter { it.jobId == job.id }
                ElevatedCard(modifier = Modifier.testTag("cloud_job_${job.id}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(job.goal, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        StatusChip(job.state)
                        val cancellable = job.state in listOf("QUEUED", "RUNNING", "CODING", "VERIFYING", "PLANNING")
                        if (cancellable) {
                            TextButton(
                                onClick = { onCancelCloudJob(job.id) },
                                modifier = Modifier.testTag("cancel_cloud_job_${job.id}"),
                            ) { Text("إلغاء") }
                        }
                        job.resultSummary?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3) }
                        jobArtifacts.forEach { art ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📎 ${art.name}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { onDownloadArtifact(art) }) { Text("تنزيل") }
                                TextButton(onClick = {
                                    val path = art.localPath
                                    if (!path.isNullOrBlank()) {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            java.io.File(path),
                                        )
                                        val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = art.mimeType
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(share, "مشاركة"))
                                    }
                                }) { Text("مشاركة") }
                            }
                        }
                    }
                }
            }
        }
        if (tasks.isNotEmpty()) {
            item { Text("مهام محلية", fontWeight = FontWeight.Bold) }
            items(tasks, key = { it.id }) { task ->
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(task.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            StatusChip(task.status)
                        }
                        Text(task.payload, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${task.source} · ${shortDate(task.updatedAt)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionsScreen(decisions: List<DecisionEntity>, onApprove: (String) -> Unit, onReject: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (decisions.isEmpty()) item { EmptyStateContent("لا توجد قرارات", "ستظهر هنا القرارات عالية المخاطر.") }
        items(decisions, key = { it.id }) { decision ->
            ElevatedCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(decision.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        StatusChip(decision.status)
                    }
                    Text(decision.summary)
                    if (decision.status == DecisionStatuses.PENDING) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { onApprove(decision.id) }, modifier = Modifier.weight(1f)) { Text("اعتماد بالبصمة") }
                            OutlinedButton(onClick = { onReject(decision.id) }, modifier = Modifier.weight(1f)) { Text("رفض") }
                        }
                    } else if (decision.signatureVerified == true) {
                        Text("تم التوقيع والتحقق محليًا", color = Color(0xFF17824B), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProvidersScreen(resources: List<ResourceLedgerEntity>) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Resource Ledger — الكلفة الفعلية لجميع الموارد: 0", fontWeight = FontWeight.Bold) }
        items(resources, key = { it.providerId }) { resource ->
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(resource.displayName, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        StatusChip(resource.costClass)
                    }
                    Text("الحصة: ${resource.quotaRemaining}")
                    Text("الخصوصية: ${resource.privacy} · الترخيص: ${resource.license}", style = MaterialTheme.typography.bodySmall)
                    Text("Human‑Gated: ${if (resource.requiresHumanBridge) "نعم" else "لا"} · Card: ${if (resource.cardRequired) "مطلوبة — محظور" else "لا"}")
                    Text("Actual money cost: ${resource.actualMoneyCostCents}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun RadarScreen(
    findings: List<RadarFindingEntity>,
    onRunRadar: () -> Unit,
    onRadarDecision: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.testTag("radar_list"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("يفحص إصدارات مستودعات رسمية يوميًا (WorkManager) ويرسل ملخصًا. لا يفعّل مزوّدًا تلقائيًا.")
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRunRadar, modifier = Modifier.fillMaxWidth().testTag("run_radar")) {
                Icon(Icons.Rounded.Radar, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("افحص المصادر المجانية الآن")
            }
        }
        if (findings.isEmpty()) item { EmptyStateContent("لا توجد نتائج بعد", "شغّل الفحص الأول؛ النتائج تُحفظ في Room.", Icons.Rounded.Radar) }
        items(findings, key = { it.id }) { finding ->
            ElevatedCard(modifier = Modifier.testTag("radar_finding_${finding.id}")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(finding.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        StatusChip(finding.candidateStatus.ifBlank { finding.status })
                    }
                    if (finding.candidateType.isNotBlank()) {
                        Text("النوع: ${finding.candidateType}", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(finding.summary)
                    Text(
                        "${finding.costClass} · ${finding.license} · ${finding.version}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (finding.radarScore > 0f) {
                        Text("Radar Score: ${"%.1f".format(finding.radarScore)}/10")
                    }
                    if (finding.riskLevel.isNotBlank()) {
                        Text("Risk: ${finding.riskLevel}")
                    }
                    Text(finding.sourceUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (finding.userDecision.isBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = { onRadarDecision(finding.id, ai.hassan.app.data.RadarUserDecisions.APPROVE) },
                                modifier = Modifier.testTag("radar_approve_${finding.id}"),
                            ) { Text("موافقة") }
                            TextButton(
                                onClick = { onRadarDecision(finding.id, ai.hassan.app.data.RadarUserDecisions.TEST_ONLY) },
                                modifier = Modifier.testTag("radar_test_${finding.id}"),
                            ) { Text("اختبار فقط") }
                            TextButton(
                                onClick = { onRadarDecision(finding.id, ai.hassan.app.data.RadarUserDecisions.REJECT) },
                                modifier = Modifier.testTag("radar_reject_${finding.id}"),
                            ) { Text("رفض") }
                        }
                    } else {
                        Text("قرارك: ${finding.userDecision}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsScreen(
    collector: DiagnosticsCollector,
    state: HassanUiState,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    val base = remember(refreshKey) { collector.collect() }
    val conversation = state.conversations.firstOrNull()
    val dynamicLines = listOf(
        "Free-only status: ACTIVE / immutable",
        "Conversation: ${stateLabel(conversation?.state ?: "DISCUSSING")}",
        "Provider resources: ${state.resources.size}",
        "Last radar: ${state.radarFindings.maxOfOrNull { it.lastVerifiedAt }?.let(::shortDate) ?: "لم يعمل"}",
        "Evidence bundles: ${state.evidenceBundles.size}",
        "Last Candidate: ${BuildConfig.VERSION_NAME}",
    )
    val fullReport = base.asPlainText() + dynamicLines.joinToString("\n", prefix = "\n", postfix = "\n")
    LazyColumn(
        modifier = Modifier.testTag("diagnostics_list"),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { refreshKey++ }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Refresh, null); Text("تحديث") }
                FilledTonalButton(onClick = { onCopy(fullReport) }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.ContentCopy, null); Text("نسخ") }
                FilledTonalButton(onClick = { onShare(fullReport) }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Share, null); Text("مشاركة") }
            }
        }
        items(dynamicLines, key = { it.substringBefore(':') }) { line -> DiagnosticCard(line.substringBefore(':'), line.substringAfter(':').trim()) }
        items(base.items, key = { it.label }) { item -> DiagnosticCard(item.label, item.value) }
    }
}

@Composable
private fun DiagnosticCard(label: String, value: String) {
    ElevatedCard {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SettingsScreen(
    conversation: ConversationEntity?,
    conversationUi: ConversationUiState,
    conversationSettings: ConversationSettings,
    hasApkBackup: Boolean,
    apkBackupLabel: String,
    onSelectLead: (String) -> Unit,
    onSelectCodexEffort: (String) -> Unit,
    onUpdateConversationSettings: (ConversationSettings) -> Unit,
    onBackupApk: () -> Unit,
    onRollbackApk: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    var cloudUrl by remember(conversationSettings.cloudBaseUrl) { mutableStateOf(conversationSettings.cloudBaseUrl) }
    var accessToken by remember(conversationSettings.accessToken) { mutableStateOf(conversationSettings.accessToken) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp).testTag("settings_scroll"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(modifier = Modifier.testTag("cloud_conversation_card")) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Hassan Cloud — المحادثة", fontWeight = FontWeight.Bold)
                Text(
                    "ChatGPT وGemini وDeepSeek يعملان عبر Hassan Cloud (بدون مفاتيح داخل التطبيق). Hassan Auto يختار المزوّد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = cloudUrl,
                    onValueChange = { cloudUrl = it },
                    modifier = Modifier.fillMaxWidth().testTag("cloud_url"),
                    label = { Text("رابط Hassan Cloud") },
                    placeholder = { Text("https://api.example.com") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it },
                    modifier = Modifier.fillMaxWidth().testTag("cloud_token"),
                    label = { Text("رمز الوصول") },
                    singleLine = true,
                )
                Text("مزوّد المحادثة", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(leadOptions, key = { "chat_${it.id}" }) { option ->
                        FilterChip(
                            selected = conversationSettings.chatProvider == option.id,
                            onClick = {
                                onUpdateConversationSettings(conversationSettings.copy(chatProvider = option.id))
                            },
                            label = { Text(option.label) },
                            modifier = Modifier.testTag("chat_provider_${option.id}"),
                        )
                    }
                }
                Button(
                    onClick = {
                        onUpdateConversationSettings(
                            conversationSettings.copy(
                                cloudBaseUrl = cloudUrl.trim(),
                                accessToken = accessToken.trim(),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_cloud_settings"),
                ) {
                    Text("حفظ إعدادات المحادثة")
                }
                Text(
                    if (conversationUi.providerConfigured) "الحالة: متصل بـ Hassan Cloud"
                    else "الحالة: غير مُعد — لن تظهر ردود AI حتى تُحفظ الإعدادات",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        ElevatedCard(modifier = Modifier.testTag("self_update_card")) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("التحديث الذاتي", fontWeight = FontWeight.Bold)
                Text(
                    "قبل أي تحديث يحفظ حسن نسخة APK احتياطية. يمكنك التحديث من المحادثة أو من هنا بدون كمبيوتر.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("النسخة الاحتياطية: $apkBackupLabel", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onBackupApk, modifier = Modifier.weight(1f).testTag("backup_apk")) {
                        Text("نسخ احتياطي")
                    }
                    OutlinedButton(
                        onClick = onRollbackApk,
                        enabled = hasApkBackup,
                        modifier = Modifier.weight(1f).testTag("rollback_apk"),
                    ) {
                        Text("استرجاع")
                    }
                }
                Button(onClick = onCheckUpdate, modifier = Modifier.fillMaxWidth().testTag("check_update")) {
                    Text("التحقق من تحديث")
                }
                Text(
                    "من المحادثة: «حسّن التطبيق» أو «عدّل جزء من …» ثم «ابدأ» للبناء السحابي والتثبيت. للتراجع: «ارجع للنسخة السابقة». أو أرفق APK وقل «ثبت التحديث».",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ElevatedCard(modifier = Modifier.testTag("conversation_provider_card")) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("محادثة Hassan", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProviderStatusDot(conversationUi)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (conversationUi.providerConfigured) "مزود المحادثة جاهز"
                        else conversationUi.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    "التطبيق مستقل عن أي كمبيوتر. المحادثة العادية تحتاج مزودًا محادثةً حقيقيًا — غير مُعدّ حاليًا.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ElevatedCard(modifier = Modifier.testTag("lead_brain_selector")) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مورد التنفيذ (للمهام فقط)", fontWeight = FontWeight.Bold)
                Text(
                    "يُستخدم عند طلب تنفيذ صريح — وليس للمحادثة العادية. معظم الخيارات human-gated عبر Share Sheet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(leadOptions, key = { it.id }) { option ->
                        FilterChip(
                            selected = conversation?.leadBrainId == option.id,
                            onClick = { onSelectLead(option.id) },
                            label = { Text(option.label) },
                            modifier = Modifier.testTag("lead_${option.id}"),
                        )
                    }
                }
                leadOptions.forEach { option ->
                    val availability = providerAvailabilityLabel(option.id)
                    if (availability.isNotBlank()) {
                        Text(
                            "${option.label}: $availability",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (conversation?.leadBrainId == "chatgpt") {
            ElevatedCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مستوى تفكير Codex", fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("codex_effort_selector"),
                    ) {
                        items(CodexReasoningEffort.entries, key = { it.apiValue }) { effort ->
                            FilterChip(
                                selected = conversation.codexReasoningEffort == effort.apiValue,
                                onClick = { onSelectCodexEffort(effort.apiValue) },
                                label = { Text(effort.arabicLabel) },
                                modifier = Modifier.testTag("effort_${effort.apiValue}"),
                            )
                        }
                    }
                }
            }
        }
        ElevatedCard {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ZeroCostPolicy", fontWeight = FontWeight.Bold)
                    Text("FREE وPREPAID مسموحان؛ METERED محظور في الكود.")
                }
                Switch(checked = true, onCheckedChange = null, enabled = false)
            }
        }
        ElevatedCard {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("قوانين مالية غير قابلة للتعديل", fontWeight = FontWeight.Bold)
                Text("additional_spend_limit = 0")
                Text("paid_api_allowed = false")
                Text("paid_gpu_allowed = false")
                Text("auto_topup = false")
                Text("agent_can_change_budget = false")
            }
        }
        ElevatedCard {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("حدود Milestone 2", fontWeight = FontWeight.Bold)
                Text("• لا نشر Stable تلقائي")
                Text("• لا Accessibility أو scraping أو session extraction")
                Text("• لا Media Factory أو self-evolution كامل")
                Text("• Radar يكتشف ويحفظ فقط؛ لا يربط Provider تلقائيًا")
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        TaskStatuses.WAITING_DECISION, DecisionStatuses.PENDING, "AWAITING_USER_APPROVAL", "NEEDS_INPUT" -> MaterialTheme.colorScheme.errorContainer
        TaskStatuses.APPROVED, DecisionStatuses.APPROVED, RadarStatuses.VERIFIED, "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer
        "EXECUTING", "VERIFYING", "QUEUED" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Text(
        stateLabel(status),
        modifier = Modifier.background(color, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall,
    )
}

private fun stateLabel(state: String): String = when (state) {
    "DISCUSSING" -> "يناقش"
    "PLAN_READY", "AWAITING_USER_APPROVAL" -> "الخطة جاهزة"
    "QUEUED" -> "في الطابور"
    "EXECUTING", TaskStatuses.RUNNING -> "ينفذ"
    "NEEDS_INPUT" -> "يحتاج جوابك"
    "VERIFYING" -> "يتحقق"
    "CANDIDATE_READY" -> "Candidate جاهز"
    "COMPLETED", DecisionStatuses.APPROVED, TaskStatuses.APPROVED, RadarStatuses.VERIFIED -> "نجح"
    "FAILED", RadarStatuses.FAILED -> "فشل"
    "REJECTED", DecisionStatuses.REJECTED, TaskStatuses.REJECTED -> "مرفوض"
    TaskStatuses.DRAFT -> "مسودة"
    "FREE" -> "FREE"
    "PREPAID" -> "PREPAID"
    "METERED" -> "محظور"
    else -> state
}

private fun providerAvailabilityLabel(providerId: String): String = when (providerId) {
    "auto" -> "يختار مورد تنفيذ عند الطلب الصريح فقط"
    "chatgpt", "gemini", "deepseek" -> "Human-gated — لا محادثة تلقائية"
    else -> ""
}

private fun shortDate(value: Long): String = "\u2066${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(value))}\u2069"

@Composable
private fun EmptyState(title: String, body: String, icon: ImageVector = Icons.Rounded.AddTask) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyStateContent(title, body, icon) }
}

@Composable
private fun EmptyStateContent(title: String, body: String, icon: ImageVector = Icons.Rounded.AddTask) {
    Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
