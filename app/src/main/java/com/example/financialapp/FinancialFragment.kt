package com.example.financialapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TreeMap

class FinancialFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private var year: Int = 0
    private var month: Int = 0
    private val financialDataBuffer = TreeMap<Int, MutableMap<String, Any>>()
    private lateinit var db: AppDatabase
    private lateinit var financialDataRepo: FinancialDataRepository

    private lateinit var tvPreTaxIncome: TextView
    private lateinit var tvInsurance: TextView
    private lateinit var tvTax: TextView
    private lateinit var tvNetIncome: TextView
    private lateinit var tvYearToDateIncome: TextView
    private lateinit var tvYearToDateTax: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Financial", "Financial fragment onCreate")

        super.onCreate(savedInstanceState)
        arguments?.let {}

        // 获取Activity的ViewModel
        // requireActivity() 返回的是宿主 MainActivity
        // 所以 ViewModelProvider 返回的 SharedViewModel 实例和 MainActivity 里的那个是同一个
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        db = AppDatabase.getDatabase(requireContext())
        financialDataRepo = FinancialDataRepository(db)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { db.clearAllTables() }

            val persisted = withContext(Dispatchers.IO) {
                financialDataRepo.loadData(year)
            }
            if (persisted.isNotEmpty()) {
                financialDataBuffer.clear()
                financialDataBuffer.putAll(java.util.TreeMap(persisted))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("Financial", "Financial fragment onCreateView")

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_financial, container, false)

        tvPreTaxIncome = view.findViewById(R.id.tv_pre_tax_income)
        tvInsurance = view.findViewById(R.id.tv_insurance)
        tvTax = view.findViewById(R.id.tv_tax)
        tvNetIncome = view.findViewById(R.id.tv_net_income)
        tvYearToDateIncome = view.findViewById(R.id.tv_year_to_date_income)
        tvYearToDateTax = view.findViewById(R.id.tv_year_to_date_tax)

        // viewLifecycleOwner 是 Fragment 的生命周期 Owner, 用于确保只在Fragment的生命周期Owner处于活跃状态时接收数据更新
        sharedViewModel.yearMonth.observe(viewLifecycleOwner) { (year, month) ->
//            Log.d("Financial", "dateUpdate: $year-$month")

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
            updateUI()
        }

        sharedViewModel.dbUpdated.observe(viewLifecycleOwner){
            Log.d("Financial", "database updated")

            lifecycleScope.launch {
                val persisted = withContext(Dispatchers.IO) {
                    financialDataRepo.loadData(year)
                }
                if (persisted.isNotEmpty()) {
                    financialDataBuffer.clear()
                    financialDataBuffer.putAll(java.util.TreeMap(persisted))
                }

                updateUI()
            }
        }

        return view
    }

    private fun updateUI() {
        // Filter data for the current year
        val thisYearData = financialDataBuffer.filterKeys { total ->
            (total - 1) / 12 == this.year
        }

        var preTaxIncome = 0.0 // Pre-tax income for current month
        var insurance = 0.0 // Social security and housing fund for current month
        var tax = 0.0 // Tax payable for current month
        var netIncome = 0.0 // Net income (after tax and deductions) for current month
        var accumulativeIncome = 0.0 // Cumulative income from January to current month
        var accumulativeTax = 0.0 // Cumulative tax paid from January to current month

        // Get current month data
        val monthData = thisYearData[this.year * 12 + this.month]
        if (monthData != null) {
            val salaryList = monthData["salaryList"] as? MutableList<MutableMap<String, Any>> ?: mutableListOf()
            val insuranceList = monthData["insuranceList"] as? MutableList<MutableMap<String, Any>> ?: mutableListOf()

            // Calculate pre-tax income (taxable items only)
            preTaxIncome = salaryList.sumOf { item ->
                if (item["isTaxable"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            // Calculate social security base (taxable income included in social security)
            val socialSecurityBase = salaryList.sumOf { item ->
                if (item["isSocialSecurity"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            // Calculate housing fund base (taxable income included in housing fund)
            val housingFundBase = salaryList.sumOf { item ->
                if (item["isHousingFund"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            // Get social security personal contribution rate
            val personalInsuranceRate = insuranceList.find {
                it["type"]?.toString() == "personal_social"
            }?.get("value")?.toString()?.toDoubleOrNull() ?: 0.0

            // Get housing fund personal contribution rate
            val personalHousingFundRate = insuranceList.find {
                it["type"]?.toString() == "personal_housing"
            }?.get("value")?.toString()?.toDoubleOrNull() ?: 0.0

            // Calculate total insurance deduction
            insurance = socialSecurityBase * personalInsuranceRate / 100 + housingFundBase * personalHousingFundRate / 100
        }

        // --- Calculate cumulative values and tax ---
        var cumulativeIncome = 0.0 // Cumulative taxable income
        var cumulativeDeduction = 0.0 // Cumulative deductions
        var cumulativeTaxPaid = 0.0 // Cumulative tax paid up to previous month
        var cumulativeTax = 0.0 // Cumulative tax payable (including current month)

        // Calculate cumulative values from January to current month
        for (month in 1..this.month) {
            val currentMonthData = thisYearData[this.year * 12 + month]
            if (currentMonthData == null) continue

            val currentSalaryList =
                currentMonthData["salaryList"] as? MutableList<MutableMap<String, Any>> ?: mutableListOf()
            val currentInsuranceList =
                currentMonthData["insuranceList"] as? MutableList<MutableMap<String, Any>> ?: mutableListOf()

            // Monthly taxable income
            val monthlyTaxableIncome = currentSalaryList.sumOf { item ->
                if (item["isTaxable"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            // Monthly social security base
            val currentSocialSecurityBase = currentSalaryList.sumOf { item ->
                if (item["isSocialSecurity"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            // Monthly housing fund base
            val currentHousingFundBase = currentSalaryList.sumOf { item ->
                if (item["isHousingFund"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            // Monthly insurance rates
            val currentPersonalInsuranceRate = currentInsuranceList.find {
                it["type"]?.toString() == "personal_social"
            }?.get("value")?.toString()?.toDoubleOrNull() ?: 0.0

            val currentPersonalHousingFundRate = currentInsuranceList.find {
                it["type"]?.toString() == "personal_housing"
            }?.get("value")?.toString()?.toDoubleOrNull() ?: 0.0

            // Monthly deductions (insurance + basic deduction)
            val monthlyInsuranceDeduction =
                currentSocialSecurityBase * currentPersonalInsuranceRate / 100 +
                        currentHousingFundBase * currentPersonalHousingFundRate / 100
            val basicDeduction = 5000.0 // Individual income tax threshold

            // Update cumulative values
            cumulativeIncome += monthlyTaxableIncome
            cumulativeDeduction += monthlyInsuranceDeduction + basicDeduction

            // Calculate cumulative taxable income
            val cumulativeTaxableIncome = cumulativeIncome - cumulativeDeduction

            if (month == this.month) {
                // Calculate cumulative tax payable up to current month
                cumulativeTax = calculateTaxByThreshold(cumulativeTaxableIncome)
                // Current month tax = cumulative tax - tax paid in previous months
                tax = cumulativeTax - cumulativeTaxPaid
                // Current month net income
                netIncome = preTaxIncome - insurance - tax
                // Update cumulative tax paid (including current month)
                cumulativeTaxPaid = cumulativeTax
            } else {
                // Calculate cumulative tax for previous months
                val previousCumulativeTax = calculateTaxByThreshold(cumulativeTaxableIncome)
                // Update cumulative tax paid (excluding current month)
                cumulativeTaxPaid = previousCumulativeTax
            }
        }

        // Set final cumulative values
        accumulativeIncome = cumulativeIncome
        accumulativeTax = cumulativeTaxPaid

        val currencyFormat = "¥%,.2f"
        tvPreTaxIncome.text = String.format(currencyFormat, preTaxIncome)
        tvInsurance.text = String.format(currencyFormat, insurance)
        tvTax.text = String.format(currencyFormat, tax)
        tvNetIncome.text = String.format(currencyFormat, netIncome)
        tvYearToDateIncome.text = String.format(currencyFormat, accumulativeIncome)
        tvYearToDateTax.text = String.format(currencyFormat, accumulativeTax)

        Log.d("Financial", "${this.year}年${this.month}月财务统计:")
        Log.d("Financial", "  税前收入: ${"%.2f".format(preTaxIncome)}")
        Log.d("Financial", "  社保公积金: ${"%.2f".format(insurance)}")
        Log.d("Financial", "  本月个税: ${"%.2f".format(tax)}")
        Log.d("Financial", "  净收入: ${"%.2f".format(netIncome)}")
        Log.d("Financial", "  本年累计收入: ${"%.2f".format(accumulativeIncome)}")
        Log.d("Financial", "  本年累计个税: ${"%.2f".format(accumulativeTax)}")
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