package com.echomusic

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class SongInfo(
    val title: String,
    val artist: String,
    val albumArtUrl: String?,
    val videoId: String
)

class YTMusicProvider {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun searchSong(query: String): List<SongInfo> = withContext(Dispatchers.IO) {
        val url = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"

        val payload = mapOf(
            "context" to mapOf(
                "client" to mapOf(
                    "clientName" to "WEB_REMIX",
                    "clientVersion" to "1.20230620.01.00",
                    "hl" to "en",
                    "gl" to "US"
                )
            ),
            "query" to query
        )

        val jsonPayload = gson.toJson(payload)
        val body = jsonPayload.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .addHeader("X-Goog-Api-Format-Version", "1")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val responseBody = response.body?.string() ?: return@withContext emptyList()
            parseSearchResponse(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseSearchResponse(jsonStr: String): List<SongInfo> {
        val results = mutableListOf<SongInfo>()
        try {
            val json = JSONObject(jsonStr)
            val contents = json.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val section = contents.optJSONObject(i)?.optJSONObject("musicShelfRenderer")
                    if (section != null) {
                        val items = section.optJSONArray("contents")
                        if (items != null) {
                            for (j in 0 until items.length()) {
                                val item = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer")
                                if (item != null) {
                                    val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
                                        ?: item.optJSONObject("doubleTapCommand")?.optJSONObject("watchEndpoint")?.optString("videoId")

                                    val flexColumns = item.optJSONArray("flexColumns")
                                    val titleObj = flexColumns?.optJSONObject(0)
                                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")
                                        ?.optJSONArray("runs")?.optJSONObject(0)
                                    val title = titleObj?.optString("text") ?: ""

                                    val artistObj = flexColumns?.optJSONObject(1)
                                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                        ?.optJSONObject("text")
                                        ?.optJSONArray("runs")?.optJSONObject(0)
                                    val artist = artistObj?.optString("text") ?: ""

                                    val thumbnails = item.optJSONObject("thumbnail")
                                        ?.optJSONObject("musicThumbnailRenderer")
                                        ?.optJSONObject("thumbnail")
                                        ?.optJSONArray("thumbnails")

                                    var albumArtUrl: String? = null
                                    if (thumbnails != null && thumbnails.length() > 0) {
                                        albumArtUrl = thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
                                    }

                                    if (videoId != null && videoId.isNotEmpty() && title.isNotEmpty()) {
                                        results.add(SongInfo(title, artist, albumArtUrl, videoId))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }
}