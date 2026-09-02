package ai.hassan.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        TaskEntity::class,
        DecisionEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        ExecutionPlanEntity::class,
        BridgeRequestEntity::class,
        ResourceLedgerEntity::class,
        RadarFindingEntity::class,
        EvidenceBundleEntity::class,
        CloudJobEntity::class,
        ArtifactEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class HassanDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun decisionDao(): DecisionDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun executionPlanDao(): ExecutionPlanDao
    abstract fun bridgeRequestDao(): BridgeRequestDao
    abstract fun resourceLedgerDao(): ResourceLedgerDao
    abstract fun radarFindingDao(): RadarFindingDao
    abstract fun evidenceBundleDao(): EvidenceBundleDao
    abstract fun cloudJobDao(): CloudJobDao
    abstract fun artifactDao(): ArtifactDao

    companion object {
        fun create(context: Context): HassanDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                HassanDatabase::class.java,
                "hassan-ai.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `conversations` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `title` TEXT NOT NULL, `leadBrainId` TEXT NOT NULL, `state` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `providerId` TEXT, `taskId` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `execution_plans` (`id` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `goal` TEXT NOT NULL, `summary` TEXT NOT NULL, `components` TEXT NOT NULL, `risks` TEXT NOT NULL, `verification` TEXT NOT NULL, `rollback` TEXT NOT NULL, `capability` TEXT NOT NULL, `status` TEXT NOT NULL, `costClass` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `approvedAt` INTEGER, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `bridge_requests` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `providerId` TEXT NOT NULL, `taskPackText` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `responseAt` INTEGER, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `resource_ledger` (`providerId` TEXT NOT NULL, `displayName` TEXT NOT NULL, `costClass` TEXT NOT NULL, `actualMoneyCostCents` INTEGER NOT NULL, `quotaRemaining` TEXT NOT NULL, `quotaResetAt` TEXT, `rateLimit` TEXT NOT NULL, `privacy` TEXT NOT NULL, `trainingPolicy` TEXT NOT NULL, `retention` TEXT NOT NULL, `securityGrade` TEXT NOT NULL, `reliability7d` REAL NOT NULL, `reliability30d` REAL NOT NULL, `latency` TEXT NOT NULL, `qualityScore` REAL NOT NULL, `commercialUse` TEXT NOT NULL, `license` TEXT NOT NULL, `geoEligibility` TEXT NOT NULL, `cardRequired` INTEGER NOT NULL, `subscriptionRequired` INTEGER NOT NULL, `requiresHumanBridge` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `lastVerifiedAt` INTEGER NOT NULL, `sourceEvidence` TEXT NOT NULL, PRIMARY KEY(`providerId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `radar_findings` (`id` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `summary` TEXT NOT NULL, `sourceUrl` TEXT NOT NULL, `version` TEXT NOT NULL, `status` TEXT NOT NULL, `costClass` TEXT NOT NULL, `license` TEXT NOT NULL, `discoveredAt` INTEGER NOT NULL, `lastVerifiedAt` INTEGER NOT NULL, `sourceEvidence` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `evidence_bundles` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `runId` TEXT NOT NULL, `conversationId` TEXT NOT NULL, `leadBrain` TEXT NOT NULL, `workerProvider` TEXT NOT NULL, `planId` TEXT NOT NULL, `approval` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `finishedAt` INTEGER NOT NULL, `branch` TEXT NOT NULL, `commit` TEXT NOT NULL, `tests` TEXT NOT NULL, `lint` TEXT NOT NULL, `security` TEXT NOT NULL, `artifact` TEXT NOT NULL, `sha256` TEXT NOT NULL, `logs` TEXT NOT NULL, `knownRisks` TEXT NOT NULL, `rollback` TEXT NOT NULL, `costClass` TEXT NOT NULL, `actualCostCents` INTEGER NOT NULL, `sourceProvenance` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `conversations` ADD COLUMN `codexReasoningEffort` TEXT NOT NULL DEFAULT 'medium'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `messages` ADD COLUMN `attachmentRefs` TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `radar_findings` ADD COLUMN `candidateStatus` TEXT NOT NULL DEFAULT 'NEW'")
                db.execSQL("ALTER TABLE `radar_findings` ADD COLUMN `candidateType` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `radar_findings` ADD COLUMN `capabilities` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cloud_jobs` (" +
                        "`id` TEXT NOT NULL, `cloudProjectId` TEXT NOT NULL, `conversationId` TEXT, " +
                        "`goal` TEXT NOT NULL, `state` TEXT NOT NULL, `resultSummary` TEXT, `log` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `artifacts` (" +
                        "`id` TEXT NOT NULL, `projectId` TEXT, `jobId` TEXT, `conversationId` TEXT, " +
                        "`name` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, " +
                        "`localPath` TEXT, `remoteUrl` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `radar_findings` ADD COLUMN `userDecision` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `radar_findings` ADD COLUMN `radarScore` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `radar_findings` ADD COLUMN `riskLevel` TEXT NOT NULL DEFAULT 'MEDIUM'")
                db.execSQL("ALTER TABLE `radar_findings` ADD COLUMN `rejectedAt` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
