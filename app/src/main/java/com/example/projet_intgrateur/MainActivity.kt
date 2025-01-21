package com.example.projet_intgrateur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.projet_intgrateur.api.SmsAnalyzer
import com.example.projet_intgrateur.service.SmsMonitoringService
import kotlinx.coroutines.launch
import com.example.projet_intgrateur.data.AppDatabase
import com.example.projet_intgrateur.data.Message
import com.example.projet_intgrateur.data.MessageRepository
import com.example.projet_intgrateur.ui.components.MessageList
import com.example.projet_intgrateur.ui.components.MessageStatistics
import com.example.projet_intgrateur.ui.theme.Projet_intégrateurTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val inputText = mutableStateOf("")
    private val smsAnalyzer = SmsAnalyzer()
    private val analysisResult = mutableStateOf<String?>(null)
    private val isAnalyzing = mutableStateOf(false)
    private val resultType = mutableStateOf<ResultType?>(null)
    private val selectedTab = mutableIntStateOf(0)
    private lateinit var messageRepository: MessageRepository
    private val messages = mutableStateOf<List<Message>>(emptyList())

    enum class ResultType {
        SAFE,
        DANGER,
        ERROR
    }

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeApp()
        } else {
            Toast.makeText(this, "Permissions nécessaires non accordées", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(applicationContext)
        messageRepository = MessageRepository(database.messageDao())


        checkAndRequestPermissions()

        setContent {
            LaunchedEffect(Unit) {
                messageRepository.allMessages.collect { newMessages ->
                    messages.value = newMessages
                }
            }
            Projet_intégrateurTheme {
                var showTips by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        Column {
                            TopAppBar(
                                title = { Text("SMS Protection") },
                                actions = {
                                    IconButton(onClick = { showTips = !showTips }) {
                                        Icon(Icons.Default.Info, "Information")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            TabRow(selectedTabIndex = selectedTab.intValue) {
                                Tab(
                                    selected = selectedTab.intValue == 0,
                                    onClick = { selectedTab.intValue = 0 },
                                    text = { Text("Analyse") }
                                )
                                Tab(
                                    selected = selectedTab.intValue == 1,
                                    onClick = { selectedTab.intValue = 1 },
                                    text = { Text("Historique") }
                                )
                                Tab(
                                    selected = selectedTab.intValue == 2,
                                    onClick = { selectedTab.intValue = 2 },
                                    text = { Text("Statistiques") }
                                )

                            }
                        }
                    }
                ) { paddingValues ->
                    when (selectedTab.intValue) {
                        0 -> AnalysisTab(
                            paddingValues = paddingValues,
                            showTips = showTips,
                            inputText = inputText.value,
                            onInputTextChange = { inputText.value = it },
                            analysisResult = analysisResult.value,
                            resultType = resultType.value,
                            isAnalyzing = isAnalyzing.value,
                            onAnalyzeClick = {
                                if (inputText.value.isNotBlank()) {
                                    analyzeMessage()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Veuillez entrer un message à analyser",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        1 -> HistoryTab(paddingValues = paddingValues)
                        2 -> MessageStatistics(
                            messages = messages.value,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun TipsCard() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Conseils de sécurité :",
                    fontWeight = FontWeight.Bold
                )
                Text("• Ne cliquez jamais sur des liens suspects")
                Text("• Ne partagez pas vos informations personnelles")
                Text("• Méfiez-vous des offres trop belles pour être vraies")
                Text("• Vérifiez toujours l'expéditeur")
            }
        }
    }

    @Composable
    fun ResultCard(analysisResult: String?, resultType: ResultType?) {
        val backgroundColor = when (resultType) {
            ResultType.SAFE -> MaterialTheme.colorScheme.primaryContainer
            ResultType.DANGER -> MaterialTheme.colorScheme.errorContainer
            ResultType.ERROR -> MaterialTheme.colorScheme.surfaceVariant
            null -> MaterialTheme.colorScheme.surfaceVariant
        }

        val textColor = when (resultType) {
            ResultType.SAFE -> MaterialTheme.colorScheme.onPrimaryContainer
            ResultType.DANGER -> MaterialTheme.colorScheme.onErrorContainer
            ResultType.ERROR -> MaterialTheme.colorScheme.onSurfaceVariant
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (resultType) {
                    ResultType.SAFE -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    ResultType.DANGER -> Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    else -> Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = analysisResult ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    fun AnalyzeButton(
        isAnalyzing: Boolean,
        enabled: Boolean,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = enabled && !isAnalyzing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAnalyzing) {
                    Text("Analyse en cours...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Analyser le message",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    @Composable
    fun AnalysisTab(
        paddingValues: PaddingValues,
        showTips: Boolean,
        inputText: String,
        onInputTextChange: (String) -> Unit,
        analysisResult: String?,
        resultType: ResultType?,
        isAnalyzing: Boolean,
        onAnalyzeClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = showTips,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                TipsCard()
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                label = { Text("Entrez le message à analyser") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            AnimatedVisibility(
                visible = analysisResult != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ResultCard(analysisResult = analysisResult, resultType = resultType)
            }

            AnalyzeButton(
                isAnalyzing = isAnalyzing,
                enabled = !isAnalyzing && inputText.isNotBlank(),
                onClick = onAnalyzeClick
            )
        }
    }

    @Composable
    fun HistoryTab(paddingValues: PaddingValues) {
        MessageList(
            messages = messages.value,
            onFeedbackChanged = { message, feedback ->
                lifecycleScope.launch {
                    messageRepository.updateUserFeedback(message.id, feedback)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }

    private fun analyzeMessage() {
        val messageToAnalyze = inputText.value
        if (messageToAnalyze.isBlank()) return

        isAnalyzing.value = true

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    smsAnalyzer.analyzeMessage(messageToAnalyze)
                }

                result.fold(
                    onSuccess = { prediction ->
                        resultType.value = if (prediction == "phishing") {
                            ResultType.DANGER
                        } else {
                            ResultType.SAFE
                        }
                        analysisResult.value = when (prediction) {
                            "phishing" -> """
                            Attention : Ce message présente des risques
                            
                            Recommandations :
                            • Ne cliquez sur aucun lien dans ce message
                            • Ne communiquez pas d'informations personnelles
                            • Pour toute vérification, contactez directement votre établissement
                        """.trimIndent()
                            else -> """
                            Message analysé : Aucune menace détectée
                            
                            L'analyse n'a révélé aucun indicateur de fraude dans ce message.
                            Vous pouvez le traiter normalement.
                        """.trimIndent()
                        }
                    },
                    onFailure = {
                        resultType.value = ResultType.ERROR
                        analysisResult.value = """
                        L'analyse n'a pas pu être effectuée
                        
                        Veuillez vérifier votre connexion internet et réessayer.
                    """.trimIndent()
                    }
                )
            } catch (e: Exception) {
                resultType.value = ResultType.ERROR
                analysisResult.value = "Service temporairement indisponible. Veuillez réessayer plus tard."
            } finally {
                isAnalyzing.value = false
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()

        requiredPermissions.add(Manifest.permission.RECEIVE_SMS)
        requiredPermissions.add(Manifest.permission.READ_SMS)
        requiredPermissions.add(Manifest.permission.READ_PHONE_STATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        val missingPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            initializeApp()
        }
    }

    private fun initializeApp() {
        try {
            createNotificationChannel()
            startSmsMonitoringService()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur d'initialisation : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertes SMS"
            val descriptionText = "Canal pour les alertes de sécurité SMS"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("SMS_ALERTS", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startSmsMonitoringService() {
        try {
            Intent(this, SmsMonitoringService::class.java).also { intent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}