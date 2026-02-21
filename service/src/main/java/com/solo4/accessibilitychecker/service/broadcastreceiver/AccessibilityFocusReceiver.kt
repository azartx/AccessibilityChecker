package com.solo4.accessibilitychecker.service.broadcastreceiver

import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.solo4.accessibilitychecker.service.AccessibilityCheckerService

private const val RECEIVER_TAG = "AccBroadcast"

private const val ACTION_SWIPE_RIGHT = "com.solo4.ACTION_SWIPE_RIGHT"
private const val ACTION_SWIPE_LEFT = "com.solo4.ACTION_SWIPE_LEFT"
private const val ACTION_FOCUS_FIRST = "com.solo4.ACTION_FOCUS_FIRST"

// TODO: should write all errors to the log file and after error check logs
// TODO: add checks is current item is last screen item
class AccessibilityFocusReceiver : BroadcastReceiver() {

    companion object {

        val receiverActions = listOf(
            ACTION_SWIPE_RIGHT,
            ACTION_SWIPE_LEFT,
            ACTION_FOCUS_FIRST,
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val service = AccessibilityCheckerService.instance
        when(intent.action) {
            ACTION_SWIPE_RIGHT -> {
                Log.e(RECEIVER_TAG, "Perform swipe right")

                performSwipeRight(
                    context,
                    service,
                    0.2,
                    0.8
                )
            }
            ACTION_SWIPE_LEFT -> {
                Log.e(RECEIVER_TAG, "Perform swipe left")

                performSwipeRight(
                    context,
                    service,
                    0.8,
                    0.2
                )
            }
            ACTION_FOCUS_FIRST -> {
                Log.e(RECEIVER_TAG, "Request focus of the first clickable element")

                requestFirstElementFocus(service)
            }
        }
    }

    private fun performSwipeRight(
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

    fun requestFirstElementFocus(service: AccessibilityCheckerService) {
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
                node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)) {
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
}