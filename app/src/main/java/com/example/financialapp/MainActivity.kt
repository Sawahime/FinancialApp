package com.example.financialapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.financialapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    // lateinit表示延迟初始化，告诉编译器这个变量会在稍后初始化
    private lateinit var binding: ActivityMainBinding
    // 每次访问application时，都会调用getApplication()获取当前应用的Application对象，并将其转换为FinancialApplication类型"
    private val application get() = getApplication() as FinancialApplication

    // 当前显示的年月
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController

            binding.bottomNavigation.setupWithNavController(navController)

            // 监听导航变化，当切换到设置页面时通知月份变更
            navController.addOnDestinationChangedListener { _, destination, _ ->
                android.util.Log.d("MainActivity", "导航到: ${destination.label}")
                // 延迟通知，确保Fragment已经创建
                binding.root.post {
                    notifyFragmentsMonthChanged()
                }
            }

            setupMonthNavigation()
            updateMonthDisplay()

            // 初始化当前月份到Repository
            notifyFragmentsMonthChanged()

            android.util.Log.d("MainActivity", "应用启动成功")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "应用启动失败", e)
            throw e
        }
    }

    private fun setupMonthNavigation() {
        binding.btnPreviousMonth.setOnClickListener {
            goToPreviousMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            goToNextMonth()
        }

        // 月份区域点击事件
        binding.tvCurrentMonth.setOnClickListener {
            showYearMonthInputDialog()
        }
    }

    private fun goToPreviousMonth() {
        val calendar = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, 1)
            add(Calendar.MONTH, -1)
        }
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH) + 1

        updateMonthDisplay()
        notifyFragmentsMonthChanged()
    }

    private fun goToNextMonth() {
        val calendar = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, 1)
            add(Calendar.MONTH, 1)
        }
        currentYear = calendar.get(Calendar.YEAR)
        currentMonth = calendar.get(Calendar.MONTH) + 1

        updateMonthDisplay()
        notifyFragmentsMonthChanged()
    }

    private fun updateMonthDisplay() {
        binding.tvCurrentMonth.text = "${currentYear}年 ${currentMonth}月"
    }

    private fun notifyFragmentsMonthChanged() {
        // 通知所有Fragment月份已改变
        application.currentDisplayYear = currentYear
        application.currentDisplayMonth = currentMonth

        // 通知repository更新当前月份（这会触发财务数据重新计算）
        application.simpleRepository.setCurrentYearMonth(currentYear, currentMonth)

        // 通知当前显示的Fragment月份已变更
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val fragments = navHostFragment?.childFragmentManager?.fragments

        android.util.Log.d("MainActivity", "当前Fragment数量: ${fragments?.size}")
        fragments?.forEach { fragment ->
            android.util.Log.d("MainActivity", "Fragment类型: ${fragment::class.simpleName}")
            if (fragment is SettingsFragment) {
                fragment.onMonthChanged(currentYear, currentMonth)
                android.util.Log.d("MainActivity", "已通知SettingsFragment月份变更")
            }
        }

        android.util.Log.d("MainActivity", "月份切换到${currentYear}年${currentMonth}月")
    }

    fun getCurrentYear() = currentYear
    fun getCurrentMonth() = currentMonth

    /**
     * 显示年月选择对话框
     */
    private fun showYearMonthInputDialog() {
        // 创建容器布局
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }

        // 年份输入
        val yearLabel = android.widget.TextView(this).apply {
            text = "年份："
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val yearEditText = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentYear.toString())
            hint = "请输入年份（1900-2100）"
            selectAll()
        }

        // 月份输入
        val monthLabel = android.widget.TextView(this).apply {
            text = "月份："
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 30, 0, 0)
        }
        val monthEditText = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentMonth.toString())
            hint = "请输入月份（1-12）"
        }

        container.addView(yearLabel)
        container.addView(yearEditText)
        container.addView(monthLabel)
        container.addView(monthEditText)

        // 显示对话框
        android.app.AlertDialog.Builder(this)
            .setTitle("📅 选择年月")
            .setMessage("请输入要跳转到的年份和月份：")
            .setView(container)
            .setPositiveButton("确定") { _, _ ->
                val yearText = yearEditText.text.toString()
                val monthText = monthEditText.text.toString()

                val year = yearText.toIntOrNull()
                val month = monthText.toIntOrNull()

                var hasError = false
                var errorMessage = ""

                if (year == null || year !in 1900..2100) {
                    hasError = true
                    errorMessage = "请输入有效的年份（1900-2100）"
                } else if (month == null || month !in 1..12) {
                    hasError = true
                    errorMessage = "请输入有效的月份（1-12）"
                }

                if (hasError) {
                    android.widget.Toast.makeText(this, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    currentYear = year!!
                    currentMonth = month!!
                    updateMonthDisplay()
                    notifyFragmentsMonthChanged()
                    android.util.Log.d("MainActivity", "用户手动切换到: ${year}年${month}月")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 确保清理资源
        try {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "清理输入法失败", e)
        }
    }
}