package ai.hassan.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object TaskStatuses {
    const val DRAFT = "DRAFT"
    const val QUEUED = "QUEUED"
    const val RUNNING = "RUNNING"
    const val WAITING_DECISION = "WAITING_DECISION"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

object DecisionStatuses {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

object MessageRoles {
    const val USER = "USER"
    const val HASSAN = "HASSAN"
    const val PROVIDER = "PROVIDER"
    const val SYSTEM = "SYSTEM"
}

object BridgeStatuses {
    const val PENDING = "PENDING"
    const val RETURNED = "RETURNED"
    const val CANCELLED = "CANCELLED"
}

object RadarStatuses {
    const val VERIFIED = "VERIFIED"
    const val FAILED = "FAILED"
}

object RadarCandidateStatuses {
    const val NEW = "NEW"
    const val EVALUATING = "EVALUATING"
    const val TESTING = "TESTING"
    const val APPROVED = "APPROVED"
    const val TEST_ONLY = "TEST_ONLY"
    const val REJECTED = "REJECTED"
    const val INTEGRATED = "INTEGRATED"
}

object RadarUserDecisions {
    const val NONE = ""
    const val APPROVE = "APPROVE"
    const val TEST_ONLY = "TEST_ONLY"
    const val REJECT = "REJECT"
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val payload: String,
    val source: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val summary: String,
    val risk: String,
    val status: String,
    val signatureBase64: String? = null,
    val signatureVerified: Boolean? = null,
    val decidedAt: Long? = null,
    val createdAt: Long,
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val leadBrainId: String,
    val codexReasoningEffort: String,
    val state: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val providerId: String? = null,
    val taskId: String? = null,
    val attachmentRefs: String? = null,
    val createdAt: Long,
)

@Entity(tableName = "execution_plans")
data class ExecutionPlanEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val goal: String,
    val summary: String,
    val components: String,
    val risks: String,
    val verification: String,
    val rollback: String,
    val capability: String,
    val status: String,
    val costClass: String,
    val createdAt: Long,
    val approvedAt: Long? = null,
)

@Entity(tableName = "bridge_requests")
data class BridgeRequestEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val conversationId: String,
    val providerId: String,
    val taskPackText: String,
    val status: String,
    val createdAt: Long,
    val responseAt: Long? = null,
)

@Entity(tableName = "resource_ledger")
data class ResourceLedgerEntity(
    @PrimaryKey val providerId: String,
    val displayName: String,
    val costClass: String,
    val actualMoneyCostCents: Long,
    val quotaRemaining: String,
    val quotaResetAt: String?,
    val rateLimit: String,
    val privacy: String,
    val trainingPolicy: String,
    val retention: String,
    val securityGrade: String,
    val reliability7d: Double,
    val reliability30d: Double,
    val latency: String,
    val qualityScore: Double,
    val commercialUse: String,
    val license: String,
    val geoEligibility: String,
    val cardRequired: Boolean,
    val subscriptionRequired: Boolean,
    val requiresHumanBridge: Boolean,
    val enabled: Boolean,
    val lastVerifiedAt: Long,
    val sourceEvidence: String,
)

@Entity(tableName = "radar_findings")
data class RadarFindingEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val title: String,
    val summary: String,
    val sourceUrl: String,
    val version: String,
    val status: String,
    val costClass: String,
    val license: String,
    val discoveredAt: Long,
    val lastVerifiedAt: Long,
    val sourceEvidence: String,
    val candidateStatus: String = RadarCandidateStatuses.NEW,
    val candidateType: String = "",
    val capabilities: String = "",
    val userDecision: String = RadarUserDecisions.NONE,
    val radarScore: Float = 0f,
    val riskLevel: String = "MEDIUM",
    val rejectedAt: Long = 0L,
)

@Entity(tableName = "cloud_jobs")
data class CloudJobEntity(
    @PrimaryKey val id: String,
    val cloudProjectId: String,
    val conversationId: String?,
    val goal: String,
    val state: String,
    val resultSummary: String?,
    val log: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "artifacts")
data class ArtifactEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val jobId: String?,
    val conversationId: String?,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localPath: String?,
    val remoteUrl: String?,
    val createdAt: Long,
)

@Entity(tableName = "evidence_bundles")
data class EvidenceBundleEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val runId: String,
    val conversationId: String,
    val leadBrain: String,
    val workerProvider: String,
    val planId: String,
    val approval: String,
    val startedAt: Long,
    val finishedAt: Long,
    val branch: String,
    val commit: String,
    val tests: String,
    val lint: String,
    val security: String,
    val artifact: String,
    val sha256: String,
    val logs: String,
    val knownRisks: String,
    val rollback: String,
    val costClass: String,
    val actualCostCents: Long,
    val sourceProvenance: String,
)
