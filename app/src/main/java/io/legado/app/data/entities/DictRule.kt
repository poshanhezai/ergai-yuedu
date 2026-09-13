package io.legado.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeRule.Companion.setCoroutineContext
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.currentCoroutineContext

/**
 * 字典规则
 */
@Entity(tableName = "dictRules")
data class DictRule(
    @PrimaryKey
    var name: String = "",
    var urlRule: String = "",
    var showRule: String = "",
    @ColumnInfo(defaultValue = "1")
    var enabled: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    var sortNumber: Int = 0
) {

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is DictRule) {
            return name == other.name
        }
        return false
    }

    /**
     * 搜索字典。
     *
     * 字典接口在请求失败或返回空 body 时不能用 !!，否则会把正常的网络错误变成
     * NullPointerException，管理/阅读页只能显示一个不明确的错误。统一转换为空结果，
     * 也避免 showRule 对 null 内容进行解析。
     */
    suspend fun search(word: String): String {
        if (word.isBlank()) {
            throw NoStackTraceException("查询内容不能为空")
        }
        if (urlRule.isBlank()) {
            throw NoStackTraceException("字典规则地址为空")
        }
        val analyzeUrl = AnalyzeUrl(
            urlRule,
            key = word,
            coroutineContext = currentCoroutineContext()
        )
        val body = analyzeUrl.getStrResponseAwait().body.orEmpty()
        if (body.isBlank()) {
            throw NoStackTraceException("字典接口返回为空")
        }
        if (showRule.isBlank()) {
            return body
        }
        val analyzeRule = AnalyzeRule().setCoroutineContext(currentCoroutineContext())
        analyzeRule.setRuleName(name)
        return analyzeRule.getString(showRule, mContent = body).ifBlank {
            "未找到匹配内容"
        }
    }

    suspend fun buttonClick(name: String, click: String) {
        val analyzeRule = AnalyzeRule().setCoroutineContext(currentCoroutineContext())
        analyzeRule.setRuleName(this.name)
        analyzeRule.evalJS(click, name)
    }

}
