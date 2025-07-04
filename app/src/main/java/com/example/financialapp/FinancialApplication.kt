package com.example.financialapp

import android.app.Application

/**
 * 应用程序类
 * 负责初始化全局组件
 */
class FinancialApplication : Application() {

    // 简化的仓库实例
    val simpleRepository by lazy { SimpleFinancialRepository() }

    // 当前显示的年月（全局共享）
    var currentDisplayYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    var currentDisplayMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1

    // 设置一些默认值用于测试
    override fun onCreate() {
        super.onCreate()

        // 锚点系统：清理旧系统记录
        Thread {
            try {
                simpleRepository.cleanupOldSystemRecords()
                android.util.Log.d("FinancialApplication", "锚点系统启动完成")
            } catch (e: Exception) {
                android.util.Log.e("FinancialApplication", "锚点系统启动失败", e)
            }
        }.start()
    }
}
