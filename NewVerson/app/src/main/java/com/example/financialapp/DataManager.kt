package com.example.financialapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson

enum class AnchorProperty {
    NOT_ANCHOR,
    AUTO_ANCHOR,
    MANUAL_ANCHOR,
}

data class MyData(
    var salary: Int = 0,
    var anchorProperty: AnchorProperty = AnchorProperty.NOT_ANCHOR,
    // 以后在这里扩展数据
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String?): MyData? {
            return json?.let { gson.fromJson(it, MyData::class.java) }
        }
    }

    fun toJson(): String {
        return gson.toJson(this)
    }
}


class DataManager(private val context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("data", Context.MODE_PRIVATE)

    private fun getKey(year: Int, month: Int): String = "$year-$month"

    fun init(currYear: Int, currMonth: Int) {
        // 查找 AUTO_ANCHOR 数据
        var autoAnchorData: Pair<String, MyData>? = null
        var manualAnchorData: Pair<String, MyData>? = null
        var minDateData: Pair<String, MyData>? = null
        var maxDateData: Pair<String, MyData>? = null

        // 遍历所有数据
        for ((key, value) in sharedPrefs.all) {
            if (value !is String) {
                continue
            }

            val data = MyData.fromJson(value) ?: continue
            val currentEntry = Pair(key, data)

            // 更新最小日期数据
            if (minDateData == null || key < minDateData.first) {
                minDateData = currentEntry
            }

            // 更新最大日期数据
            if (maxDateData == null || key > maxDateData.first) {
                maxDateData = currentEntry
            }

            when (data.anchorProperty) {
                AnchorProperty.AUTO_ANCHOR -> autoAnchorData = currentEntry

                AnchorProperty.MANUAL_ANCHOR -> {
                    if (manualAnchorData == null || key > manualAnchorData.first) {
                        manualAnchorData = currentEntry
                    }
                }

                else -> {}
            }
        }

        if (autoAnchorData != null) {
            val (key, data) = autoAnchorData
            val (yearStr, monthStr) = key.split("-")
            val year = yearStr.toInt()
            val month = monthStr.toInt()

            if (year * 12 + month < currYear * 12 + currMonth) {
                data.anchorProperty = AnchorProperty.NOT_ANCHOR
                generateData(year, month + 1, currYear, currMonth - 1, data)

                // 保持最新月份为AUTO_ANCHOR
                val newData = data.copy()
                newData.anchorProperty = AnchorProperty.AUTO_ANCHOR
                addData(currYear, currMonth, newData)
            }
        } else {
            // 查找最近的 MANUAL_ANCHOR 数据
            if (manualAnchorData != null) {
                val (key, data) = manualAnchorData
                val (yearStr, monthStr) = key.split("-")
                val year = yearStr.toInt()
                val month = monthStr.toInt()

                if (year * 12 + month == currYear * 12 + currMonth) {
                    // 无需处理
                } else if (year * 12 + month < currYear * 12 + currMonth) {
                    // 保持最新月份为AUTO_ANCHOR
                    val newData = data.copy()
                    newData.anchorProperty = AnchorProperty.AUTO_ANCHOR
                    addData(currYear, currMonth, newData)

                    // 生成中间数据
                    if (month + 1 <= currMonth - 1) {
                        data.anchorProperty = AnchorProperty.NOT_ANCHOR
                        generateData(year, month + 1, currYear, currMonth - 1, data)
                    }
                }
                // 忽略大于当前日期的情况
            } else {
                // 第一次启动或数据被清空
                val newData = MyData().apply {
                    anchorProperty = AnchorProperty.AUTO_ANCHOR
                }
                addData(currYear, currMonth, newData)
            }
        }

        // 确保最小和最大日期的数据是锚点
        minDateData?.let { (key, data) ->
            if (data.anchorProperty == AnchorProperty.NOT_ANCHOR) {
                data.anchorProperty = AnchorProperty.AUTO_ANCHOR
                sharedPrefs.edit { putString(key, data.toJson()) }
            }
        }

        maxDateData?.let { (key, data) ->
            if (data.anchorProperty == AnchorProperty.NOT_ANCHOR) {
                data.anchorProperty = AnchorProperty.AUTO_ANCHOR
                sharedPrefs.edit { putString(key, data.toJson()) }
            }
        }
    }

    fun addData(year: Int, month: Int, data: MyData) {
        sharedPrefs.edit {
            putString(getKey(year, month), data.toJson())
        }
    }

    fun addDataAndRefresh(year: Int, month: Int, data: MyData) {
        addData(year, month, data)

        val nextAnchor = findNextAnchor(year, month)
        if (nextAnchor == null) return

        val (nextY, nextM, nextData) = nextAnchor
        data.anchorProperty = AnchorProperty.NOT_ANCHOR

        var startY = year
        var startM = month + 1
        if (startM > 12) {
            startM = 1
            startY++
        }

        var endY = nextY
        var endM = nextM
        if (nextData.anchorProperty == AnchorProperty.MANUAL_ANCHOR) {
            endM -= 1
            if (endM < 1) {
                endM = 12
                endY--
            }
        }

        generateData(startY, startM, endY, endM, data)
    }

    fun deleteData(year: Int, month: Int) {
        sharedPrefs.edit {
            remove(getKey(year, month))
        }
    }

    fun getData(year: Int, month: Int): MyData? {
        val json = sharedPrefs.getString(getKey(year, month), null)
        return json?.let { MyData.fromJson(it) }
    }

    fun generateData(
        startYear: Int, startMonth: Int,
        endYear: Int, endMonth: Int,
        baseData: MyData
    ) {
        var startTotal = startYear * 12 + startMonth
        val endTotal = endYear * 12 + endMonth
        if (startTotal > endTotal) {
            Log.w("DataManager", "Invalid section $startYear-$startMonth ~ $endYear-$endMonth")
            return
        }

        var currentYear = startYear
        var currentMonth = startMonth

        while (startTotal <= endTotal) {
            val data = baseData.copy()
            data.anchorProperty = AnchorProperty.NOT_ANCHOR
            addData(currentYear, currentMonth, data)

            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
            startTotal = currentYear * 12 + currentMonth
        }
    }

    fun findNextAnchor(year: Int, month: Int): Triple<Int, Int, MyData>? {
        val allEntries = sharedPrefs.all
        var nextAnchor: Triple<Int, Int, MyData>? = null

        for ((key, value) in allEntries) {
            if (value is String) {
                val data = MyData.fromJson(value) ?: continue
                if (data.anchorProperty == AnchorProperty.NOT_ANCHOR) continue

                val (yStr, mStr) = key.split("-")
                val y = yStr.toInt()
                val m = mStr.toInt()

                if (y * 12 + m > year * 12 + month) {
                    if (nextAnchor == null || y * 12 + m < nextAnchor.first * 12 + nextAnchor.second) {
                        nextAnchor = Triple(y, m, data)
                    }
                }
            }
        }

        return nextAnchor
    }

    fun findPreviousAnchor(year: Int, month: Int): Triple<Int, Int, MyData>? {
        val allEntries = sharedPrefs.all
        var prevAnchor: Triple<Int, Int, MyData>? = null

        for ((key, value) in allEntries) {
            if (value is String) {
                val data = MyData.fromJson(value) ?: continue
                if (data.anchorProperty == AnchorProperty.NOT_ANCHOR) continue

                val (yStr, mStr) = key.split("-")
                val y = yStr.toInt()
                val m = mStr.toInt()

                if (y * 12 + m < year * 12 + month) {
                    if (prevAnchor == null || y * 12 + m > prevAnchor.first * 12 + prevAnchor.second) {
                        prevAnchor = Triple(y, m, data)
                    }
                }
            }
        }

        return prevAnchor
    }

    private fun reviseYearMonth(year: Int, month: Int): Pair<Int, Int> {
        return when {
            month > 12 -> Pair(year + 1 + (month - 12) / 12, 1 + (month - 13) % 12)
            month < 1 -> Pair(year - 1 + month / 12, 12 + month % 12)
            else -> Pair(year, month)
        }
    }
}