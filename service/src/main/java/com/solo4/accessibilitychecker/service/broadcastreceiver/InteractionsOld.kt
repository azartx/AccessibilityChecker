package com.solo4.accessibilitychecker.service.broadcastreceiver

import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.solo4.accessibilitychecker.service.AccessibilityCheckerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// old screen navigations functions

private const val RECEIVER_TAG = "AccBroadcast"

private fun performSwipe(
    context: Context,
    service: AccessibilityCheckerService,
    startPointXPercent: Double, // point from screen width
    endPointXPercent: Double,
) {
    val displayMetrics = context.resources.displayMetrics
    val width = displayMetrics.widthPixels
    val height = displayMetrics.heightPixels

    val startX = (width * startPointXPercent).toInt()
    val endX = (width * endPointXPercent).toInt()
    val centerY = height / 2

    val path = Path().apply {
        moveTo(startX.toFloat(), centerY.toFloat())
        lineTo(endX.toFloat(), centerY.toFloat())
    }

    val stroke = GestureDescription.StrokeDescription(
        path, 0, 100
    )

    val gesture = GestureDescription.Builder()
        .addStroke(stroke)
        .build()

    service.dispatchGesture(gesture, null, null)
}

private fun requestFirstElementFocus(service: AccessibilityCheckerService) {
    Handler(service.mainLooper).post {
        try {
            val rootNode = service.rootInActiveWindow
            if (rootNode == null) {
                Log.e(RECEIVER_TAG, "Root node is null")
                return@post
            }

            val firstFocusableNode = findFirstFocusableNode(rootNode)

            if (firstFocusableNode != null) {
                val success = firstFocusableNode.performAction(
                    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS
                )
                if (success) {
                    Log.d(RECEIVER_TAG, "Focus set on first element")
                } else {
                    Log.e(RECEIVER_TAG, "Failed to set focus")
                }
            } else {
                Log.w(RECEIVER_TAG, "No focusable element found")
            }
        } catch (e: Exception) {
            Log.e(RECEIVER_TAG, "Error focusing element", e)
        }
    }
}

private fun findFirstFocusableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    if (node.isVisibleToUser && node.isImportantForAccessibility && !node.isAccessibilityFocused) {
        if (node.isFocusable || node.isClickable ||
            node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
        ) {
            return node
        }
    }

    for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        if (child != null) {
            val result = findFirstFocusableNode(child)
            if (result != null) {
                return result
            }
        }
    }

    return null
}

private fun requestIsFocusedItemLats(service: AccessibilityCheckerService): Boolean {
    val rootNode = service.rootInActiveWindow

    val focusedItem: AccessibilityNodeInfo? =
        rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)

    val lastItem = findLastFocusableNode(rootNode)

    val isLastItem = if (focusedItem == null) {
        // focused item is not the part of current info (like system UI, talkback bnt)
        true
    } else if (lastItem == null) {
        true
    } else {
        focusedItem == lastItem
    }

    Log.i(RECEIVER_TAG, "Is current focused item last: $isLastItem")
    return isLastItem
}

private fun findLastFocusableNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    for (i in rootNode.childCount - 1 downTo 0) {
        val child = rootNode.getChild(i) ?: continue
        val result = findLastFocusableNode(child)
        return if (result.isAccessible()) {
            result
        } else if (child.isAccessible()) {
            child
        } else {
            null
        }
    }
    return null
}

private fun getScreenA11Y(service: AccessibilityCheckerService) {
    //service.removeMakerFile()
    //service.clearA11yLogFile()

    CoroutineScope(Dispatchers.IO).launch {
        var itemsFocusedCount = 0
        requestFirstElementFocus(service)
        do {
            delay(3000)
            performSwipe(service, service, 0.2, 0.8)
            ++itemsFocusedCount
            if (itemsFocusedCount > 100) {
                Log.w(
                    RECEIVER_TAG,
                    "Too many items on screen& Is screen contains large recycler list?"
                )
                break
            }
            if (requestIsFocusedItemLats(service)) {
                break
            }
        } while (true)

        //service.createMakerFile()

        Log.i(
            RECEIVER_TAG,
            "Screen accessibility analysing completed."
        )
    }
}

private fun AccessibilityNodeInfo?.isAccessible(): Boolean {
    return this != null &&
            this.isImportantForAccessibility &&
            this.isVisibleToUser &&
            (this.isClickable || this.isFocusable)
}