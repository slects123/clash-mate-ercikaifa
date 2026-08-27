package com.github.kr328.clash.service

import android.content.Context
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.pinxixi.Pinxixi
import com.github.kr328.clash.service.data.ImportedDao
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.util.importedDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 拼夕夕同步守护：
 * 1) 每 10 秒（VPN 开着也跑）强制拉面板订阅并重载内核
 * 2) 到期 / 即将到期：先清本地缓存再拉
 * 3) 轮询 165 Webhook epoch，面板渔云节点变更立即同步
 */
object PinxixiSyncLoop {
    private val started = AtomicBoolean(false)
    private val lastEpoch = AtomicLong(-1L)

    fun ensureStarted(context: Context) {
        if (!started.compareAndSet(false, true)) {
            return
        }

        val app = context.applicationContext
        Global.launch(Dispatchers.IO) {
            Log.i("拼夕夕同步守护：每 ${Pinxixi.SYNC_INTERVAL_SECONDS}s / Webhook epoch / 到期强制清缓存")
            while (isActive) {
                try {
                    tick(app)
                } catch (e: Exception) {
                    Log.w("PinxixiSyncLoop: ${e.message}", e)
                }
                delay(Pinxixi.SYNC_INTERVAL_MS)
            }
        }
    }

    private fun tick(context: Context) {
        val vpnOn = StatusProvider.serviceRunning
        val webhookForce = pollWebhookForce()
        val expireForce = anyPinxixiExpired(context)

        val reason = when {
            expireForce -> "expire_force"
            webhookForce -> "webhook_epoch"
            vpnOn -> "vpn_active"
            else -> "interval"
        }

        // VPN 使用中 / 到期 / Webhook 变更：一律强制同步（清缓存 + 拉新节点 + 内核重载）
        if (vpnOn || webhookForce || expireForce) {
            forceSyncAll(context, reason, clearCache = expireForce || webhookForce)
        }
    }

    private fun anyPinxixiExpired(context: Context): Boolean {
        return ImportedDao().queryAllUUIDs()
            .mapNotNull { ImportedDao().queryByUUID(it) }
            .any {
                it.type != Profile.Type.File &&
                    Pinxixi.isSubscriptionUrl(it.source) &&
                    Pinxixi.needsExpireForceRefresh(it.expire)
            }
    }

    private fun pollWebhookForce(): Boolean {
        for (url in Pinxixi.SYNC_STATUS_URLS) {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "ClashMate-Pinxixi/1.0")
                }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: continue
                conn.disconnect()

                val root = JSONObject(body)
                val data = root.optJSONObject("data") ?: root
                val epoch = data.optLong("provider_epoch", 0L)
                val changed = data.optBoolean("changed", false)
                val action = data.optString("action", "")
                val prev = lastEpoch.get()
                if (prev < 0L) {
                    lastEpoch.set(epoch)
                    if (changed || action == "refresh_subscription") return true
                    continue
                }
                if (epoch > prev || changed || action == "refresh_subscription") {
                    lastEpoch.set(maxOf(epoch, prev))
                    Log.i("拼夕夕 Webhook：epoch $prev -> $epoch changed=$changed action=$action")
                    return true
                }
            } catch (e: Exception) {
                Log.w("拼夕夕 Webhook 轮询失败 $url: ${e.message}")
            }
        }
        return false
    }

    fun forceSyncAll(context: Context, reason: String, clearCache: Boolean) {
        ImportedDao().queryAllUUIDs()
            .mapNotNull { ImportedDao().queryByUUID(it) }
            .filter { it.type != Profile.Type.File && Pinxixi.isSubscriptionUrl(it.source) }
            .forEach { imported ->
                if (clearCache || Pinxixi.needsExpireForceRefresh(imported.expire)) {
                    clearProfileCache(context, imported.uuid.toString())
                }
                Log.i("拼夕夕强制同步($reason): uuid=${imported.uuid} expire=${imported.expire}")
                ProfileReceiver.schedule(context, imported)
            }
    }

    /** 清本地 provider 缓存，避免沿用过期节点（保留 config 至拉取成功后替换） */
    fun clearProfileCache(context: Context, uuid: String) {
        try {
            val dir = context.importedDir.resolve(uuid)
            dir.resolve("providers").deleteRecursively()
            dir.resolve("providers").mkdirs()
            Log.i("拼夕夕已清理 provider 缓存: $uuid")
        } catch (e: Exception) {
            Log.w("拼夕夕清缓存失败 $uuid: ${e.message}", e)
        }
    }
}
