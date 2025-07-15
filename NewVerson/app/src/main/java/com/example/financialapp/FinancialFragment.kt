package com.example.financialapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class FinancialFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("Financial", "进入Financial页面")

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_financial, container, false)

        // 获取Activity的ViewModel
        // requireActivity() 返回的是宿主 MainActivity
        // 所以 ViewModelProvider 返回的 SharedViewModel 实例和 MainActivity 里的那个是同一个
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        sharedPrefs = requireContext().getSharedPreferences("financial_data", Context.MODE_PRIVATE)
        // 开发阶段：需要时使用以下代码清空旧数据
        if (false) {
            sharedPrefs.edit { clear() }
            Log.d("Financial", "已清除所有SharedPreferences数据")
        }

        // viewLifecycleOwner:
        //      Fragment 的生命周期Owner
        //      作用：确保只在Fragment的生命周期Owner处于活跃状态时接收数据更新
        sharedViewModel.yearMonth.observe(viewLifecycleOwner) { (year, month) ->
            Log.d("Financial", "dateUpdate: $year-$month")
            val tvSalary = view.findViewById<TextView>(R.id.tvSalary)

            val yearMonth = "${year}-${month.toString().padStart(2, '0')}"
            val json = sharedPrefs.getString(yearMonth, null)
            FinanceData.fromJson(json)?.let { data ->
                tvSalary.text = "本月工资: ${data.salary}元"
            } ?: run {
                tvSalary.text = "本月工资: 未设置"
            }
        }

        return view
    }
}