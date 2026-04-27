package com.solo4.accessibilitychecker.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.IntentFilter
import android.os.Environment
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.solo4.accessibilitychecker.service.broadcastreceiver.AccessibilityFocusReceiver
import com.solo4.accessibilitychecker.service.broadcastreceiver.AttyCheckerBridge
import com.solo4.accessibilitychecker.service.model.Settings
import com.solo4.accessibilitychecker.service.utils.LogeEror
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

// adb shell settings put secure enabled_accessibility_services "$(adb shell settings get secure enabled_accessibility_services):com.solo4.accessibilitychecker/com.solo4.accessibilitychecker.service.AccessibilityCheckerService"

// TODO: add ability to update settings sync
// TODO: service working status in notification
@Transient
var serviceSettings = Settings()

@SuppressLint("AccessibilityPolicy")
class AccessibilityCheckerService : AccessibilityService() {

    companion object {
        @Volatile
        lateinit var instance: AccessibilityCheckerService; private set
    }

    private var receiver: AccessibilityFocusReceiver? = null

    private val scope = CoroutineScope(Dispatchers.Default)
    private var eventsJob: Job? = null
    private val mutex = Mutex()

    private val eventsProcessorFlow = MutableSharedFlow<AccessibilityEvent>()

    override fun onServiceConnected() {
        instance = this
        val filter = IntentFilter().apply {
            AttyCheckerBridge.receiverActions.forEach { addAction(it) }
        }
        receiver = AccessibilityFocusReceiver()
        registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        collectEvents()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        eventsJob?.cancel()
        receiver?.let { unregisterReceiver(it) }
        receiver = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        scope.launch {
            eventsProcessorFlow.emit(event)
        }
    }

    private fun collectEvents() {
        if (eventsJob?.isActive == true) {
            LogeEror("Events job is active.")
            return
        }
        eventsJob = scope.launch {
            eventsProcessorFlow
                .filter { serviceSettings.filters.canProceedEvent(it) }
                .debounce(200)
                .map {
                    dumpActiveWindow()
                }
                .distinctUntilChanged { old, new -> old.hashCode() == new.hashCode() }
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


    private fun dumpActiveWindow(): String {
        val root: AccessibilityNodeInfoCompat? = rootInActiveWindow?.let {
            AccessibilityNodeInfoCompat.wrap(it)
        }
        if (root == null) {
            Log.w(TAG, "Root node is null")
            return ""
        }

        val jsonRoot = nodeToJson(root)
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
}