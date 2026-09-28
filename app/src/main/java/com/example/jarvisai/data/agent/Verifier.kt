package com.example.jarvisai.data.agent

class Verifier {
    fun verifyTask(task: AgentTask, output: String): Boolean {
        if (output.contains("Error") || output.contains("⚠️") || output.contains("Denegado")) {
            return false
        }
        return output.isNotBlank()
    }
}
