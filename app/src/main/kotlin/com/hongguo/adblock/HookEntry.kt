package com.hongguo.adblock

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "HongguoAdBlock"
        private const val PKG = "com.phoenix.read"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != PKG) return
        XposedBridge.log("[$TAG] hooked into ${lpparam.packageName} (v${lpparam.processName})")

        hookSplashAd(lpparam)
        hookPauseAd(lpparam)

        XposedBridge.log("[$TAG] all hooks installed")
    }

    // ===================== 开屏广告 =====================
    private fun hookSplashAd(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 主闸门（7.2.9.32+）：needRequestAd = AttributionManager.hasHitAttribution()
        // 强制 false → 整条开屏广告请求/展示链路被跳过
        runCatching {
            XposedHelpers.findAndHookMethod(
                "com.dragon.read.pages.splash.AttributionManager",
                lpparam.classLoader,
                "hasHitAttribution",
                XC_MethodReplacement.returnConstant(false)
            )
            XposedBridge.log("[$TAG] [splash] AttributionManager.hasHitAttribution -> false")
        }.onFailure {
            XposedBridge.log("[$TAG] [splash] hasHitAttribution hook failed: $it")
        }

        // 兜底：x0.run() = "开屏广告开始展示"入口
        //   拿字段"c"(b06/d = SplashPresenter)，调 Fb() 直接进主页，然后跳过整个 run()
        //   新版 7.3.3.32：Fb() = enter main（等价旧版 Ra()）
        //   旧版 7.2.9.32：字段"c" 是 xo5/d，Ra() = enter main（如果 Ra 不存在则静默跳过）
        runCatching {
            XposedHelpers.findAndHookMethod(
                "com.dragon.read.pages.splash.x0",
                lpparam.classLoader,
                "run",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null
                        runCatching {
                            val presenter = XposedHelpers.getObjectField(param.thisObject, "c")
                            // 新版调 Fb()，旧版调 Ra() — 先尝试新版本
                            try {
                                XposedHelpers.callMethod(presenter, "Fb")
                            } catch (_: Throwable) {
                                // 旧版 7.2.9.32 没有 Fb()，回退 Ra()
                                XposedHelpers.callMethod(presenter, "Ra")
                            }
                        }
                    }
                }
            )
            XposedBridge.log("[$TAG] [splash] x0.run -> skip ad, enter main (Fb/Ra fallback)")
        }.onFailure {
            XposedBridge.log("[$TAG] [splash] x0.run hook failed: $it")
        }
    }

    // ===================== 集间/暂停广告 =====================
    private fun hookPauseAd(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 新版暂停开关（7.3.3.32）：q13/b.a() 读 AdAbSettingsHelper.D() → SeriesPauseAdConfig.enablePauseAd
        // 同时取消 n13/f 中的 series_rerank_pause_ad 任务
        runCatching {
            XposedHelpers.findAndHookMethod(
                "q13.b",
                lpparam.classLoader,
                "a",
                XC_MethodReplacement.returnConstant(false)
            )
            XposedBridge.log("[$TAG] [pause] q13.b.a() -> false")
        }.onFailure {
            XposedBridge.log("[$TAG] [pause] q13.b.a hook failed: $it")
        }

        // 兜底：SeriesPauseAdImpl.enablePauseAd → false（覆盖系列剧暂停广告服务）
        runCatching {
            XposedHelpers.findAndHookMethod(
                "com.dragon.read.ad.onestop.seriespause.impl.SeriesPauseAdImpl",
                lpparam.classLoader,
                "enablePauseAd",
                XC_MethodReplacement.returnConstant(false)
            )
            XposedBridge.log("[$TAG] [pause] SeriesPauseAdImpl.enablePauseAd -> false")
        }.onFailure {
            XposedBridge.log("[$TAG] [pause] SeriesPauseAdImpl.enablePauseAd hook failed: $it")
        }

        // 兼容旧版 7.2.9.32：tx2/b.a() 为暂停广告开关
        runCatching {
            XposedHelpers.findAndHookMethod(
                "tx2.b",
                lpparam.classLoader,
                "a",
                XC_MethodReplacement.returnConstant(false)
            )
            XposedBridge.log("[$TAG] [pause] tx2.b.a() -> false (old version)")
        }.onFailure {
            // 新版无此类，忽略
        }

        // 兼容旧版 7.1.3.32：fd2.b.b()
        runCatching {
            XposedHelpers.findAndHookMethod(
                "fd2.b",
                lpparam.classLoader,
                "b",
                XC_MethodReplacement.returnConstant(false)
            )
            XposedBridge.log("[$TAG] [pause] fd2.b.b() -> false (old version)")
        }.onFailure {
            // 新版无此类，忽略
        }
    }
}
