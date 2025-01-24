package com.example.projet_intgrateur.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import java.util.*
import com.example.projet_intgrateur.data.sync.MessageSyncManager

class MessageRepository(
    private val messageDao: MessageDao,
    private val syncManager: MessageSyncManager
) {
    val allMessages: Flow<List<Message>> = messageDao.getAllMessages()

    suspend fun insertMessage(
        sender: String,
        content: String,
        modelPrediction: String,
        userFeedback: String? = null
    ): Long {
        val message = Message(
            sender = sender,
            content = content,
            modelPrediction = modelPrediction,
            userFeedback = userFeedback,
            receivedDate = Date(),
            lastModifiedDate = Date()
        )
        return messageDao.insertMessage(message).also {
            Log.d("MessageRepository", "Message inséré avec ID: $it")
        }
    }

    suspend fun updateUserFeedback(messageId: Long, feedback: String) {
        messageDao.getMessageById(messageId)?.let { message ->
            messageDao.updateMessage(
                message.copy(
                    userFeedback = feedback,
                    lastModifiedDate = Date()
                )
            )

            if (messageDao.getCommentedMessagesCount() >= 20) {
                syncManager.syncMessages()
            }
        }
    }

    suspend fun getStatistics(): Map<String, Int> {
        return mapOf(
            "ham_predictions" to messageDao.getCountByPrediction("ham"),
            "phishing_predictions" to messageDao.getCountByPrediction("phishing"),
            "ham_feedback" to messageDao.getCountByUserFeedback("ham"),
            "phishing_feedback" to messageDao.getCountByUserFeedback("phishing")
        )
    }
}