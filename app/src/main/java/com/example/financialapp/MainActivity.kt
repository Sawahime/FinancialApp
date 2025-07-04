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
    private lateinit var binding: ActivityMainBinding
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
        val monthNames = arrayOf(
            "1月", "2月", "3月", "4月", "5月", "6月",
            "7月", "8月", "9月", "10月", "11月", "12月"
        )
        binding.tvCurrentMonth.text = "${currentYear}年${monthNames[currentMonth - 1]}"
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