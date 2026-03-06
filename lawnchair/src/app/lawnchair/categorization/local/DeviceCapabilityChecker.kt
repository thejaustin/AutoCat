package app.lawnchair.categorization.local

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import app.lawnchair.categorization.llm.HttpClientFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class DeviceCapabilities(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val supportedAbis: List<String>,
    val androidVersion: Int,
    val isAiCoreAvailable: Boolean,
    val gpuFamily: GpuFamily,
)

enum class GpuFamily { ADRENO, XCLIPSE, MALI, UNKNOWN }

object DeviceCapabilityChecker {

    const val RAM_MB_TINYLLAMA = 3_500L
    const val RAM_MB_GEMMA_2B = 5_500L
    const val RAM_MB_GEMMA_7B = 9_000L

    private val pingClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    fun getCapabilities(context: Context): DeviceCapabilities {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availableRamMb = memInfo.availMem / (1024 * 1024)

        return DeviceCapabilities(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            androidVersion = Build.VERSION.SDK_INT,
            isAiCoreAvailable = probeAiCoreAvailable(),
            gpuFamily = detectGpuFamily(),
        )
    }

    suspend fun isLocalServerReachable(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl.trimEnd('/') + "/api/version"
            val request = Request.Builder().url(url).get().build()
            pingClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun probeAiCoreAvailable(): Boolean {
        return try {
            Class.forName("android.app.ondeviceintelligence.OnDeviceIntelligenceManager")
            Build.VERSION.SDK_INT >= 35
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun detectGpuFamily(): GpuFamily {
        val hardware = Build.HARDWARE.lowercase()
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.lowercase()
        } else {
            ""
        }
        return when {
            hardware.contains("qcom") || hardware.contains("msm") ||
                soc.contains("snapdragon") || soc.contains("sm") -> GpuFamily.ADRENO

            hardware.contains("s5e") || soc.contains("exynos") -> GpuFamily.XCLIPSE

            hardware.contains("mt") || hardware.contains("mediatek") ||
                soc.contains("dimensity") -> GpuFamily.MALI

            else -> GpuFamily.UNKNOWN
        }
    }
}
