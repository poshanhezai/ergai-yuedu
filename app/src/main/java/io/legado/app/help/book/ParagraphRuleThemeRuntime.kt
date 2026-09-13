package io.legado.app.help.book

import android.graphics.Typeface
import android.net.Uri
import io.legado.app.data.appDb
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import splitties.init.appCtx

/** Resolves assets referenced by an imported paragraph-rule theme. */
object ParagraphRuleThemeRuntime {
    private val typefaceCache = ConcurrentHashMap<String, Typeface>()
    private val varsCache = ConcurrentHashMap<Long, Map<String, String>>()

    fun varsForRule(ruleId: Long): Map<String, String> = varsCache[ruleId] ?: readVars(ruleId).also {
        varsCache[ruleId] = it
    }

    fun invalidate(ruleId: Long? = null) {
        if (ruleId == null) {
            varsCache.clear()
            typefaceCache.clear()
        } else {
            varsCache.remove(ruleId)
            typefaceCache.keys.removeIf { it.startsWith("$ruleId|") }
        }
    }

    fun typefaceForTag(tag: String): Typeface? {
        val separator = tag.indexOf('_')
        if (separator <= 0 || separator >= tag.lastIndex) return null
        val ruleId = tag.substring(0, separator).toLongOrNull() ?: return null
        val variable = tag.substring(separator + 1)
        val value = findFontVar(ruleId, variable) ?: return null
        val cacheKey = "$ruleId|$variable|$value"
        typefaceCache[cacheKey]?.let { return it }
        return loadTypeface(value)?.also { typefaceCache[cacheKey] = it }
    }

    fun pageBackground(ruleId: Long): String? {
        val values = varsForRule(ruleId)
        return listOf("pageBackground", "pageBackgroundPath", "background", "backgroundPath")
            .firstNotNullOfOrNull { key -> values[key]?.trim()?.takeIf { it.isNotEmpty() } }
    }

    fun pageBackgroundColor(ruleId: Long): String? {
        val values = varsForRule(ruleId)
        return listOf("pageBackgroundColor", "pageBgColor", "pageColor")
            .firstNotNullOfOrNull { key ->
                values[key]?.trim()?.takeIf { it.matches(Regex("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$")) }
            }
    }

    fun bodyFontVariable(ruleId: Long): String? {
        val values = varsForRule(ruleId)
        return listOf("bodyFont", "bodyFontPath", "font", "fontPath", "fontFile", "ruleFont", "ruleFontPath")
            .firstNotNullOfOrNull { key -> values[key]?.trim()?.takeIf { it.isNotEmpty() }?.let { key } }
    }

    fun bodyStyle(ruleId: Long): BodyStyle {
        val values = varsForRule(ruleId)
        val color = listOf("bodyColor", "textColor", "fontColor")
            .firstNotNullOfOrNull { key -> values[key]?.trim()?.takeIf { it.matches(Regex("^#[0-9a-fA-F]{6}$")) } }
        val background = listOf("bodyBackgroundColor", "bodyBackground", "textBackgroundColor", "paragraphBackground")
            .firstNotNullOfOrNull { key -> values[key]?.trim()?.let { value ->
                value.takeIf { it.matches(Regex("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$")) }
            } }
        return BodyStyle(
            color = color,
            background = background,
            bold = enabled(values["bodyBold"] ?: values["bold"]),
            italic = enabled(values["bodyItalic"] ?: values["italic"]),
            underline = enabled(values["bodyUnderline"] ?: values["underline"]),
            strike = enabled(values["bodyStrike"] ?: values["strike"]),
            size = values["bodySize"] ?: values["textSize"]
        )
    }

    data class BodyStyle(
        val color: String? = null,
        val background: String? = null,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strike: Boolean = false,
        val size: String? = null
    ) {
        fun isEmpty() = color == null && background == null && !bold && !italic &&
            !underline && !strike && size.isNullOrBlank()
    }

    private fun enabled(value: String?): Boolean {
        return value?.trim()?.lowercase() in setOf("true", "1", "yes", "on", "是")
    }

    private fun findFontVar(ruleId: Long, variable: String): String? {
        val values = varsForRule(ruleId)
        val candidates = listOf(
            "${variable}Font", "${variable}FontPath", "${variable}FontFile",
            "font.$variable", "font_$variable", variable,
            "font", "fontPath", "fontFile", "ruleFont", "ruleFontPath"
        )
        return candidates.firstNotNullOfOrNull { key ->
            values[key]?.trim()?.takeIf { it.isNotEmpty() }
        }
    }

    private fun readVars(ruleId: Long): Map<String, String> = runCatching {
        appDb.paragraphRuleDao.vars(ruleId).associate { it.name to it.value }
    }.getOrDefault(emptyMap())

    private fun loadTypeface(path: String): Typeface? = runCatching {
        when {
            path.startsWith("content://", true) -> {
                appCtx.contentResolver.openFileDescriptor(Uri.parse(path), "r")?.use {
                    Typeface.Builder(it.fileDescriptor).build()
                }
            }
            path.startsWith("file://", true) -> {
                Uri.parse(path).path?.takeIf { it.isNotBlank() }?.let(Typeface::createFromFile)
            }
            else -> Typeface.createFromFile(File(path))
        }
    }.getOrNull()
}
