package com.example.jarvisai.di

/**
 * Architecture Contracts and Dependency Injection specifications for JarvisAi.
 * Pure Cloud / Remote Multi-Provider Architecture (Google Gemini, OpenAI, Claude, DeepSeek, Groq, OpenRouter).
 */

// Architecture specifications:
// 1. AppModule: Provides Application Context
// 2. DatabaseModule: Provides Room JarvisDatabase, ConversationDao, MessageDao, MemoryDao, DocumentDao
// 3. DataStoreModule: Provides AppPreferences
// 4. NetworkModule: Provides GeminiApiClient, UniversalAiApiClient
// 5. RepositoryModule: Provides IConversationRepository, IInferenceRepository, ISettingsRepository, IMemoryRepository, IDocumentRepository, ITtsRepository
