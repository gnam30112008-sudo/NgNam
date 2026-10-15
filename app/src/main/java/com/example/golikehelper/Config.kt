package com.example.golikehelper

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Cấu hình do người dùng nhập. Không có giá trị mặc định đoán sẵn cho app GoLike. */
data class Config(
    val golikePackage: String = "",
    val acceptText: String = "",      // chữ trên nút nhận nhiệm vụ (đúng như hiển thị)
    val completeText: String = "",    // chữ trên nút hoàn thành
    val skipText: String = "",        // (tuỳ chọn) chữ trên nút bỏ qua nhiệm vụ
    val dailyLimit: Int = 50,
    val minDelaySec: Int = 8,
    val maxDelaySec: Int = 20,
    val confirmTimeoutSec: Int = 300,
) {
    fun isReady() = golikePackage.isNotBlank() && acceptText.isNotBlank() && completeText.isNotBlank()

    fun sanitized(): Config {
        val min = minDelaySec.coerceAtLeast(5)
        return copy(
            golikePackage = golikePackage.trim(),
            acceptText = acceptText.trim(),
            completeText = completeText.trim(),
            skipText = skipText.trim(),
            dailyLimit = dailyLimit.coerceIn(1, 500),
            minDelaySec = min,
            maxDelaySec = maxDelaySec.coerceAtLeast(min),
            confirmTimeoutSec = confirmTimeoutSec.coerceAtLeast(30),
        )
    }

    companion object {
        private const val FILE = "golike_helper"

        private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

        fun load(c: Context): Config {
            val s = prefs(c)
            return Config(
                golikePackage = s.getString("pkg", "") ?: "",
                acceptText = s.getString("accept", "") ?: "",
                completeText = s.getString("complete", "") ?: "",
                skipText = s.getString("skip", "") ?: "",
                dailyLimit = s.getInt("limit", 50),
                minDelaySec = s.getInt("min", 8),
                maxDelaySec = s.getInt("max", 20),
                confirmTimeoutSec = s.getInt("timeout", 300),
            ).sanitized()
        }

        fun save(c: Context, cfg: Config) {
            val v = cfg.sanitized()
            prefs(c).edit()
                .putString("pkg", v.golikePackage)
                .putString("accept", v.acceptText)
                .putString("complete", v.completeText)
                .putString("skip", v.skipText)
                .putInt("limit", v.dailyLimit)
                .putInt("min", v.minDelaySec)
                .putInt("max", v.maxDelaySec)
                .putInt("timeout", v.confirmTimeoutSec)
                .apply()
        }

        private fun today(): String =
            SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

        fun doneToday(c: Context): Int = prefs(c).getInt("done_" + today(), 0)

        fun addDone(c: Context) {
            val key = "done_" + today()
            val p = prefs(c)
            p.edit().putInt(key, p.getInt(key, 0) + 1).apply()
        }
    }
}
