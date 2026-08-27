package com.github.kr328.clash.service

import android.content.Context
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.pinxixi.Pinxixi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** 拼夕夕：VPN 运行时每 10 秒强制同步面板/上游订阅 */
object PinxixiSyncLoop {
    private val started = AtomicBoolean(false)

    fun ensureStarted(context: Context) {
        if (!started.compareAndSet(false, true)) {
            return
        }

        val app = context.applicationContext
        Global.launch(Dispatchers.IO) {
            Log.i("拼夕夕同步守护启动：VPN 运行时每 ${Pinxixi.SYNC_INTERVAL_SECONDS}s 拉取")
            while (isActive) {
                try {
                    if (StatusProvider.serviceRunning) {
                        ProfileReceiver.syncPinxixiProfilesNow(app, "vpn_active")
                    }
                } catch (e: Exception) {
                    Log.w("PinxixiSyncLoop: ${e.message}", e)
                }
                delay(Pinxixi.SYNC_INTERVAL_MS)
            }
        }
    }
}
