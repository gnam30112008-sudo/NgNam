package com.example.golikehelper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random

/**
 * Chế độ BÁN TỰ ĐỘNG:
 *  1. Tự bấm nút "nhận nhiệm vụ" trong app GoLike.
 *  2. Chờ BẠN tự làm nhiệm vụ, rồi bấm «Đã làm xong» (hoặc «Bỏ qua») ở thanh nổi.
 *  3. Chỉ sau khi bạn xác nhận mới quay lại GoLike và bấm nút hoàn thành.
 * Dịch vụ không tự like/follow/comment và không xử lý CAPTCHA.
 */
class TaskAccessibilityService : AccessibilityService() {

    private enum class State { OFF, IDLE, WAITING_USER, COMPLETING }
    private enum class Action { COMPLETE, SKIP }

    private val handler = Handler(Looper.getMainLooper())
    private var state = State.OFF
    private var cfg = Config()
    private var nextRunAt = 0L
    private var deadlineAt = 0L
    private var pending = Action.COMPLETE

    private var overlay: LinearLayout? = null
    private var statusView: TextView? = null
    private var doneBtn: Button? = null
    private var skipBtn: Button? = null

    private val loop = object : Runnable {
        override fun run() {
            tick()
            if (state != State.OFF) handler.postDelayed(this, 1000)
        }
    }
    private val eventTick = Runnable { tick() }

    companion object {
        @Volatile
        var instance: TaskAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        stopTasks()
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (state != State.IDLE && state != State.COMPLETING) return
        if (event?.packageName?.toString() != cfg.golikePackage) return
        handler.removeCallbacks(eventTick)
        handler.postDelayed(eventTick, 400)
    }

    // ---------- điều khiển từ MainActivity / thanh nổi ----------

    fun startTasks(): Boolean {
        cfg = Config.load(this)
        if (!cfg.isReady()) {
            Toast.makeText(this, "Hãy lưu đủ: gói GoLike, chữ nút Nhận, chữ nút Hoàn thành.", Toast.LENGTH_LONG).show()
            return false
        }
        state = State.IDLE
        nextRunAt = 0L
        showOverlay()
        setStatus("Đang tìm nhiệm vụ… (hãy mở app GoLike ở màn hình nhiệm vụ)")
        showButtons(false)
        handler.removeCallbacks(loop)
        handler.post(loop)
        return true
    }

    fun stopTasks() {
        state = State.OFF
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
    }

    private fun onUserDone() {
        if (state != State.WAITING_USER) return
        pending = Action.COMPLETE
        beginReturn("Đang quay lại GoLike để báo hoàn thành…")
    }

    private fun onUserSkip() {
        if (state != State.WAITING_USER) return
        pending = Action.SKIP
        beginReturn("Đang quay lại GoLike để bỏ qua…")
    }

    // ---------- máy trạng thái ----------

    private fun tick() {
        val now = System.currentTimeMillis()
        when (state) {
            State.OFF -> return

            State.WAITING_USER -> {
                if (now > deadlineAt) {
                    pending = Action.SKIP
                    beginReturn("Hết thời gian chờ — bỏ qua nhiệm vụ (không báo hoàn thành)")
                }
            }

            State.IDLE -> {
                if (now < nextRunAt) return
                if (Config.doneToday(this) >= cfg.dailyLimit) {
                    setStatus("Đã đạt giới hạn ${cfg.dailyLimit} nhiệm vụ hôm nay")
                    nextRunAt = now + 600_000
                    return
                }
                val root = golikeRoot() ?: run {
                    setStatus("Hãy mở app GoLike ở màn hình nhiệm vụ")
                    return
                }
                val node = find(root, cfg.acceptText) ?: run {
                    setStatus("Chưa thấy nút «${cfg.acceptText}»…")
                    return
                }
                if (clickNode(node)) {
                    state = State.WAITING_USER
                    deadlineAt = now + cfg.confirmTimeoutSec * 1000L
                    setStatus("Hãy làm nhiệm vụ, xong bấm «Đã làm xong»")
                    showButtons(true)
                }
            }

            State.COMPLETING -> {
                if (now > deadlineAt) {
                    finishCycle(false, "Không tìm thấy nút trong GoLike — kiểm tra lại chữ nút")
                    return
                }
                val label = if (pending == Action.COMPLETE) cfg.completeText else cfg.skipText
                if (label.isBlank()) {
                    finishCycle(false, "Đã bỏ qua (chưa cấu hình nút bỏ qua nên không bấm gì)")
                    return
                }
                val root = golikeRoot() ?: return
                val node = find(root, label) ?: return
                if (clickNode(node)) {
                    if (pending == Action.COMPLETE) {
                        finishCycle(true, "Đã báo hoàn thành")
                    } else {
                        finishCycle(false, "Đã bỏ qua nhiệm vụ")
                    }
                }
            }
        }
    }

    private fun beginReturn(message: String) {
        state = State.COMPLETING
        deadlineAt = System.currentTimeMillis() + 20_000
        showButtons(false)
        setStatus(message)
        try {
            val intent = packageManager.getLaunchIntentForPackage(cfg.golikePackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }
        } catch (_: Exception) {
            // Nếu hệ thống chặn, hãy tự bấm quay lại app GoLike.
        }
    }

    private fun finishCycle(done: Boolean, message: String) {
        if (done) Config.addDone(this)
        state = State.IDLE
        nextRunAt = System.currentTimeMillis() +
            Random.nextLong(cfg.minDelaySec * 1000L, cfg.maxDelaySec * 1000L + 1)
        showButtons(false)
        setStatus("$message · hôm nay: ${Config.doneToday(this)}/${cfg.dailyLimit}")
    }

    // ---------- thao tác trên giao diện app GoLike ----------

    private fun golikeRoot(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return if (root.packageName?.toString() == cfg.golikePackage) root else null
    }

    private fun find(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val list = root.findAccessibilityNodeInfosByText(text)
        if (list.isNullOrEmpty()) return null
        return list.firstOrNull { it.text?.toString()?.trim().equals(text.trim(), true) } ?: list[0]
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var n: AccessibilityNodeInfo? = node
        var depth = 0
        while (n != null && depth < 6) {
            if (n.isClickable && n.isEnabled) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            n = n.parent
            depth++
        }
        return false
    }

    // ---------- thanh nổi ----------

    private fun showOverlay() {
        if (overlay != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(0xEE222222.toInt())
        box.setPadding(24, 16, 24, 16)

        val tv = TextView(this)
        tv.setTextColor(0xFFFFFFFF.toInt())
        tv.textSize = 13f
        box.addView(tv)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        val done = Button(this)
        done.text = "✔ Đã làm xong"
        done.setOnClickListener { onUserDone() }
        val skip = Button(this)
        skip.text = "Bỏ qua"
        skip.setOnClickListener { onUserSkip() }
        val off = Button(this)
        off.text = "Tắt"
        off.setOnClickListener { stopTasks() }
        row.addView(done)
        row.addView(skip)
        row.addView(off)
        box.addView(row)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        lp.y = 160
        wm.addView(box, lp)

        overlay = box
        statusView = tv
        doneBtn = done
        skipBtn = skip
    }

    private fun removeOverlay() {
        val box = overlay ?: return
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(box)
        } catch (_: Exception) {
        }
        overlay = null
        statusView = null
        doneBtn = null
        skipBtn = null
    }

    private fun setStatus(text: String) {
        statusView?.text = text
    }

    private fun showButtons(enabled: Boolean) {
        doneBtn?.isEnabled = enabled
        skipBtn?.isEnabled = enabled
    }
}
