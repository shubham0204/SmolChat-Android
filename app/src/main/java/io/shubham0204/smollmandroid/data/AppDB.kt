package io.shubham0204.smollmandroid.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import java.util.Date

private val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE Chat ADD COLUMN enableThinking INTEGER NOT NULL DEFAULT 1")
        }
    }

@Database(
    entities = [Chat::class, ChatMessage::class, LLMModel::class, Task::class, Folder::class],
    version = 2,
)
@TypeConverters(Converters::class)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun chatsDao(): ChatsDao

    abstract fun chatMessagesDao(): ChatMessageDao

    abstract fun llmModelDao(): LLMModelDao

    abstract fun taskDao(): TaskDao

    abstract fun folderDao(): FolderDao
}

@Single
class AppDB(context: Context, private val sharedPrefStore: SharedPrefStore) {
    private val db =
        Room.databaseBuilder(context, AppRoomDatabase::class.java, "app-database")
            .addMigrations(MIGRATION_1_2)
            .build()

    /** Get all chats from the database sorted by dateUsed in descending order. */
    fun getChats(): Flow<List<Chat>> = db.chatsDao().getChats()

    fun getChat(chatId: Long): Chat =
        runBlocking(Dispatchers.IO) { db.chatsDao().getChat(chatId) }

    fun loadDefaultChat(): Chat {
        val defaultChat =
            if (getChatsCount() == 0L) {
                addChat("Untitled")
                getRecentlyUsedChat()!!
            } else {
                // Given that chatsDB has at least one chat
                // chatsDB.getRecentlyUsedChat() will never return null
                getRecentlyUsedChat()!!
            }
        return defaultChat
    }

    /**
     * Get the most recently used chat from the database. This function might return null, if there
     * are no chats in the database.
     */
    fun getRecentlyUsedChat(): Chat? =
        runBlocking(Dispatchers.IO) { db.chatsDao().getRecentlyUsedChat() }

    /**
     * Adds a new chat to the database initialized with given arguments and returns the new Chat
     * object
     */
    fun addChat(
        chatName: String,
        chatTemplate: String = "",
        systemPrompt: String? = null,
        llmModelId: Long = -1,
        isTask: Boolean = false,
    ): Chat =
        runBlocking(Dispatchers.IO) {
            val globalSettings = sharedPrefStore.getGlobalChatSettings()
            val newChat =
                Chat(
                    name = chatName,
                    systemPrompt = systemPrompt ?: globalSettings.systemPrompt,
                    dateCreated = Date(),
                    dateUsed = Date(),
                    llmModelId = llmModelId,
                    minP = globalSettings.minP,
                    temperature = globalSettings.temperature,
                    nThreads = globalSettings.nThreads,
                    useMmap = globalSettings.useMmap,
                    useMlock = globalSettings.useMlock,
                    enableThinking = globalSettings.enableThinking,
                    contextSize = globalSettings.contextSize,
                    chatTemplate = chatTemplate,
                    isTask = isTask,
                )
            val newChatId = db.chatsDao().insertChat(newChat)
            newChat.copy(id = newChatId)
        }

    fun applyGlobalSettingsToChats(
        oldSettings: GlobalChatSettings,
        newSettings: GlobalChatSettings,
    ) =
        runBlocking(Dispatchers.IO) {
            db.chatsDao().getAllChatsList().forEach { chat ->
                val updatedChat =
                    chat.copy(
                        systemPrompt =
                            if (!chat.isTask && chat.systemPrompt == oldSettings.systemPrompt) {
                                newSettings.systemPrompt
                            } else {
                                chat.systemPrompt
                            },
                        minP =
                            if (chat.minP == oldSettings.minP) {
                                newSettings.minP
                            } else {
                                chat.minP
                            },
                        temperature =
                            if (chat.temperature == oldSettings.temperature) {
                                newSettings.temperature
                            } else {
                                chat.temperature
                            },
                        contextSize =
                            if (chat.contextSize == oldSettings.contextSize) {
                                newSettings.contextSize
                            } else {
                                chat.contextSize
                            },
                        nThreads =
                            if (chat.nThreads == oldSettings.nThreads) {
                                newSettings.nThreads
                            } else {
                                chat.nThreads
                            },
                        useMmap =
                            if (chat.useMmap == oldSettings.useMmap) {
                                newSettings.useMmap
                            } else {
                                chat.useMmap
                            },
                        useMlock =
                            if (chat.useMlock == oldSettings.useMlock) {
                                newSettings.useMlock
                            } else {
                                chat.useMlock
                            },
                        enableThinking =
                            if (chat.enableThinking == oldSettings.enableThinking) {
                                newSettings.enableThinking
                            } else {
                                chat.enableThinking
                            },
                    )
                if (updatedChat != chat) {
                    db.chatsDao().updateChat(updatedChat)
                }
            }
        }

    /** Update the chat in the database. */
    fun updateChat(modifiedChat: Chat) =
        runBlocking(Dispatchers.IO) { db.chatsDao().updateChat(modifiedChat) }

    fun deleteChat(chat: Chat) = runBlocking(Dispatchers.IO) { db.chatsDao().deleteChat(chat.id) }

    fun getChatsCount(): Long = runBlocking(Dispatchers.IO) { db.chatsDao().getChatsCount() }

    fun getChatsForFolder(folderId: Long): Flow<List<Chat>> =
        db.chatsDao().getChatsForFolder(folderId)

    // Chat Messages

    fun getMessages(chatId: Long): Flow<List<ChatMessage>> =
        db.chatMessagesDao().getMessages(chatId)

    fun getMessagesForModel(chatId: Long): List<ChatMessage> =
        runBlocking(Dispatchers.IO) { db.chatMessagesDao().getMessagesForModel(chatId) }

    fun addUserMessage(chatId: Long, message: String) =
        runBlocking(Dispatchers.IO) {
            db.chatMessagesDao()
                .insertMessage(
                    ChatMessage(chatId = chatId, message = message, isUserMessage = true)
                )
        }

    fun addAssistantMessage(chatId: Long, message: String) =
        runBlocking(Dispatchers.IO) {
            db.chatMessagesDao()
                .insertMessage(
                    ChatMessage(chatId = chatId, message = message, isUserMessage = false)
                )
        }

    fun deleteMessage(messageId: Long) =
        runBlocking(Dispatchers.IO) { db.chatMessagesDao().deleteMessage(messageId) }

    fun deleteMessages(chatId: Long) =
        runBlocking(Dispatchers.IO) { db.chatMessagesDao().deleteMessages(chatId) }

    // Models

    fun addModel(name: String, url: String, path: String, contextSize: Int, chatTemplate: String) =
        runBlocking(Dispatchers.IO) {
            db.llmModelDao()
                .insertModels(
                    LLMModel(
                        name = name,
                        url = url,
                        path = path,
                        contextSize = contextSize,
                        chatTemplate = chatTemplate,
                    )
                )
        }

    fun getModel(id: Long): LLMModel = runBlocking(Dispatchers.IO) { db.llmModelDao().getModel(id) }

    fun getModels(): Flow<List<LLMModel>> =
        runBlocking(Dispatchers.IO) { db.llmModelDao().getAllModels() }

    fun getModelsList(): List<LLMModel> =
        runBlocking(Dispatchers.IO) { db.llmModelDao().getAllModelsList() }

    fun deleteModel(id: Long) = runBlocking(Dispatchers.IO) { db.llmModelDao().deleteModel(id) }

    // Tasks

    fun getTask(taskId: Long): Task = runBlocking(Dispatchers.IO) { db.taskDao().getTask(taskId) }

    fun getTasks(): Flow<List<Task>> = db.taskDao().getTasks()

    fun addTask(name: String, systemPrompt: String, modelId: Long) =
        runBlocking(Dispatchers.IO) {
            db.taskDao()
                .insertTask(Task(name = name, systemPrompt = systemPrompt, modelId = modelId))
        }

    fun deleteTask(taskId: Long) = runBlocking(Dispatchers.IO) { db.taskDao().deleteTask(taskId) }

    fun updateTask(task: Task) = runBlocking(Dispatchers.IO) { db.taskDao().updateTask(task) }

    // Folders

    fun getFolders(): Flow<List<Folder>> = db.folderDao().getFolders()

    fun addFolder(folderName: String) =
        runBlocking(Dispatchers.IO) { db.folderDao().insertFolder(Folder(name = folderName)) }

    fun updateFolder(folder: Folder) =
        runBlocking(Dispatchers.IO) { db.folderDao().updateFolder(folder) }

    /** Deletes the folder from the Folder table only */
    fun deleteFolder(folderId: Long) =
        runBlocking(Dispatchers.IO) {
            db.folderDao().deleteFolder(folderId)
            db.chatsDao().updateFolderIds(folderId, -1L)
        }

    /** Deletes the folder from the Folder table and corresponding chats from the Chat table */
    fun deleteFolderWithChats(folderId: Long) =
        runBlocking(Dispatchers.IO) {
            db.folderDao().deleteFolder(folderId)
            db.chatsDao().deleteChatsInFolder(folderId)
        }
}
