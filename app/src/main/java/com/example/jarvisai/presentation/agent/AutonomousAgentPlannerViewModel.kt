package com.example.jarvisai.presentation.agent

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvisai.data.agent.AgentOrchestrator
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
    val errorMessage: String? = null
)

class AutonomousAgentPlannerViewModel(
    private val inferenceRepository: IInferenceRepository,
    private val settingsRepository: ISettingsRepository,
    private val context: Context,
    private val memoryRepository: IMemoryRepository,
    private val documentRepository: IDocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentPlannerUiState())
    val uiState: StateFlow<AgentPlannerUiState> = _uiState.asStateFlow()

    private val orchestrator = AgentOrchestrator(
        context = context,
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

        viewModelScope.launch {
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
                    tasks = initialTasks
                )
            }

            try {
                val synthesis = orchestrator.runPlan(initialTasks, goal) { updatedTasks ->
                    _uiState.update { state -> state.copy(tasks = updatedTasks) }
                }

                _uiState.update { state ->
                    state.copy(
                        isExecuting = false,
                        finalSynthesis = "$synthesis\n\nObjetivo atendido: $goal"
                    )
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

    fun resetPlan() {
        _uiState.update { AgentPlannerUiState() }
    }
}
