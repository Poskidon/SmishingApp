package com.example.projet_intgrateur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import com.example.projet_intgrateur.data.Message

@Composable
fun MessageCard(
    message: Message,
    onFeedbackChanged: (Message, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "De: ${message.sender}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Reçu le: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(message.receivedDate)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "IA",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Icon(
                            imageVector = if (message.modelPrediction == "ham")
                                Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Prédiction",
                            tint = if (message.modelPrediction == "ham")
                                Color(0xFF4CAF50) else Color(0xFFE91E63)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Vous",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Icon(
                            imageVector = when (message.userFeedback) {
                                "ham" -> Icons.Default.ThumbUp
                                "phishing" -> Icons.Default.ThumbDown
                                else -> Icons.Default.Help
                            },
                            contentDescription = "Feedback",
                            tint = when (message.userFeedback) {
                                "ham" -> Color(0xFF4CAF50)
                                "phishing" -> Color(0xFFE91E63)
                                else -> Color.Gray
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onFeedbackChanged(message, "ham") },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (message.userFeedback == "ham")
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Sûr")
                }
                OutlinedButton(
                    onClick = { onFeedbackChanged(message, "phishing") },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (message.userFeedback == "phishing")
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("Frauduleux")
                }
            }
        }
    }
}