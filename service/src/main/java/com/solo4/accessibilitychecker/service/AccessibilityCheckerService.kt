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
import com.solo4.accessibilitychecker.service.mapper.toJsonObject
import com.solo4.accessibilitychecker.service.model.Settings
import com.solo4.accessibilitychecker.service.utils.LogeEror
import com.solo4.accessibilitychecker.service.utils.getAppDownloadsDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
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

private const val SERVICE_TAG = "AService"
internal const val TAG = SERVICE_TAG
internal const val DUMP_FILE_NAME = "current_screen_dump.json"

// adb shell settings put secure enabled_accessibility_services "$(adb shell settings get secure enabled_accessibility_services):com.solo4.accessibilitychecker/com.solo4.accessibilitychecker.service.AccessibilityCheckerService"

// TODO: add ability to update settings sync
// TODO: service working status in notification
@Volatile
var serviceSettings = Settings()

@SuppressLint("AccessibilityPolicy")
class AccessibilityCheckerService : AccessibilityService() {

    companion object {
        @Volatile
        lateinit var instance: AccessibilityCheckerService
            private set
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
        receiver = AccessibilityFocusReceiver(this)
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

    @OptIn(FlowPreview::class)
    private fun collectEvents() {
        if (eventsJob?.isActive == true) {
            LogeEror("Events job is active.")
            return
        }
        eventsJob = scope.launch {
            eventsProcessorFlow
                .filter { serviceSettings.filters.canProceedEvent(it) }
                .debounce(200)
                .map { getJsonAccessibilityDump() }
                .distinctUntilChanged { old, new -> old.hashCode() == new.hashCode() }
                .collectLatest { dump ->
                    mutex.withLock {
                        getAppDownloadsDir()?.writeText(dump)
                    }
                }
        }
    }

    private fun getJsonAccessibilityDump(): String {
        return rootInActiveWindow
            ?.let(AccessibilityNodeInfoCompat::wrap)
            ?.toJsonObject()
            ?.toString(2)
            ?: run {
                Log.w(TAG, "Root node is null")
                ""
            }
    }
}