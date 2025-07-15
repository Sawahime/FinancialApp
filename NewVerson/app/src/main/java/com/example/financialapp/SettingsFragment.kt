package com.example.financialapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class SettingsFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var sharedPrefs: SharedPreferences
    private var year: Int = 0
    private var month: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("Settings", "进入Setting页面")

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val btnSaveSettings = view.findViewById<Button>(R.id.btnSave)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedPrefs = requireContext().getSharedPreferences("financial_data", Context.MODE_PRIVATE)

        // 监听月份切换事件
        sharedViewModel.yearMonth.observe(viewLifecycleOwner) { (year, month) ->
            Log.d("Settings", "dateUpdate: $year-$month")
            this.year = year
            this.month = month
            flushSettings(view)
        }

        // 保存按钮点击事件
        btnSaveSettings.setOnClickListener {
            val etSalary = view.findViewById<EditText>(R.id.etSalary)
            val sharedPrefsKey = "${this.year}-${this.month.toString().padStart(2, '0')}"

            val data = FinanceData(
                salary = etSalary.text.toString().toIntOrNull() ?: 0,
                isAnchor = true, isAuto = false
            )
            sharedPrefs.edit {
                putString(sharedPrefsKey, data.toJson())
            }

            Toast.makeText(context, "设置保存成功", Toast.LENGTH_SHORT).show()

            val key = findNearestNextRecordKey(this.year, this.month)
            if (key != null) {
                val (year, month) = key.split("-").let {
                    it[0].toInt() to it[1].toInt()
                }

                generateAndSaveData(this.year, this.month, year, month)
            }

        }

        return view
    }

    private fun flushSettings(view: View) {
        val currentYearMonth = "${this.year}-${this.month.toString().padStart(2, '0')}" ?: ""
        val etSalary = view.findViewById<EditText>(R.id.etSalary)
        val json = sharedPrefs.getString(currentYearMonth, null)
        FinanceData.fromJson(json)?.let { data ->
            etSalary.setText(data.salary.toString())
        } ?: run {
            etSalary.setText("")
        }
    }

    private fun findNearestNextRecordKey(currentYear: Int, currentMonth: Int): String? {
        var nearestRecord: String? = null
        var minDiff = Int.MAX_VALUE

        // 将当前年月转换为可比较的数字（年份*12 + 月份）
        val currentTotal = currentYear * 12 + currentMonth

        for ((key, value) in sharedPrefs.all) {
            // 如果key的正则表达式不符合"4个数字-2个数字"，则跳过
            if (!key.matches(Regex("\\d{4}-\\d{2}"))) continue
            // 如果这不是手动保存的记录，而是自动生成的记录，则跳过
            if (value !is String) continue
            val financeData = FinanceData.fromJson(value)
            if (financeData == null || !financeData.isAnchor) continue

            val (year, month) = key.split("-").let {
                it[0].toInt() to it[1].toInt()
            }
            val recordTotal = year * 12 + month
            val diff = recordTotal - currentTotal

            // 只考虑未来的记录（diff > 0）且差值更小的
            if (diff > 0 && diff < minDiff) {
                minDiff = diff
                nearestRecord = key
            }
        }

        return nearestRecord
    }

    // 生成并保存(year1-month1, year2-month2)之间的数据
    private fun generateAndSaveData(startYear: Int, startMonth: Int, endYear: Int, endMonth: Int) {
        val startTotal = startYear * 12 + startMonth
        val endTotal = endYear * 12 + endMonth
        if (startTotal >= endTotal) {
            Log.w("SettingsFragment", "Invalid date range: start >= end")
            return
        }

        val baseData =
            sharedPrefs.getString("${startYear}-${startMonth.toString().padStart(2, '0')}", null)
                ?.let {
                    FinanceData.fromJson(it)
                } ?: run {
                Log.e("SettingsFragment", "Base data not found")
                return
            }

        var currentYear = startYear
        var currentMonth = startMonth + 1
        if (currentMonth > 12) {
            currentYear++
            currentMonth = 1
        }

        while (currentYear * 12 + currentMonth < endTotal) {
            val key = "${currentYear}-${currentMonth.toString().padStart(2, '0')}"

            val data = FinanceData(
                salary = baseData.salary,
                // 自动生成的记录，不是锚点（手动保存的记录）
                isAnchor = false, isAuto = false
            )
            sharedPrefs.edit {
                putString(key, data.toJson())
            }

            currentMonth++
            if (currentMonth > 12) {
                currentYear++
                currentMonth = 1
            }
        }
    }
}