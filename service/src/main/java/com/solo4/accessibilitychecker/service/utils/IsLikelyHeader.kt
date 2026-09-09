package com.solo4.accessibilitychecker.service.utils

import android.graphics.Rect
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * Определяет, является ли нода вероятным заголовком экрана (по эвристике).
 *
 * Эвристика не опирается на имени класса ноды, поэтому корректно обрабатывает
 * заголовки, свёрстанные контейнерами (например, LinearLayout). При этом
 * корректным значением класса для заголовка считается TextView — это учитывается
 * при оформлении дефекта отдельно.
 *
 * Нода считается вероятным заголовком, если одновременно выполнены условия:
 *  - присутствует читаемый текст или contentDescription;
 *  - нода не является кликабельной или редактируемой;
 *  - среди предков ноды нет кликабельных (текст внутри кнопки не заголовок);
 *  - нода важна для доступности;
 *  - текст короткий (не длиннее 60 символов) и не заканчивается точкой;
 *  - высота ноды не менее 40 px либо нода расположена у верхней границы экрана.
 *
 * Нода НЕ считается заголовком, если её читаемый текст содержит следующие признаки:
 *  - паттерн «ключ. значение» в начале (короткое слово с заглавной буквы, затем
 *    точка с пробелом и значение) — типичная подпись поля в списке, а не заголовок
 *    раздела (например, «Валюта. Российский рубль» — текст, а не заголовок);
 *  - символ переноса строки («\n») — многострочный текст не является заголовком
 *    (например, «Открытие и обслуживание\nБесплатно всегда»);
 *  - заглавная буква в середине текста вне контекста — внезапно появившееся слово
 *    с большой буквы посреди текста нехарактерно для заголовка
 *    (например, «Бесплатно» в «Открытие и обслуживание Бесплатно всегда»).
 */
fun AccessibilityNodeInfoCompat.isLikelyHeader(bounds: Rect?): Boolean {
    val textValue = text?.toString().orEmpty()
    val contentDescValue = contentDescription?.toString().orEmpty()
    val readableText = textValue + contentDescValue

    if (readableText.isBlank()) return false
    if (isClickable || isEditable) return false
    if (hasClickableAncestor()) return false
    if (matchesKeyValuePattern(readableText)) return false
    if (readableText.contains('\n')) return false
    if (hasMidTextCapital(readableText)) return false

    val shortText = readableText.trim().length <= 60 &&
            !readableText.trim().endsWith(".")
    val runsAtTop = bounds != null && bounds.top < 400

    return isImportantForAccessibility && shortText &&
            ((bounds?.height() ?: 0) >= 40 || runsAtTop)
}

/**
 * Возвращает true, если среди предков ноды есть кликабельный элемент.
 * Проход ограничен глубиной 5 уровней для защиты от циклических ссылок.
 */
private fun AccessibilityNodeInfoCompat.hasClickableAncestor(): Boolean {
    var node = parent
    var depth = 0
    while (node != null && depth < MAX_HEADER_ANCESTOR_DEPTH) {
        if (node.isClickable) return true
        node = node.parent
        depth++
    }
    return false
}

/**
 * Проверяет, начинается ли читаемый текст с паттерна «ключ. значение»
 * (короткое слово с заглавной буквы, точка с пробелом, значение).
 * Такая форма типична для подписи поля («Валюта. Российский рубль»),
 * а не для заголовка раздела, поэтому такая нода не является заголовком.
 */
private fun matchesKeyValuePattern(readableText: String): Boolean {
    val trimmed = readableText.trim()
    return trimmed.matches(Regex("""^\p{Lu}\p{L}+\s*\.\s+\S.*$""", RegexOption.DOT_MATCHES_ALL))
}

/**
 * Возвращает true, если в середине текста присутствует слово, начинающееся
 * с заглавной буквы и не являющееся началом нового предложения (не стоит после
 * точки, восклицательного или вопросительного знака). Такое «внезапно вырванное
 * из контекста» заглавное слово нехарактерно для заголовка
 * (например, «Бесплатно» в «Открытие и обслуживание Бесплатно всегда»).
 */
private fun hasMidTextCapital(readableText: String): Boolean {
    val text = readableText.trim()
    var pendingCapital = false
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (ch == '.' || ch == '!' || ch == '?') {
            pendingCapital = true
        } else if (ch == ' ' || ch == '\t') {
            if (pendingCapital) pendingCapital = false
        } else if (ch.isUpperCase() && !pendingCapital && i > 0 && text[i - 1] == ' ') {
            return true
        }
        i++
    }
    return false
}

private const val MAX_HEADER_ANCESTOR_DEPTH = 5