package com.example.financialapp

import androidx.room.withTransaction

class SettingsRepository(private val db: AppDatabase) {
    private val dao = db.settingsDao()

    suspend fun upsertSettings(
        year: Int,
        month: Int,
        bManual: Boolean,
        salaryItems: List<SalaryItemEntity>,
        insuranceItems: List<InsuranceItemEntity>
    ) {
        db.withTransaction {
            val existing = dao.getSettingsByYearMonth(year, month)
            if (existing != null) {
                val settingsId = existing.settings.id
                dao.updateSettings(SettingsEntity(settingsId, year, month, bManual))
                dao.deleteSalaryBySettings(settingsId)
                dao.deleteInsuranceBySettings(settingsId)
                dao.insertSalaryItems(salaryItems.map { it.copy(settingsId = settingsId) })
                dao.insertInsuranceItems(insuranceItems.map { it.copy(settingsId = settingsId) })
            } else {
                val newId = dao.insertSettings(SettingsEntity(0, year, month, bManual)).toInt()
                dao.insertSalaryItems(salaryItems.map { it.copy(settingsId = newId) })
                dao.insertInsuranceItems(insuranceItems.map { it.copy(settingsId = newId) })
            }
        }
    }

    /**
     * 把 DB 中的数据转换成与 settingsData 兼容的 Map<Int, MutableMap<String, Any>> 结构
     */
    suspend fun loadAllAsSettingsMap(): Map<Int, MutableMap<String, Any>> {
        val list = dao.getAllSettingsWithItems()
        val result = mutableMapOf<Int, MutableMap<String, Any>>()
        list.forEach { swi ->
            val key = swi.settings.year * 12 + swi.settings.month
            val salaryList = swi.salaryList.map { item ->
                mutableMapOf<String, Any>(
                    "type" to item.type,
                    "amount" to item.amount,
                    "isTaxable" to item.isTaxable,
                    "isInsured" to item.isInsured
                )
            }.toMutableList<MutableMap<String, Any>>()
            val insuranceList = swi.insuranceList.map { item ->
                mutableMapOf<String, Any>(
                    "type" to item.type,
                    "value" to item.value
                )
            }.toMutableList<MutableMap<String, Any>>()
            result[key] = mutableMapOf(
                "salaryList" to salaryList,
                "insuranceList" to insuranceList,
                "bManual" to swi.settings.bManual
            )
        }
        return result
    }
}
