package ai.hassan.app.selfupdate

import android.content.Context

/** Tracks self-improve cloud jobs already announced to the user. */
class SelfImproveJobStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lastAnnouncedState(jobId: String): String? = prefs.getString(key(jobId), null)

    fun markAnnounced(jobId: String, state: String) {
        prefs.edit().putString(key(jobId), state).apply()
    }

    fun rememberPendingJob(conversationId: String, jobId: String) {
        prefs.edit()
            .putString(KEY_PENDING_JOB, jobId)
            .putString(KEY_PENDING_CONV, conversationId)
            .apply()
    }

    fun pendingJobId(): String? = prefs.getString(KEY_PENDING_JOB, null)?.takeIf { it.isNotBlank() }

    fun pendingConversationId(): String? =
        prefs.getString(KEY_PENDING_CONV, null)?.takeIf { it.isNotBlank() }

    fun clearPending() {
        prefs.edit().remove(KEY_PENDING_JOB).remove(KEY_PENDING_CONV).apply()
    }

    private fun key(jobId: String) = "job_state_$jobId"

    companion object {
        private const val PREFS = "hassan_self_improve"
        private const val KEY_PENDING_JOB = "pending_job_id"
        private const val KEY_PENDING_CONV = "pending_conversation_id"
        const val CAPABILITY = "SELF_IMPROVE"
        const val JOB_TYPE = "candidate_self_improve"
        const val CLOUD_PROJECT_NAME = "FrishtaSelfImprove"
    }
}
