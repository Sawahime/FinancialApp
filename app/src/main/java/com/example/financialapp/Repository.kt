package com.example.financialapp

import androidx.room.withTransaction

class FinancialDataRepository(private val db: AppDatabase) {
    private val dao = db.financialDataDao()

    suspend fun upsertData(
        year: Int,
        month: Int,
        salaryItems: List<SalaryItemEntity>,
        insuranceItems: List<InsuranceItemEntity>
    ) {
        db.withTransaction {
            val existing = dao.getDataByYearMonth(year, month)
            if (existing != null) {
                val dataId = existing.financialDataEntity.id
                dao.updateData(FinancialDataEntity(dataId, year, month))
                dao.deleteSalaryByDataId(dataId)
                dao.deleteInsuranceByDataId(dataId)
                dao.insertSalaryItems(salaryItems.map { it.copy(financialDataTableId = dataId) })
                dao.insertInsuranceItems(insuranceItems.map { it.copy(financialDataTableId = dataId) })
            } else {
                val newId = dao.insertData(FinancialDataEntity(0, year, month)).toInt()
                dao.insertSalaryItems(salaryItems.map { it.copy(financialDataTableId = newId) })
                dao.insertInsuranceItems(insuranceItems.map { it.copy(financialDataTableId = newId) })
            }
        }
    }

    suspend fun loadData(year: Int? = null): Map<Int, MutableMap<String, Any>> {
        val list = when {
            year != null -> dao.getDataByYear(year)
            else -> dao.getAllData()
        }

        val result = mutableMapOf<Int, MutableMap<String, Any>>()
        list.forEach { swi ->
            val key = swi.financialDataEntity.year * 12 + swi.financialDataEntity.month
            val salaryList = swi.salaryList.map { item ->
                mutableMapOf<String, Any>(
                    "type" to item.type,
                    "amount" to item.amount,
                    "isTaxable" to item.isTaxable,
                )
            }.toMutableList<MutableMap<String, Any>>()
            val insuranceList = swi.insuranceList.map { item ->
                mutableMapOf<String, Any>(
                    "type" to item.type,
                    "value" to item.value
                )
            }.toMutableList<MutableMap<String, Any>>()

            // 把 DB 中的数据转换成 Map<Int, MutableMap<String, Any>> 结构
            result[key] = mutableMapOf(
                "salaryList" to salaryList,
                "insuranceList" to insuranceList,
            )
        }

        if (year != null) {
            loadDataByYearMonth(year - 1, 12)?.let {
                result[(year - 1) * 12 + 12] = it
            }
        }

        return result
    }

    suspend fun loadDataByYearMonth(year: Int, month: Int): MutableMap<String, Any>? {
        val singleMonthData = dao.getDataByYearMonth(year, month);
        if (singleMonthData == null) {
            return null
        }

        val salaryList = singleMonthData.salaryList.map { item ->
            mutableMapOf<String, Any>(
                "type" to item.type,
                "amount" to item.amount,
                "isTaxable" to item.isTaxable,
            )
        }.toMutableList<MutableMap<String, Any>>()

        val insuranceList = singleMonthData.insuranceList.map { item ->
            mutableMapOf<String, Any>(
                "type" to item.type,
                "value" to item.value
            )
        }.toMutableList<MutableMap<String, Any>>()

        return mutableMapOf(
            "salaryList" to salaryList,
            "insuranceList" to insuranceList,
        )
    }

    suspend fun deleteDataByYearMonth(year: Int, month: Int) {
        db.withTransaction {
            dao.deleteByYearMonth(year, month)
        }
    }
}
