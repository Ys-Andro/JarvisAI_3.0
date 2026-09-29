package com.example.jarvisai.data.agent

import com.example.jarvisai.data.rag.ContextProvider
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.repository.IConversationRepository
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IMemoryRepository
import kotlinx.coroutines.flow.first

/**
 * Read-only assembler for the agent working context.
 */
class AgentContextManager(
    private val conversationRepository: IConversationRepository,
    private val memoryRepository: IMemoryRepository,
    private val documentRepository: IDocumentRepository,
    private val contextProvider: ContextProvider = ContextProvider(),
    private val maxConversationMessages: Int = 20,
    private val maxObservationItems: Int = 12,
    private val maxTaskItems: Int = 12,
    private val maxKnowledgeChars: Int = 9000
) {
    suspend fun build(
        goal: String,
        conversationId: String? = null,
        suppliedHistory: List<Message> = emptyList(),
        tasks: List<AgentTask> = emptyList(),
        observations: List<String> = emptyList()
    ): AgentContextSnapshot {
        val conversation = loadConversation(conversationId, suppliedHistory)
        val memories = memoryRepository.getAllMemories().first()
        val documents = documentRepository.getAllDocuments().first()

        val retrievalQuery = buildString {
            append(goal)
            if (conversation.isNotEmpty()) {
                append("\nContexto reciente:\n")
                conversation.takeLast(8).forEach {
                    append(it.content.take(700)).append("\n")
                }
            }
            if (observations.isNotEmpty()) {
                append("\nObservaciones de herramientas:\n")
                observations.takeLast(4).forEach {
                    append(it.take(700)).append("\n")
                }
            }
        }

        val knowledge = contextProvider
            .buildAugmentedContext(retrievalQuery, documents, memories)
            .take(maxKnowledgeChars)

        return AgentContextSnapshot(
            goal = goal,
            conversation = conversation.takeLast(maxConversationMessages),
            relevantKnowledge = knowledge,
            observations = observations.takeLast(maxObservationItems),
            taskState = tasks.takeLast(maxTaskItems).map {
                AgentTaskContext(
                    id = it.id,
                    title = it.title,
                    status = it.status.name,
                    toolName = it.toolName,
                    output = it.outputResult,
                    error = it.error,
                    lastObservation = it.lastObservation,
                    dependencies = it.dependencies
                )
            }
        )
    }

    private suspend fun loadConversation(
        conversationId: String?,
        suppliedHistory: List<Message>
    ): List<Message> {
        if (!conversationId.isNullOrBlank()) {
            return conversationRepository
                .getMessagesForConversation(conversationId)
                .first()
                .ifEmpty { suppliedHistory }
        }
        return suppliedHistory
    }
}

data class AgentContextSnapshot(
    val goal: String,
    val conversation: List<Message>,
    val relevantKnowledge: String,
    val observations: List<String>,
    val taskState: List<AgentTaskContext>
) {
    fun toPromptBlock(): String = buildString {
        append("[AGENT WORKING CONTEXT]\n")
        append("Current goal: ").append(goal).append("\n")

        if (conversation.isNotEmpty()) {
            append("\n[RECENT CONVERSATION]\n")
            conversation.forEach { message ->
                val role = message.role.name
                append("$role: ").append(message.content.take(1200)).append("\n")
            }
        }

        if (relevantKnowledge.isNotBlank()) {
            append("\n[RELEVANT MEMORY + RAG]\n")
            append(relevantKnowledge).append("\n")
        }

        if (observations.isNotEmpty()) {
            append("\n[TOOL OBSERVATIONS]\n")
            observations.forEach {
                append("- ").append(it.take(1200)).append("\n")
            }
        }

        if (taskState.isNotEmpty()) {
            append("\n[TASK STATE]\n")
            taskState.forEach { task ->
                append("- ${task.id}: ${task.title} [${task.status}]")
                if (task.dependencies.isNotEmpty()) {
                    append(" deps=").append(task.dependencies.joinToString(","))
                }
                if (!task.lastObservation.isNullOrBlank()) {
                    append(" observation=").append(task.lastObservation!!.take(700))
                }
                if (!task.output.isNullOrBlank()) {
                    append(" output=").append(task.output!!.take(700))
                }
                if (!task.error.isNullOrBlank()) {
                    append(" error=").append(task.error!!.take(700))
                }
                append("\n")
            }
        }
    }
}

data class AgentTaskContext(
    val id: String,
    val title: String,
    val status: String,
    val toolName: String,
    val output: String?,
    val error: String?,
    val lastObservation: String?,
    val dependencies: List<String>
)
