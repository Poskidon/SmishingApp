package com.example.projet_intgrateur.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.projet_intgrateur.monitoring.BatteryMetrics
import com.example.projet_intgrateur.monitoring.BatteryMonitor
import kotlinx.coroutines.flow.Flow

@Composable
fun BatteryMonitorScreen(
    metricsFlow: Flow<BatteryMetrics>,
    modifier: Modifier = Modifier
) {
    var currentMetrics by remember { mutableStateOf<BatteryMetrics?>(null) }

    LaunchedEffect(metricsFlow) {
        metricsFlow.collect { metrics ->
            currentMetrics = metrics
        }
    }

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
                text = "Battery Monitor",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            currentMetrics?.let { metrics ->
                MetricsRow("Battery Level", "${metrics.batteryLevel}%")
                MetricsRow("Charging Status", if (metrics.isCharging) "Charging" else "Not Charging")
                MetricsRow("CPU Usage", String.format("%.1f%%", metrics.cpuUsage))
                MetricsRow("Network Usage", "${metrics.networkUsage / 1024} KB")
            } ?: Text("Loading metrics...")
        }
    }
}

@Composable
private fun MetricsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}