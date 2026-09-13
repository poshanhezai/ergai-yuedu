package io.legado.app.ui.dict.rule

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.DictRule
import io.legado.app.help.DefaultData
import io.legado.app.utils.toastOnUi

class DictRuleViewModel(application: Application) : BaseViewModel(application) {


    fun update(vararg dictRule: DictRule) {
        execute {
            appDb.dictRuleDao.update(*dictRule)
        }.onError {
            val msg = "更新字典规则出错\n${it.localizedMessage}"
            AppLog.put(msg, it)
            context.toastOnUi(msg)
        }
    }

    fun delete(vararg dictRule: DictRule) {
        execute {
            appDb.dictRuleDao.delete(*dictRule)
        }.onError {
            val msg = "删除字典规则出错\n${it.localizedMessage}"
            AppLog.put(msg, it)
            context.toastOnUi(msg)
        }
    }

    fun upSortNumber(rules: List<DictRule>) {
        if (rules.isEmpty()) return
        val normalized = rules.mapIndexed { index, rule ->
            rule.copy(sortNumber = index + 1)
        }
        execute {
            appDb.dictRuleDao.update(*normalized.toTypedArray())
        }
    }

    fun enableSelection(vararg dictRule: DictRule) {
        if (dictRule.isEmpty()) return
        execute {
            val array = dictRule.map { it.copy(enabled = true) }.toTypedArray()
            appDb.dictRuleDao.update(*array)
        }
    }

    fun disableSelection(vararg dictRule: DictRule) {
        if (dictRule.isEmpty()) return
        execute {
            val array = dictRule.map { it.copy(enabled = false) }.toTypedArray()
            appDb.dictRuleDao.update(*array)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultDictRules()
        }
    }

}
