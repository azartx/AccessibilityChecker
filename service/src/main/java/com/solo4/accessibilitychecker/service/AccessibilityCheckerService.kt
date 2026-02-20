package com.solo4.accessibilitychecker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.solo4.accessibilitychecker.service.model.ComponentInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val SERVICE_TAG = "AService"

@SuppressLint("AccessibilityPolicy")
class AccessibilityCheckerService : AccessibilityService() {

    override fun onServiceConnected() {
        AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
            notificationTimeout = 100
            packageNames = emptyArray()
            this@AccessibilityCheckerService.serviceInfo = this
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // todo should filter package names outside the library

        if (event.eventType != TYPE_VIEW_ACCESSIBILITY_FOCUSED) return

        val source = event.source?.let { AccessibilityNodeInfoCompat.wrap(it) } ?: run {
            Log.e(SERVICE_TAG, "Event source is null. Event info:\n$event")
            return
        }

        val dataFromBundle = source.extras.keySet().map { source.extras.get(it) }.joinToString()

        Log.e(SERVICE_TAG, "Bundle data: " + dataFromBundle.ifBlank { "Bundle is empty" })

        val info = ComponentInfo(
            text = source.text?.takeIf { it.isNotBlank() }
                ?: source.contentDescription?.takeIf { it.isNotBlank() }
                ?: "No label",
            recordText = event.text.joinToString(),
            name = source.className ?: "",
            roleDescription =  parseRoleDescription(source),
            isClickable = source.isClickable
        )

        Log.e(SERVICE_TAG, info.toString())
        Log.e(SERVICE_TAG, event.toString())

        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (dir == null || !dir.exists()) {
            dir?.mkdirs()
        }

        val file = File(dir, "service_response.txt")

        if (!file.exists()) {
            file.createNewFile()
        }

        try {
            FileOutputStream(file).use {
                it.write(info.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(SERVICE_TAG, "Error writing file: ${e.message}")
        }
    }

    private fun parseRoleDescription(source: AccessibilityNodeInfoCompat): CharSequence {
        return source.roleDescription
            ?: source.className?.let { className ->
                when(className) {
                    "android.widget.Button" -> "Кнопка"
                    "android.widget.ImageView" -> "Изображение"
                    else -> className
                }
            }
            ?: "ClassName is null" // todo replace to an empty string
    }

    override fun onInterrupt() = Unit
}