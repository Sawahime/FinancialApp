package com.example.financialapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class SettingsFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var salaryItemsList: LinearLayout
    private lateinit var btnAddSalaryItem: Button
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

        // 保存按钮点击事件
        view.findViewById<Button>(R.id.btn_save_settings).setOnClickListener {
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

    private fun addSalaryItem() {
        val salaryItemView = LayoutInflater.from(requireContext())
            .inflate(R.layout.salary_item, salaryItemsList, false)

        // 设置删除按钮点击事件
        salaryItemView.findViewById<ImageButton>(R.id.btn_delete_salary_items).setOnClickListener {
            salaryItemsList.removeView(salaryItemView)
        }

        salaryItemsList.addView(salaryItemView)
    }
}