package com.github.kr328.clash.common.pinxixi

import java.util.concurrent.TimeUnit

/**
 * 拼夕夕面板 / 上游订阅通道识别与同步策略。
 * - 使用中（VPN 运行）：每 [SYNC_INTERVAL_SECONDS] 秒强制拉取
 * - 未开 VPN：启动 / 切回 App 立即拉 + 1 分钟 Alarm 兜底
 */
object Pinxixi {
    /** 梯子使用中强制同步间隔（秒） */
    const val SYNC_INTERVAL_SECONDS = 10L
    val SYNC_INTERVAL_MS: Long = TimeUnit.SECONDS.toMillis(SYNC_INTERVAL_SECONDS)

    /** Alarm 兜底轮询（分钟） */
    const val DEFAULT_INTERVAL_MINUTES = 1L
    const val MIN_INTERVAL_MINUTES = 1L

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
}
