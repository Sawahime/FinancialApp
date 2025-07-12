package com.example.financialapp

import android.icu.util.Calendar

class DateNavigator {
    private var year = Calendar.getInstance().get(Calendar.YEAR)
    private var month = Calendar.getInstance().get(Calendar.MONTH) + 1

    fun setYearMonth(year: Int, month: Int) {
        this.year = year
        this.month = month
    }

    fun getCurrYear(): Int {
        return year
    }

    fun getCurrMonth(): Int {
        return month
    }

    fun getCurrYearMonthPair(): Pair<Int, Int> {
        return Pair(year, month)
    }

    fun getYearMonthText(): String {
        return "$year-${month.toString().padStart(2, '0')}"
    }

    fun prevMonth() {
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
}