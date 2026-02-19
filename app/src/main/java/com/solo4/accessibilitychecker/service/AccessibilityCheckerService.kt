package com.solo4.accessibilitychecker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent

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
        val text = event.text.joinToString()
        val text2 = event.source?.let { info ->
            "${info.text} | ${info.contentDescription} | ${info.isClickable} | ${info.className}"
        } ?: "empty"
        Log.e(SERVICE_TAG, text + " ::: " + text2)
    }

    override fun onInterrupt() {

    }
}