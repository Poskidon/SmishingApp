package com.example.projet_intgrateur.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY receivedDate DESC")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Query("SELECT COUNT(*) FROM messages WHERE modelPrediction = :prediction")
    suspend fun getCountByPrediction(prediction: String): Int

    @Query("SELECT COUNT(*) FROM messages WHERE userFeedback = :feedback")
    suspend fun getCountByUserFeedback(feedback: String): Int
}