package ai.hassan.app.data

import android.content.Context
import android.net.Uri
import ai.hassan.app.conversation.AttachmentCodec
import ai.hassan.app.conversation.MessageAttachmentRef
import ai.hassan.app.conversation.ConversationMessage
import ai.hassan.app.conversation.ConversationProvider
import ai.hassan.app.conversation.ConversationSettings
import ai.hassan.app.conversation.ConversationSettingsStore
import ai.hassan.app.conversation.ConversationResult
import ai.hassan.app.conversation.ConversationUiState
import ai.hassan.app.conversation.ConversationIntent
import ai.hassan.app.conversation.IntentRouter
import ai.hassan.app.conversation.PendingAttachment
import ai.hassan.app.execution.ApprovalPhraseParser
import ai.hassan.app.execution.ExecutionEvent
import ai.hassan.app.execution.ExecutionState
import ai.hassan.app.execution.ExecutionStateMachine
import ai.hassan.app.identity.CanonicalPayload
import ai.hassan.app.identity.DeviceIdentityManager
import ai.hassan.app.policy.CostClass
import ai.hassan.app.policy.SpendRequest
import ai.hassan.app.policy.ZeroCostPolicy
import ai.hassan.app.providers.Capability
import ai.hassan.app.providers.CodexReasoningEffort
import ai.hassan.app.providers.DeterministicAutoRouter
import ai.hassan.app.providers.HumanGatedLeadBrain
import ai.hassan.app.providers.LeadBrainContext
import ai.hassan.app.providers.ProviderCatalog
import ai.hassan.app.providers.ProviderDescriptor
import ai.hassan.app.cloud.CloudTaskOrchestrator
import ai.hassan.app.cloud.HassanCloudApi
import ai.hassan.app.radar.RadarScanner
import ai.hassan.app.selfupdate.SelfImproveJobStore
import ai.hassan.app.selfupdate.SelfImprovementAction
import ai.hassan.app.selfupdate.SelfImprovementRouter
import ai.hassan.app.selfupdate.SelfUpdateManager
import ai.hassan.app.selfupdate.SelfUpdateResult
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class HassanRepository(
    private val context: Context,
    private val database: HassanDatabase,
    private val identityManager: DeviceIdentityManager,
    private val radarScanner: RadarScanner,
    private val conversationProvider: ConversationProvider,
    private val activeConversationStore: ActiveConversationStore,
    private val selfUpdateManager: SelfUpdateManager,
    private val conversationSettingsStore: ConversationSettingsStore,
    private val cloudTaskOrchestrator: CloudTaskOrchestrator,
    private val hassanCloudApi: HassanCloudApi,
) {
    private val selfImproveStore = SelfImproveJobStore(context)

    val projects: Flow<List<ProjectEntity>> = database.projectDao().observeAll()
    val tasks: Flow<List<TaskEntity>> = database.taskDao().observeAll()
    val decisions: Flow<List<DecisionEntity>> = database.decisionDao().observeAll()
    val conversations: Flow<List<ConversationEntity>> = database.conversationDao().observeAll()
    val messages: Flow<List<MessageEntity>> = database.messageDao().observeAll()
    val plans: Flow<List<ExecutionPlanEntity>> = database.executionPlanDao().observeAll()
    val bridgeRequests: Flow<List<BridgeRequestEntity>> = database.bridgeRequestDao().observeAll()
    val resources: Flow<List<ResourceLedgerEntity>> = database.resourceLedgerDao().observeAll()
    val radarFindings: Flow<List<RadarFindingEntity>> = database.radarFindingDao().observeAll()

    fun visibleRadarFindings(): Flow<List<RadarFindingEntity>> =
        radarFindings // UI filters rejected items
    val evidenceBundles: Flow<List<EvidenceBundleEntity>> = database.evidenceBundleDao().observeAll()
    val cloudJobs: Flow<List<CloudJobEntity>> = database.cloudJobDao().observeAll()
    val artifacts: Flow<List<ArtifactEntity>> = database.artifactDao().observeAll()

    private val _conversationUi = MutableStateFlow(
        ConversationUiState(providerConfigured = conversationProvider.isConfigured),
    )
    val conversationUi: StateFlow<ConversationUiState> = _conversationUi.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(activeConversationStore.read())
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val selfUpdateEvents = selfUpdateManager.events
    val conversationSettings = conversationSettingsStore.settings

    suspend fun initialize() {
        val now = System.currentTimeMillis()
        if (database.projectDao().count() == 0) {
            database.projectDao().insert(
                ProjectEntity(
                    id = HASSAN_PROJECT_ID,
                    name = "Hassan AI",
                    description = "المشروع الشخصي الأساسي — Personal AI Command Center",
                    createdAt = now,
                ),
            )
        }
        val activeId = activeConversationStore.read()
        if (activeId != null && database.conversationDao().getById(activeId) == null) {
            activeConversationStore.clear()
            _activeConversationId.value = null
        }
        ensureConversation(now)
        seedResourceLedger(now)
        if (database.decisionDao().count() == 0) seedBiometricDecision(now)
        _conversationUi.value = ConversationUiState(
            providerConfigured = conversationProvider.isConfigured,
            statusMessage = if (conversationProvider.isConfigured) {
                "Hassan Cloud متصل — Hassan Auto يختار المزوّد المناسب."
            } else {
                "لم يتم إعداد مزود المحادثة بعد. أدخل رابط Hassan Cloud من الإعدادات."
            },
        )
        if (conversationSettingsStore.isCloudConfigured()) {
            syncCloudJobs()
        }
    }

    private suspend fun ensureConversation(now: Long = System.currentTimeMillis()): ConversationEntity {
        _activeConversationId.value?.let { activeId ->
            database.conversationDao().getById(activeId)?.let { return it }
        }
        database.conversationDao().getLatest()?.let { conversation ->
            setActiveConversation(conversation.id)
            return conversation
        }
        return createConversationRecord(
            title = "المحادثة الرئيسية",
            now = now,
            welcomeMessage = "مرحبًا! أنا Frishta AI. تحدث معي بشكل طبيعي.",
        )
    }

    suspend fun createNewConversation(title: String = "محادثة جديدة"): ConversationEntity {
        val now = System.currentTimeMillis()
        return createConversationRecord(
            title = title,
            now = now,
            welcomeMessage = null,
        )
    }

    suspend fun selectConversation(conversationId: String) {
        database.conversationDao().getById(conversationId) ?: return
        setActiveConversation(conversationId)
    }

    suspend fun deleteConversation(conversationId: String) {
        if (database.conversationDao().count() <= 1) return
        database.conversationDao().deleteById(conversationId)
        if (_activeConversationId.value == conversationId) {
            val fallback = database.conversationDao().getLatest()
            if (fallback != null) {
                setActiveConversation(fallback.id)
            } else {
                activeConversationStore.clear()
                _activeConversationId.value = null
                ensureConversation()
            }
        }
    }

    suspend fun renameConversation(conversationId: String, newTitle: String) {
        val title = newTitle.trim()
        if (title.isBlank()) return
        val conversation = database.conversationDao().getById(conversationId) ?: return
        database.conversationDao().update(
            conversation.copy(title = title, updatedAt = System.currentTimeMillis()),
        )
    }

    suspend fun syncCloudJobs(): Int {
        val orchestrator = cloudTaskOrchestrator
        val count = if (orchestrator is ai.hassan.app.cloud.CloudJobOrchestratorImpl) {
            orchestrator.syncAll().first
        } else {
            0
        }
        processSelfImproveJobUpdates()
        return count
    }

    suspend fun notifySyncResult(jobCount: Int, artifactCount: Int) {
        selfUpdateManager.notifyUser("تمت المزامنة: $jobCount مهمة، $artifactCount ملف")
    }

    suspend fun syncCloudState(): Pair<Int, Int> {
        val orchestrator = cloudTaskOrchestrator
        return if (orchestrator is ai.hassan.app.cloud.CloudJobOrchestratorImpl) {
            val result = orchestrator.syncAll()
            processSelfImproveJobUpdates()
            result
        } else {
            0 to 0
        }
    }

    suspend fun listWorkspaceFiles(projectId: String): List<ai.hassan.app.cloud.CloudWorkspaceFileDto> {
        val orchestrator = cloudTaskOrchestrator
        return if (orchestrator is ai.hassan.app.cloud.CloudJobOrchestratorImpl) {
            orchestrator.listWorkspaceFiles(projectId)
        } else {
            emptyList()
        }
    }

    suspend fun readWorkspaceFile(projectId: String, path: String): Result<String> {
        val orchestrator = cloudTaskOrchestrator
        return if (orchestrator is ai.hassan.app.cloud.CloudJobOrchestratorImpl) {
            orchestrator.readWorkspaceFile(projectId, path)
        } else {
            Result.failure(IllegalStateException("Hassan Cloud غير مُعد"))
        }
    }

    suspend fun cancelCloudJob(jobId: String): Result<Unit> =
        cloudTaskOrchestrator.cancelTask(jobId)

    /** Removes finished cloud jobs from the local list (does not affect GitHub history). */
    suspend fun clearFinishedCloudJobs(): Int {
        val removed = database.cloudJobDao().deleteFinished()
        database.artifactDao().deleteOrphans()
        return removed
    }

    suspend fun deleteCloudJobLocally(jobId: String) {
        database.cloudJobDao().deleteById(jobId)
        database.artifactDao().deleteOrphans()
    }

    suspend fun downloadArtifact(
        artifact: ArtifactEntity,
        onProgress: ((bytesRead: Long, totalBytes: Long?) -> Unit)? = null,
    ): Result<File> {
        val settings = conversationSettingsStore.read()
        if (!conversationSettingsStore.isCloudConfigured()) {
            return Result.failure(IllegalStateException("Hassan Cloud غير مُعد"))
        }
        selfUpdateManager.notifyUser("جارٍ تنزيل ${artifact.name}…")
        val dest = File(context.cacheDir, "artifacts/${artifact.name}")
        dest.parentFile?.mkdirs()
        val knownTotal = artifact.sizeBytes.takeIf { it > 0L }
        val result = hassanCloudApi.downloadArtifact(
            settings.cloudBaseUrl,
            settings.accessToken,
            artifact.id,
            dest,
        ) { read, total ->
            onProgress?.invoke(read, total ?: knownTotal)
        }
        result.onSuccess { file ->
            database.artifactDao().upsert(
                artifact.copy(localPath = file.absolutePath, sizeBytes = file.length().coerceAtLeast(artifact.sizeBytes)),
            )
            selfUpdateManager.notifyUser("اكتمل تنزيل ${artifact.name} (${formatArtifactBytes(file.length())})")
        }
        result.exceptionOrNull()?.let { err ->
            selfUpdateManager.notifyUser("تعذر تنزيل ${artifact.name}: ${err.message ?: "خطأ شبكة"}")
        }
        return result
    }

    suspend fun installCloudApk(artifact: ArtifactEntity): Result<Unit> {
        val local = artifact.localPath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
        val file = local ?: downloadArtifact(artifact).getOrElse { return Result.failure(it) }
        selfUpdateManager.notifyUser("جارٍ تجهيز التثبيت…")
        val results = selfUpdateManager.installUpdateFromFile(file.absolutePath, backupFirst = true)
        val error = results.filterIsInstance<SelfUpdateResult.Error>().firstOrNull()
        if (error != null) {
            selfUpdateManager.notifyUser(error.text)
            return Result.failure(IllegalStateException(error.text))
        }
        return Result.success(Unit)
    }

    suspend fun updateRadarDecision(findingId: String, decision: String) {
        val finding = database.radarFindingDao().getById(findingId) ?: return
        val now = System.currentTimeMillis()
        val status = when (decision) {
            RadarUserDecisions.APPROVE -> RadarCandidateStatuses.APPROVED
            RadarUserDecisions.TEST_ONLY -> RadarCandidateStatuses.TESTING
            RadarUserDecisions.REJECT -> RadarCandidateStatuses.REJECTED
            else -> finding.candidateStatus
        }
        database.radarFindingDao().update(
            finding.copy(
                userDecision = decision,
                candidateStatus = status,
                rejectedAt = if (decision == RadarUserDecisions.REJECT) now else finding.rejectedAt,
            ),
        )
    }

    private suspend fun uploadAttachmentsIfConfigured(
        attachments: List<PendingAttachment>,
        projectId: String,
        conversationId: String,
    ): List<MessageAttachmentRef> {
        val settings = conversationSettingsStore.read()
        if (!conversationSettingsStore.isCloudConfigured()) {
            return attachments.map(AttachmentCodec::fromPending)
        }
        return attachments.map { pending ->
            val base = AttachmentCodec.fromPending(pending)
            val temp = File.createTempFile("hassan-upload-", "-${pending.displayName}", context.cacheDir)
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(pending.uri))?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
                val uploaded = hassanCloudApi.uploadFile(
                    baseUrl = settings.cloudBaseUrl,
                    token = settings.accessToken,
                    file = temp,
                    mimeType = pending.mimeType,
                    projectId = projectId,
                    jobId = null,
                    conversationId = conversationId,
                ).getOrNull()
                if (uploaded != null) {
                    val remote = "${settings.cloudBaseUrl.trimEnd('/')}/v1/files/${uploaded.id}"
                    database.artifactDao().upsert(
                        ArtifactEntity(
                            id = uploaded.id,
                            projectId = uploaded.project_id,
                            jobId = uploaded.job_id,
                            conversationId = uploaded.conversation_id,
                            name = uploaded.name,
                            mimeType = uploaded.mime_type,
                            sizeBytes = uploaded.size_bytes,
                            localPath = null,
                            remoteUrl = remote,
                            createdAt = uploaded.created_at,
                        ),
                    )
                    base.copy(cloudArtifactId = uploaded.id, remoteUrl = remote)
                } else {
                    base
                }
            }.getOrElse { base }.also { temp.delete() }
        }
    }

    private suspend fun createConversationRecord(
        title: String,
        now: Long,
        welcomeMessage: String?,
    ): ConversationEntity {
        val conversation = ConversationEntity(
            id = UUID.randomUUID().toString(),
            projectId = "",
            title = title,
            leadBrainId = AUTO_PROVIDER_ID,
            codexReasoningEffort = CodexReasoningEffort.MEDIUM.apiValue,
            state = ExecutionState.DISCUSSING.name,
            createdAt = now,
            updatedAt = now,
        )
        database.conversationDao().insert(conversation)
        welcomeMessage?.let { message ->
            database.messageDao().insert(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversation.id,
                    role = MessageRoles.HASSAN,
                    content = message,
                    providerId = "frishta",
                    createdAt = now,
                ),
            )
        }
        setActiveConversation(conversation.id)
        return conversation
    }

    private suspend fun setActiveConversation(conversationId: String) {
        activeConversationStore.write(conversationId)
        _activeConversationId.value = conversationId
    }

    suspend fun selectLeadBrain(providerId: String) {
        require(providerId in LEAD_BRAIN_IDS)
        val current = ensureConversation()
        database.conversationDao().update(
            current.copy(leadBrainId = providerId, updatedAt = System.currentTimeMillis()),
        )
    }

    suspend fun selectCodexReasoningEffort(apiValue: String) {
        val effort = CodexReasoningEffort.fromApiValue(apiValue)
        require(effort.apiValue == apiValue)
        val current = ensureConversation()
        database.conversationDao().update(
            current.copy(codexReasoningEffort = effort.apiValue, updatedAt = System.currentTimeMillis()),
        )
    }

    suspend fun sendChat(text: String, attachments: List<PendingAttachment> = emptyList()) {
        val clean = text.trim()
        if (clean.isBlank() && attachments.isEmpty()) return
        val now = System.currentTimeMillis()
        val conversation = ensureConversation(now)
        val attachmentRefs = attachments.takeIf { it.isNotEmpty() }?.let { pending ->
            AttachmentCodec.encode(uploadAttachmentsIfConfigured(pending, conversation.projectId, conversation.id))
        }
        val messageText = clean.ifBlank { attachments.joinToString { it.displayName } }
        database.messageDao().insert(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversation.id,
                role = MessageRoles.USER,
                content = messageText,
                attachmentRefs = attachmentRefs,
                createdAt = now,
            ),
        )
        database.conversationDao().update(
            conversation.copy(
                title = maybeAutoTitle(conversation, messageText),
                updatedAt = now,
            ),
        )

        val state = conversation.executionState()
        if (ApprovalPhraseParser.isApproval(clean)) {
            val awaitingPlan = database.executionPlanDao().latestForConversation(conversation.id)
                ?.takeIf { it.status == ExecutionState.AWAITING_USER_APPROVAL.name }
            if (state == ExecutionState.AWAITING_USER_APPROVAL || awaitingPlan != null) {
                // Restore awaiting state if chat briefly changed it, then execute.
                if (state != ExecutionState.AWAITING_USER_APPROVAL) {
                    database.conversationDao().update(
                        conversation.copy(
                            state = ExecutionState.AWAITING_USER_APPROVAL.name,
                            updatedAt = now,
                        ),
                    )
                }
                approveAndExecute(
                    database.conversationDao().getById(conversation.id) ?: conversation,
                    now,
                )
                return
            }
            addHassanMessage(
                conversation.id,
                "لا توجد خطة بانتظار الاعتماد الآن. اطلب أولًا «حسّن التطبيق: …» لإنشاء خطة، ثم قل «ابدأ».",
            )
            return
        }

        SelfImprovementRouter.classify(clean)?.let { action ->
            handleSelfImprovement(conversation, action, attachments, messageText)
            return
        }

        when (IntentRouter.classify(clean)) {
            ConversationIntent.CHAT,
            ConversationIntent.MEDIA,
            ConversationIntent.AUTOMATION,
            ConversationIntent.RESEARCH,
            -> handleChatMessage(conversation, messageText)
            ConversationIntent.PROJECT,
            ConversationIntent.EXECUTION,
            ConversationIntent.PLAN,
            -> handleExecutionIntent(conversation, messageText, now)
        }
    }

    private fun maybeAutoTitle(conversation: ConversationEntity, firstUserMessage: String): String {
        if (conversation.title != "محادثة جديدة") return conversation.title
        return firstUserMessage.lineSequence().firstOrNull()?.take(48)?.ifBlank { conversation.title }
            ?: conversation.title
    }

    private suspend fun handleSelfImprovement(
        conversation: ConversationEntity,
        action: SelfImprovementAction,
        attachments: List<PendingAttachment>,
        originalGoal: String = "",
    ) {
        when (action) {
            SelfImprovementAction.REQUEST_SELF_IMPROVE -> {
                requestSelfImprovePlan(conversation, originalGoal.ifBlank {
                    attachments.joinToString { it.displayName }
                })
            }
            SelfImprovementAction.APPLY_UPDATE,
            SelfImprovementAction.ROLLBACK,
            SelfImprovementAction.CHECK_UPDATE,
            -> {
                val apkAttachment = attachments.firstOrNull {
                    it.mimeType.contains("apk", ignoreCase = true) ||
                        it.displayName.endsWith(".apk", ignoreCase = true)
                }
                val results = selfUpdateManager.handleSelfImprovement(action, apkAttachment?.uri)
                val reply = buildString {
                    results.forEach { result ->
                        when (result) {
                            is SelfUpdateResult.BackupCreated -> appendLine(
                                "تم حفظ نسخة APK احتياطية: ${result.metadata.versionName} (${result.metadata.versionCode}).",
                            )
                            is SelfUpdateResult.UpdateReady -> appendLine("التحديث جاهز للتثبيت.")
                            is SelfUpdateResult.Message -> appendLine(result.text)
                            is SelfUpdateResult.Error -> appendLine("تنبيه: ${result.text}")
                        }
                    }
                    if (action == SelfImprovementAction.APPLY_UPDATE && apkAttachment == null) {
                        if (!selfUpdateManager.hasStagedUpdate()) {
                            appendLine()
                            appendLine("لا يوجد ملف APK حقيقي جاهز للتثبيت الآن.")
                            appendLine("أي ذكر لاسم APK داخل رد المحادثة لا يكفي.")
                            appendLine("يلزم إما:")
                            appendLine("• إرفاق ملف .apk ثم «ثبت التحديث»، أو")
                            appendLine("• «حسّن التطبيق: …» ثم مباشرة «ابدأ» حتى تكتمل مهمة السحابة ويُنزَّل APK فعلي.")
                        }
                    }
                }.trim()
                addHassanMessage(conversation.id, reply.ifBlank { "تمت معالجة طلب التحديث الذاتي." })
                updateConversationState(conversation, ExecutionState.DISCUSSING)
            }
        }
    }

    private suspend fun requestSelfImprovePlan(conversation: ConversationEntity, goal: String) {
        val now = System.currentTimeMillis()
        val backup = selfUpdateManager.backupCurrentApk()
        val backupLine = when (backup) {
            is SelfUpdateResult.BackupCreated ->
                "نسخة احتياطية محفوظة: ${backup.metadata.versionName} (${backup.metadata.versionCode})."
            is SelfUpdateResult.Error -> "تحذير النسخ الاحتياطي: ${backup.text}"
            else -> "تمت محاولة النسخ الاحتياطي."
        }

        if (!conversationSettingsStore.isCloudConfigured()) {
            addHassanMessage(
                conversation.id,
                buildString {
                    appendLine(backupLine)
                    appendLine()
                    appendLine("طلبت تعديل التطبيق ذاتيًا، لكن Hassan Cloud غير مُعد.")
                    appendLine("المسار الواقعي: السحابة تبني APK → الهاتف يثبت → عند الفشل «ارجع للنسخة السابقة».")
                    appendLine("أعدّ رابط السحابة ورمز الوصول من الإعدادات، أو أرفق APK جاهز وقل «ثبت التحديث».")
                }.trim(),
            )
            updateConversationState(conversation, ExecutionState.DISCUSSING)
            return
        }

        clearObsoletePlans(conversation)
        val awaiting = ExecutionState.AWAITING_USER_APPROVAL
        val plan = ExecutionPlanEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversation.id,
            goal = goal,
            summary = "حلقة التعديل الذاتي لـ Frishta AI (Candidate): نسخ احتياطي → بناء سحابي → تثبيت → تراجع.",
            components = "1) Backup APK محلي\n2) مهمة سحابة job_type=${SelfImproveJobStore.JOB_TYPE}\n3) تنزيل APK وتثبيت بموافقتك\n4) Rollback من النسخة الاحتياطية عند الحاجة",
            risks = "قد يفشل البناء إن لم يتوفر مصدر التطبيق في السحابة، أو يرفض Android التثبيت إن اختلف التوقيع. لن يُمس Stable.",
            verification = "اكتمال مهمة السحابة + ظهور APK + نجاح شاشة التثبيت، أو رسالة فشل صريحة.",
            rollback = "قل «ارجع للنسخة السابقة» أو استخدم زر الرجوع في الإعدادات لتثبيت hassan-previous.apk.",
            capability = SelfImproveJobStore.CAPABILITY,
            status = awaiting.name,
            costClass = CostClass.FREE.name,
            createdAt = now,
        )
        database.executionPlanDao().insert(plan)
        database.conversationDao().update(conversation.copy(state = awaiting.name, updatedAt = now))
        addHassanMessage(
            conversation.id,
            buildString {
                appendLine(backupLine)
                appendLine()
                appendLine("خطة التعديل الذاتي جاهزة لطلبك:")
                appendLine("«${goal.take(200)}»")
                appendLine()
                appendLine("قل «ابدأ» أو «نفّذ» لبدء البناء على السحابة ثم التثبيت.")
                appendLine("عند الفشل أو عدم الرضا: «ارجع للنسخة السابقة».")
            }.trim(),
        )
    }

    suspend fun backupApkNow(): SelfUpdateResult = selfUpdateManager.backupCurrentApk()

    suspend fun rollbackToBackup(): List<SelfUpdateResult> =
        selfUpdateManager.handleSelfImprovement(SelfImprovementAction.ROLLBACK)

    suspend fun checkForAppUpdate(): SelfUpdateResult = selfUpdateManager.checkRemoteUpdate()

    fun hasApkBackup(): Boolean = selfUpdateManager.hasBackup()

    fun updateConversationSettings(settings: ConversationSettings) {
        conversationSettingsStore.update { settings }
        _conversationUi.value = ConversationUiState(
            providerConfigured = conversationSettingsStore.isCloudConfigured(),
            statusMessage = if (conversationSettingsStore.isCloudConfigured()) {
                "Hassan Cloud متصل — Hassan Auto يختار المزوّد المناسب."
            } else {
                "لم يتم إعداد مزود المحادثة بعد. أدخل رابط Hassan Cloud من الإعدادات."
            },
        )
    }

    suspend fun latestRadarFindings(): List<RadarFindingEntity> = radarFindings.first()

    fun apkBackupMetadata() = selfUpdateManager.backupMetadata()

    private suspend fun handleChatMessage(conversation: ConversationEntity, text: String) {
        // Keep plans that wait for «ابدأ» — clarifying chat must not reject them.
        val awaitingApproval =
            conversation.executionState() == ExecutionState.AWAITING_USER_APPROVAL ||
                database.executionPlanDao().latestForConversation(conversation.id)
                    ?.status == ExecutionState.AWAITING_USER_APPROVAL.name
        if (!awaitingApproval) {
            clearObsoletePlans(conversation)
            updateConversationState(conversation, ExecutionState.DISCUSSING)
        }
        _conversationUi.value = _conversationUi.value.copy(isSending = true)

        val history = database.messageDao().listForConversation(conversation.id).map {
            ConversationMessage(role = it.role, content = it.content)
        }

        when (val result = conversationProvider.sendMessage(history, text)) {
            is ConversationResult.Success -> {
                _conversationUi.value = ConversationUiState(
                    providerConfigured = true,
                    isSending = false,
                )
                addHassanMessage(
                    conversation.id,
                    result.answer,
                    providerId = result.providerId ?: conversationSettingsStore.read().chatProvider,
                )
            }
            is ConversationResult.NotConfigured -> {
                _conversationUi.value = ConversationUiState(
                    providerConfigured = false,
                    statusMessage = result.message,
                    isSending = false,
                )
                addHassanMessage(conversation.id, result.message, providerId = "frishta")
            }
            is ConversationResult.Error -> {
                _conversationUi.value = ConversationUiState(
                    providerConfigured = conversationProvider.isConfigured,
                    statusMessage = result.message,
                    isSending = false,
                )
                addHassanMessage(conversation.id, result.message, providerId = "frishta")
            }
        }
    }

    private suspend fun handleProjectIntent(conversation: ConversationEntity, goal: String) {
        handleCloudExecution(conversation, goal)
    }

    private suspend fun handleExecutionIntent(
        conversation: ConversationEntity,
        goal: String,
        now: Long,
    ) {
        val lower = goal.lowercase()
        val looksLikeLocalRadar = lower.contains("رادار") || lower.contains("radar") ||
            lower.contains("افحص الجهاز") || lower.contains("افحص الهاتف")
        if (looksLikeLocalRadar) {
            createOrRevisePlan(conversation, goal, now)
            return
        }
        if (cloudTaskOrchestrator.isConfigured) {
            runCatching { syncCloudJobs() }
            val resolved = resolveCloudProject(conversation, goal)
            if (resolved != null) {
                handleCloudExecution(conversation, goal)
                return
            }
            if (looksLikeBoundableAppWork(goal)) {
                addHassanMessage(
                    conversation.id,
                    "أي مشروع تقصد؟ اذكر اسم المشروع (مثل HassanTodoBenchmark) أو افتحه من المشاريع ثم أعد الطلب.",
                )
                return
            }
        } else if (looksLikeBoundableAppWork(goal)) {
            addHassanMessage(
                conversation.id,
                "للتنفيذ السحابي من المحادثة أعدّ Hassan Cloud من الإعدادات (URL + token). الحالة: NOT_CONFIGURED",
            )
            return
        }
        // Local zero-cost plan / human-gated path for generic engineering asks.
        createOrRevisePlan(conversation, goal, now)
    }

    private fun looksLikeBoundableAppWork(goal: String): Boolean {
        val g = goal.lowercase()
        return listOf(
            "hassantodobenchmark",
            "تطبيق المهام",
            "android",
            "apk",
            "compose",
            "وضع ليلي",
            "night mode",
        ).any { it in g }
    }

    private suspend fun handleCloudExecution(conversation: ConversationEntity, goal: String) {
        if (!cloudTaskOrchestrator.isConfigured) {
            addHassanMessage(
                conversation.id,
                "لإنشاء مهمة سحابية، أعد Hassan Cloud من الإعدادات. الحالة: NOT_CONFIGURED",
            )
            return
        }
        runCatching { syncCloudJobs() }
        val resolved = resolveCloudProject(conversation, goal)
        if (resolved == null) {
            addHassanMessage(
                conversation.id,
                "أي مشروع تقصد؟ اذكر اسم المشروع (مثل HassanTodoBenchmark) أو افتحه من المشاريع ثم أعد الطلب.",
            )
            return
        }
        val (projectId, projectName) = resolved
        val jobType = inferJobType(goal)
        cloudTaskOrchestrator.submitTask(projectId, conversation.id, goal, jobType)
            .onSuccess { ref ->
                database.conversationDao().update(
                    conversation.copy(
                        projectId = projectId,
                        updatedAt = System.currentTimeMillis(),
                        state = ExecutionState.DISCUSSING.name,
                    ),
                )
                addHassanMessage(
                    conversationId = conversation.id,
                    content = "بدأت التنفيذ على $projectName",
                    taskId = ref.taskId,
                )
            }
            .onFailure {
                addHassanMessage(conversation.id, "تعذر بدء المهمة: ${it.message ?: "خطأ غير معروف"}")
            }
    }

    private suspend fun resolveCloudProject(
        conversation: ConversationEntity,
        goal: String,
    ): Pair<String, String>? {
        val projects = database.projectDao().listAll()
        val boundId = conversation.projectId
        if (boundId.isNotBlank() && boundId != HASSAN_PROJECT_ID) {
            val bound = projects.firstOrNull { it.id == boundId }
                ?: database.projectDao().getById(boundId)
            if (bound != null) return bound.id to bound.name
            // Keep bound UUID even if local cache missed name.
            return boundId to "المشروع الحالي"
        }
        projects.firstOrNull { project ->
            goal.contains(project.name, ignoreCase = true)
        }?.let { return it.id to it.name }
        // Friendly aliases for the AgentOS benchmark app.
        if (goal.contains("HassanTodoBenchmark", ignoreCase = true) ||
            goal.contains("تطبيق المهام", ignoreCase = true) ||
            (goal.contains("المهام", ignoreCase = true) && goal.contains("وضع", ignoreCase = true))
        ) {
            projects.firstOrNull { it.name.contains("HassanTodo", ignoreCase = true) }
                ?.let { return it.id to it.name }
        }
        return null
    }

    private fun inferJobType(goal: String): String {
        val g = goal.lowercase()
        val androidHints = listOf(
            "android", "apk", "compose", "todo", "مهام", "ليلي", "night",
            "hassantodobenchmark", "واجهة", "تطبيق",
        )
        return if (androidHints.any { it in g }) "agentos_android" else "workspace_coding"
    }

    private suspend fun clearObsoletePlans(conversation: ConversationEntity) {
        database.executionPlanDao().latestForConversation(conversation.id)?.let { activePlan ->
            // Never auto-reject a plan waiting for explicit user approval.
            if (activePlan.status == ExecutionState.AWAITING_USER_APPROVAL.name) return
            if (activePlan.status != ExecutionState.COMPLETED.name &&
                activePlan.status != ExecutionState.REJECTED.name
            ) {
                database.executionPlanDao().update(activePlan.copy(status = ExecutionState.REJECTED.name))
            }
        }
    }

    private suspend fun createOrRevisePlan(conversation: ConversationEntity, goal: String, now: Long) {
        val intent = IntentRouter.classify(goal)
        val capability = DeterministicAutoRouter.classify(goal, intent)
        val provider = resolveProvider(conversation.leadBrainId, goal, intent)
        if (provider == null) {
            updateConversationState(conversation, ExecutionState.FAILED)
            addHassanMessage(conversation.id, "لا يوجد مورد مجاني أو موجود مسبقًا مناسب الآن. لم أدفع شيئًا وسأنتظر.")
            return
        }
        val policy = ZeroCostPolicy.evaluate(
            SpendRequest(
                providerId = provider.id,
                costClass = provider.costClass,
                estimatedAdditionalCostCents = 0,
                requiresCard = provider.requiresCard,
            ),
        )
        if (!policy.allowed) {
            updateConversationState(conversation, ExecutionState.FAILED)
            addHassanMessage(conversation.id, "أوقفت الخطة: ${policy.reason}")
            return
        }
        val planReady = ExecutionStateMachine.transition(ExecutionState.DISCUSSING, ExecutionEvent.PLAN_CREATED)
        val awaiting = ExecutionStateMachine.transition(planReady, ExecutionEvent.REQUEST_APPROVAL)
        val plan = ExecutionPlanEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversation.id,
            goal = goal,
            summary = planSummary(capability, provider),
            components = planComponents(capability, provider, conversation.codexReasoningEffort),
            risks = if (provider.requiresHumanBridge) "عودة الرد تعتمد على المشاركة الصريحة من المستخدم." else "قد يتعذر المصدر العام أو يبلغ حد الطلبات المجاني.",
            verification = "حالة ثابتة محليًا، حفظ Room، سياسة كلفة صفر، ودليل مصدر قابل للمراجعة.",
            rollback = "إلغاء الطلب وإبقاء Stable دون تغيير؛ كل البيانات الجديدة محلية وقابلة للحذف.",
            capability = capability.name,
            status = awaiting.name,
            costClass = provider.costClass.name,
            createdAt = now,
        )
        database.executionPlanDao().insert(plan)
        database.conversationDao().update(conversation.copy(state = awaiting.name, updatedAt = now))
        addHassanMessage(
            conversation.id,
            "الخطة جاهزة. راجعها ثم قل «ابدأ» أو «نفّذ». المورد: ${provider.name}، والكلفة الإضافية: 0.",
        )
    }

    private suspend fun approveAndExecute(conversation: ConversationEntity, now: Long) {
        val plan = database.executionPlanDao().latestForConversation(conversation.id) ?: return
        val queued = ExecutionStateMachine.transition(conversation.executionState(), ExecutionEvent.USER_APPROVED)
        val executing = ExecutionStateMachine.transition(queued, ExecutionEvent.START_EXECUTION)
        database.executionPlanDao().update(plan.copy(status = queued.name, approvedAt = now))
        database.conversationDao().update(conversation.copy(state = executing.name, updatedAt = now))
        addHassanMessage(conversation.id, "تم اعتماد الخطة محليًا. بدأ التنفيذ ضمن سياسة الكلفة الصفرية.")

        if (plan.capability == SelfImproveJobStore.CAPABILITY) {
            executeSelfImprovePlan(conversation, plan)
            return
        }

        val provider = resolveProvider(conversation.leadBrainId, plan.goal)
        if (provider == null || provider.costClass == CostClass.METERED) {
            updateConversationState(conversation, ExecutionState.FAILED)
            addHassanMessage(conversation.id, "توقف التنفيذ: لا يوجد fallback مسموح. الكلفة بقيت 0.")
            return
        }
        if (provider.id == ProviderCatalog.localRadar.id) {
            executeRadarPlan(conversation, plan, provider, now)
        } else {
            createHumanBridge(conversation, plan, provider, now)
        }
    }

    private suspend fun executeSelfImprovePlan(
        conversation: ConversationEntity,
        plan: ExecutionPlanEntity,
    ) {
        if (!conversationSettingsStore.isCloudConfigured()) {
            updateConversationState(conversation, ExecutionState.FAILED)
            addHassanMessage(
                conversation.id,
                "تعذر بدء التعديل الذاتي: Hassan Cloud غير مُعد. النسخة الاحتياطية ما زالت متاحة عبر «ارجع للنسخة السابقة».",
            )
            return
        }
        val projectId = ensureSelfImproveCloudProject()
        if (projectId == null) {
            updateConversationState(conversation, ExecutionState.FAILED)
            addHassanMessage(
                conversation.id,
                "تعذر إنشاء/العثور على مشروع السحابة للتعديل الذاتي. تحقق من اتصال Hassan Cloud.",
            )
            return
        }
        cloudTaskOrchestrator.submitTask(
            projectId = projectId,
            conversationId = conversation.id,
            goal = plan.goal,
            jobType = SelfImproveJobStore.JOB_TYPE,
        ).onSuccess { ref ->
            selfImproveStore.rememberPendingJob(conversation.id, ref.taskId)
            database.conversationDao().update(
                conversation.copy(
                    projectId = projectId,
                    updatedAt = System.currentTimeMillis(),
                    state = ExecutionState.EXECUTING.name,
                ),
            )
            database.executionPlanDao().update(plan.copy(status = ExecutionState.EXECUTING.name))
            addHassanMessage(
                conversationId = conversation.id,
                content = buildString {
                    appendLine("بدأت مهمة التعديل الذاتي على السحابة.")
                    appendLine("Job: ${ref.taskId}")
                    appendLine("النوع: ${SelfImproveJobStore.JOB_TYPE}")
                    appendLine("عند اكتمال البناء سأحاول تنزيل APK وطلب التثبيت تلقائيًا.")
                    appendLine("يمكنك مزامنة المهام من المشاريع، أو انتظار المزامنة التلقائية.")
                }.trim(),
                taskId = ref.taskId,
            )
        }.onFailure {
            updateConversationState(conversation, ExecutionState.FAILED)
            addHassanMessage(
                conversation.id,
                "تعذر بدء مهمة السحابة: ${it.message ?: "خطأ غير معروف"}. يمكنك «ارجع للنسخة السابقة» إن لزم.",
            )
        }
    }

    private suspend fun ensureSelfImproveCloudProject(): String? {
        val settings = conversationSettingsStore.read()
        if (!conversationSettingsStore.isCloudConfigured()) return null
        runCatching {
            val orchestrator = cloudTaskOrchestrator
            if (orchestrator is ai.hassan.app.cloud.CloudJobOrchestratorImpl) {
                orchestrator.syncProjects()
            }
        }
        database.projectDao().listAll()
            .firstOrNull { it.name.equals(SelfImproveJobStore.CLOUD_PROJECT_NAME, ignoreCase = true) }
            ?.let { return it.id }

        return hassanCloudApi.createProject(
            baseUrl = settings.cloudBaseUrl,
            token = settings.accessToken,
            name = SelfImproveJobStore.CLOUD_PROJECT_NAME,
            description = "Self-improve builds for Frishta AI candidate",
        ).map { dto ->
            database.projectDao().insert(
                ProjectEntity(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description.ifBlank { "Frishta self-improve" },
                    createdAt = dto.created_at,
                ),
            )
            dto.id
        }.getOrNull()
    }

    private suspend fun processSelfImproveJobUpdates() {
        val pendingJobId = selfImproveStore.pendingJobId() ?: return
        val conversationId = selfImproveStore.pendingConversationId() ?: return
        val job = database.cloudJobDao().getById(pendingJobId) ?: return
        val previous = selfImproveStore.lastAnnouncedState(pendingJobId)
        if (previous == job.state) return

        when (job.state.uppercase()) {
            "COMPLETED" -> {
                val apk = database.artifactDao().listForJob(pendingJobId)
                    .firstOrNull {
                        it.name.endsWith(".apk", ignoreCase = true) ||
                            it.mimeType.contains("android.package", ignoreCase = true)
                    }
                if (apk == null) {
                    // Artifacts may lag one sync behind the job state — retry next sync.
                    return
                }
                selfImproveStore.markAnnounced(pendingJobId, job.state)
                addHassanMessage(
                    conversationId,
                    "اكتمل البناء السحابي. جارٍ تنزيل ${apk.name} مع إعادة المحاولة عند انقطاع الشبكة…",
                )
                val downloaded = downloadArtifact(apk)
                downloaded.onSuccess { file ->
                    val results = selfUpdateManager.installUpdateFromFile(file.absolutePath, backupFirst = true)
                    val reply = buildString {
                        results.forEach { result ->
                            when (result) {
                                is SelfUpdateResult.BackupCreated -> appendLine(
                                    "نسخة احتياطية: ${result.metadata.versionName} (${result.metadata.versionCode}).",
                                )
                                is SelfUpdateResult.UpdateReady -> appendLine("APK جاهز: ${result.apkPath}")
                                is SelfUpdateResult.Message -> appendLine(result.text)
                                is SelfUpdateResult.Error -> appendLine("تنبيه: ${result.text}")
                            }
                        }
                        appendLine("إذا فشل التثبيت أو لم يعجبك التغيير: قل «ارجع للنسخة السابقة».")
                    }.trim()
                    addHassanMessage(conversationId, reply)
                    database.executionPlanDao().latestForConversation(conversationId)?.let {
                        if (it.capability == SelfImproveJobStore.CAPABILITY) {
                            database.executionPlanDao().update(it.copy(status = ExecutionState.COMPLETED.name))
                        }
                    }
                    updateConversationState(
                        database.conversationDao().getById(conversationId) ?: return,
                        ExecutionState.COMPLETED,
                    )
                    selfImproveStore.clearPending()
                }.onFailure { err ->
                    addHassanMessage(
                        conversationId,
                        buildString {
                            appendLine("اكتمل البناء لكن تعذر تنزيل APK بعد عدة محاولات: ${err.message ?: "خطأ شبكة"}.")
                            appendLine("افتح «المهام» ثم «مزامنة» واضغط تنزيل على ${apk.name}، أو قل «ثبت التحديث» بعد التنزيل.")
                        }.trim(),
                    )
                    selfImproveStore.markAnnounced(pendingJobId, "COMPLETED_DOWNLOAD_FAILED")
                }
            }
            "FAILED", "CANCELLED" -> {
                selfImproveStore.markAnnounced(pendingJobId, job.state)
                addHassanMessage(
                    conversationId,
                    buildString {
                        appendLine("فشلت مهمة التعديل الذاتي (${job.state}).")
                        appendLine(job.resultSummary.orEmpty().ifBlank { "لا يوجد ملخص من السحابة." })
                        appendLine("التطبيق الحالي لم يُستبدل. للتراجع عن أي تثبيت لاحق: «ارجع للنسخة السابقة».")
                    }.trim(),
                )
                database.executionPlanDao().latestForConversation(conversationId)?.let {
                    if (it.capability == SelfImproveJobStore.CAPABILITY) {
                        database.executionPlanDao().update(it.copy(status = ExecutionState.FAILED.name))
                    }
                }
                updateConversationState(
                    database.conversationDao().getById(conversationId) ?: return,
                    ExecutionState.FAILED,
                )
                selfImproveStore.clearPending()
            }
            else -> {
                if (previous == null) {
                    selfImproveStore.markAnnounced(pendingJobId, job.state)
                    addHassanMessage(
                        conversationId,
                        "حالة مهمة التعديل الذاتي: ${job.state}",
                    )
                } else if (previous != job.state) {
                    selfImproveStore.markAnnounced(pendingJobId, job.state)
                    addHassanMessage(
                        conversationId,
                        "تحديث حالة البناء: ${job.state}",
                    )
                }
            }
        }
    }

    private suspend fun executeRadarPlan(
        conversation: ConversationEntity,
        plan: ExecutionPlanEntity,
        provider: ProviderDescriptor,
        startedAt: Long,
    ) {
        val findings = radarScanner.scan()
        database.radarFindingDao().upsertAll(findings)
        val verifiedCount = findings.count { it.status == RadarStatuses.VERIFIED }
        val verifying = ExecutionStateMachine.transition(ExecutionState.EXECUTING, ExecutionEvent.START_VERIFICATION)
        val completed = ExecutionStateMachine.transition(verifying, ExecutionEvent.COMPLETE)
        updateConversationState(conversation, completed)
        addHassanMessage(
            conversation.id,
            "اكتمل فحص الرادار: $verifiedCount مصادر مجانية موثقة من ${findings.size}. لم يُفعّل أي مزوّد تلقائيًا.",
        )
        createEvidence(
            conversation = conversation,
            plan = plan,
            provider = provider,
            startedAt = startedAt,
            result = "Radar verified $verifiedCount/${findings.size} sources and persisted the findings.",
            provenance = findings.joinToString { it.sourceEvidence },
        )
    }

    private suspend fun createHumanBridge(
        conversation: ConversationEntity,
        plan: ExecutionPlanEntity,
        provider: ProviderDescriptor,
        now: Long,
    ) {
        val taskId = createTask(plan.goal.take(72), plan.goal, "CHAT_PLAN")
        val gated = HumanGatedLeadBrain(
            provider,
            CodexReasoningEffort.fromApiValue(conversation.codexReasoningEffort),
        )
        val context = LeadBrainContext(conversation.id, plan.goal, ExecutionState.EXECUTING)
        val taskPack = gated.buildTaskPack(context, taskId)
        database.bridgeRequestDao().insert(
            BridgeRequestEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                conversationId = conversation.id,
                providerId = provider.id,
                taskPackText = taskPack.asShareText(),
                status = BridgeStatuses.PENDING,
                createdAt = now,
            ),
        )
        val needsInput = ExecutionStateMachine.transition(ExecutionState.EXECUTING, ExecutionEvent.REQUEST_INPUT)
        updateConversationState(conversation, needsInput)
        addHassanMessage(
            conversation.id,
            "جهزت TaskPack لـ${provider.name}. الإرسال والعودة يتمان بموافقتك الصريحة، دون scraping أو Accessibility أو API مدفوعة.",
        )
    }

    suspend fun runRadarNow(silent: Boolean = false) {
        val findings = radarScanner.scan()
        database.radarFindingDao().upsertAll(findings)
        if (!silent) {
            val conversation = ensureConversation()
            addHassanMessage(
                conversation.id,
                "فحص الرادار حفظ ${findings.count { it.status == RadarStatuses.VERIFIED }} نتائج موثقة. لم تُفعّل أي نتيجة تلقائيًا.",
            )
        }
    }

    suspend fun createTask(title: String, payload: String = title, source: String = "APP"): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        database.taskDao().insert(
            TaskEntity(
                id = id,
                projectId = HASSAN_PROJECT_ID,
                title = title.trim(),
                payload = payload.trim(),
                source = source,
                status = TaskStatuses.DRAFT,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    suspend fun ingestShare(text: String?, imageUri: String?) {
        val payload = listOfNotNull(text?.takeIf { it.isNotBlank() }, imageUri?.let { "image:$it" }).joinToString("\n")
        if (payload.isBlank()) return
        val title = text?.lineSequence()?.firstOrNull()?.take(72)?.takeIf { it.isNotBlank() }
            ?: "صورة واردة من المشاركة"
        createTask(title = title, payload = payload, source = "SHARE_SHEET")

        val pending = database.bridgeRequestDao().latestPending() ?: return
        if (text.isNullOrBlank()) return
        val now = System.currentTimeMillis()
        database.messageDao().insert(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = pending.conversationId,
                role = MessageRoles.PROVIDER,
                providerId = pending.providerId,
                taskId = pending.taskId,
                content = text.trim(),
                createdAt = now,
            ),
        )
        database.bridgeRequestDao().update(pending.copy(status = BridgeStatuses.RETURNED, responseAt = now))
        val conversation = database.conversationDao().getById(pending.conversationId) ?: return
        val verifying = ExecutionStateMachine.transition(conversation.executionState(), ExecutionEvent.INPUT_RECEIVED)
        val completed = ExecutionStateMachine.transition(verifying, ExecutionEvent.COMPLETE)
        updateConversationState(conversation, completed)
        val plan = database.executionPlanDao().latestForConversation(conversation.id) ?: return
        val provider = ProviderCatalog.all.firstOrNull { it.id == pending.providerId } ?: return
        createEvidence(
            conversation = conversation,
            plan = plan,
            provider = provider,
            startedAt = pending.createdAt,
            result = "Human-gated response returned explicitly through Android Share Sheet.",
            provenance = "USER_EXPLICIT_SHARE",
        )
    }

    suspend fun approveDecision(decisionId: String): Boolean {
        val decision = database.decisionDao().getById(decisionId) ?: return false
        if (decision.status != DecisionStatuses.PENDING) return false
        val decidedAt = System.currentTimeMillis()
        val payload = CanonicalPayload.decision(decision.id, "APPROVE", decidedAt)
        val signed = identityManager.sign(payload)
        val verified = identityManager.verify(payload, signed.signatureBase64)
        database.decisionDao().update(
            decision.copy(status = DecisionStatuses.APPROVED, signatureBase64 = signed.signatureBase64, signatureVerified = verified, decidedAt = decidedAt),
        )
        database.taskDao().getById(decision.taskId)?.let {
            database.taskDao().update(it.copy(status = if (verified) TaskStatuses.APPROVED else TaskStatuses.REJECTED, updatedAt = decidedAt))
        }
        return verified
    }

    suspend fun rejectDecision(decisionId: String) {
        val decision = database.decisionDao().getById(decisionId) ?: return
        val now = System.currentTimeMillis()
        database.decisionDao().update(decision.copy(status = DecisionStatuses.REJECTED, decidedAt = now))
        database.taskDao().getById(decision.taskId)?.let {
            database.taskDao().update(it.copy(status = TaskStatuses.REJECTED, updatedAt = now))
        }
    }

    private suspend fun seedBiometricDecision(now: Long) {
        val taskId = UUID.randomUUID().toString()
        database.taskDao().insert(
            TaskEntity(
                id = taskId, projectId = HASSAN_PROJECT_ID, title = "تجربة هوية الجهاز",
                payload = "اعتماد قرار تجريبي بعد التحقق البيومتري وتوقيعه بمفتاح الجهاز.",
                source = "BOOTSTRAP", status = TaskStatuses.WAITING_DECISION, createdAt = now, updatedAt = now,
            ),
        )
        database.decisionDao().insert(
            DecisionEntity(
                id = UUID.randomUUID().toString(), taskId = taskId, title = "اعتماد قرار تجريبي",
                summary = "تحقق بيومتري ثم توقيع ECDSA غير قابل للتصدير والتحقق محليًا.",
                risk = "DEMO", status = DecisionStatuses.PENDING, createdAt = now,
            ),
        )
    }

    private suspend fun seedResourceLedger(now: Long) {
        database.resourceLedgerDao().upsertAll(
            ProviderCatalog.all.map { provider ->
                ResourceLedgerEntity(
                    providerId = provider.id, displayName = provider.name, costClass = provider.costClass.name,
                    actualMoneyCostCents = 0, quotaRemaining = provider.quota, quotaResetAt = provider.quotaReset,
                    rateLimit = provider.quota, privacy = provider.privacyGrade,
                    trainingPolicy = "راجع سياسة المصدر قبل كل ربط آلي",
                    retention = "لا يُخزن داخل Hassan إلا الرد المعاد صراحةً", securityGrade = provider.privacyGrade,
                    reliability7d = provider.reliability, reliability30d = provider.reliability, latency = provider.latency,
                    qualityScore = 0.0, commercialUse = provider.commercialUse, license = provider.licenseInfo,
                    geoEligibility = provider.geoEligibility, cardRequired = provider.requiresCard,
                    subscriptionRequired = provider.requiresSubscription, requiresHumanBridge = provider.requiresHumanBridge,
                    enabled = provider.enabled, lastVerifiedAt = now,
                    sourceEvidence = when (provider.id) {
                        "chatgpt" -> "https://developers.openai.com/api/docs/models/gpt-5.6-sol"
                        "official-radar" -> "https://docs.github.com/rest/releases/releases"
                        else -> "Local Milestone 2 provider declaration"
                    },
                )
            },
        )
    }

    private suspend fun createEvidence(
        conversation: ConversationEntity,
        plan: ExecutionPlanEntity,
        provider: ProviderDescriptor,
        startedAt: Long,
        result: String,
        provenance: String,
    ) {
        val now = System.currentTimeMillis()
        val bundle = EvidenceBundleEntity(
            id = UUID.randomUUID().toString(), taskId = "conversation:${conversation.id}", runId = UUID.randomUUID().toString(),
            conversationId = conversation.id, leadBrain = conversation.leadBrainId, workerProvider = provider.id,
            planId = plan.id, approval = "DETERMINISTIC_LOCAL_APPROVAL", startedAt = startedAt, finishedAt = now,
            branch = "candidate-local", commit = "not-created-by-runtime", tests = "Runtime state and persistence checks",
            lint = "Build-time verifier only", security = "ZeroCostPolicy=PASS", artifact = "none", sha256 = "none",
            logs = result, knownRisks = plan.risks, rollback = plan.rollback, costClass = provider.costClass.name,
            actualCostCents = 0, sourceProvenance = provenance,
        )
        check(bundle.actualCostCents == 0L)
        database.evidenceBundleDao().insert(bundle)
    }

    private suspend fun updateConversationState(conversation: ConversationEntity, state: ExecutionState) {
        val current = database.conversationDao().getById(conversation.id) ?: conversation
        database.conversationDao().update(current.copy(state = state.name, updatedAt = System.currentTimeMillis()))
    }

    private suspend fun addHassanMessage(
        conversationId: String,
        content: String,
        taskId: String? = null,
        providerId: String? = "frishta",
    ) {
        database.messageDao().insert(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRoles.HASSAN,
                content = content,
                providerId = providerId,
                taskId = taskId,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun resolveProvider(
        leadBrainId: String,
        goal: String,
        intent: ConversationIntent = ConversationIntent.EXECUTION,
    ): ProviderDescriptor? =
        if (leadBrainId == AUTO_PROVIDER_ID) DeterministicAutoRouter.route(goal, intent)
        else ProviderCatalog.all.firstOrNull { it.id == leadBrainId && it.enabled && it.costClass != CostClass.METERED }

    private fun planSummary(capability: Capability, provider: ProviderDescriptor): String = when (capability) {
        Capability.DIAGNOSTIC, Capability.RESEARCH, Capability.WEB -> "فحص مصادر رسمية مجانية، حفظ الأدلة، ثم عرض النتائج دون تفعيل تلقائي."
        Capability.REVIEW -> "إرسال سياق مضبوط إلى مراجع مستقل وإعادة الرد صراحةً إلى Hassan."
        Capability.CODING, Capability.BUILD, Capability.ANDROID_TEST -> "تجهيز TaskPack هندسي، انتظار عامل مسموح، ثم التحقق خارج مساحة العامل."
        else -> "تنفيذ أصغر مسار قابل للتحقق عبر ${provider.name}."
    }

    private fun planComponents(
        capability: Capability,
        provider: ProviderDescriptor,
        codexReasoningEffort: String,
    ): String = buildString {
        append("القدرة: ${capability.name}\nالمورد: ${provider.name}")
        provider.modelId?.let {
            val effort = CodexReasoningEffort.fromApiValue(codexReasoningEffort)
            append("\nالنموذج المطلوب: $it\nمستوى التفكير: ${effort.arabicLabel} (${effort.apiValue})")
        }
        append("\nالمسار: ${if (provider.requiresHumanBridge) "Human-Gated Bridge" else "محلي/مصدر عام"}")
    }

    private fun ConversationEntity.executionState(): ExecutionState =
        runCatching { ExecutionState.valueOf(state) }.getOrDefault(ExecutionState.DISCUSSING)

    companion object {
        const val HASSAN_PROJECT_ID = "hassan-ai"
        const val AUTO_PROVIDER_ID = "auto"
        val LEAD_BRAIN_IDS = setOf(AUTO_PROVIDER_ID, "chatgpt", "gemini", "deepseek")
    }
}

internal fun formatArtifactBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
