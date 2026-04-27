package com.solo4.accessibilitychecker.service.broadcastreceiver

const val ACTION_ADD_ACTIVITY_FILTERING = "com.solo4.ACTION_ADD_ACTIVITY_FILTERING"
const val ACTION_RM_ACTIVITY_FILTERING = "com.solo4.ACTION_RM_ACTIVITY_FILTERING"
const val ACTION_CLEAR_ACTIVITY_FILTERING = "com.solo4.ACTION_CLEAR_ACTIVITY_FILTERING"
const val ACTION_SHOW_SETTINGS = "com.solo4.ACTION_SHOW_SETTINGS"

interface AttyCheckerBridge {

    companion object {

        val receiverActions = listOf(
            ACTION_ADD_ACTIVITY_FILTERING,
            ACTION_RM_ACTIVITY_FILTERING,
            ACTION_CLEAR_ACTIVITY_FILTERING,
            ACTION_SHOW_SETTINGS,
        )
    }

    fun addActivityFiltering(activityName: String)

    fun removeActivityFiltering(activityName: String)

    fun clearFilteringByActivity()

    fun getActualSettings(): String
}