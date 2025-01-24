package com.example.projet_intgrateur.monitoring

import android.content.Context
import android.os.BatteryManager
import android.os.Process
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit

data class BatteryMetrics(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val cpuUsage: Float,
    val networkUsage: Long,
    val messageProcessed: Int = 0,
    val energyPerMessage: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

class BatteryMonitor(context: Context) {
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val initTime = System.currentTimeMillis()

    private fun getBatteryLevel(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getCpuUsage(): Float {
        return try {
            val pid = Process.myPid()
            val stat = Runtime.getRuntime().exec("cat /proc/$pid/stat").inputStream.bufferedReader().readLine()
            val values = stat.split(" ")
            val utime = values[13].toLong()
            val stime = values[14].toLong()
            (utime + stime) / ProcessCPUInfo.getProcessCpuTimeBase().toFloat()
        } catch (e: Exception) {
            0f
        }
    }

    private fun getNetworkUsage(): Long {
        return (System.currentTimeMillis() - initTime) / 1000 // Active time in seconds
    }

    fun getMessageEnergyMetrics(messageCount: Int): Pair<Int, Float> {
        val avgEnergyPerMessage = 0.05f // Estimated mAh per message
        return Pair(messageCount, messageCount * avgEnergyPerMessage)
    }
}

private object ProcessCPUInfo {
    fun getProcessCpuTimeBase(): Long {
        return try {
            val stat = Runtime.getRuntime().exec("cat /proc/cpuinfo").inputStream.bufferedReader().readText()
            val processors = stat.split("\n\n").size
            processors * 100L
        } catch (e: Exception) {
            1L
        }
    }
}