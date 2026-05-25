package com.solo4.accessibilitychecker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : Activity() {

    override fun onResume() {
        super.onResume()

        val tvServiceInfo = findViewById<TextView>(R.id.tvServiceInfo)
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK)
        val isEnabled = enabledServices?.any { it.resolveInfo?.serviceInfo?.name?.contains("AccessibilityCheckerService") == true }

        tvServiceInfo.text = "Включен ли a11y сервис: @"
        tvServiceInfo.text = tvServiceInfo.text.subSequence(0, tvServiceInfo.length() - 1)
        tvServiceInfo.text = "Включен ли a11y сервис: $isEnabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mainn)

        val v = findViewById<ImageView>(R.id.imageview)
        val bSendLog = findViewById<Button>(R.id.button_send_log)

        v.contentDescription = "123123123"

        val linearBtn = findViewById<LinearLayout>(R.id.linear_button)

        linearBtn.setOnClickListener {  }

        bSendLog.setOnClickListener {
            val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, "current_screen_dump.json")

            if (!file.exists()) return@setOnClickListener

            val authority = "${packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(this, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Отправить файл"))
        }
    }
}