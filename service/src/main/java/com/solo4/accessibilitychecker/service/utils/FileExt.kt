package com.solo4.accessibilitychecker.service.utils

import android.content.Context
import android.os.Environment
import com.solo4.accessibilitychecker.service.DUMP_FILE_NAME
import java.io.File

internal fun Context.getAppDownloadsDir(): File? {
    val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    if (dir != null) {
        val file = File(dir, DUMP_FILE_NAME)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }
    return null
}