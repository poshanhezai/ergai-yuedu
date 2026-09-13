package io.legado.app.ui.book.read.config

import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookParagraphRule
import io.legado.app.data.entities.ParagraphRule
import io.legado.app.data.entities.ParagraphRuleVar
import io.legado.app.data.entities.ParagraphRuleThemePackage
import io.legado.app.databinding.ActivityThemeManageBinding
import io.legado.app.databinding.ItemThemePackageBinding
import io.legado.app.help.http.newCallResponseBody
import io.legado.app.help.http.okHttpClient
import io.legado.app.lib.theme.UiCorner
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.applyUiBodyTypefaceDeep
import io.legado.app.lib.theme.applyUiLabelStyle
import io.legado.app.lib.theme.applyUiSectionTitleStyle
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.lib.theme.secondaryTextColor
import io.legado.app.lib.theme.themeCardColorOrDefault
import io.legado.app.lib.theme.themeMutedColorOrDefault
import io.legado.app.lib.theme.uiTypeface
import io.legado.app.model.ReadBook
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.widget.compose.showComposeChoiceListDialog
import io.legado.app.ui.widget.compose.showComposeConfirmDialog
import io.legado.app.ui.widget.compose.showComposeTextInputDialog
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.readText
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showHelp
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ParagraphRuleManageActivity : BaseActivity<ActivityThemeManageBinding>(), ItemTouchCallback.Callback {

    override val binding by viewBinding(ActivityThemeManageBinding::inflate)
    private val adapter = RuleAdapter()
    private var bookUrl: String? = null
    private var book: Book? = null
    private var enabledIds: Set<Long> = emptySet()
    private var refreshOnResume = false

    private val importDoc = registerForActivityResult(HandleFileContract()) { result ->
        result.uri?.let { uri ->
            lifecycleScope.launch {
                kotlin.runCatching {
                    parseImportedRules(uri.readText(this@ParagraphRuleManageActivity))
                }.onSuccess { themes ->
                    if (themes.isEmpty()) {
                        toastOnUi(R.string.wrong_format)
                    } else {
                        withContext(Dispatchers.IO) { insertImportedThemes(themes) }
                        load()
                        toastOnUi(R.string.success)
                    }
                }.onFailure {
                    toastOnUi(it.localizedMessage ?: getString(R.string.wrong_format))
                }
            }
        }
    }

    private val exportRuleResult = registerForActivityResult(HandleFileContract()) { result ->
        result.uri?.let { uri ->
            val url = uri.toString()
            if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
                showComposeConfirmDialog(
                    title = getString(R.string.upload_url),
                    message = url,
                    positiveText = getString(R.string.copy_text),
                    negativeText = getString(R.string.cancel),
                    onPositive = {
                        sendToClip(url)
                        toastOnUi(R.string.copy_complete)
                    }
                )
            } else {
                toastOnUi(R.string.export_success)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        bookUrl = intent.getStringExtra("bookUrl") ?: ReadBook.book?.bookUrl
        book = bookUrl?.let { appDb.bookDao.getBook(it) }
        initView()
        load()
    }

    override fun onResume() {
        super.onResume()
        if (refreshOnResume) {
            refreshOnResume = false
            refreshReadBookIfNeeded()
        }
        load()
    }

    private fun initView() = binding.run {
        titleBar.title = getString(R.string.paragraph_rule_manage)
        tabBar.visibility = View.GONE
        btnAdd.text = getString(R.string.add)
        btnAdd.background = UiCorner.actionSelector(
            themeCardColorOrDefault(),
            themeMutedColorOrDefault(),
            UiCorner.actionRadius(this@ParagraphRuleManageActivity)
        )
        btnAdd.setOnClickListener { showAddActions() }
        tvSummary.applyUiLabelStyle(this@ParagraphRuleManageActivity)
        tvSummary.setTextColor(secondaryTextColor)
        recyclerView.layoutManager = LinearLayoutManager(this@ParagraphRuleManageActivity)
        recyclerView.adapter = adapter
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        ItemTouchHelper(ItemTouchCallback(this@ParagraphRuleManageActivity).apply {
            isCanDrag = true
        }).attachToRecyclerView(recyclerView)
        root.applyUiBodyTypefaceDeep(this@ParagraphRuleManageActivity.uiTypeface())
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_IMPORT, 0, R.string.import_str).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(0, MENU_HELP, 1, R.string.help).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_IMPORT -> showImportActions()
            MENU_HELP -> showHelp("paragraphRuleHelp")
        }
        return true
    }

    private fun showAddActions() {
        showComposeChoiceListDialog(
            title = getString(R.string.paragraph_rule_manage),
            labels = listOf(getString(R.string.add), getString(R.string.import_str), getString(R.string.import_on_line))
        ) { index ->
            when (index) {
                0 -> openEditRule()
                1 -> launchImportFile()
                2 -> showImportUrlDialog()
            }
        }
    }

    private fun showImportActions() {
        showComposeChoiceListDialog(
            title = getString(R.string.import_str),
            labels = listOf(getString(R.string.import_str), getString(R.string.import_on_line))
        ) { index ->
            when (index) {
                0 -> launchImportFile()
                1 -> showImportUrlDialog()
            }
        }
    }

    private fun launchImportFile() {
        importDoc.launch {
            mode = HandleFileContract.FILE
            title = getString(R.string.import_str)
            allowExtensions = arrayOf("json")
        }
    }

    private fun showImportUrlDialog() {
        showComposeTextInputDialog(
            title = getString(R.string.import_on_line),
            hint = "https://...",
            onPositive = { value ->
                val url = value.trim()
                if (url.isNotEmpty()) importRulesFromUrl(url)
            }
        )
    }

    private fun importRulesFromUrl(url: String) {
        lifecycleScope.launch {
            kotlin.runCatching {
                val text = withContext(Dispatchers.IO) {
                    okHttpClient.newCallResponseBody { url(url) }.use { it.string() }
                }
                parseImportedRules(text)
            }.onSuccess { themes ->
                if (themes.isEmpty()) {
                    toastOnUi(R.string.wrong_format)
                } else {
                    withContext(Dispatchers.IO) { insertImportedThemes(themes) }
                    load()
                    toastOnUi(R.string.success)
                }
            }.onFailure {
                toastOnUi(it.localizedMessage ?: getString(R.string.wrong_format))
            }
        }
    }

    private fun load() {
        lifecycleScope.launch {
            val rules = withContext(Dispatchers.IO) {
                bookUrl?.let { enabledIds = appDb.paragraphRuleDao.enabledRuleIdsForBook(it).toSet() }
                appDb.paragraphRuleDao.all()
            }
            adapter.items = rules
            binding.tvSummary.text = when {
                bookUrl == null -> getString(R.string.paragraph_rule_no_book_hint)
                rules.isEmpty() -> getString(R.string.paragraph_rule_empty)
                else -> getString(R.string.paragraph_rule_manage_summary)
            }
        }
    }

    private fun toggle(rule: ParagraphRule, enable: Boolean) {
        val url = bookUrl
        if (url == null) {
            toastOnUi(R.string.paragraph_rule_no_book_hint)
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (enable) {
                    appDb.paragraphRuleDao.insertBookRule(
                        BookParagraphRule(url, rule.id, true, adapter.items.indexOfFirst { it.id == rule.id })
                    )
                } else {
                    appDb.paragraphRuleDao.deleteBookRule(url, rule.id)
                }
            }
            refreshReadBookIfNeeded()
            load()
        }
    }

    private fun login(rule: ParagraphRule) {
        refreshOnResume = true
        startActivity<SourceLoginActivity> {
            putExtra("bookType", -1)
            putExtra("type", "paragraphRule")
            putExtra("key", rule.id.toString())
            bookUrl?.let { putExtra("bookUrl", it) }
            putExtra("chapterIndex", ReadBook.durChapterIndex)
        }
    }

    private fun openEditRule(ruleId: Long? = null) {
        refreshOnResume = true
        startActivity<ParagraphRuleEditActivity> {
            ruleId?.let { putExtra("id", it) }
        }
    }

    private fun showActions(rule: ParagraphRule) {
        val actions = listOf(
            Action.EDIT,
            Action.EXPORT,
            Action.COPY,
            Action.VARS,
            Action.DELETE
        )
        showComposeChoiceListDialog(rule.displayName(), actions.map { getString(it.titleRes) }) { index ->
            when (actions.getOrNull(index)) {
                Action.EDIT -> openEditRule(rule.id)
                Action.EXPORT -> exportRule(rule)
                Action.COPY -> sendToClip(serializeRule(rule))
                Action.VARS -> editVars(rule)
                Action.DELETE -> deleteRule(rule)
                null -> Unit
            }
        }
    }

    private fun editVars(rule: ParagraphRule) {
        lifecycleScope.launch {
            val raw = withContext(Dispatchers.IO) {
                GSON.toJson(appDb.paragraphRuleDao.vars(rule.id).associate { it.name to it.value })
            }
            showComposeTextInputDialog(
                title = getString(R.string.paragraph_rule_vars),
                hint = getString(R.string.paragraph_rule_vars_json),
                initialValue = raw,
                onPositive = { text -> saveVars(rule.id, text) }
            )
        }
    }

    private fun saveVars(ruleId: Long, raw: String) {
        lifecycleScope.launch {
            kotlin.runCatching {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val values: Map<String, String> = if (raw.isBlank()) emptyMap() else GSON.fromJson(raw, type)
                withContext(Dispatchers.IO) {
                    appDb.paragraphRuleDao.deleteVars(ruleId)
                    values.forEach { (key, value) ->
                        appDb.paragraphRuleDao.putVar(ParagraphRuleVar(ruleId, key, value))
                    }
                    io.legado.app.help.book.ParagraphRuleThemeRuntime.invalidate(ruleId)
                }
            }.onSuccess {
                toastOnUi(R.string.success)
                refreshReadBookIfNeeded()
            }.onFailure {
                toastOnUi(it.localizedMessage ?: getString(R.string.wrong_format))
            }
        }
    }

    private data class ImportedTheme(
        val rule: ParagraphRule,
        val vars: Map<String, String> = emptyMap(),
        val assets: Map<String, String> = emptyMap()
    )

    private fun parseImportedRules(raw: String): List<ImportedTheme> {
        val text = raw.trim()
        if (text.isBlank()) return emptyList()
        val root = JsonParser.parseString(text)
        if (root.isJsonObject && root.asJsonObject.get("format")?.asString == ParagraphRuleThemePackage.FORMAT) {
            val theme = GSON.fromJson(root, ParagraphRuleThemePackage::class.java)
                ?: throw IOException("主题包为空")
            if (theme.schemaVersion != ParagraphRuleThemePackage.SCHEMA_VERSION) {
                throw IOException("不支持的主题包版本：${theme.schemaVersion}")
            }
            validateTheme(theme)
            return listOf(ImportedTheme(theme.rule, theme.vars, theme.assets))
        }
        GSON.fromJsonArray<ParagraphRule>(text).getOrNull()?.let { rules ->
            return rules.map { ImportedTheme(it) }
        }
        GSON.fromJsonObject<ParagraphRule>(text).getOrNull()?.let {
            return listOf(ImportedTheme(it))
        }
        return emptyList()
    }

    private fun validateTheme(theme: ParagraphRuleThemePackage) {
        if (theme.rule.name.isBlank()) throw IOException("主题规则名称不能为空")
        if (theme.assets.size > MAX_THEME_ASSETS) throw IOException("主题资源过多")
        var total = 0L
        for ((name, encoded) in theme.assets) {
            validateAssetName(name)
            if (encoded.length > MAX_THEME_ASSET_BASE64) throw IOException("主题资源过大：$name")
            total += encoded.length.toLong()
            if (total > MAX_THEME_TOTAL_BASE64) throw IOException("主题资源总大小过大")
        }
        if (theme.vars.size > MAX_THEME_VARS) throw IOException("主题变量过多")
    }

    private fun validateAssetName(name: String): String {
        val clean = name.trim().replace('\\', '/')
        if (clean.isBlank() || clean.startsWith('/') || clean.contains(':') ||
            clean.split('/').any { it.isBlank() || it == "." || it == ".." }
        ) throw IOException("主题资源路径无效")
        return clean
    }

    private fun insertImportedThemes(themes: List<ImportedTheme>) {
        var order = (appDb.paragraphRuleDao.maxOrder() ?: 0) + 1
        themes.forEach { imported ->
            val rule = imported.rule.copy(id = 0L, order = order++, updateTime = System.currentTimeMillis())
            val ruleId = appDb.paragraphRuleDao.insert(rule)
            val resolvedVars = if (imported.assets.isEmpty()) {
                imported.vars
            } else {
                val assetDir = File(filesDir, "paragraphRuleThemes/$ruleId")
                assetDir.mkdirs()
                val assetPaths = decodeThemeAssets(assetDir, imported.assets)
                imported.vars.mapValues { (_, value) -> assetPaths[value] ?: value }
            }
            appDb.paragraphRuleDao.deleteVars(ruleId)
            resolvedVars.forEach { (name, value) ->
                appDb.paragraphRuleDao.putVar(ParagraphRuleVar(ruleId, name, value))
            }
        }
    }

    private fun decodeThemeAssets(dir: File, assets: Map<String, String>): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for ((rawName, encoded) in assets) {
            val name = validateAssetName(rawName)
            val data = try {
                Base64.decode(encoded, Base64.DEFAULT)
            } catch (error: IllegalArgumentException) {
                throw IOException("主题资源编码无效：$name", error)
            }
            if (data.size > MAX_THEME_ASSET_BYTES) throw IOException("主题资源过大：$name")
            val target = File(dir, name)
            val canonicalDir = dir.canonicalFile
            val canonicalTarget = target.canonicalFile
            if (canonicalTarget.parentFile?.canonicalFile?.let { it.path.startsWith(canonicalDir.path) } != true) {
                throw IOException("主题资源路径无效")
            }
            canonicalTarget.parentFile?.mkdirs()
            canonicalTarget.writeBytes(data)
            result[name] = canonicalTarget.absolutePath
        }
        return result
    }

    private fun exportRule(rule: ParagraphRule) {
        lifecycleScope.launch {
            val json = withContext(Dispatchers.IO) { serializeRule(rule) }
            exportRuleResult.launch {
                mode = HandleFileContract.EXPORT
                fileData = HandleFileContract.FileData(
                    "paragraphRule-${rule.displayName()}.json",
                    json.toByteArray(Charsets.UTF_8),
                    "application/json"
                )
            }
        }
    }

    private fun serializeRule(rule: ParagraphRule): String {
        val vars = appDb.paragraphRuleDao.vars(rule.id).associate { it.name to it.value }
        val assets = LinkedHashMap<String, String>()
        val exportedVars = vars.mapValues { (_, value) ->
            val source = localAssetSource(value) ?: return@mapValues value
            val bytes = source.readBytesOrNull() ?: return@mapValues value
            if (bytes.size > MAX_THEME_ASSET_BYTES || assets.values.sumOf { it.length.toLong() } > MAX_THEME_TOTAL_BASE64) {
                return@mapValues value
            }
            val key = "assets/${source.name.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
            assets.putIfAbsent(key, Base64.encodeToString(bytes, Base64.NO_WRAP))
            key
        }
        return if (assets.isEmpty()) {
            GSON.toJson(rule)
        } else {
            GSON.toJson(ParagraphRuleThemePackage(rule = rule.copy(id = 0L), vars = exportedVars, assets = assets))
        }
    }

    private fun localAssetSource(value: String): File? {
        val text = value.trim()
        val file = when {
            text.startsWith("file://", true) -> runCatching { File(android.net.Uri.parse(text).path.orEmpty()) }.getOrNull()
            text.startsWith("content://", true) -> return null
            else -> File(text)
        }
        return file?.takeIf { it.isFile && it.length() <= MAX_THEME_ASSET_BYTES }
    }

    private fun File.readBytesOrNull(): ByteArray? = runCatching { readBytes() }.getOrNull()

    private fun deleteRule(rule: ParagraphRule) {
        showComposeConfirmDialog(
            title = getString(R.string.draw),
            message = getString(R.string.sure_del) + "\n" + rule.displayName(),
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onPositive = {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { appDb.paragraphRuleDao.deleteWithRelations(rule) }
                    refreshReadBookIfNeeded()
                    load()
                }
            }
        )
    }

    override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
        if (srcPosition !in adapter.items.indices || targetPosition !in adapter.items.indices) return false
        val mutable = adapter.items.toMutableList()
        val item = mutable.removeAt(srcPosition)
        mutable.add(targetPosition, item)
        adapter.items = mutable
        adapter.notifyItemMoved(srcPosition, targetPosition)
        return true
    }

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                adapter.items.forEachIndexed { index, rule ->
                    if (rule.order != index) appDb.paragraphRuleDao.update(rule.copy(order = index, updateTime = System.currentTimeMillis()))
                    bookUrl?.let { url ->
                        if (enabledIds.contains(rule.id)) {
                            appDb.paragraphRuleDao.insertBookRule(BookParagraphRule(url, rule.id, true, index))
                        }
                    }
                }
            }
            refreshReadBookIfNeeded()
            load()
        }
    }

    private fun refreshReadBookIfNeeded() {
        if (bookUrl != ReadBook.book?.bookUrl) return
        ReadBook.invalidateParagraphRuleLayout()
        ReadBook.callBack?.upContent(resetPageOffset = false)
        ReadBook.loadContent(resetPageOffset = false)
    }

    private inner class RuleAdapter : RecyclerView.Adapter<RuleAdapter.Holder>() {
        var items: List<ParagraphRule> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        init { setHasStableIds(true) }

        override fun getItemId(position: Int): Long = items[position].id
        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(ItemThemePackageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(items[position])
        }

        inner class Holder(private val itemBinding: ItemThemePackageBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(rule: ParagraphRule) = itemBinding.run {
                val enabled = enabledIds.contains(rule.id)
                root.background = UiCorner.panelRounded(
                    this@ParagraphRuleManageActivity,
                    themeCardColorOrDefault(),
                    UiCorner.panelRadius(this@ParagraphRuleManageActivity)
                )
                cardPreview.visibility = View.GONE
                tvSource.visibility = View.GONE
                (layInfo.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                    it.marginStart = 0
                    layInfo.layoutParams = it
                }
                tvName.text = rule.displayName()
                tvInfo.text = getString(if (enabled) R.string.paragraph_rule_enabled_for_book else R.string.paragraph_rule_disabled_for_book)
                tvName.applyUiSectionTitleStyle(this@ParagraphRuleManageActivity)
                tvInfo.applyUiLabelStyle(this@ParagraphRuleManageActivity)
                tvInfo.setTextColor(secondaryTextColor)
                listOf(btnApply, btnEdit, btnMore).forEach {
                    it.background = UiCorner.actionSelector(
                        Color.TRANSPARENT,
                        themeMutedColorOrDefault(),
                        UiCorner.actionRadius(this@ParagraphRuleManageActivity)
                    )
                    it.typeface = this@ParagraphRuleManageActivity.uiTypeface()
                }
                btnApply.text = getString(if (enabled) R.string.disable else R.string.enable)
                btnEdit.text = getString(R.string.login)
                btnMore.text = getString(R.string.more)
                btnApply.setTextColor(accentColor)
                btnEdit.setTextColor(primaryTextColor)
                btnMore.setTextColor(primaryTextColor)
                btnApply.setOnClickListener { toggle(rule, !enabled) }
                btnEdit.setOnClickListener { login(rule) }
                btnMore.setOnClickListener { showActions(rule) }
                root.setOnClickListener { showActions(rule) }
            }
        }
    }

    private enum class Action(val titleRes: Int) {
        EDIT(R.string.edit),
        EXPORT(R.string.export_str),
        COPY(R.string.copy_rule),
        VARS(R.string.paragraph_rule_vars),
        DELETE(R.string.delete)
    }

    companion object {
        private const val MAX_THEME_ASSETS = 32
        private const val MAX_THEME_VARS = 256
        private const val MAX_THEME_ASSET_BYTES = 8 * 1024 * 1024
        private const val MAX_THEME_ASSET_BASE64 = MAX_THEME_ASSET_BYTES * 2
        private const val MAX_THEME_TOTAL_BASE64 = 24L * 1024L * 1024L
        private const val MENU_IMPORT = 1
        private const val MENU_HELP = 2
    }
}
