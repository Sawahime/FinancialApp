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
import java.util.TreeMap

class SettingsFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var salaryItemsList: LinearLayout
    private lateinit var btnAddSalaryItem: Button
    private lateinit var insuranceItemList: LinearLayout
    private val settingsData = TreeMap<Int, MutableMap<String, Any>>()
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
            flushSettings(year, month)
        }

        initSalaryModule(view)
        initInsuranceModule(view)

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
            Log.d("Settings", "保存成功")
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

        val insuranceList = mutableListOf<MutableMap<String, Any>>()
        for (i in 0 until insuranceItemList.childCount) {
            val insuranceItem = insuranceItemList.getChildAt(i) as LinearLayout

            val typeTextView = insuranceItem.getChildAt(0) as TextView
            val valueEditText = insuranceItem.getChildAt(1) as EditText

            val type = typeTextView.tag.toString()
            val value = valueEditText.text.toString()

            val mutableMap: MutableMap<String, Any> = mutableMapOf(
                "type" to type,
                "value" to value,
            )

            insuranceList.add(mutableMap)
        }

        val bManual = true// 暂时没什么用，只是用来标记这份数据是手动设置的。
        // 将数据存入有序Map
        settingsData[year * 12 + month] = mutableMapOf(
            "salaryList" to salaryList,
            "insuranceList" to insuranceList,
            "bManual" to bManual
        )

        // 打印所有存储的数据
        settingsData.forEach { (totalMonth, data) ->
            Log.d("SettingsData", "Date: ${(totalMonth - 1) / 12}-${(totalMonth - 1) % 12 + 1}")
            (data["salaryList"] as? List<*>)?.forEachIndexed { index, item ->
                Log.d("SettingsData", "Salary[$index]: $item")
            }
            (data["insuranceList"] as? List<*>)?.forEachIndexed { index, item ->
                Log.d("SettingsData", "Insurance[$index]: $item")
            }
        }

        calTax(year)
    }

    private fun flushSettings(year: Int, month: Int) {
        // 清空页面内容
        salaryItemsList.removeAllViews()
        for (i in 0 until insuranceItemList.childCount) {
            val insuranceItem = insuranceItemList.getChildAt(i) as LinearLayout
            val valueEditText = insuranceItem.getChildAt(1) as EditText
            valueEditText.text.clear()
        }

        val currentData =
            settingsData[year * 12 + month] as? Map<*, *> ?: return

        val salaryList = currentData["salaryList"] as? List<Map<String, Any>> ?: return
        // 创建相应数量的工资项标签
        repeat(salaryList.size) { addSalaryItem() }
        // 显示工资值
        for (i in 0 until salaryItemsList.childCount) {
            val salaryItem = salaryItemsList.getChildAt(i) as LinearLayout
            val typeEditText = salaryItem.getChildAt(0) as EditText
            val amountEditText = salaryItem.getChildAt(1) as EditText
            val taxableCheckBox = salaryItem.getChildAt(2) as CheckBox
            val insuredCheckBox = salaryItem.getChildAt(3) as CheckBox

            val salaryData = salaryList[i]
            typeEditText.setText(salaryData["type"]?.toString() ?: "")
            amountEditText.setText(salaryData["amount"]?.toString() ?: "0")
            taxableCheckBox.isChecked = salaryData["isTaxable"] as? Boolean ?: false
            insuredCheckBox.isChecked = salaryData["isInsured"] as? Boolean ?: false
        }

        val insuranceList = currentData["insuranceList"] as? List<Map<String, Any>> ?: return
        for (i in 0 until insuranceItemList.childCount) {
            val insuranceItem = insuranceItemList.getChildAt(i) as LinearLayout
            val valueEditText = insuranceItem.getChildAt(1) as EditText

            val insuranceData = insuranceList[i]
            valueEditText.setText(insuranceData["value"]?.toString() ?: "0")
        }
    }

    private fun calTax(year: Int) {
        val thisYearData = settingsData.filterKeys { total ->
            (total - 1) / 12 == year
        }

        // 累计收入
        var cumulativeIncome = 0.0
        // 累计扣除（这部分不用交税）
        var cumulativeDeduction = 0.0
        // 累计已缴税额
        var cumulativeTaxPaid = 0.0

        // 累计应纳税所得额
        var cumulativeTaxableIncome = 0.0
        // 累计应缴税额
        var cumulativeTax = 0.0

        thisYearData.forEach { (totalMonth, data) ->
            val month = (totalMonth - 1) % 12 + 1

            // 获取当月数据
            val salaryList =
                data["salaryList"] as? MutableList<MutableMap<String, Any>> ?: mutableListOf()
            val insuranceList =
                data["insuranceList"] as? MutableList<MutableMap<String, Any>> ?: mutableListOf()

            // 计算当月收入（应税部分）
            val taxableMonthlyIncome = salaryList.sumOf { item ->
                if (item["isTaxable"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            // 计算当月社保公积金个人部分扣除
            val personalInsuranceRate = insuranceList.find {
                it["type"]?.toString() == "personal_social"
            }?.get("value")?.toString()?.toDoubleOrNull() ?: 0.0

            val personalHousingFundRate = insuranceList.find {
                it["type"]?.toString() == "personal_housing"
            }?.get("value")?.toString()?.toDoubleOrNull() ?: 0.0

            // 计算社保公积金基数（应税且参保的收入）
            val insuranceBase = salaryList.sumOf { item ->
                if (item["isInsured"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            val monthlyDeduction =
                insuranceBase * (personalInsuranceRate + personalHousingFundRate) / 100
            val basicDeduction = 5000.0// 个税起征点

            cumulativeIncome += taxableMonthlyIncome
            cumulativeDeduction += monthlyDeduction + basicDeduction
            cumulativeTaxableIncome = cumulativeIncome - cumulativeDeduction
            cumulativeTax = calculateTaxByThreshold(cumulativeTaxableIncome)
            // 本月应缴税额 = 累计应缴税额 - 累计已缴税额
            val monthlyTax = cumulativeTax - cumulativeTaxPaid
            // 更新累计已缴税额
            cumulativeTaxPaid = cumulativeTax

            Log.d("TaxCalculation", "${year}年${month}月个人所得税计算:")
            Log.d("TaxCalculation", "  本月收入: ${"%.2f".format(taxableMonthlyIncome)}")
            Log.d(
                "TaxCalculation",
                "  本月扣除: ${"%.2f".format(monthlyDeduction)} (社保公积金) + 5000.0 (基本减除) = ${
                    "%.2f".format(monthlyDeduction + basicDeduction)
                }"
            )
            Log.d("TaxCalculation", "  本月应缴税额: ${"%.2f".format(monthlyTax)}")
        }
        Log.d("TaxCalculation", "${year}年财务累计值:")
        Log.d("TaxCalculation", "  累计收入: ${"%.2f".format(cumulativeIncome)}")
        Log.d("TaxCalculation", "  累计扣除: ${"%.2f".format(cumulativeDeduction)}")
        Log.d("TaxCalculation", "  累计应纳税所得额: ${"%.2f".format(cumulativeTaxableIncome)}")
        Log.d("TaxCalculation", "  累计应缴税额: ${"%.2f".format(cumulativeTax)}")
    }

    private fun calculateTaxByThreshold(amount: Double): Double {
        return when {
            amount <= 0 -> 0.0
            amount <= 36000 -> amount * 0.03
            amount <= 144000 -> amount * 0.10 - 2520
            amount <= 300000 -> amount * 0.20 - 16920
            amount <= 420000 -> amount * 0.25 - 31920
            amount <= 660000 -> amount * 0.30 - 52920
            amount <= 960000 -> amount * 0.35 - 85920
            else -> amount * 0.45 - 181920
        }
    }
}