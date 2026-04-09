/*
 * Copyright 2021, AutoCat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.update

import android.util.Log
import com.android.launcher3.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

enum class UpdateChannel { STABLE, DEV }

data class UpdateInfo(
    val versionName: String,
    val buildNum: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String,
    val isPrerelease: Boolean,
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val RELEASES_URL = "https://api.github.com/repos/thejaustin/AutoCat/releases"
    private const val BUILD_REGEX_PATTERN = """autocat\.(\d+)"""

    private val buildRegex = Regex(BUILD_REGEX_PATTERN)

    /** Extract build number from a tag like `v16.0.dev-autocat.418` */
    private fun parseBuildNum(tag: String): Int? {
        return buildRegex.find(tag)?.groupValues?.get(1)?.toIntOrNull()
    }

    /** Fetch current build number from BuildConfig.VERSION_NAME */
    private fun currentBuildNum(): Int? = parseBuildNum(BuildConfig.VERSION_NAME)

    /**
     * Fetch releases list from GitHub API (first page only — newest first).
     * Returns the raw JSON array string or null on error.
     */
    private fun fetchReleasesJson(): JSONArray? {
        return try {
            val url = URL(RELEASES_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "AutoCat/${BuildConfig.VERSION_NAME}")
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.connect()
            if (conn.responseCode != 200) {
                Log.w(TAG, "HTTP ${conn.responseCode} fetching releases")
                return null
            }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            JSONArray(body)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching releases", e)
            null
        }
    }

    /**
     * Check for an update on the given channel.
     * Returns [UpdateInfo] if a newer build is available, null otherwise.
     */
    suspend fun checkForUpdate(channel: UpdateChannel): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentBuild = currentBuildNum()
            if (currentBuild == null) {
                Log.w(TAG, "Could not parse current build number from VERSION_NAME: ${BuildConfig.VERSION_NAME}")
                return@withContext null
            }

            val releases = fetchReleasesJson() ?: return@withContext null

            // Find the matching release for the requested channel
            val release = (0 until releases.length())
                .map { releases.getJSONObject(it) }
                .firstOrNull { release ->
                    val tag = release.getString("tag_name")
                    when (channel) {
                        UpdateChannel.STABLE -> tag.contains("b1-autocat.") && !tag.contains("dev")
                        UpdateChannel.DEV -> tag.contains("dev-autocat.")
                    }
                } ?: run {
                Log.d(TAG, "No matching release found for channel $channel")
                return@withContext null
            }

            val tag = release.getString("tag_name")
            val remoteBuild = parseBuildNum(tag)
            if (remoteBuild == null) {
                Log.w(TAG, "Could not parse build number from tag: $tag")
                return@withContext null
            }

            if (remoteBuild <= currentBuild) {
                Log.d(TAG, "Already up to date (current=$currentBuild, remote=$remoteBuild)")
                return@withContext null
            }

            // Find APK asset
            val assets = release.getJSONArray("assets")
            val apkUrl = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk") }
                ?.getString("browser_download_url")
                ?: run {
                    Log.w(TAG, "No APK asset found in release $tag")
                    return@withContext null
                }

            UpdateInfo(
                versionName = tag,
                buildNum = remoteBuild,
                downloadUrl = apkUrl,
                releaseNotes = release.optString("body", ""),
                publishedAt = release.optString("published_at", ""),
                isPrerelease = release.optBoolean("prerelease", false),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update", e)
            null
        }
    }
}
