package com.github.kr328.clash.common.pinxixi

import java.util.concurrent.TimeUnit

/** 拼夕夕面板订阅识别与默认自动更新间隔（仅此类 URL 生效） */
object Pinxixi {
    /** 后台兜底轮询：1 分钟 */
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
