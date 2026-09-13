package io.legado.app.data.entities

/** Import/export format for a paragraph-rule style package. Assets are base64 encoded. */
data class ParagraphRuleThemePackage(
    val format: String = FORMAT,
    val schemaVersion: Int = 1,
    val rule: ParagraphRule = ParagraphRule(),
    val vars: Map<String, String> = emptyMap(),
    val assets: Map<String, String> = emptyMap()
) {
    companion object {
        const val FORMAT = "legado.paragraph-rule-theme"
        const val SCHEMA_VERSION = 1
    }
}
