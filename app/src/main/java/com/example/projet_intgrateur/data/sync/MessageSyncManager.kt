package com.example.projet_intgrateur.data.sync

import android.util.Log
import com.example.projet_intgrateur.api.RetrofitClient
import com.example.projet_intgrateur.api.FeedbackBatch
import com.example.projet_intgrateur.data.MessageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessageSyncManager(
    private val messageDao: MessageDao,
    private val threshold: Int = 1
) {
    private val TAG = "MessageSyncManager"

    suspend fun syncMessages() {
        withContext(Dispatchers.IO) {
            try {
                val messagesToSync = messageDao.getMessagesForSync(threshold)
                Log.d(TAG, "Messages à synchroniser: ${messagesToSync.size}")

                if (messagesToSync.isEmpty()) {
                    Log.d(TAG, "Pas de messages à synchroniser")
                    return@withContext
                }

                val messages = mutableListOf<String>()
                val labels = mutableListOf<Int>()

                messagesToSync.forEach { message ->
                    message.userFeedback?.let { feedback ->
                        messages.add(message.content)
                        labels.add(if (feedback == "ham") 0 else 1)
                        Log.d(TAG, "Ajout message: '${message.content.take(20)}...' avec feedback: $feedback")
                    }
                }

                if (messages.isEmpty()) {
                    Log.d(TAG, "Pas de messages avec feedback")
                    return@withContext
                }

                val feedbackBatch = FeedbackBatch(
                    messages = messages,
                    labels = labels
                )

                val response = RetrofitClient.feedbackService.syncFeedback(feedbackBatch)

                if (response.isSuccessful) {
                    messagesToSync.forEach { message ->
                        try {
                            messageDao.deleteMessage(message.id)
                            Log.d(TAG, "Message ${message.id} supprimé")
                        } catch (e: Exception) {
                            Log.e(TAG, "Erreur suppression message ${message.id}", e)
                        }
                    }
                    Log.d(TAG, "${messages.size} messages traités")
                } else {
                    Log.e(TAG, "Erreur: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception dans syncMessages", e)
                e.printStackTrace()
            }
        }
    }

    suspend fun syncMessage(messageId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val message = messageDao.getMessageById(messageId) ?: run {
                    Log.e(TAG, "Message $messageId non trouvé")
                    return@withContext
                }

                if (message.userFeedback == null) {
                    Log.d(TAG, "Message $messageId sans feedback")
                    return@withContext
                }

                val feedbackBatch = FeedbackBatch(
                    messages = listOf(message.content),
                    labels = listOf(if (message.userFeedback == "ham") 0 else 1)
                )

                try {
                    val response = RetrofitClient.feedbackService.syncFeedback(feedbackBatch)

                    if (response.isSuccessful) {
                        messageDao.deleteMessage(message.id)
                        Log.d(TAG, "Message $messageId synchronisé et supprimé")
                    } else {
                        Log.e(TAG, "Erreur sync message $messageId: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur réseau message $messageId", e)
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception message $messageId", e)
                e.printStackTrace()
            }
        }
    }
}