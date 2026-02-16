package app.lawnchair.bugreport

import app.lawnchair.preferences2.PreferenceManager2
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object UploaderUtils {

    private val katbinService = KatbinService.create()
    private val httpClient = OkHttpClient()
    const val IS_ALIVE_AVAILABLE = true

    suspend fun upload(report: BugReport): String {
        if (BuildConfig.CRASH_REPORT_WEBHOOK.isNotEmpty()) {
            sendToWebhook(BuildConfig.CRASH_REPORT_WEBHOOK, report)
        }

        val body = KatbinUploadBody(KatbinPaste(content = report.contents))
        val result = katbinService.upload(body)
        return "https://katb.in/${result.id}"
    }

    private fun sendToWebhook(url: String, report: BugReport) {
        val json = JSONObject()
        json.put("content", "🚨 **New Crash Detected in AutoCat**")

        val embed = JSONObject()
        embed.put("title", "Crash Report: ${report.type}")
        embed.put("description", "```\n${report.description}\n```")
        embed.put("color", 0xFF0000)

        val field = JSONObject()
        field.put("name", "Details")
        field.put("value", "[View Full Log](${report.link ?: "Log attached below"})")

        val embeds = org.json.JSONArray()
        embeds.put(embed)
        json.put("embeds", embeds)

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                android.util.Log.e("UploaderUtils", "Webhook failed: ${response.code}")
            }
        }
    }
}
