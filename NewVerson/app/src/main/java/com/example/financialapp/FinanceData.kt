package com.example.financialapp

import com.google.gson.Gson

data class FinanceData(
    var salary: Int = 0,
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String?): FinanceData? {
            return json?.let { gson.fromJson(it, FinanceData::class.java) }
        }
    }

    fun toJson(): String {
        return gson.toJson(this)
    }
}