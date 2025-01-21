// SmsReceiver.kt
package com.example.projet_intgrateur

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.Telephony
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.projet_intgrateur.api.SmsAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.projet_intgrateur.data.AppDatabase
import com.example.projet_intgrateur.data.MessageRepository
import java.util.Date

class SmsReceiver : BroadcastReceiver() {
    private val smsAnalyzer = SmsAnalyzer()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val TAG = "SmsReceiver"

    companion object {
        const val ACTION_MARK_PHISHING = "com.example.projet_intgrateur.ACTION_MARK_PHISHING"
        const val ACTION_MARK_SAFE = "com.example.projet_intgrateur.ACTION_MARK_SAFE"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_MESSAGE_CONTENT = "message_content"
        const val EXTRA_SENDER = "sender"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MARK_PHISHING -> handleUserFeedback(context, intent, isPhishing = true)
            ACTION_MARK_SAFE -> handleUserFeedback(context, intent, isPhishing = false)
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> handleIncomingSms(context, intent)
        }
    }

    private fun handleIncomingSms(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages?.forEach { smsMessage ->
            val sender = smsMessage.originatingAddress
            val messageBody = smsMessage.messageBody

            Log.d(TAG, "SMS reçu de: $sender")

            scope.launch {
                try {
                    val result = smsAnalyzer.analyzeMessage(messageBody)
                    result.onSuccess { prediction ->
                        val isPhishing = prediction == "phishing"
                        Log.d(TAG, "Analyse terminée. Résultat: $prediction")

                        val savedMessageId = saveMessageToDatabase(
                            context,
                            sender ?: "Inconnu",
                            messageBody,
                            prediction
                        )

                        Log.d(TAG, "Message sauvegardé avec ID: $savedMessageId")

                        sendInteractiveNotification(
                            context,
                            savedMessageId,
                            sender,
                            messageBody,
                            isPhishing
                        )
                    }.onFailure { exception ->
                        Log.e(TAG, "Erreur lors de l'analyse", exception)
                        sendInteractiveNotification(
                            context,
                            System.currentTimeMillis(),
                            sender,
                            messageBody,
                            isError = true
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception lors de l'analyse", e)
                }
            }
        }
    }

    private suspend fun saveMessageToDatabase(
        context: Context,
        sender: String,
        content: String,
        prediction: String
    ): Long {
        return try {
            val repository = MessageRepository(AppDatabase.getDatabase(context).messageDao())
            repository.insertMessage(
                sender = sender,
                content = content,
                modelPrediction = prediction
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde en base de données", e)
            throw e
        }
    }

    private fun handleUserFeedback(context: Context, intent: Intent, isPhishing: Boolean) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1)
        val messageContent = intent.getStringExtra(EXTRA_MESSAGE_CONTENT) ?: ""
        val sender = intent.getStringExtra(EXTRA_SENDER) ?: ""

        Log.d(TAG, "Feedback reçu pour message $messageId: ${if (isPhishing) "phishing" else "ham"}")

        scope.launch {
            try {
                val repository = MessageRepository(AppDatabase.getDatabase(context).messageDao())
                repository.updateUserFeedback(messageId, if (isPhishing) "phishing" else "ham")

                Log.d(TAG, "Feedback enregistré pour message $messageId")

                sendFeedbackNotification(
                    context,
                    messageId,
                    sender,
                    messageContent,
                    isPhishing
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur mise à jour feedback: $messageId", e)
            }
        }
    }

    private fun createFeedbackIntent(
        context: Context,
        action: String,
        messageId: Long,
        messageContent: String,
        sender: String
    ): PendingIntent {
        val intent = Intent(context, SmsReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_MESSAGE_ID, messageId)
            putExtra(EXTRA_MESSAGE_CONTENT, messageContent)
            putExtra(EXTRA_SENDER, sender)
        }
        return PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun sendInteractiveNotification(
        context: Context,
        messageId: Long,
        sender: String?,
        message: String,
        isPhishing: Boolean = false,
        isError: Boolean = false
    ) {
        createNotificationChannel(context)
        val channelId = "SMS_ALERTS"

        val (title, description, style) = when {
            isError -> Triple(
                "⚠️ Vérification nécessaire",
                "Impossible d'analyser automatiquement ce message",
                NotificationCompat.BigTextStyle()
                    .bigText("""
                        Le système n'a pas pu analyser ce message.
                        Merci de nous aider en indiquant s'il s'agit d'une tentative de fraude.
                        
                        Expéditeur: $sender
                        Message: $message
                    """.trimIndent())
            )
            isPhishing -> Triple(
                "🚨 Message Suspect Détecté",
                "Ce message semble être une tentative de fraude",
                NotificationCompat.BigTextStyle()
                    .bigText("""
                        Notre système a détecté ce message comme potentiellement frauduleux.
                        
                        ⚠️ Précautions à prendre:
                        • Ne cliquez sur aucun lien
                        • Ne communiquez pas d'informations personnelles
                        • Ne rappelez pas de numéros inconnus
                        
                        Expéditeur: $sender
                        Message: $message
                        
                        Cette analyse est-elle correcte?
                    """.trimIndent())
            )
            else -> Triple(
                "✅ Message Analysé",
                "Ce message semble légitime",
                NotificationCompat.BigTextStyle()
                    .bigText("""
                        Notre système a analysé ce message comme étant sûr.
                        
                        Expéditeur: $sender
                        Message: $message
                        
                        Cette analyse est-elle correcte?
                    """.trimIndent())
            )
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(
                when {
                    isPhishing -> android.R.drawable.ic_dialog_alert
                    isError -> android.R.drawable.ic_dialog_info
                    else -> android.R.drawable.ic_dialog_email
                }
            )
            .setContentTitle(title)
            .setContentText(description)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(
                when {
                    isPhishing -> Color.RED
                    isError -> Color.YELLOW
                    else -> Color.GREEN
                }
            )

        if (!isError) {
            builder.addAction(
                android.R.drawable.ic_dialog_alert,
                "Frauduleux",
                createFeedbackIntent(context, ACTION_MARK_PHISHING, messageId, message, sender ?: "")
            )
            builder.addAction(
                android.R.drawable.ic_dialog_info,
                "Sûr",
                createFeedbackIntent(context, ACTION_MARK_SAFE, messageId, message, sender ?: "")
            )
        }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(messageId.toInt(), builder.build())
            }
        }
    }

    private fun sendFeedbackNotification(
        context: Context,
        messageId: Long,
        sender: String,
        message: String,
        isPhishing: Boolean
    ) {
        val channelId = "SMS_ALERTS"
        val title = if (isPhishing) "Message marqué comme frauduleux" else "Message marqué comme sûr"
        val description = if (isPhishing) {
            "Merci de votre vigilance! Ce message a été marqué comme frauduleux."
        } else {
            "Message confirmé comme sûr. Merci de votre participation!"
        }

        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(
                if (isPhishing) android.R.drawable.ic_dialog_alert
                else android.R.drawable.ic_dialog_info
            )
            .setContentTitle(title)
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("""
                    $description
                    
                    Expéditeur: $sender
                    Message: $message
                """.trimIndent()))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(if (isPhishing) Color.RED else Color.GREEN)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(messageId.toInt(), builder.build())
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "SMS Security Alerts"
            val descriptionText = "Alertes de sécurité pour les SMS"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("SMS_ALERTS", name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}