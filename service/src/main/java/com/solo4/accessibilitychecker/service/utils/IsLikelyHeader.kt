package com.solo4.accessibilitychecker.service.utils

import android.graphics.Rect
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

fun AccessibilityNodeInfoCompat.isLikelyHeader(bounds: Rect?): Boolean {
    val classPath = className?.toString() ?: return false
    val textValue = text?.toString().orEmpty()
    val contentDescValue = contentDescription?.toString().orEmpty()

    val isTextLike = classPath.contains("TextView") || classPath.endsWith("TextView")
    if (!isTextLike) return false
    if (textValue.isBlank() && contentDescValue.isBlank()) return false
    if (isClickable || isEditable) return false

    val height = bounds?.height() ?: 0
    val shortText = textValue.length <= 60 && !textValue.trim().endsWith(".")
    val runsAtTop = bounds != null && bounds.top < 400

    return (isImportantForAccessibility) && shortText && (height >= 40 || runsAtTop)
}