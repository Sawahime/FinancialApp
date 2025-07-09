package com.example.financialapp

import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.example.financialapp.databinding.DialogAccountingBinding
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

/**
 * 记账对话框
 */
class AccountingDialog(
    private val context: Context,
    private val onSave: (isExpense: Boolean, amount: Double, note: String, date: Date, category: String?, subCategory: String?) -> Unit
) {
    
    private lateinit var binding: DialogAccountingBinding
    private lateinit var dialog: AlertDialog
    private val calculator = Calculator()
    private var selectedDate = Date()
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

    // 分类相关
    private val categories = mutableListOf<ExpenseCategory>()
    private val subCategories = mutableListOf<ExpenseSubCategory>()
    private var selectedCategory: ExpenseCategory? = null
    private var selectedSubCategory: ExpenseSubCategory? = null
    
    fun show() {
        binding = DialogAccountingBinding.inflate(LayoutInflater.from(context))
        
        setupUI()
        setupCalculator()
        setupDateNavigation()
        
        dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()
            
        dialog.show()
    }
    
    private fun setupUI() {
        // 设置默认日期为今天
        updateDateDisplay()

        // 设置默认金额显示
        binding.tvAmount.text = "0"

        // 初始化分类数据
        setupCategories()

        // 设置Tab切换监听
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isExpense = tab?.position == 0
                binding.llCategorySelection.visibility = if (isExpense) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupCategories() {
        // 加载默认分类
        categories.clear()
        categories.add(ExpenseCategory(0, "未分类", true))
        categories.addAll(CategoryManager.getDefaultCategories())

        // 加载默认子分类
        subCategories.clear()
        subCategories.addAll(CategoryManager.getDefaultSubCategories())

        // 设置分类下拉框
        val categoryNames = categories.map { it.name }
        val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, categoryNames)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter

        // 设置分类选择监听
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = if (position == 0) null else categories[position]
                updateSubCategorySpinner()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategory = null
                updateSubCategorySpinner()
            }
        }

        // 初始化子分类下拉框
        updateSubCategorySpinner()
    }

    private fun updateSubCategorySpinner() {
        val subCategoryNames = mutableListOf<String>()
        subCategoryNames.add("未分类")

        selectedCategory?.let { category ->
            val categorySubCategories = CategoryManager.getSubCategoriesByCategory(category.id, subCategories)
            subCategoryNames.addAll(categorySubCategories.map { it.name })
        }

        val subCategoryAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, subCategoryNames)
        subCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSubCategory.adapter = subCategoryAdapter

        // 设置子分类选择监听
        binding.spinnerSubCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSubCategory = if (position == 0 || selectedCategory == null) {
                    null
                } else {
                    val categorySubCategories = CategoryManager.getSubCategoriesByCategory(selectedCategory!!.id, subCategories)
                    if (position - 1 < categorySubCategories.size) categorySubCategories[position - 1] else null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSubCategory = null
            }
        }
    }

    private fun setupDateNavigation() {
        // 前一天按钮
        binding.btnPreviousDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                time = selectedDate
                add(Calendar.DAY_OF_MONTH, -1)
            }
            selectedDate = calendar.time
            updateDateDisplay()
        }
        
        // 后一天按钮
        binding.btnNextDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply {
                time = selectedDate
                add(Calendar.DAY_OF_MONTH, 1)
            }
            selectedDate = calendar.time
            updateDateDisplay()
        }
        
        // 点击日期显示日历选择器
        binding.tvSelectedDate.setOnClickListener {
            showDatePicker()
        }
    }
    
    private fun updateDateDisplay() {
        binding.tvSelectedDate.text = dateFormat.format(selectedDate)
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { time = selectedDate }
        
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                selectedDate = newCalendar.time
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun setupCalculator() {
        // 数字按钮
        binding.btn0.setOnClickListener { inputNumber("0") }
        binding.btn1.setOnClickListener { inputNumber("1") }
        binding.btn2.setOnClickListener { inputNumber("2") }
        binding.btn3.setOnClickListener { inputNumber("3") }
        binding.btn4.setOnClickListener { inputNumber("4") }
        binding.btn5.setOnClickListener { inputNumber("5") }
        binding.btn6.setOnClickListener { inputNumber("6") }
        binding.btn7.setOnClickListener { inputNumber("7") }
        binding.btn8.setOnClickListener { inputNumber("8") }
        binding.btn9.setOnClickListener { inputNumber("9") }
        binding.btnDot.setOnClickListener { inputNumber(".") }
        
        // 运算符按钮
        binding.btnAdd.setOnClickListener { inputOperator("+") }
        binding.btnSubtract.setOnClickListener { inputOperator("-") }
        binding.btnMultiply.setOnClickListener { inputOperator("×") }
        binding.btnDivide.setOnClickListener { inputOperator("÷") }
        
        // 功能按钮
        binding.btnClear.setOnClickListener {
            val result = calculator.clear()
            binding.tvAmount.text = result
        }
        
        binding.btnDelete.setOnClickListener {
            val result = calculator.delete()
            binding.tvAmount.text = result
        }
        
        binding.btnOk.setOnClickListener {
            saveRecord()
        }
    }
    
    private fun inputNumber(number: String) {
        val result = calculator.inputNumber(number)
        binding.tvAmount.text = result
    }
    
    private fun inputOperator(operator: String) {
        val result = calculator.inputOperator(operator)
        binding.tvAmount.text = result
    }

    private fun calculateResult() {
        val result = calculator.calculate()
        binding.tvAmount.text = result
    }
    
    private fun saveRecord() {
        // 先执行一次计算，确保获取最终结果
        calculateResult()

        val amount = calculator.getCurrentValue()
        if (amount <= 0) {
            android.widget.Toast.makeText(context, "请输入有效金额", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val isExpense = binding.tabLayout.selectedTabPosition == 0 // 0=支出, 1=收入
        val note = binding.etNote.text.toString().trim()

        // 获取分类信息
        val categoryName = selectedCategory?.name
        val subCategoryName = selectedSubCategory?.name

        onSave(isExpense, amount, note, selectedDate, categoryName, subCategoryName)
        dialog.dismiss()
    }
}
