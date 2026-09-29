package com.example.jarvisai.data.agent

class Verifier {
    fun verifyTask(task: AgentTask, output: String): Boolean {
        // Tool failures are handled by ToolExecutionResult. The verifier only checks
        // that a successful tool produced a usable observation.
        return output.isNotBlank()
    }
}
