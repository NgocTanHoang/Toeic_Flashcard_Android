package com.toeicflashcard.core.update

import com.toeicflashcard.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    ) : UpdateCheckResult

    data class UpToDate(val currentVersion: String) : UpdateCheckResult

    data class Error(val message: String) : UpdateCheckResult
}

/**
 * Opt-in offline-first updater using pure Java [HttpURLConnection].
 * Connects to GitHub Releases API only when the user explicitly taps "Kiểm tra cập nhật".
 */
object GitHubUpdateChecker {

    private const val LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/NgocTanHoang/Toeic_Flashcard_Android/releases/latest"
    private const val RELEASES_PAGE_URL =
        "https://github.com/NgocTanHoang/Toeic_Flashcard_Android/releases/latest"
    private const val TIMEOUT_MS = 8_000

    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdates(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(LATEST_RELEASE_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "TOEIC-Flashcard-Android/$currentVersion")
            }

            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parseReleaseResponse(body, currentVersion)
                }

                HttpURLConnection.HTTP_NOT_FOUND -> UpdateCheckResult.UpToDate(currentVersion)
                else -> UpdateCheckResult.Error("Máy chủ phản hồi mã $code. Vui lòng thử lại sau.")
            }
        } catch (_: Exception) {
            UpdateCheckResult.Error("Không thể kết nối mạng. Vui lòng thử lại sau.")
        } finally {
            connection?.disconnect()
        }
    }

    internal fun parseReleaseResponse(
        jsonBody: String,
        currentVersion: String
    ): UpdateCheckResult {
        return try {
            val root = jsonParser.parseToJsonElement(jsonBody).jsonObject
            val tagName = root["tag_name"]?.jsonPrimitive?.content.orEmpty()
            val cleanLatest = normalizeVersion(tagName)
            val cleanCurrent = normalizeVersion(currentVersion)

            if (cleanLatest.isEmpty() || !isNewerVersion(cleanLatest, cleanCurrent)) {
                return UpdateCheckResult.UpToDate(cleanCurrent.ifEmpty { currentVersion })
            }

            val assets = root["assets"]?.jsonArray
            val apkDownloadUrl = assets
                ?.asSequence()
                ?.mapNotNull { element -> runCatching { element.jsonObject }.getOrNull() }
                ?.firstOrNull { asset ->
                    val name = asset["name"]?.jsonPrimitive?.content.orEmpty()
                    name.endsWith(".apk", ignoreCase = true)
                }
                ?.get("browser_download_url")
                ?.jsonPrimitive
                ?.content

            val htmlUrl = root["html_url"]?.jsonPrimitive?.content
            val downloadUrl = apkDownloadUrl ?: htmlUrl ?: RELEASES_PAGE_URL
            val releaseNotes = root["body"]?.jsonPrimitive?.content.orEmpty()

            UpdateCheckResult.UpdateAvailable(
                latestVersion = cleanLatest,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes
            )
        } catch (_: Exception) {
            UpdateCheckResult.Error("Dữ liệu phiên bản không hợp lệ.")
        }
    }

    internal fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val latestParts = parseVersionParts(latestTag)
        val currentParts = parseVersionParts(currentVersion)
        if (latestParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val latestSegment = latestParts.getOrElse(i) { 0 }
            val currentSegment = currentParts.getOrElse(i) { 0 }
            if (latestSegment != currentSegment) {
                return latestSegment > currentSegment
            }
        }
        return false
    }

    private fun normalizeVersion(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").substringBefore('-')

    private fun parseVersionParts(raw: String): List<Int> =
        normalizeVersion(raw)
            .split('.')
            .mapNotNull { it.toIntOrNull() }
}
