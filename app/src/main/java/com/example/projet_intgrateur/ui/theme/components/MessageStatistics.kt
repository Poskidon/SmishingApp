package com.example.projet_intgrateur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projet_intgrateur.data.Message

@Composable
fun MessageStatistics(
    messages: List<Message>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistiques des Messages",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            val totalMessages = messages.size
            val hamPredictions = messages.count { it.modelPrediction == "ham" }
            val phishingPredictions = messages.count { it.modelPrediction == "phishing" }
            val hamFeedback = messages.count { it.userFeedback == "ham" }
            val phishingFeedback = messages.count { it.userFeedback == "phishing" }
            val pendingFeedback = messages.count { it.userFeedback == null }

            StatisticRow("Total des messages", totalMessages)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Prédictions du modèle:",
                style = MaterialTheme.typography.titleMedium
            )
            StatisticRow("Messages sûrs", hamPredictions)
            StatisticRow("Messages frauduleux", phishingPredictions)

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Retours utilisateur:",
                style = MaterialTheme.typography.titleMedium
            )
            StatisticRow("Confirmés sûrs", hamFeedback)
            StatisticRow("Signalés frauduleux", phishingFeedback)
            StatisticRow("En attente de retour", pendingFeedback)
        }
    }
}

@Composable
private fun StatisticRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}