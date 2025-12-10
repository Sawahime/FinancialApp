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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
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
        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        db = AppDatabase.getDatabase(requireContext())
        financialDataRepo = FinancialDataRepository(db)
        lifecycleScope.launch {
            // Clear data base, only enable while develop
            withContext(Dispatchers.IO) { db.clearAllTables() }

            val persisted = withContext(Dispatchers.IO) {
                financialDataRepo.loadData(year)
            }
            if (persisted.isNotEmpty()) {
                financialDataBuffer.clear()
                financialDataBuffer.putAll(TreeMap(persisted))
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
                        financialDataBuffer.putAll(TreeMap(persisted))
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
                    financialDataBuffer.putAll(TreeMap(persisted))
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
        var cumulativeIncome : Double // Cumulative income from January to current month
        var cumulativeTax : Double // Cumulative tax paid from January to current month

        // Get current month data
        val monthData = thisYearData[this.year * 12 + this.month]
        if (monthData != null) {
            val salaryList = (monthData["salaryList"] as? List<*>)?.filterIsInstance<MutableMap<String, Any>>()?.toMutableList() ?: mutableListOf()
            val insuranceList = (monthData["insuranceList"] as? List<*>)?.filterIsInstance<MutableMap<String, Any>>()?.toMutableList() ?: mutableListOf()

            // TODO: How to deal with the income which is not taxable?
            // Calculate pre-tax income (taxable items only)
            preTaxIncome = salaryList.sumOf { item ->
                if (item["isTaxable"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            // Calculate social security base (taxable income included in social security)
            val socialSecurityBase = insuranceList.find {
                it["type"]?.toString() == "social_security_base"
            }?.get("value") as Double

            // Calculate housing fund base (taxable income included in housing fund)
            val housingFundBase = insuranceList.find {
                it["type"]?.toString() == "housing_fund_base"
            }?.get("value") as Double

            // Get social security personal contribution rate
            val personalInsuranceRate = insuranceList.find {
                it["type"]?.toString() == "personal_social"
            }?.get("value") as Double

            // Get housing fund personal contribution rate
            val personalHousingFundRate = insuranceList.find {
                it["type"]?.toString() == "personal_housing"
            }?.get("value") as Double

            // Calculate total insurance deduction
            insurance = socialSecurityBase * personalInsuranceRate / 100 + housingFundBase * personalHousingFundRate / 100
        }

        // --- Calculate cumulative values and tax ---
        var accumulativeIncome = 0.0 // Cumulative taxable income
        var cumulativeDeduction = 0.0 // Cumulative deductions
        var accumulativeTaxPaid = 0.0 // Cumulative tax paid up to previous month
        var accumulativeTax : Double // Cumulative tax payable (including current month)

        // Calculate cumulative values from January to current month
        for (month in 1..this.month) {
            val currentMonthData = thisYearData[this.year * 12 + month]
            if (currentMonthData == null) continue

            val currentSalaryList = (currentMonthData["salaryList"] as? List<*>)?.filterIsInstance<MutableMap<String, Any>>()?.toMutableList() ?: mutableListOf()
            val currentInsuranceList = (currentMonthData["insuranceList"] as? List<*>)?.filterIsInstance<MutableMap<String, Any>>()?.toMutableList() ?: mutableListOf()

            // Monthly taxable income
            val monthlyTaxableIncome = currentSalaryList.sumOf { item ->
                if (item["isTaxable"] as? Boolean == true) {
                    (item["amount"] as? Double) ?: 0.0
                } else {
                    0.0
                }
            }

            // Monthly social security base
            val currentSocialSecurityBase = currentInsuranceList.find {
                it["type"]?.toString() == "social_security_base"
            }?.get("value") as Double

            // Monthly housing fund base
            val currentHousingFundBase = currentInsuranceList.find {
                it["type"]?.toString() == "housing_fund_base"
            }?.get("value") as Double

            // Monthly insurance rates
            val currentPersonalInsuranceRate = currentInsuranceList.find {
                it["type"]?.toString() == "personal_social"
            }?.get("value") as Double

            val currentPersonalHousingFundRate = currentInsuranceList.find {
                it["type"]?.toString() == "personal_housing"
            }?.get("value") as Double

            // Monthly deductions (insurance + basic deduction)
            val monthlyInsurance = currentSocialSecurityBase * currentPersonalInsuranceRate / 100 + currentHousingFundBase * currentPersonalHousingFundRate / 100

            val basicDeduction = 5000.0 // Individual income tax threshold

            // Update cumulative values
            accumulativeIncome += monthlyTaxableIncome
            cumulativeDeduction += monthlyInsurance + basicDeduction

            // Calculate cumulative taxable income
            val cumulativeTaxableIncome = accumulativeIncome - cumulativeDeduction

            if (month == this.month) {
                // Calculate cumulative tax payable up to current month
                accumulativeTax = calculateTaxByThreshold(cumulativeTaxableIncome)
                // Current month tax = cumulative tax - tax paid in previous months
                tax = accumulativeTax - accumulativeTaxPaid
                // Current month net income
                netIncome = preTaxIncome - insurance - tax
                // Update cumulative tax paid (including current month)
                accumulativeTaxPaid = accumulativeTax
            } else {
                // Calculate cumulative tax for previous months
                val previousCumulativeTax = calculateTaxByThreshold(cumulativeTaxableIncome)
                // Update cumulative tax paid (excluding current month)
                accumulativeTaxPaid = previousCumulativeTax
            }
        }

        // Set final cumulative values
        cumulativeIncome = accumulativeIncome
        cumulativeTax = accumulativeTaxPaid

        val currencyFormat = "¥%,.2f"
        tvPreTaxIncome.text = String.format(Locale.getDefault(), currencyFormat, preTaxIncome)
        tvInsurance.text = String.format(Locale.getDefault(), currencyFormat, insurance)
        tvTax.text = String.format(Locale.getDefault(), currencyFormat, tax)
        tvNetIncome.text = String.format(Locale.getDefault(), currencyFormat, netIncome)
        tvYearToDateIncome.text = String.format(Locale.getDefault(), currencyFormat, cumulativeIncome)
        tvYearToDateTax.text = String.format(Locale.getDefault(), currencyFormat, cumulativeTax)

        Log.d("Financial", "${this.year}年${this.month}月财务统计:")
        Log.d("Financial", "  税前收入: ${"%.2f".format(preTaxIncome)}")
        Log.d("Financial", "  社保公积金: ${"%.2f".format(insurance)}")
        Log.d("Financial", "  本月个税: ${"%.2f".format(tax)}")
        Log.d("Financial", "  净收入: ${"%.2f".format(netIncome)}")
        Log.d("Financial", "  本年累计收入: ${"%.2f".format(cumulativeIncome)}")
        Log.d("Financial", "  本年累计个税: ${"%.2f".format(cumulativeTax)}")
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