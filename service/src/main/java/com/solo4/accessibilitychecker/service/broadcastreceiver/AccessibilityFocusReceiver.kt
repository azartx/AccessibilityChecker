package com.solo4.accessibilitychecker.service.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.solo4.accessibilitychecker.service.AccessibilityCheckerService
import com.solo4.accessibilitychecker.service.model.toJson
import com.solo4.accessibilitychecker.service.serviceSettings

class AccessibilityFocusReceiver(
    private val service: AccessibilityCheckerService
) : BroadcastReceiver(), AttyCheckerBridge {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ADD_ACTIVITY_FILTERING -> {
                addActivityFiltering(intent.getStringExtra("activity") ?: return)
                postResult("Active activities filter: ${serviceSettings.filters.screens}")
            }

            ACTION_RM_ACTIVITY_FILTERING -> {
                removeActivityFiltering(intent.getStringExtra("activity") ?: return)
                postResult("Active activities filter: ${serviceSettings.filters.screens}")
            }

            ACTION_CLEAR_ACTIVITY_FILTERING -> {
                clearFilteringByActivity()
                postResult("Active activities filter: ${serviceSettings.filters.screens}")
            }

            ACTION_SHOW_SETTINGS -> {
                postResult("Actual settings: ${getActualSettings()}")
            }

            else -> {
                setResult(0, null, null)
            }
        }
    }

    override fun addActivityFiltering(activityName: String) {
        val currentFilters = serviceSettings.filters
        val updatedScreens = currentFilters.screens.toMutableList()
        if (activityName !in updatedScreens) {
            updatedScreens.add(activityName)
        }
        serviceSettings = serviceSettings.copy(
            filters = currentFilters.copy(screens = updatedScreens)
        )
    }

    override fun removeActivityFiltering(activityName: String) {
        val currentFilters = serviceSettings.filters
        val updatedScreens = currentFilters.screens - activityName
        serviceSettings = serviceSettings.copy(
            filters = currentFilters.copy(screens = updatedScreens)
        )
    }

    override fun clearFilteringByActivity() {
        serviceSettings = serviceSettings.copy(
            filters = serviceSettings.filters.copy(screens = emptyList())
        )
    }

    override fun getActualSettings(): String {
        return runCatching { serviceSettings.toJson() }.getOrElse { "" }
    }
}

private fun AccessibilityFocusReceiver.postResult(result: String? = null) {
    setResult(0, result, null)
}