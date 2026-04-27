package com.solo4.accessibilitychecker.service.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf

class AccessibilityFocusReceiver : BroadcastReceiver(), AttyCheckerBridge {

    override fun onReceive(context: Context, intent: Intent) {
        // TODO: handle results (receiverActions)
        setResult(0, resultData.toString(), bundleOf())
    }

    override fun setFilteringByActivity(activityName: String) {
        // TODO
    }

    override fun removeFilteringByActivity(activityName: String) {
        // TODO
    }

    override fun clearFilteringByActivity() {
        // TODO
    }

    override fun getActualSettings() {
        TODO("Not yet implemented")
    }
}