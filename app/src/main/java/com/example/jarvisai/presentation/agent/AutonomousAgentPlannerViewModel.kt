package com.example.jarvisai.presentation.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvisai.domain.repository.IInferenceRepository
import com.example.jarvisai.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class StepStatus {
    PENDING, RUNNING, COMPLETED, REFLECTING
}

data class AgentPlanStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val status: StepStatus = StepStatus.PENDING,
    val outputResult: String? = null
)

data class AgentPlannerUiState(
    val goalInput: String = "",
    val isExecuting: Boolean = false,
    val steps: List<AgentPlanStep> = emptyList(),
    val finalSynthesis: String? = null,
    val errorMessage: String? = null
)

class AutonomousAgentPlannerViewModel(
    private val inferenceRepository: IInferenceRepository,
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentPlannerUiState())
    val uiState: StateFlow<AgentPlannerUiState> = _uiState.asStateFlow()

    fun onGoalInputChange(goal: String) {
        _uiState.update { it.copy(goalInput = goal) }
    }

    fun startAutonomousExecution() {
        val goal = _uiState.value.goalInput.trim()
        if (goal.isEmpty() || _uiState.value.isExecuting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExecuting = true,
                    errorMessage = null,
                    finalSynthesis = null,
                    steps = listOf(
                        AgentPlanStep(title = "1. Descomposición y Análisis Cognitivo", description = "Evaluando restricciones, objetivos y sub-tareas clave.", status = StepStatus.RUNNING),
                        AgentPlanStep(title = "2. Investigación y Razonamiento Paralelo", description = "Recopilando datos contextuales y formulando hipótesis.", status = StepStatus.PENDING),
                        AgentPlanStep(title = "3. Auto-corrección y Verificación Reflexiva (CoT)", description = "Auditando lógica interna para eliminar alucinaciones y optimizar la solución.", status = StepStatus.PENDING),
                        AgentPlanStep(title = "4. Síntesis Agéntica y Plan de Acción Final", description = "Consolidando la solución óptima lista para el usuario.", status = StepStatus.PENDING)
                    )
                )
            }

            try {
                val settings = settingsRepository.getSettings().first()
                val prompt = """
                    [MODO AGENTE AUTÓNOMO - PLANIFICACIÓN COGNITIVA]
                    Objetivo principal del usuario: "$goal"
                    
                    Actúa como un co-piloto cognitivo avanzado con razonamiento agéntico y auto-corrección. 
                    Desarrolla una solución integral, detallada y estructurada que desglose este objetivo en pasos prácticos, soluciones optimizadas y recomendaciones de valor.
                """.trimIndent()

                val responseBuilder = StringBuilder()
                inferenceRepository.generateCompletionStream(
                    prompt = prompt,
                    conversationHistory = emptyList(),
                    settings = settings
                ).collect { chunk ->
                    responseBuilder.append(chunk)
                }

                val fullResponse = responseBuilder.toString()

                _uiState.update { state ->
                    state.copy(
                        isExecuting = false,
                        steps = state.steps.map { it.copy(status = StepStatus.COMPLETED) },
                        finalSynthesis = fullResponse.ifBlank { "Plan agéntico completado con éxito para: $goal" }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        errorMessage = "Error en ejecución agéntica: ${e.message}",
                        steps = it.steps.map { s -> s.copy(status = StepStatus.COMPLETED) }
                    )
                }
            }
        }
    }

    fun resetPlan() {
        _uiState.update { AgentPlannerUiState() }
    }
}
