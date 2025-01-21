package com.example.projet_intgrateur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.projet_intgrateur.data.Message

@Composable
fun MainScreen(
    messages: List<Message>,
    onFeedbackChanged: (Message, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Historique") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Statistiques") }
            )
        }

        when (selectedTab) {
            0 -> MessageList(
                messages = messages,
                onFeedbackChanged = onFeedbackChanged
            )
            1 -> MessageStatistics(messages = messages)
        }
    }
}