package ai.hassan.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.hassan.app.cloud.CloudWorkspaceFileDto
import ai.hassan.app.conversation.ConversationSettings
import ai.hassan.app.conversation.ConversationUiState
import ai.hassan.app.conversation.PendingAttachment
import ai.hassan.app.data.BridgeRequestEntity
import ai.hassan.app.data.ConversationEntity
import ai.hassan.app.data.DecisionEntity
import ai.hassan.app.data.EvidenceBundleEntity
import ai.hassan.app.data.ExecutionPlanEntity
import ai.hassan.app.data.HassanRepository
import ai.hassan.app.data.MessageEntity
import ai.hassan.app.data.ProjectEntity
import ai.hassan.app.data.RadarFindingEntity
import ai.hassan.app.data.ResourceLedgerEntity
import ai.hassan.app.data.TaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkspaceFilePreview(
    val projectId: String,
    val path: String,
    val content: String,
)

data class HassanUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val decisions: List<DecisionEntity> = emptyList(),
    val conversations: List<ConversationEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val plans: List<ExecutionPlanEntity> = emptyList(),
    val bridgeRequests: List<BridgeRequestEntity> = emptyList(),
    val resources: List<ResourceLedgerEntity> = emptyList(),
    val radarFindings: List<RadarFindingEntity> = emptyList(),
    val evidenceBundles: List<EvidenceBundleEntity> = emptyList(),
    val cloudJobs: List<ai.hassan.app.data.CloudJobEntity> = emptyList(),
    val artifacts: List<ai.hassan.app.data.ArtifactEntity> = emptyList(),
    val workspaceFilesByProject: Map<String, List<CloudWorkspaceFileDto>> = emptyMap(),
    val workspaceFilePreview: WorkspaceFilePreview? = null,
    val conversationUi: ConversationUiState = ConversationUiState(),
    val conversationSettings: ConversationSettings = ConversationSettings(),
    val activeConversationId: String? = null,
)

private data class CoreState(
    val projects: List<ProjectEntity>,
    val tasks: List<TaskEntity>,
    val decisions: List<DecisionEntity>,
    val conversations: List<ConversationEntity>,
    val messages: List<MessageEntity>,
)

private data class ExecutionData(
    val plans: List<ExecutionPlanEntity>,
    val bridges: List<BridgeRequestEntity>,
    val resources: List<ResourceLedgerEntity>,
    val findings: List<RadarFindingEntity>,
    val evidence: List<EvidenceBundleEntity>,
    val cloudJobs: List<ai.hassan.app.data.CloudJobEntity>,
    val artifacts: List<ai.hassan.app.data.ArtifactEntity>,
)

class MainViewModel(
    private val repository: HassanRepository,
) : ViewModel() {
    private val workspaceFilesByProject = MutableStateFlow<Map<String, List<CloudWorkspaceFileDto>>>(emptyMap())
    private val workspaceFilePreview = MutableStateFlow<WorkspaceFilePreview?>(null)

    private val coreState = combine(
        repository.projects,
        repository.tasks,
        repository.decisions,
        repository.conversations,
        repository.messages,
    ) { projects, tasks, decisions, conversations, messages ->
        CoreState(projects, tasks, decisions, conversations, messages)
    }

    private val executionData = combine(
        combine(
            repository.plans,
            repository.bridgeRequests,
            repository.resources,
            repository.radarFindings,
            repository.evidenceBundles,
        ) { plans, bridges, resources, findings, evidence ->
            ExecutionData(
                plans = plans,
                bridges = bridges,
                resources = resources,
                findings = findings,
                evidence = evidence,
                cloudJobs = emptyList(),
                artifacts = emptyList(),
            )
        },
        combine(repository.cloudJobs, repository.artifacts) { cloudJobs, artifacts ->
            cloudJobs to artifacts
        },
    ) { partial, cloudAndArtifacts ->
        partial.copy(
            cloudJobs = cloudAndArtifacts.first,
            artifacts = cloudAndArtifacts.second,
        )
    }

    private val workspaceUi = combine(workspaceFilesByProject, workspaceFilePreview) { files, preview ->
        files to preview
    }

    val state = combine(
        combine(
            coreState,
            executionData,
            repository.conversationUi,
            repository.activeConversationId,
            repository.conversationSettings,
        ) { core, execution, conversationUi, activeConversationId, conversationSettings ->
            HassanUiState(
                projects = core.projects,
                tasks = core.tasks,
                decisions = core.decisions,
                conversations = core.conversations,
                messages = core.messages,
                plans = execution.plans,
                bridgeRequests = execution.bridges,
                resources = execution.resources,
                radarFindings = execution.findings,
                evidenceBundles = execution.evidence,
                cloudJobs = execution.cloudJobs,
                artifacts = execution.artifacts,
                conversationUi = conversationUi,
                conversationSettings = conversationSettings,
                activeConversationId = activeConversationId,
            )
        },
        workspaceUi,
    ) { base, workspace ->
        base.copy(
            workspaceFilesByProject = workspace.first,
            workspaceFilePreview = workspace.second,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HassanUiState(),
    )

    fun sendMessage(text: String, attachments: List<PendingAttachment> = emptyList()) {
        if (text.isBlank() && attachments.isEmpty()) return
        viewModelScope.launch { repository.sendChat(text, attachments) }
    }

    fun createNewChat() {
        viewModelScope.launch { repository.createNewConversation() }
    }

    fun selectConversation(conversationId: String) {
        viewModelScope.launch { repository.selectConversation(conversationId) }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch { repository.deleteConversation(conversationId) }
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch { repository.renameConversation(conversationId, newTitle) }
    }

    fun syncCloudJobs() {
        viewModelScope.launch {
            repository.syncCloudState()
            // Room Flow may lag one frame; refresh using freshly synced job project IDs too.
            kotlinx.coroutines.delay(250)
            val ids = (
                state.value.projects.map { it.id } +
                    state.value.cloudJobs.map { it.cloudProjectId }
                ).distinct()
            ids.forEach { id ->
                val files = repository.listWorkspaceFiles(id)
                workspaceFilesByProject.update { current -> current + (id to files) }
            }
        }
    }

    fun refreshWorkspaceFiles(projectId: String? = null) {
        viewModelScope.launch {
            val targets = if (projectId.isNullOrBlank()) {
                (
                    state.value.projects.map { it.id } +
                        state.value.cloudJobs.map { it.cloudProjectId }
                    ).distinct()
            } else {
                listOf(projectId)
            }
            targets.forEach { id ->
                val files = repository.listWorkspaceFiles(id)
                workspaceFilesByProject.update { current -> current + (id to files) }
            }
        }
    }

    fun openWorkspaceFile(projectId: String, path: String) {
        viewModelScope.launch {
            repository.readWorkspaceFile(projectId, path)
                .onSuccess { content ->
                    workspaceFilePreview.value = WorkspaceFilePreview(projectId, path, content)
                }
                .onFailure {
                    workspaceFilePreview.value = WorkspaceFilePreview(
                        projectId = projectId,
                        path = path,
                        content = "تعذر قراءة الملف: ${it.message ?: "خطأ غير معروف"}",
                    )
                }
        }
    }

    fun clearWorkspaceFilePreview() {
        workspaceFilePreview.value = null
    }

    fun cancelCloudJob(jobId: String) {
        viewModelScope.launch {
            repository.cancelCloudJob(jobId)
            repository.syncCloudState()
        }
    }

    fun clearFinishedCloudJobs() {
        viewModelScope.launch { repository.clearFinishedCloudJobs() }
    }

    fun deleteCloudJobLocally(jobId: String) {
        viewModelScope.launch { repository.deleteCloudJobLocally(jobId) }
    }

    fun downloadArtifact(artifact: ai.hassan.app.data.ArtifactEntity) {
        viewModelScope.launch { repository.downloadArtifact(artifact) }
    }

    fun installCloudApk(artifact: ai.hassan.app.data.ArtifactEntity) {
        viewModelScope.launch { repository.installCloudApk(artifact) }
    }

    fun updateRadarDecision(findingId: String, decision: String) {
        viewModelScope.launch { repository.updateRadarDecision(findingId, decision) }
    }

    fun selectLeadBrain(providerId: String) {
        viewModelScope.launch { repository.selectLeadBrain(providerId) }
    }

    fun selectCodexReasoningEffort(apiValue: String) {
        viewModelScope.launch { repository.selectCodexReasoningEffort(apiValue) }
    }

    fun runRadar() {
        viewModelScope.launch { repository.runRadarNow() }
    }

    fun createTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.createTask(title) }
    }

    fun ingestShare(text: String?, imageUri: String?) {
        viewModelScope.launch { repository.ingestShare(text, imageUri) }
    }

    fun approveDecision(decisionId: String) {
        viewModelScope.launch { repository.approveDecision(decisionId) }
    }

    fun rejectDecision(decisionId: String) {
        viewModelScope.launch { repository.rejectDecision(decisionId) }
    }

    fun updateConversationSettings(settings: ConversationSettings) {
        repository.updateConversationSettings(settings)
    }

    fun backupApkNow() {
        viewModelScope.launch { repository.backupApkNow() }
    }

    fun rollbackApk() {
        viewModelScope.launch { repository.rollbackToBackup() }
    }

    fun checkAppUpdate() {
        viewModelScope.launch { repository.checkForAppUpdate() }
    }

    fun hasApkBackup(): Boolean = repository.hasApkBackup()

    fun apkBackupLabel(): String {
        val meta = repository.apkBackupMetadata()
        return meta?.let { "${it.versionName} · ${shortBackupDate(it.backedUpAt)}" } ?: "لا توجد نسخة"
    }

    private fun shortBackupDate(value: Long): String =
        java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(value))

    class Factory(
        private val repository: HassanRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
    }
}
