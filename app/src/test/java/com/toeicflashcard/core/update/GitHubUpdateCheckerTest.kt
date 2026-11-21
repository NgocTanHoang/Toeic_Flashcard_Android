package com.toeicflashcard.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateCheckerTest {

    @Test
    fun `isNewerVersion compares semantic version numbers numerically`() {
        assertTrue(GitHubUpdateChecker.isNewerVersion("v1.2.1", "1.2.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("v1.10.0", "1.2.0"))
        assertTrue(GitHubUpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.2.0", "1.2.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.1.9", "1.2.0"))
        assertFalse(GitHubUpdateChecker.isNewerVersion("v1.2.0-beta.1", "1.2.0"))
    }

    @Test
    fun `parseReleaseResponse prefers direct apk asset url when newer`() {
        val json = """
            {
              "tag_name": "v1.3.0",
              "html_url": "https://github.com/NgocTanHoang/Toeic_Flashcard_Android/releases/tag/v1.3.0",
              "body": "Cải thiện hiệu năng 120Hz",
              "assets": [
                {
                  "name": "checksums.txt",
                  "browser_download_url": "https://example.com/checksums.txt"
                },
                {
                  "name": "TOEIC-Flashcard-v1.3.0.apk",
                  "browser_download_url": "https://github.com/NgocTanHoang/Toeic_Flashcard_Android/releases/download/v1.3.0/TOEIC-Flashcard-v1.3.0.apk"
                }
              ]
            }
        """.trimIndent()

        val result = GitHubUpdateChecker.parseReleaseResponse(json, "1.2.0")
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val available = result as UpdateCheckResult.UpdateAvailable
        assertEquals("1.3.0", available.latestVersion)
        assertEquals(
            "https://github.com/NgocTanHoang/Toeic_Flashcard_Android/releases/download/v1.3.0/TOEIC-Flashcard-v1.3.0.apk",
            available.downloadUrl
        )
        assertEquals("Cải thiện hiệu năng 120Hz", available.releaseNotes)
    }

    @Test
    fun `parseReleaseResponse falls back to release html url without apk asset`() {
        val json = """
            {
              "tag_name": "v1.2.5",
              "html_url": "https://github.com/NgocTanHoang/Toeic_Flashcard_Android/releases/tag/v1.2.5",
              "body": "Bản vá lỗi nhỏ",
              "assets": []
            }
        """.trimIndent()

        val result = GitHubUpdateChecker.parseReleaseResponse(json, "1.2.0")
        assertEquals(
            UpdateCheckResult.UpdateAvailable(
                latestVersion = "1.2.5",
                downloadUrl = "https://github.com/NgocTanHoang/Toeic_Flashcard_Android/releases/tag/v1.2.5",
                releaseNotes = "Bản vá lỗi nhỏ"
            ),
            result
        )
    }

    @Test
    fun `parseReleaseResponse returns UpToDate when version matches`() {
        val json = """
            {
              "tag_name": "v1.2.0",
              "html_url": "https://github.com/NgocTanHoang/Toeic_Flashcard_Android/releases/tag/v1.2.0",
              "assets": []
            }
        """.trimIndent()

        val result = GitHubUpdateChecker.parseReleaseResponse(json, "1.2.0")
        assertEquals(UpdateCheckResult.UpToDate("1.2.0"), result)
    }

    @Test
    fun `parseReleaseResponse handles malformed json, empty tag, and missing html_url gracefully`() {
        val malformed = GitHubUpdateChecker.parseReleaseResponse("{bad-json", "1.2.0")
        assertTrue(malformed is UpdateCheckResult.Error)

        val emptyTag = GitHubUpdateChecker.parseReleaseResponse("""{"tag_name": ""}""", "1.2.0")
        assertEquals(UpdateCheckResult.UpToDate("1.2.0"), emptyTag)

        val noHtmlOrAssets = GitHubUpdateChecker.parseReleaseResponse("""{"tag_name": "v2.0.0"}""", "1.2.0")
        assertTrue(noHtmlOrAssets is UpdateCheckResult.UpdateAvailable)
        assertEquals(
            "https://github.com/NgocTanHoang/Toeic_Flashcard_Android/releases/latest",
            (noHtmlOrAssets as UpdateCheckResult.UpdateAvailable).downloadUrl
        )
    }
}
