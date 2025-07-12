package com.example.financialapp

import android.icu.util.Calendar

class MonthNavigator {
    private var year = Calendar.getInstance().get(Calendar.YEAR)
    private var month = Calendar.getInstance().get(Calendar.MONTH) + 1

    fun preMonth() {
        month--
        if (month < 1) {
            month = 12
            year--
        }
    }

    fun nextMonth() {
        month++
        if (month > 12) {
            month = 1
            year++
        }
    }

    fun getCurrMonth(): String {
        // 返回 "YYYY-MM" 格式, padStart(2, '0') 保证月份总是两位数
        return "$year-${month.toString().padStart(2, '0')}"
    }
}