package com.example.financialapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val financialFragment = FinancialFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}