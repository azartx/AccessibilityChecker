package com.solo4.accessibilitychecker.service.broadcastreceiver

import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.util.Log
import com.solo4.accessibilitychecker.service.AccessibilityCheckerService

private const val RECEIVER_TAG = "AccBroadcast"

private const val ACTION_SWIPE_RIGHT = "com.solo4.ACTION_SWIPE_RIGHT"
private const val ACTION_SWIPE_LEFT = "com.solo4.ACTION_SWIPE_LEFT"

class AccessibilityFocusReceiver : BroadcastReceiver() {

    companion object {

        val receiverActions = listOf(
            ACTION_SWIPE_RIGHT,
            ACTION_SWIPE_LEFT,
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            ACTION_SWIPE_RIGHT -> {
                Log.e(RECEIVER_TAG, "Perform swipe right")

                performSwipeRight(
                    context,
                    AccessibilityCheckerService.instance,
                    0.2,
                    0.8
                )
            }
            ACTION_SWIPE_LEFT -> {
                Log.e(RECEIVER_TAG, "Perform swipe left")

                performSwipeRight(
                    context,
                    AccessibilityCheckerService.instance,
                    0.8,
                    0.2
                )
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
}