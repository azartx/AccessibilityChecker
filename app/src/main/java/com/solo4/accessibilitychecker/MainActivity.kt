package com.solo4.accessibilitychecker

import android.app.Activity
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainn)

        val v = findViewById<ImageView>(R.id.imageview)

        v.contentDescription = "123123123"

        val linearBtn = findViewById<LinearLayout>(R.id.linear_button)

        linearBtn.setOnClickListener {  }
    }
}