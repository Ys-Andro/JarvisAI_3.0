package com.example.jarvisai.presentation.agent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvisai.data.action.ActionExecutionResult
import com.example.jarvisai.data.action.ActionExecutor
import com.example.jarvisai.data.action.ActionRequest
import com.example.jarvisai.data.agent.AgentOrchestrator
import com.example.jarvisai.data.agent.AgentRunResult
import com.example.jarvisai.data.agent.AgentTask
import com.example.jarvisai.data.agent.TaskStatus
import com.example.jarvisai.domain.repository.IDocumentRepository
import com.example.jarvisai.domain.repository.IMemoryRepository
import com.example.jarvisai.domain.repository.IInferenceRepository
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentPlannerUiState(
    val goalInput: String = "",
    val isExecuting: Boolean = false,
    val tasks: List<AgentTask> = emptyList(),
    val finalSynthesis: String? = null,
    val errorMessage: String? = null,
    val pendingActionConfirmation: ActionRequest? = null
)

class AutonomousAgentPlannerViewModel(
    private val conversationRepository: com.example.jarvisai.domain.repository.IConversationRepository,
    private val inferenceRepository: IInferenceRepository,
    private val settingsRepository: ISettingsRepository,
    private val context: Context,
    private val memoryRepository: IMemoryRepository,
    private val documentRepository: IDocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentPlannerUiState())
    val uiState: StateFlow<AgentPlannerUiState> = _uiState.asStateFlow()

    private val actionExecutor = ActionExecutor(context, memoryRepository)
    private val orchestrator = AgentOrchestrator(
        context = context,
        conversationRepository = conversationRepository,
        memoryRepository = memoryRepository,
        documentRepository = documentRepository,
        inferenceRepository = inferenceRepository,
        settingsRepository = settingsRepository
    )

    fun onGoalInputChange(goal: String) {
        _uiState.update { it.copy(goalInput = goal) }
    }

    fun startAutonomousExecution() {
        val goal = _uiState.value.goalInput.trim()
        if (goal.isEmpty() || _uiState.value.isExecuting) return

        val initialTasks = listOf(
            AgentTask(title = "1. Análisis Cognitivo y Descomposición", description = "Evaluando restricciones y objetivos.", toolName = "LLM_SYNTHESIS", toolInput = goal),
            AgentTask(title = "2. Búsqueda y Recuperación RAG", description = "Recuperando fragmentos de conocimiento relevantes.", toolName = "RAG_SEARCH", toolInput = goal),
            AgentTask(title = "3. Verificación y Auto-corrección", description = "Auditando contexto y resultados disponibles.", toolName = "MEMORY", toolInput = goal),
            AgentTask(title = "4. Síntesis Final del Plan", description = "Preparando el resultado final.", toolName = "LLM_SYNTHESIS", toolInput = goal)
        )

        _uiState.update {
            it.copy(
                isExecuting = true,
                errorMessage = null,
                finalSynthesis = null,
                pendingActionConfirmation = null,
                tasks = initialTasks
            )
        }

        runPlanInternal(initialTasks, goal)
    }

    private fun runPlanInternal(tasks: List<AgentTask>, goal: String) {
        viewModelScope.launch {
            try {
                when (val result = orchestrator.executePlan(tasks, goal) { updatedTasks ->
                    _uiState.update { state -> state.copy(tasks = updatedTasks) }
                }) {
                    is AgentRunResult.Completed -> {
                        _uiState.update { state ->
                            state.copy(
                                isExecuting = false,
                                finalSynthesis = "${result.message}\n\nObjetivo atendido: $goal",
                                pendingActionConfirmation = null
                            )
                        }
                    }
                    is AgentRunResult.Failed -> {
                        _uiState.update { state ->
                            state.copy(
                                isExecuting = false,
                                errorMessage = "Plan interrumpido: ${result.message}",
                                pendingActionConfirmation = null
                            )
                        }
                    }
                    is AgentRunResult.WaitingConfirmation -> {
                        _uiState.update { state ->
                            state.copy(
                                isExecuting = false,
                                pendingActionConfirmation = result.pendingAction,
                                tasks = result.tasks
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        errorMessage = "Error en ejecución agéntica: ${e.message}",
                        tasks = it.tasks.map { t -> t.copy(status = TaskStatus.FAILED, lastObservation = e.message) }
                    )
                }
            }
        }
    }

    fun confirmPendingAction() {
        val pendingAction = _uiState.value.pendingActionConfirmation ?: return
        val currentTasks = _uiState.value.tasks.toMutableList()
        val waitingIndex = currentTasks.indexOfFirst { it.status == TaskStatus.WAITING_CONFIRMATION }
        val goal = _uiState.value.goalInput.trim()

        _uiState.update { it.copy(pendingActionConfirmation = null, isExecuting = true) }

        viewModelScope.launch {
            val execResult = actionExecutor.executeConfirmed(pendingAction)
            if (waitingIndex != -1) {
                val waitingTask = currentTasks[waitingIndex]
                val updatedTask = when (execResult) {
                    is ActionExecutionResult.Success -> waitingTask.copy(
                        status = TaskStatus.COMPLETED,
                        outputResult = execResult.message,
                        lastObservation = execResult.message,
                        error = null,
                        pendingAction = null
                    )
                    is ActionExecutionResult.Error -> waitingTask.copy(
                        status = TaskStatus.FAILED,
                        error = execResult.reason,
                        lastObservation = execResult.reason,
                        pendingAction = null
                    )
                    is ActionExecutionResult.Denied -> waitingTask.copy(
                        status = TaskStatus.FAILED,
                        error = "Acción denegada: ${execResult.reason}",
                        lastObservation = "Acción denegada: ${execResult.reason}",
                        pendingAction = null
                    )
                    is ActionExecutionResult.RequiresConfirmation -> waitingTask.copy(
                        status = TaskStatus.FAILED,
                        error = "Fallo de confirmación anidada.",
                        pendingAction = null
                    )
                }
                currentTasks[waitingIndex] = updatedTask
                _uiState.update { it.copy(tasks = currentTasks.toList()) }

                if (updatedTask.status == TaskStatus.COMPLETED) {
                    runPlanInternal(currentTasks.toList(), goal)
                } else {
                    _uiState.update { it.copy(isExecuting = false, errorMessage = updatedTask.error) }
                }
            } else {
                _uiState.update { it.copy(isExecuting = false) }
            }
        }
    }

    fun cancelPendingAction() {
        val currentTasks = _uiState.value.tasks.toMutableList()
        val waitingIndex = currentTasks.indexOfFirst { it.status == TaskStatus.WAITING_CONFIRMATION }

        if (waitingIndex != -1) {
            val waitingTask = currentTasks[waitingIndex]
            currentTasks[waitingIndex] = waitingTask.copy(
                status = TaskStatus.CANCELLED,
                error = "Acción cancelada por el usuario.",
                lastObservation = "Acción cancelada por el usuario.",
                pendingAction = null
            )
            for (i in (waitingIndex + 1) until currentTasks.size) {
                if (currentTasks[i].status != TaskStatus.COMPLETED) {
                    currentTasks[i] = currentTasks[i].copy(status = TaskStatus.SKIPPED)
                }
            }
        }

        _uiState.update {
            it.copy(
                pendingActionConfirmation = null,
                isExecuting = false,
                tasks = currentTasks.toList(),
                errorMessage = "La acción fue rechazada. Plan cancelado."
            )
        }
    }

    fun resetPlan() {
        _uiState.update { AgentPlannerUiState() }
    }
}
