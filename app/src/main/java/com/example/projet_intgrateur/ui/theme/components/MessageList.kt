package com.example.projet_intgrateur.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projet_intgrateur.data.Message

@Composable
fun MessageList(
    messages: List<Message>,
    onFeedbackChanged: (Message, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("MessageList", "Nombre de messages reçus : ${messages.size}")

    if (messages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aucun message reçu",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                MessageCard(
                    message = message,
                    onFeedbackChanged = onFeedbackChanged
                )
            }
        }
    }
}