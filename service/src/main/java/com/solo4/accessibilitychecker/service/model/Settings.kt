package com.solo4.accessibilitychecker.service.model

import android.view.accessibility.AccessibilityEvent
import com.solo4.accessibilitychecker.service.utils.activityName
import org.json.JSONArray
import org.json.JSONObject

data class Settings(
    val filters: Filters = Filters(),
)

data class Filters(
    val screens: List<String> = emptyList(),
    val events: List<String> = listOf(
        "TYPE_WINDOW_CONTENT_CHANGED",
    ),
) {

    fun canProceedEvent(event: AccessibilityEvent): Boolean {
        val isValidScreen = if (screens.isNotEmpty()) {
            val activityName = event.activityName
            if (activityName.isBlank()) {
                false
            } else {
                screens.any { it == activityName }
            }
        } else {
            true
        }

        val eventName = AccessibilityEvent.eventTypeToString(event.eventType)
        val isValidEvent = events.any { it == eventName }

        return isValidEvent && isValidScreen
    }
}

fun Settings.toJson(): String {
    val json = JSONObject()
    json.put("filters", JSONObject().apply {
        put("screens", JSONArray(filters.screens))
        put("events", JSONArray(filters.events))
    })
    return json.toString()
}