package com.lizongying.mytv

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyApplication : Application() {
    private lateinit var displayMetrics: DisplayMetrics

    override fun onCreate() {
        super.onCreate()

        displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        installCrashHandler()
        installUiGuard()
    }

    /**
     * 崩溃黑匣子：未捕获异常写入 files/crash.log 后交回系统默认处理（进程仍会退出）。
     * 手机控制页 GET /crash 可读取，便于真机问题定位。
     */
    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())
                File(filesDir, CRASH_FILE).writeText("$time\nthread: ${thread.name}\n$sw")
            } catch (e: Exception) {
                Log.e(TAG, "write crash log error", e)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /**
     * 主线程兜底：捕获UI层未预期异常（真机ROM焦点实现差异等），
     * 记录日志并保持运行，避免闪退。严重错误仍交黑匣子退出。
     */
    fun installUiGuard() {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Throwable) {
                    Log.e(TAG, "ui guard caught", e)
                    try {
                        val sw = StringWriter()
                        e.printStackTrace(PrintWriter(sw))
                        val time =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())
                        val f = File(filesDir, CRASH_FILE)
                        val prev = if (f.isFile) f.readText() else ""
                        f.writeText("$time [ui-guard]\n${sw}\n${prev.take(4000)}")
                    } catch (ignored: Exception) {
                    }
                }
            }
        }
    }

    fun getDisplayMetrics(): DisplayMetrics {
        return displayMetrics
    }

    companion object {
        private const val TAG = "MyApplication"
        const val CRASH_FILE = "crash.log"
    }
}
