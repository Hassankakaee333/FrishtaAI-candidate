package ai.hassan.app.cloud

/**
 * Future cloud orchestration boundary.
 * Android submits jobs and observes durable server-side state — it does not own long-running work.
 */
enum class CloudTaskState {
    QUEUED,
    PLANNING,
    RUNNING,
    RESEARCHING,
    CODING,
    TESTING,
    REVIEWING,
    BUILDING,
    VERIFYING,
    WAITING_FOR_USER,
    RETRYING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class CloudTaskRef(
    val taskId: String,
    val projectId: String,
    val conversationId: String?,
    val goal: String,
    val state: CloudTaskState,
    val updatedAt: Long,
)

/** Placeholder until a standalone Hassan cloud backend exists. */
interface CloudTaskOrchestrator {
    val isConfigured: Boolean

    suspend fun submitTask(
        projectId: String,
        conversationId: String?,
        goal: String,
        jobType: String = "general",
    ): Result<CloudTaskRef>

    suspend fun getTask(taskId: String): CloudTaskRef?

    suspend fun cancelTask(taskId: String): Result<Unit>
}

class UnconfiguredCloudTaskOrchestrator : CloudTaskOrchestrator {
    override val isConfigured: Boolean = false

    override suspend fun submitTask(
        projectId: String,
        conversationId: String?,
        goal: String,
        jobType: String,
    ): Result<CloudTaskRef> = Result.failure(
        UnsupportedOperationException("لم يتم إعداد طبقة التنفيذ السحابي بعد."),
    )

    override suspend fun getTask(taskId: String): CloudTaskRef? = null

    override suspend fun cancelTask(taskId: String): Result<Unit> = Result.failure(
        UnsupportedOperationException("لم يتم إعداد طبقة التنفيذ السحابي بعد."),
    )
}
