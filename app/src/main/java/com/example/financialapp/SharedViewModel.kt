package com.example.financialapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val year = MutableLiveData<Int>()
    val month = MutableLiveData<Int>()
    val yearMonth = MutableLiveData<Pair<Int, Int>>()

    // 更新月份
    fun updateDate(year: Int, month: Int) {
        this.year.value = year
        this.month.value = month
        yearMonth.value = Pair(year, month)
    }

    val dbUpdated= MutableLiveData<Boolean>()
    fun updateDataBase() {
        dbUpdated.value = !(dbUpdated.value ?: true)
    }
}