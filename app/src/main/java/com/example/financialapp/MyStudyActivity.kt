package com.example.financialapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Person(
    var name: String,
    var age: Int
) {
    // 次构造函数：无参构造（提供默认值）
    constructor() : this("未知", 0) {
        println("调用了次构造函数")
    }

    fun greet() {
        println("Hi, I'm $name, I'm $age")
    }
}

class MyStudyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_study)

        var person1 = Person("Louis", 18)
        person1.greet()

        val person2 = Person()
        person2.greet()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}