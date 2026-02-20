package com.solo4.accessibilitychecker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.os.Environment
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
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
            this@AccessibilityCheckerService.serviceInfo = this
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        //Log.e(SERVICE_TAG, AccessibilityEvent.eventTypeToString(event.eventType))
        if (event.eventType != TYPE_VIEW_ACCESSIBILITY_FOCUSED) return

        val source = event.source ?: run {
            Log.e(SERVICE_TAG, "Event source is null. Event info:\n$event")
            return
        }

        val info = ComponentInfo(
            text = source.text ?: source.contentDescription ?: "",
            name = source.className ?: "",
            isClickable = source.isClickable
        )

        Log.e(SERVICE_TAG, info.toString())

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

    override fun onInterrupt() = Unit
}