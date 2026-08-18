package com.hongguo.adblock

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.util.TypedValue
import android.view.Gravity
import android.graphics.Color

class ModuleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(Color.WHITE)
        }
        layout.addView(TextView(this).apply {
            text = "红果漫剧去广告"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(Color.parseColor("#FF1A1A"))
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(this).apply {
            text = "v2.0 · LSPosed 模块"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        })
        layout.addView(TextView(this).apply {
            text = "✓ 开屏广告已屏蔽\n✓ 集间/暂停广告已屏蔽\n\n适配版本：7.3.3.32+\n兼容版本：7.2.9.32 / 7.1.3.32"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#333333"))
        })
        setContentView(layout)
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
