package com.solo4.accessibilitychecker.service.mapper

import android.util.Log
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.solo4.accessibilitychecker.service.TAG
import org.json.JSONArray
import org.json.JSONObject

fun AccessibilityNodeInfoCompat.toJsonObject(): JSONObject {
    val node = this
    val jsonObject = JSONObject()
    try {
        jsonObject.put("viewId", node.viewIdResourceName)
        jsonObject.put("class", node.className)
        jsonObject.put("contentDesc", node.contentDescription)
        jsonObject.put("text", node.text)
        jsonObject.put("hint", node.hintText)
        jsonObject.put("isClickable", node.isClickable)
        jsonObject.put("isFocusable", node.isFocusable)
        jsonObject.put("isAccessibilityFocused", node.isAccessibilityFocused)
        jsonObject.put("isEnabled", node.isEnabled)
        jsonObject.put("isVisibleToUser", node.isVisibleToUser)
        jsonObject.put("isHeader", node.isHeading)
        jsonObject.put("isCheckable", node.isCheckable)
        jsonObject.put("isChecked", node.isChecked)
        jsonObject.put("isImportantForAccessibility", node.isImportantForAccessibility)
        jsonObject.put("isScrollable", node.isScrollable)
        jsonObject.put("isEditable", node.isEditable)

        if (node.className.contains("RecyclerView")) {
            jsonObject.put("hasCollectionInfo", (node.collectionInfo != null).toString())
        }

        // ----- Children -------------------------------------------------
        val childCount = node.childCount
        if (childCount > 0) {
            val childrenArray = JSONArray()
            for (i in 0 until childCount) {
                val child = node.getChild(i) ?: continue
                childrenArray.put(child.toJsonObject())
            }
            jsonObject.put("children", childrenArray)
        } else {
            jsonObject.put("children", JSONArray())
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error while building JSON for node", e)
    }
    return jsonObject
}