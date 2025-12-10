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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TreeMap


class SettingsFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private var year: Int = 0
    private var month: Int = 0
    private lateinit var salaryItemsList: LinearLayout
    private lateinit var btnAddSalaryItem: Button
    private lateinit var insuranceItemList: LinearLayout
    private val financialDataBuffer = TreeMap<Int, MutableMap<String, Any>>()
    private lateinit var db: AppDatabase
    private lateinit var financialDataRepo: FinancialDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Settings", "Settings fragment onCreate")

        super.onCreate(savedInstanceState)
        arguments?.let {}

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        db = AppDatabase.getDatabase(requireContext())
        financialDataRepo = FinancialDataRepository(db)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("Settings", "Settings fragment onCreateView")
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        sharedViewModel.yearMonth.observe(viewLifecycleOwner) { (year, month) ->
//            Log.d("Settings", "dateUpdate: $year-$month")

            if (this.year != year) {
                lifecycleScope.launch {
                    val persisted = withContext(Dispatchers.IO) {
                        financialDataRepo.loadData(year)
                    }
                    if (persisted.isNotEmpty()) {
                        financialDataBuffer.clear()
                        financialDataBuffer.putAll(java.util.TreeMap(persisted))
                    }
                }
            }

            this.year = year
            this.month = month
            flushSettings(year, month)
        }

        initSalaryModule(view)
        initInsuranceModule(view)

        db = AppDatabase.getDatabase(requireContext())
        financialDataRepo = FinancialDataRepository(db)

        lifecycleScope.launch {
            val persisted = withContext(Dispatchers.IO) {
                financialDataRepo.loadData(this@SettingsFragment.year)
            }
            if (persisted.isNotEmpty()) {
                financialDataBuffer.clear()
                financialDataBuffer.putAll(java.util.TreeMap(persisted))
            }
        }

        // "获取上个月数据"按钮点击事件
        view.findViewById<Button>(R.id.btn_get_prev_data).setOnClickListener {
            if (this.month == 1) {
                flushSettings(this.year - 1, 12)
            } else {
                flushSettings(this.year, this.month - 1)
            }
            Log.d("Settings", "成功获取上个月数据")
        }
        // "保存"按钮点击事件
        view.findViewById<Button>(R.id.btn_save_settings).setOnClickListener {
            saveSettings()
            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
            sharedViewModel.updateDataBase()
        }

        return view
    }

    private fun initSalaryModule(view: View) {
        salaryItemsList = view.findViewById(R.id.salary_items_list)

        // 添加工资项按钮点击事件
        btnAddSalaryItem = view.findViewById(R.id.btn_add_salary_item)
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

            val type = typeEditText.text.toString()
            val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val isTaxable = taxableCheckBox.isChecked

            if (amount==0.0) {
                continue
            }

            val mutableMap: MutableMap<String, Any> = mutableMapOf(
                "type" to type,
                "amount" to amount,
                "isTaxable" to isTaxable,
            )

            salaryList.add(mutableMap)
        }

        if(salaryList.isEmpty()) return

        val insuranceList = mutableListOf<MutableMap<String, Any>>()
        for (i in 0 until insuranceItemList.childCount) {
            val insuranceItem = insuranceItemList.getChildAt(i) as LinearLayout

            val typeTextView = insuranceItem.getChildAt(0) as TextView
            val valueEditText = insuranceItem.getChildAt(1) as EditText

            val type = typeTextView.tag.toString()
            val value = valueEditText.text.toString().toDoubleOrNull() ?: 0.0

            val mutableMap: MutableMap<String, Any> = mutableMapOf(
                "type" to type,
                "value" to value,
            )

            insuranceList.add(mutableMap)
        }

        // 将数据存入有序Map
        financialDataBuffer[year * 12 + month] = mutableMapOf(
            "salaryList" to salaryList,
            "insuranceList" to insuranceList,
        )

        // 打印所有存储的数据
//        financialDataBuffer.forEach { (totalMonth, data) ->
//            Log.d("SettingsData", "Date: ${(totalMonth - 1) / 12}-${(totalMonth - 1) % 12 + 1}")
//            (data["salaryList"] as? List<*>)?.forEachIndexed { index, item ->
//                Log.d("SettingsData", "Salary[$index]: $item")
//            }
//            (data["insuranceList"] as? List<*>)?.forEachIndexed { index, item ->
//                Log.d("SettingsData", "Insurance[$index]: $item")
//            }
//        }

        val salaryEntities = salaryList.map { item ->
            SalaryItemEntity(
                id = 0,
                financialDataTableId = 0, // repository 会在 upsert 时替换为正确的 settingsId
                type = item["type"] as? String ?: "",
                amount = item["amount"] as? Double ?: 0.0,
                isTaxable = item["isTaxable"] as? Boolean ?: false,
            )
        }
        val insuranceEntities = insuranceList.map { item ->
            InsuranceItemEntity(
                id = 0,
                financialDataTableId = 0,
                type = item["type"] as? String ?: "",
                value = item["value"] as? Double ?:0.0
            )
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                financialDataRepo.upsertData(
                    year,
                    month,
                    salaryEntities,
                    insuranceEntities
                )
                // 可选：写日志或发送主线程通知
            } catch (e: Exception) {
                // 处理持久化失败的情况（日志/上报），UI 不阻塞
                Log.e("Settings", "persist settings failed", e)
            }
        }
    }

    private fun flushSettings(year: Int, month: Int) {
        // 清空页面内容
        salaryItemsList.removeAllViews()
        for (i in 0 until insuranceItemList.childCount) {
            val insuranceItem = insuranceItemList.getChildAt(i) as LinearLayout
            val valueEditText = insuranceItem.getChildAt(1) as EditText
            valueEditText.text.clear()
        }

        val currentData = financialDataBuffer[year * 12 + month] as? Map<*, *> ?: return
        val salaryList = currentData["salaryList"] as? List<Map<String, Any>> ?: return

        // 创建相应数量的工资项标签
        repeat(salaryList.size) { addSalaryItem() }

        // 显示工资值
        for (i in 0 until salaryItemsList.childCount) {
            val salaryItem = salaryItemsList.getChildAt(i) as LinearLayout
            val typeEditText = salaryItem.getChildAt(0) as EditText
            val amountEditText = salaryItem.getChildAt(1) as EditText
            val taxableCheckBox = salaryItem.getChildAt(2) as CheckBox

            val salaryData = salaryList[i]
            typeEditText.setText(salaryData["type"]?.toString() ?: "")
            amountEditText.setText(salaryData["amount"]?.toString() ?: "0")
            taxableCheckBox.isChecked = salaryData["isTaxable"] as? Boolean ?: false
        }

        val insuranceList = currentData["insuranceList"] as? List<Map<String, Any>> ?: return
        for (i in 0 until insuranceItemList.childCount) {
            val insuranceItem = insuranceItemList.getChildAt(i) as LinearLayout
            val valueEditText = insuranceItem.getChildAt(1) as EditText

            val insuranceData = insuranceList[i]
            valueEditText.setText(insuranceData["value"].toString())
        }
    }
}