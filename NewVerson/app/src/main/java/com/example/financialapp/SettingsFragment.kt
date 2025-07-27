package com.example.financialapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class SettingsFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var salaryItemsList: LinearLayout
    private lateinit var btnAddSalaryItem: Button
    private lateinit var insuranceItemList: LinearLayout
    private var year: Int = 0
    private var month: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.yearMonth.observe(viewLifecycleOwner) { (year, month) ->
            Log.d("Settings", "dateUpdate: $year-$month")
            this.year = year
            this.month = month
        }

        initSalaryModule(view)
        initInsuranceModule(view)

        // 保存按钮点击事件
        view.findViewById<Button>(R.id.btn_save_settings).setOnClickListener {
            saveSettings()
            Log.d("Settings", "保存成功")
        }

        return view
    }

    private fun initSalaryModule(view: View) {
        salaryItemsList = view.findViewById(R.id.salary_items_list)
        btnAddSalaryItem = view.findViewById(R.id.btn_add_salary_item)

        // 添加工资项按钮点击事件
        btnAddSalaryItem.setOnClickListener {
            addSalaryItem()
        }
    }

    private fun initInsuranceModule(view: View) {
        insuranceItemList = view.findViewById(R.id.insurance_items_list)
    }

    private fun addSalaryItem() {
        val salaryItemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.salary_item, salaryItemsList, false)

        // 设置删除按钮点击事件
        salaryItemView.findViewById<ImageButton>(R.id.btn_delete_salary_items).setOnClickListener {
            salaryItemsList.removeView(salaryItemView)
        }

        salaryItemsList.addView(salaryItemView)
    }

    private fun saveSettings() {
        val salaryList = mutableListOf<MutableMap<String, Any>>()
        // 遍历所有已添加的工资项视图
        for (i in 0 until salaryItemsList.childCount) {
            val salaryItem = salaryItemsList.getChildAt(i) as LinearLayout

            val typeEditText = salaryItem.getChildAt(0) as EditText
            val amountEditText = salaryItem.getChildAt(1) as EditText
            val taxableCheckBox = salaryItem.getChildAt(2) as CheckBox
            val insuredCheckBox = salaryItem.getChildAt(3) as CheckBox

            val type = typeEditText.text.toString()
            val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val isTaxable = taxableCheckBox.isChecked
            val isInsured = insuredCheckBox.isChecked

            val mutableMap: MutableMap<String, Any> = mutableMapOf(
                "type" to type,
                "amount" to amount,
                "isTaxable" to isTaxable,
                "isInsured" to isInsured,
            )

            salaryList.add(mutableMap)
        }
        salaryList.forEachIndexed { index, item ->
            Log.d(
                "Settings",
                "[${index + 1}]: ${item["type"]} ￥${item["amount"]} 纳税(${item["isTaxable"]}) 参保(${item["isInsured"]})"
            )
        }

        val insuranceList = mutableListOf<MutableMap<String, Any>>()
        for (i in 0 until insuranceItemList.childCount) {
            val insuranceItem = insuranceItemList.getChildAt(i) as LinearLayout

            val typeTextView = insuranceItem.getChildAt(0) as TextView
            val valueEditText = insuranceItem.getChildAt(1) as EditText

            val type = typeTextView.text.toString()
            val value = valueEditText.text.toString()

            val mutableMap: MutableMap<String, Any> = mutableMapOf(
                "type" to type,
                "value" to value,
            )

            insuranceList.add(mutableMap)
        }
        insuranceList.forEachIndexed { index, item ->
            Log.d(
                "Settings",
                "${item["type"]}: ${item["value"]}%"
            )
        }
    }
}