package com.solo4.accessibilitychecker.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.IntentFilter
import android.os.Environment
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.solo4.accessibilitychecker.service.broadcastreceiver.AccessibilityFocusReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val SERVICE_TAG = "AService"
private const val TAG = SERVICE_TAG
private const val COMPONENTS_INFO_FILE_NAME = "service_response.txt"
private const val DUMP_FILE_NAME = "current_screen_dump.json"
private const val STORAGE_A11Y_INFO_FILE_PATH = "/storage/emulated/0/Android/data/com.solo4.accessibilitychecker/files/Download/$DUMP_FILE_NAME"

// TODO: Стянуть токбэк из гитхаб репозитория, в классе AccessibilityNodeFeedbackUtils есть получение озвучки для роли компонента
@SuppressLint("AccessibilityPolicy")
class AccessibilityCheckerService : AccessibilityService() {

    companion object {
        @Volatile
        lateinit var instance: AccessibilityCheckerService; private set
    }

    private var receiver: AccessibilityFocusReceiver? = null

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val eventsProcessorFlow = MutableSharedFlow<String>() // AccessibilityEvent.eventTypeToString(event.eventType)

    init {
        scope.launch {
            eventsProcessorFlow
                .filter { it == "TYPE_WINDOW_CONTENT_CHANGED" }
                .debounce(200)
                .map {
                    dumpActiveWindow()
                }
                .distinctUntilChanged()
                .collectLatest { dump ->
                    mutex.withLock {
                        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        if (dir != null) {
                            val file = File(dir, DUMP_FILE_NAME)
                            if (!file.exists()) {
                                file.createNewFile()
                            }
                            file.writeText(dump)
                        }
                    }
                }
        }
    }

    override fun onServiceConnected() {
        instance = this
        val filter = IntentFilter().apply {
            AccessibilityFocusReceiver.receiverActions.forEach { addAction(it) }
        }
        receiver = AccessibilityFocusReceiver()
        registerReceiver(receiver, filter, RECEIVER_EXPORTED)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        scope.launch(Dispatchers.Main) {
            eventsProcessorFlow.emit(AccessibilityEvent.eventTypeToString(event.eventType))
        }
    }


    private fun dumpActiveWindow(): String {
        val root: AccessibilityNodeInfo? = rootInActiveWindow
        if (root == null) {
            Log.w(TAG, "Root node is null")
            return ""
        }

        val jsonRoot = nodeToJson(AccessibilityNodeInfoCompat.wrap(root))
        Log.i(TAG, jsonRoot.toString(2))
        root.recycle()
        return jsonRoot.toString(2)
    }

    private fun nodeToJson(node: AccessibilityNodeInfoCompat): JSONObject {
        val obj = JSONObject()
        try {
            obj.put("viewId", node.viewIdResourceName)
            obj.put("class", node.className)
            obj.put("contentDesc", node.contentDescription)
            obj.put("text", node.text)
            obj.put("hint", node.hintText)
            obj.put("clickable", node.isClickable)
            obj.put("focusable", node.isFocusable)
            obj.put("accessibilityFocused", node.isAccessibilityFocused)
            obj.put("enabled", node.isEnabled)
            obj.put("isVisible", node.isVisibleToUser)

            if (node.className.contains("RecyclerView")) {
                obj.put("hasCollectionInfo", (node.collectionInfo != null).toString())
            }

            // ----- Children -------------------------------------------------
            val childCount = node.childCount
            if (childCount > 0) {
                val childrenArray = JSONArray()
                for (i in 0 until childCount) {
                    val child = node.getChild(i) ?: continue
                    childrenArray.put(nodeToJson(child))
                    child.recycle()
                }
                obj.put("children", childrenArray)
            } else {
                obj.put("children", JSONArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while building JSON for node", e)
        }
        return obj
    }

    fun clearA11yLogFile() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (dir != null && dir.exists()) {
            val file = File(dir, COMPONENTS_INFO_FILE_NAME)
            if (file.exists() && file.length() > 0) {
                file.outputStream().use { it.write(byteArrayOf()) }
            }
        }
    }

    fun createMakerFile() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (dir != null) {
            val file = File(dir, "marker.txt")
            if (!file.exists()) {
                file.createNewFile()
            }
        }
    }

    fun removeMakerFile() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (dir != null) {
            val file = File(dir, "marker.txt")
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override fun onInterrupt() {
        scope.cancel()
        receiver?.let { unregisterReceiver(it) }
        receiver = null
    }
}