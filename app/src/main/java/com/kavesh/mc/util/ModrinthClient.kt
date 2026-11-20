package com.kavesh.mc.util

import com.kavesh.mc.model.ModrinthProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ModrinthClient {
    suspend fun searchProjects(query: String, page: Int = 1, limit: Int = 10): Pair<List<ModrinthProject>, Int> =
        withContext(Dispatchers.IO) {
            val offset = (page - 1) * limit
            val encoded = URLEncoder.encode(query, "UTF-8")
            val urlStr = "https://api.modrinth.com/v2/search?query=$encoded&limit=$limit&offset=$offset"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Kavesh-MC/1.0.0 (android)")
                connectTimeout = 10000
                readTimeout = 10000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val totalHits = json.optInt("total_hits", 0)
            val hits = json.optJSONArray("hits") ?: JSONArray()
            val items = mutableListOf<ModrinthProject>()
            for (i in 0 until hits.length()) {
                val obj = hits.getJSONObject(i)
                items.add(
                    ModrinthProject(
                        projectId = obj.optString("project_id"),
                        slug = obj.optString("slug"),
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                        iconUrl = obj.optString("icon_url").takeIf { it.isNotBlank() },
                        downloads = obj.optInt("downloads", 0)
                    )
                )
            }
            val totalPages = ((totalHits + limit - 1) / limit).coerceAtLeast(1)
            Pair(items, totalPages)
        }

    suspend fun downloadLatestVersion(projectId: String, targetDir: File): String =
        withContext(Dispatchers.IO) {
            targetDir.mkdirs()
            val verUrl = "https://api.modrinth.com/v2/project/$projectId/version"
            val conn = (URL(verUrl).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Kavesh-MC/1.0.0 (android)")
                connectTimeout = 10000
                readTimeout = 10000
            }
            val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
            if (arr.length() == 0) throw IllegalStateException("No downloadable versions found")
            val files = arr.getJSONObject(0).getJSONArray("files")
            val primary = files.getJSONObject(0)
            val fileUrl = primary.getString("url")
            val fileName = primary.getString("filename")

            val outFile = File(targetDir, fileName)
            val dlConn = (URL(fileUrl).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", "Kavesh-MC/1.0.0 (android)")
            }
            dlConn.inputStream.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            fileName
        }
}
