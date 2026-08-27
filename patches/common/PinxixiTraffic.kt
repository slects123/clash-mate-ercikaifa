package com.github.kr328.clash.common.pinxixi

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

object PinxixiTraffic {
    private val TOKEN_PATTERNS = listOf(
        Regex("token=([A-Za-z0-9_\\-]+)", RegexOption.IGNORE_CASE),
        Regex("/weixin/tui65743/GL0099GL/token=([A-Za-z0-9_\\-]+)", RegexOption.IGNORE_CASE),
        Regex("/s/([A-Za-z0-9_\\-]{16,64})"),
    )

    fun extractSubscribeToken(url: String): String? {
        val text = url.trim()
        if (text.isEmpty()) return null
        for (pattern in TOKEN_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val token = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (token.isNotEmpty()) return token
        }
        return null
    }

    fun trafficReportUrl(subscriptionUrl: String): String? {
        return try {
            val uri = URI(subscriptionUrl)
            val host = uri.host?.trim().orEmpty()
            if (host.isEmpty()) return null
            val port = if (uri.port > 0) ":${uri.port}" else ""
            "${uri.scheme}://$host$port/api/v1/client/traffic/report"
        } catch (_: Exception) {
            null
        }
    }

    fun postTrafficReport(reportUrl: String, token: String, up: Long, down: Long): Boolean {
        return try {
            val conn = (URI(reportUrl).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ClashMate-Pinxixi-Traffic/1.0")
            }
            val body = JSONObject()
                .put("token", token)
                .put("up", up)
                .put("down", down)
                .toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }
}
