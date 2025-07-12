package com.example.financialapp

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val dateNavigator = DateNavigator()
    private val financialFragment = FinancialFragment()
    private val settingsFragment = SettingsFragment()
    private lateinit var sharedViewModel: SharedViewModel// 用于将日期更新同步到各个页面

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化各个组件
        initSharedViewModel()
        initDateNavigator()
        initBottomNavigator()
    }

    private fun initSharedViewModel() {
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        val tvDate = findViewById<TextView>(R.id.tvCurrentDate)
        val (year, month) = dateNavigator.getCurrYearMonthPair()
        sharedViewModel.updateDate(year, month)
    }

    private fun initDateNavigator() {
        val tvDate = findViewById<TextView>(R.id.tvCurrentDate)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = findViewById<ImageButton>(R.id.btnNextMonth)

        // 更新月份导航栏文字
        val (year, month) = dateNavigator.getCurrYearMonthPair()
        tvDate.text = getString(R.string.date_format, year, month)

        // 点击月份文字弹出月历选择器
        tvDate.setOnClickListener {
            showYearMonthInputDialog()
        }

        // 上个月按钮点击
        btnPrev.setOnClickListener {
            dateNavigator.prevMonth()
            updateDateNavigatorTextAndSharedViewModel()
        }

        // 下个月按钮点击
        btnNext.setOnClickListener {
            dateNavigator.nextMonth()
            updateDateNavigatorTextAndSharedViewModel()
        }
    }

    private fun initBottomNavigator() {
        val bottomNavigator = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigator.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_nav_menu_financial -> {
                    switchFragment(financialFragment)
                    true
                }

                R.id.bottom_nav_menu_settings -> {
                    switchFragment(settingsFragment)
                    true
                }

                else -> false
            }
        }
        // 初始默认页面
        bottomNavigator.selectedItemId = R.id.bottom_nav_menu_financial
        switchFragment(financialFragment)
    }

    
    private fun updateDateNavigatorTextAndSharedViewModel() {
        val tvDate = findViewById<TextView>(R.id.tvCurrentDate)
        val (year, month) = dateNavigator.getCurrYearMonthPair()
        tvDate.text = getString(R.string.date_format, year, month)
        sharedViewModel.updateDate(year, month)
    }

    private fun showYearMonthInputDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_date_input, null)
        val etYear = dialogView.findViewById<EditText>(R.id.etYear)
        val etMonth = dialogView.findViewById<EditText>(R.id.etMonth)

        // 设置当前年月为默认值
        val (currentYear, currentMonth) = dateNavigator.getCurrYearMonthPair()
        etYear.setText(currentYear.toString())
        etMonth.setText(currentMonth.toString())

        AlertDialog.Builder(this)
            .setTitle("请输入目标月份")
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val year = etYear.text.toString().toIntOrNull() ?: currentYear
                val month = etMonth.text.toString().toIntOrNull() ?: currentMonth

                // 验证月份范围
                if (month in 1..12) {
                    dateNavigator.setYearMonth(year, month)
                    updateDateNavigatorTextAndSharedViewModel()
                } else {
                    Toast.makeText(this, "月份必须是1-12", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}