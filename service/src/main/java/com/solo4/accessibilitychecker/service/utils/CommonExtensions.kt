package com.solo4.accessibilitychecker.service.utils

import android.view.accessibility.AccessibilityEvent

val AccessibilityEvent.activityName: String
    get() = className?.toString() ?: ""