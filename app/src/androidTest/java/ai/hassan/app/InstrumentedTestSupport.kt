package ai.hassan.app

import ai.hassan.app.data.ConversationEntity
import ai.hassan.app.data.HassanRepository
import kotlinx.coroutines.flow.first

object InstrumentedTestSupport {
    suspend fun isolatedConversation(repository: HassanRepository, label: String): ConversationEntity {
        val conversation = repository.createNewConversation("$label-${System.currentTimeMillis()}")
        repository.selectConversation(conversation.id)
        return conversation
    }

    suspend fun planIdsForConversation(repository: HassanRepository, conversationId: String): Set<String> =
        repository.plans.first()
            .filter { it.conversationId == conversationId }
            .map { it.id }
            .toSet()
}
