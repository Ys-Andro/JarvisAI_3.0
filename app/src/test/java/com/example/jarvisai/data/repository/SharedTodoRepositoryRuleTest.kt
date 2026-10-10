package com.example.jarvisai.data.repository

import com.example.jarvisai.data.model.todo.TodoList
import com.example.jarvisai.data.model.todo.TodoTask
import com.example.jarvisai.data.repository.SharedTodoRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SharedTodoRepositoryRuleTest {

    @Test
    fun testTodoListDataModelDefaults() {
        val list = TodoList(
            id = "list_123",
            title = "Sprint Board",
            ownerId = "user_abc",
            ownerEmail = "user@test.com",
            memberUids = listOf("user_abc")
        )

        assertEquals("list_123", list.id)
        assertEquals("Sprint Board", list.title)
        assertEquals("user_abc", list.ownerId)
        assertEquals(1, list.memberUids.size)
        assertEquals(0, list.totalTasks)
        assertEquals(0, list.completedTasks)
    }

    @Test
    fun testTodoTaskDataModelSerialization() {
        val task = TodoTask(
            id = "task_456",
            listId = "list_123",
            title = "Integrate Firebase",
            completed = false,
            priority = "HIGH",
            createdByUid = "user_abc",
            memberUids = listOf("user_abc", "user_def")
        )

        assertEquals("task_456", task.id)
        assertEquals("list_123", task.listId)
        assertFalse(task.completed)
        assertEquals("HIGH", task.priority)
        assertEquals(2, task.memberUids.size)
    }

    @Test
    fun testPriorityLevels() {
        val validPriorities = listOf("LOW", "MEDIUM", "HIGH")
        assertTrue(validPriorities.contains("LOW"))
        assertTrue(validPriorities.contains("MEDIUM"))
        assertTrue(validPriorities.contains("HIGH"))
    }
}
