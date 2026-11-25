package com.example.data.telemetry

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.data.model.DeviceTelemetry
import java.io.File

class DeviceTelemetryRepository(private val context: Context) {

    fun readTelemetry(): DeviceTelemetry {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024L * 1024L)
        val availRamMb = memInfo.availMem / (1024L * 1024L)

        val (usedStorageGb, totalStorageGb) = readInternalStorageGb()
        val (batteryPercent, isCharging, batteryTempC) = readBatteryInfo()
        val maxFreqMhz = readCpuFreqMhz("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
        val curFreqMhz = readCpuFreqMhz("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")

        return DeviceTelemetry(
            deviceModel = Build.MODEL ?: "Android Device",
            manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "",
            androidVersion = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString(),
            sdkInt = Build.VERSION.SDK_INT,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            usedStorageGb = usedStorageGb,
            totalStorageGb = totalStorageGb,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            batteryTempC = batteryTempC,
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            cpuMaxFreqMhz = maxFreqMhz,
            cpuCurFreqMhz = curFreqMhz
        )
    }

    private fun readInternalStorageGb(): Pair<Float, Float> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes.toDouble()
            val availBytes = stat.availableBytes.toDouble()
            val bytesPerGb = 1024.0 * 1024.0 * 1024.0
            val totalGb = (totalBytes / bytesPerGb).toFloat()
            val usedGb = ((totalBytes - availBytes) / bytesPerGb).toFloat().coerceAtLeast(0f)
            usedGb to totalGb
        } catch (_: Exception) {
            0f to 0f
        }
    }

    private fun readBatteryInfo(): Triple<Int, Boolean, Float> {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 0
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = tempTenths / 10f
            Triple(pct.coerceIn(0, 100), charging, tempC)
        } catch (_: Exception) {
            Triple(0, false, 0f)
        }
    }

    private fun readCpuFreqMhz(path: String): Int? {
        return try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return null
            val khz = file.readText().trim().toLongOrNull() ?: return null
            (khz / 1000L).toInt()
        } catch (_: Exception) {
            null
        }
    }
}
