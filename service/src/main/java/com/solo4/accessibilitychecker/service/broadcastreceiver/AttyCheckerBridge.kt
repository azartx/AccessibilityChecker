package com.solo4.accessibilitychecker.service.broadcastreceiver

private const val ACTION_SWIPE_RIGHT = "com.solo4.ACTION_SWIPE_RIGHT"
private const val ACTION_SWIPE_LEFT = "com.solo4.ACTION_SWIPE_LEFT"
private const val ACTION_FOCUS_FIRST = "com.solo4.ACTION_FOCUS_FIRST"
private const val ACTION_IS_FOCUSED_ITEM_LAST = "com.solo4.ACTION_IS_FOCUSED_ITEM_LAST"
private const val ACTION_GET_SCREEN_A11Y = "com.solo4.ACTION_GET_SCREEN_A11Y"

interface AttyCheckerBridge {

    companion object {

        val receiverActions = listOf(
            ACTION_SWIPE_RIGHT,
            ACTION_SWIPE_LEFT,
            ACTION_FOCUS_FIRST,
            ACTION_IS_FOCUSED_ITEM_LAST,
            ACTION_GET_SCREEN_A11Y,
        )
    }

    fun setFilteringByActivity(activityName: String)

    fun removeFilteringByActivity(activityName: String)

    fun clearFilteringByActivity()

    fun getActualSettings()
}