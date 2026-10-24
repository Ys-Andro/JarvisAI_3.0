package com.example.jarvisai.data.session

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.jarvisai.data.local.database.JarvisDatabase
import com.example.jarvisai.data.repository.ConversationRepositoryImpl
import com.example.jarvisai.domain.model.Message
import com.example.jarvisai.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionManagerTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var database: JarvisDatabase
    private lateinit var repository: ConversationRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ConversationRepositoryImpl(database.conversationDao(), database.messageDao())
        sessionManager = SessionManager.getInstance()
        sessionManager.endSession() // ensure fresh starting state
    }

    @After
    fun tearDown() {
        sessionManager.endSession()
        database.close()
    }

    @Test
    fun `test new sessionId generated when no active session exists`() {
        assertFalse(sessionManager.hasActiveSession())
        val sessionId = sessionManager.getOrCreateSessionId()
        assertNotNull(sessionId)
        assertTrue(sessionId.isNotBlank())
        assertTrue(sessionManager.hasActiveSession())
        assertEquals(sessionId, sessionManager.getActiveSessionId())
    }

    @Test
    fun `test same sessionId preserved while app remains open or backgrounded`() {
        val firstSessionId = sessionManager.getOrCreateSessionId()
        // Simulating subsequent interaction / return from background
        val secondSessionId = sessionManager.getOrCreateSessionId()
        assertEquals("Session ID must remain identical during the active session", firstSessionId, secondSessionId)
    }

    @Test
    fun `test session termination and new session creation after complete close`() {
        val session1 = sessionManager.getOrCreateSessionId()
        assertTrue(sessionManager.hasActiveSession())

        // Simulate complete close
        sessionManager.endSession()
        assertFalse(sessionManager.hasActiveSession())

        // Simulate app reopening
        val session2 = sessionManager.getOrCreateSessionId()
        assertTrue(sessionManager.hasActiveSession())
        assertNotEquals("A new session ID must be generated after complete close", session1, session2)
    }

    @Test
    fun `test conversation and messages are isolated per session without deleting history`() = runBlocking {
        // Session 1: create conversation and message
        val session1Id = sessionManager.getOrCreateSessionId()
        repository.createConversation("Session 1 Title", "gemini-3.6-flash", customId = session1Id)
        val msg1 = Message(
            id = UUID.randomUUID().toString(),
            conversationId = session1Id,
            role = Role.USER,
            content = "Mensaje en sesión 1",
            timestamp = System.currentTimeMillis()
        )
        repository.insertMessage(msg1)

        val session1Messages = repository.getMessagesForConversation(session1Id).first()
        assertEquals(1, session1Messages.size)
        assertEquals("Mensaje en sesión 1", session1Messages.first().content)

        // Simulate app complete closure
        sessionManager.endSession()

        // Session 2: reopening app
        val session2Id = sessionManager.getOrCreateSessionId()
        assertNotEquals(session1Id, session2Id)
        repository.createConversation("Session 2 Title", "gemini-3.6-flash", customId = session2Id)

        // Session 2 should have 0 messages
        val session2Messages = repository.getMessagesForConversation(session2Id).first()
        assertEquals("New session conversation must not have messages from previous session", 0, session2Messages.size)

        // Add message to Session 2
        val msg2 = Message(
            id = UUID.randomUUID().toString(),
            conversationId = session2Id,
            role = Role.USER,
            content = "Mensaje en sesión 2",
            timestamp = System.currentTimeMillis()
        )
        repository.insertMessage(msg2)

        // Verify Session 1 messages are still preserved intact in database
        val session1MessagesAfter = repository.getMessagesForConversation(session1Id).first()
        assertEquals(1, session1MessagesAfter.size)
        assertEquals("Mensaje en sesión 1", session1MessagesAfter.first().content)

        // Verify both conversations exist in history
        val allConversations = repository.getAllConversations().first()
        assertEquals(2, allConversations.size)
    }
}
