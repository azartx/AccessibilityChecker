package com.solo4.accessibilitychecker.service.utils

import android.util.Log

private const val TAG = "A11yChecker"

fun LogeEror(message: String, tag: String = TAG) {
    Log.e(tag, message)
}