package com.github.kr328.clash.common.pinxixi

import java.util.concurrent.TimeUnit

/**
 * 拼夕夕面板 / 上游订阅通道识别与同步策略。
 * - VPN 使用中：每 [SYNC_INTERVAL_SECONDS] 秒强制拉订阅并重载内核
 * - 到期前后：清本地缓存后强制拉取
 * - 轮询 165 Webhook：provider_epoch 变化立即同步
 */
object Pinxixi {
    /** 梯子使用中强制同步间隔（秒） */
    const val SYNC_INTERVAL_SECONDS = 10L
    val SYNC_INTERVAL_MS: Long = TimeUnit.SECONDS.toMillis(SYNC_INTERVAL_SECONDS)

    /** Alarm 兜底轮询（分钟） */
    const val DEFAULT_INTERVAL_MINUTES = 1L
    const val MIN_INTERVAL_MINUTES = 1L

    /** 到期前多久开始强制加速（秒） */
    const val EXPIRE_FORCE_WINDOW_SECS = 3600L

    /** 165 渔云实时同步状态 API（客户端轮询） */
    val SYNC_STATUS_URLS: Array<String> = arrayOf(
        "http://165.99.42.254:8801/api/v1/pinxixi/sync/status",
        "http://ccjiasu.top:8801/api/v1/pinxixi/sync/status",
    )

    fun isSubscriptionUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("slects.xyz") ||
            lower.contains("wzpxx") ||
            lower.contains("weixin/tui65743") ||
            lower.contains("ccjiasu.top") ||
            lower.contains("caicaijiasu.top") ||
            lower.contains("panel.wzpxx")
    }

    fun defaultIntervalMs(source: String): Long =
        if (isSubscriptionUrl(source)) TimeUnit.MINUTES.toMillis(DEFAULT_INTERVAL_MINUTES) else 0L

    fun minIntervalMinutes(source: String): Long =
        if (isSubscriptionUrl(source)) MIN_INTERVAL_MINUTES else 15L

    fun minIntervalMs(source: String): Long =
        TimeUnit.MINUTES.toMillis(minIntervalMinutes(source))

    /** expire 为秒级 unix 时间戳；0 表示未知 */
    fun isExpired(expireEpochSec: Long): Boolean {
        if (expireEpochSec <= 0L) return false
        return expireEpochSec <= (System.currentTimeMillis() / 1000L)
    }

    /** 已到期，或即将到期 → 强制清缓存拉新节点 */
    fun needsExpireForceRefresh(expireEpochSec: Long): Boolean {
        if (expireEpochSec <= 0L) return false
        val now = System.currentTimeMillis() / 1000L
        if (expireEpochSec <= now) return true
        return (expireEpochSec - now) <= EXPIRE_FORCE_WINDOW_SECS
    }
}
