package com.github.kr328.clash.service

import android.content.Context
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.pinxixi.Pinxixi
import com.github.kr328.clash.common.pinxixi.PinxixiTraffic
import com.github.kr328.clash.core.bridge.Bridge
import com.github.kr328.clash.service.data.ImportedDao
import com.github.kr328.clash.service.model.Profile

/**
 * VPN 使用中：读取 Mihomo 会话累计流量并上报面板个人计费。
 */
object PinxixiTrafficReport {
    fun reportWhenVpnActive(context: Context) {
        if (!StatusProvider.serviceRunning) {
            return
        }

        val total = try {
            Bridge.nativeQueryTrafficTotal()
        } catch (e: Exception) {
            Log.w("PinxixiTrafficReport: queryTrafficTotal failed: ${e.message}")
            return
        }

        val profiles = ImportedDao().queryAllUUIDs()
            .mapNotNull { ImportedDao().queryByUUID(it) }
            .filter { it.type != Profile.Type.File && Pinxixi.isSubscriptionUrl(it.source) }

        if (profiles.isEmpty()) {
            return
        }

        // CMFA 当前 API 返回单 Long（会话总字节）；记入 down，up 置 0，面板按增量计费
        val up = 0L
        val down = maxOf(0L, total)

        for (imported in profiles) {
            val token = PinxixiTraffic.extractSubscribeToken(imported.source) ?: continue
            val reportUrl = PinxixiTraffic.trafficReportUrl(imported.source) ?: continue
            val ok = PinxixiTraffic.postTrafficReport(reportUrl, token, up, down)
            Log.i("拼夕夕流量上报 uuid=${imported.uuid} ok=$ok down=$down")
        }
    }
}
