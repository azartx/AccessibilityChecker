package com.solo4.accessibilitychecker.service.model

data class ComponentInfo(
    val text: CharSequence,
    val name: CharSequence,
    val isClickable: Boolean,
) {

    override fun toString(): String {
        return "ComponentInfo: text = $text; name = $name; isClickable = $isClickable"
    }

    fun toByteArray(): ByteArray {
        return toString().toByteArray()
    }
}
