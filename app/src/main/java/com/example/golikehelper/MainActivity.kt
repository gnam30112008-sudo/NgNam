package com.example.golikehelper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val fields = LinkedHashMap<String, EditText>()
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cfg = Config.load(this)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(32, 32, 32, 48)

        fun title(text: String) {
            val t = TextView(this)
            t.text = text
            t.textSize = 20f
            root.addView(t)
        }

        fun field(key: String, label: String, value: String, number: Boolean = false) {
            val l = TextView(this)
            l.text = label
            l.setPadding(0, 24, 0, 0)
            root.addView(l)
            val e = EditText(this)
            e.setText(value)
            e.setSingleLine()
            if (number) e.inputType = InputType.TYPE_CLASS_NUMBER
            fields[key] = e
            root.addView(e)
        }

        fun button(text: String, onClick: () -> Unit) {
            val b = Button(this)
            b.text = text
            b.setOnClickListener { onClick() }
            root.addView(b)
        }

        title("GoLike Helper (bán tự động)")
        statusText = TextView(this)
        statusText.setPadding(0, 12, 0, 0)
        root.addView(statusText)

        field("pkg", "Gói (package) của app GoLike", cfg.golikePackage)
        button("Tự tìm gói GoLike đã cài") { autoFindPackage() }
        field("accept", "Chữ trên nút NHẬN nhiệm vụ (đúng như hiển thị)", cfg.acceptText)
        field("complete", "Chữ trên nút HOÀN THÀNH", cfg.completeText)
        field("skip", "Chữ trên nút BỎ QUA (tuỳ chọn)", cfg.skipText)
        field("limit", "Giới hạn nhiệm vụ mỗi ngày", cfg.dailyLimit.toString(), true)
        field("min", "Nghỉ tối thiểu giữa 2 nhiệm vụ (giây, ≥5)", cfg.minDelaySec.toString(), true)
        field("max", "Nghỉ tối đa (giây)", cfg.maxDelaySec.toString(), true)
        field("timeout", "Thời gian chờ bạn xác nhận (giây)", cfg.confirmTimeoutSec.toString(), true)

        button("Lưu cấu hình") { save(); toast("Đã lưu") }
        button("1) Mở cài đặt Trợ năng") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        button("2) Bật nhận nhiệm vụ") {
            save()
            val svc = TaskAccessibilityService.instance
            if (svc == null) {
                toast("Hãy bật «GoLike Helper» trong Cài đặt Trợ năng trước.")
            } else if (svc.startTasks()) {
                toast("Đã bật. Mở app GoLike ở màn hình nhiệm vụ.")
            }
        }
        button("Tắt nhận nhiệm vụ") { TaskAccessibilityService.instance?.stopTasks() }

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        val on = TaskAccessibilityService.instance != null
        statusText.text = "Dịch vụ Trợ năng: " + (if (on) "ĐÃ BẬT" else "CHƯA BẬT") +
            " · hôm nay hoàn thành: " + Config.doneToday(this)
    }

    private fun save() {
        fun text(k: String) = fields[k]?.text?.toString()?.trim() ?: ""
        fun num(k: String, d: Int) = text(k).toIntOrNull() ?: d
        Config.save(
            this,
            Config(
                golikePackage = text("pkg"),
                acceptText = text("accept"),
                completeText = text("complete"),
                skipText = text("skip"),
                dailyLimit = num("limit", 50),
                minDelaySec = num("min", 8),
                maxDelaySec = num("max", 20),
                confirmTimeoutSec = num("timeout", 300),
            )
        )
    }

    private fun autoFindPackage() {
        val matches = packageManager.getInstalledApplications(0).filter {
            it.packageName.contains("golike", true) ||
                packageManager.getApplicationLabel(it).toString().contains("golike", true)
        }
        when {
            matches.isEmpty() -> toast("Không thấy app nào có tên GoLike. Hãy cài app GoLike trước.")
            else -> {
                fields["pkg"]?.setText(matches[0].packageName)
                toast("Tìm thấy: " + matches.joinToString { it.packageName })
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
