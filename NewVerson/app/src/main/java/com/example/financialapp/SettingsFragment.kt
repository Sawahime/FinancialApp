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
            val sharedPrefsKey = "${this.year}-${this.month.toString().padStart(2, '0')}" ?: ""

            val data = FinanceData(
                salary = etSalary.text.toString().toIntOrNull() ?: 0
            )
            sharedPrefs.edit {
                putString(sharedPrefsKey, data.toJson())
            }

            Toast.makeText(context, "设置保存成功", Toast.LENGTH_SHORT).show()
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
}