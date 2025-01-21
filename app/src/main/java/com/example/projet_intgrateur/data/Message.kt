package com.example.projet_intgrateur.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val content: String,
    val modelPrediction: String, // "ham" ou "phishing"
    val userFeedback: String?, // "ham" ou "phishing" ou null
    val receivedDate: Date,
    val lastModifiedDate: Date
)