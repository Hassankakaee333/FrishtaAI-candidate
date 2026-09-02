package ai.hassan.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    suspend fun listAll(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)
}

@Dao
interface DecisionDao {
    @Query("SELECT * FROM decisions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions WHERE id = :id")
    suspend fun getById(id: String): DecisionEntity?

    @Query("SELECT COUNT(*) FROM decisions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(decision: DecisionEntity)

    @Update
    suspend fun update(decision: DecisionEntity)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatest(): ConversationEntity?

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY createdAt")
    fun observeAll(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt")
    suspend fun listForConversation(conversationId: String): List<MessageEntity>
}

@Dao
interface ExecutionPlanDao {
    @Query("SELECT * FROM execution_plans ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExecutionPlanEntity>>

    @Query("SELECT * FROM execution_plans WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestForConversation(conversationId: String): ExecutionPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: ExecutionPlanEntity)

    @Update
    suspend fun update(plan: ExecutionPlanEntity)
}

@Dao
interface BridgeRequestDao {
    @Query("SELECT * FROM bridge_requests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BridgeRequestEntity>>

    @Query("SELECT * FROM bridge_requests WHERE status = 'PENDING' ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestPending(): BridgeRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: BridgeRequestEntity)

    @Update
    suspend fun update(request: BridgeRequestEntity)
}

@Dao
interface ResourceLedgerDao {
    @Query("SELECT * FROM resource_ledger ORDER BY costClass, displayName")
    fun observeAll(): Flow<List<ResourceLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(resources: List<ResourceLedgerEntity>)
}

@Dao
interface RadarFindingDao {
    @Query("SELECT * FROM radar_findings ORDER BY lastVerifiedAt DESC")
    fun observeAll(): Flow<List<RadarFindingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(findings: List<RadarFindingEntity>)

    @Update
    suspend fun update(finding: RadarFindingEntity)

    @Query("SELECT * FROM radar_findings WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RadarFindingEntity?
}

@Dao
interface EvidenceBundleDao {
    @Query("SELECT * FROM evidence_bundles ORDER BY finishedAt DESC")
    fun observeAll(): Flow<List<EvidenceBundleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bundle: EvidenceBundleEntity)
}

@Dao
interface CloudJobDao {
    @Query("SELECT * FROM cloud_jobs ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CloudJobEntity>>

    @Query("SELECT * FROM cloud_jobs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CloudJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: CloudJobEntity)
}

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM artifacts WHERE jobId = :jobId ORDER BY createdAt DESC")
    suspend fun listForJob(jobId: String): List<ArtifactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(artifact: ArtifactEntity)
}
