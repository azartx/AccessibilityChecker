package com.solo4.accessibilitychecker.service.model

data class ComponentInfo(
    val text: CharSequence,
    val recordText: CharSequence, // text from component, taken by system
    val name: CharSequence,
    val roleDescription: CharSequence,
    val isClickable: Boolean,
) {

    fun toByteArray(): ByteArray {
        return toString().toByteArray()
    }
}
