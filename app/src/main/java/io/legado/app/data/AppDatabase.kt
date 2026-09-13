package io.legado.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.util.Log
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.data.dao.AiAgentDao
import io.legado.app.data.dao.AiMemoryDao
import io.legado.app.data.dao.AiReadAloudRoleCacheDao
import io.legado.app.data.dao.AiGeneratedImageDao
import io.legado.app.data.dao.AiImageGroupDao
import io.legado.app.data.dao.AiReadAloudUsageRecordDao
import io.legado.app.data.dao.BookAiChapterSummaryDao
import io.legado.app.data.dao.BookChapterDao
import io.legado.app.data.dao.BookCharacterDao
import io.legado.app.data.dao.BookDao
import io.legado.app.data.dao.BookGroupDao
import io.legado.app.data.dao.BookSourceDao
import io.legado.app.data.dao.BookmarkDao
import io.legado.app.data.dao.CacheDao
import io.legado.app.data.dao.CookieDao
import io.legado.app.data.dao.DictRuleDao
import io.legado.app.data.dao.HttpTTSDao
import io.legado.app.data.dao.KeyboardAssistsDao
import io.legado.app.data.dao.ParagraphRuleDao
import io.legado.app.data.dao.ReadAloudBgmDao
import io.legado.app.data.dao.ReadAloudSpeakerGroupDao
import io.legado.app.data.dao.ReadMenuCustomButtonDao
import io.legado.app.data.dao.ReadRecentBookDao
import io.legado.app.data.dao.ReadRecordDao
import io.legado.app.data.dao.ReadRecordDailyDao
import io.legado.app.data.dao.ReplaceRuleDao
import io.legado.app.data.dao.RssArticleDao
import io.legado.app.data.dao.RssReadRecordDao
import io.legado.app.data.dao.RssSourceDao
import io.legado.app.data.dao.RssStarDao
import io.legado.app.data.dao.RuleSubDao
import io.legado.app.data.dao.SearchBookDao
import io.legado.app.data.dao.SearchKeywordDao
import io.legado.app.data.dao.ServerDao
import io.legado.app.data.dao.TxtTocRuleDao
import io.legado.app.data.entities.AiAgentJob
import io.legado.app.data.entities.AiAgentSession
import io.legado.app.data.entities.AiAgentTrace
import io.legado.app.data.entities.AiGeneratedImage
import io.legado.app.data.entities.AiImageGroup
import io.legado.app.data.entities.AiMemoryFragment
import io.legado.app.data.entities.AiMemoryFragmentFts
import io.legado.app.data.entities.AiMemoryItem
import io.legado.app.data.entities.AiMemoryItemFts
import io.legado.app.data.entities.AiReadAloudRoleCache
import io.legado.app.data.entities.AiReadAloudUsageRecord
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookAiChapterSummary
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookCharacter
import io.legado.app.data.entities.BookCharacterRelation
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookParagraphRule
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.Bookmark
import io.legado.app.data.entities.Cache
import io.legado.app.data.entities.Cookie
import io.legado.app.data.entities.DictRule
import io.legado.app.data.entities.HttpTTS
import io.legado.app.data.entities.KeyboardAssist
import io.legado.app.data.entities.ParagraphRule
import io.legado.app.data.entities.ParagraphRuleVar
import io.legado.app.data.entities.ReadAloudBgmAssignmentCache
import io.legado.app.data.entities.ReadAloudBgmGroup
import io.legado.app.data.entities.ReadAloudBgmTrack
import io.legado.app.data.entities.ReadAloudSpeakerGroup
import io.legado.app.data.entities.ReadAloudSpeakerGroupItem
import io.legado.app.data.entities.ReadMenuCustomButton
import io.legado.app.data.entities.ReadRecentBook
import io.legado.app.data.entities.ReadRecord
import io.legado.app.data.entities.ReadRecordDaily
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssReadRecord
import io.legado.app.data.entities.RssSource
import io.legado.app.data.entities.RssStar
import io.legado.app.data.entities.RuleSub
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.data.entities.Server
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.DefaultData
import org.intellij.lang.annotations.Language
import splitties.init.appCtx
import java.util.Locale

val appDb by lazy {
    Room.databaseBuilder(appCtx, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
        .fallbackToDestructiveMigrationFrom(false, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        .addMigrations(*DatabaseMigrations.migrations)
        .allowMainThreadQueries()
        .addCallback(AppDatabase.dbCallback)
        .build()
}

@Database(
    version = 111,
    exportSchema = true,
    entities = [Book::class, BookGroup::class, BookSource::class, BookChapter::class,
        ReplaceRule::class, SearchBook::class, SearchKeyword::class, Cookie::class,
        RssSource::class, Bookmark::class, RssArticle::class, RssReadRecord::class,
        RssStar::class, TxtTocRule::class, ReadRecord::class, ReadRecordDaily::class,
        HttpTTS::class, Cache::class,
        RuleSub::class, DictRule::class, KeyboardAssist::class, Server::class,
        ReadRecentBook::class, ParagraphRule::class, BookParagraphRule::class,
        ParagraphRuleVar::class, ReadMenuCustomButton::class,
        AiImageGroup::class, AiGeneratedImage::class,
        BookCharacter::class, BookCharacterRelation::class,
        BookAiChapterSummary::class, AiReadAloudRoleCache::class,
        ReadAloudBgmGroup::class, ReadAloudBgmTrack::class, ReadAloudBgmAssignmentCache::class,
        ReadAloudSpeakerGroup::class, ReadAloudSpeakerGroupItem::class,
        AiReadAloudUsageRecord::class,
        AiAgentSession::class, AiAgentJob::class, AiAgentTrace::class,
        AiMemoryItem::class, AiMemoryFragment::class, AiMemoryItemFts::class, AiMemoryFragmentFts::class],
    views = [BookSourcePart::class],
    autoMigrations = [
        AutoMigration(from = 43, to = 44),
        AutoMigration(from = 44, to = 45),
        AutoMigration(from = 45, to = 46),
        AutoMigration(from = 46, to = 47),
        AutoMigration(from = 47, to = 48),
        AutoMigration(from = 48, to = 49),
        AutoMigration(from = 49, to = 50),
        AutoMigration(from = 50, to = 51),
        AutoMigration(from = 51, to = 52),
        AutoMigration(from = 52, to = 53),
        AutoMigration(from = 53, to = 54),
        AutoMigration(from = 54, to = 55, spec = DatabaseMigrations.Migration_54_55::class),
        AutoMigration(from = 55, to = 56),
        AutoMigration(from = 56, to = 57),
        AutoMigration(from = 57, to = 58),
        AutoMigration(from = 58, to = 59),
        AutoMigration(from = 59, to = 60),
        AutoMigration(from = 60, to = 61),
        AutoMigration(from = 61, to = 62),
        AutoMigration(from = 62, to = 63),
        AutoMigration(from = 63, to = 64),
        AutoMigration(from = 64, to = 65, spec = DatabaseMigrations.Migration_64_65::class),
        AutoMigration(from = 65, to = 66),
        AutoMigration(from = 66, to = 67),
        AutoMigration(from = 67, to = 68),
        AutoMigration(from = 68, to = 69),
        AutoMigration(from = 69, to = 70),
        AutoMigration(from = 70, to = 71),
        AutoMigration(from = 71, to = 72),
        AutoMigration(from = 72, to = 73),
        AutoMigration(from = 73, to = 74),
        AutoMigration(from = 74, to = 75),
        AutoMigration(from = 75, to = 76),
        AutoMigration(from = 76, to = 77),
        AutoMigration(from = 77, to = 78),
        AutoMigration(from = 78, to = 79),
        AutoMigration(from = 79, to = 80),
        AutoMigration(from = 80, to = 81, spec = DatabaseMigrations.Migration_80_81::class),
        AutoMigration(from = 81, to = 82),
        AutoMigration(from = 82, to = 83),
        AutoMigration(from = 83, to = 84, spec = DatabaseMigrations.Migration_83_84::class),
        AutoMigration(from = 84, to = 85, spec = DatabaseMigrations.Migration_84_85::class),
        AutoMigration(from = 85, to = 86),
        AutoMigration(from = 86, to = 87),
        AutoMigration(from = 87, to = 88),
        AutoMigration(from = 88, to = 89),
        AutoMigration(from = 89, to = 90),
        AutoMigration(from = 92, to = 93)
    ]
)
abstract class AppDatabase : RoomDatabase() {

    abstract val bookDao: BookDao
    abstract val bookGroupDao: BookGroupDao
    abstract val bookSourceDao: BookSourceDao
    abstract val bookChapterDao: BookChapterDao
    abstract val bookCharacterDao: BookCharacterDao
    abstract val replaceRuleDao: ReplaceRuleDao
    abstract val searchBookDao: SearchBookDao
    abstract val searchKeywordDao: SearchKeywordDao
    abstract val rssSourceDao: RssSourceDao
    abstract val bookmarkDao: BookmarkDao
    abstract val rssArticleDao: RssArticleDao
    abstract val rssStarDao: RssStarDao
    abstract val rssReadRecordDao: RssReadRecordDao
    abstract val cookieDao: CookieDao
    abstract val txtTocRuleDao: TxtTocRuleDao
    abstract val readRecordDao: ReadRecordDao
    abstract val readRecordDailyDao: ReadRecordDailyDao
    abstract val readRecentBookDao: ReadRecentBookDao
    abstract val httpTTSDao: HttpTTSDao
    abstract val cacheDao: CacheDao
    abstract val ruleSubDao: RuleSubDao
    abstract val dictRuleDao: DictRuleDao
    abstract val keyboardAssistsDao: KeyboardAssistsDao
    abstract val serverDao: ServerDao
    abstract val paragraphRuleDao: ParagraphRuleDao
    abstract val readMenuCustomButtonDao: ReadMenuCustomButtonDao
    abstract val aiImageGroupDao: AiImageGroupDao
    abstract val aiGeneratedImageDao: AiGeneratedImageDao
    abstract val bookAiChapterSummaryDao: BookAiChapterSummaryDao
    abstract val aiReadAloudRoleCacheDao: AiReadAloudRoleCacheDao
    abstract val readAloudBgmDao: ReadAloudBgmDao
    abstract val readAloudSpeakerGroupDao: ReadAloudSpeakerGroupDao
    abstract val aiReadAloudUsageRecordDao: AiReadAloudUsageRecordDao
    abstract val aiAgentDao: AiAgentDao
    abstract val aiMemoryDao: AiMemoryDao

    companion object {

    const val DATABASE_NAME = "reader.db"

        const val BOOK_TABLE_NAME = "books"
        const val BOOK_SOURCE_TABLE_NAME = "book_sources"
        const val RSS_SOURCE_TABLE_NAME = "rssSources"
        private const val MaxBookTextFieldLength = 262_144
        private const val MaxBookUrlFieldLength = 32_768

        val dbCallback = object : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                // 只在 API 级别 23 (Marshmallow) 及以上版本尝试设置区域设置
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        Log.d("AppDatabaseCallback", "准备 设置 locale for API ${Build.VERSION.SDK_INT}...")
                        db.setLocale(Locale.CHINESE)
                        // 在 21 上报错，但无法拦截
                        Log.d("AppDatabaseCallback", "成功 设置 locale for API ${Build.VERSION.SDK_INT}.")
                    } catch (e: Exception) {
                        Log.e("AppDatabaseCallback", "错误 设置 locale in onCreate for API ${Build.VERSION.SDK_INT}", e)
                    }
                } else {
                    Log.i("AppDatabaseCallback", "跳过 setLocale for API ${Build.VERSION.SDK_INT} (below M).")
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                @Language("sql")
                val insertBookGroupAllSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdAll}, '全部', -10, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdAll})
                """.trimIndent()
                db.execSQL(insertBookGroupAllSql)
                @Language("sql")
                val insertBookGroupLocalSql = """
                    insert into book_groups(groupId, groupName, 'order', enableRefresh, show) 
                    select ${BookGroup.IdLocal}, '本地', -9, 0, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdLocal})
                """.trimIndent()
                db.execSQL(insertBookGroupLocalSql)
                @Language("sql")
                val insertBookGroupMusicSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdAudio}, '音频', -8, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdAudio})
                """.trimIndent()
                db.execSQL(insertBookGroupMusicSql)
                @Language("sql")
                val insertBookGroupNetNoneGroupSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdNetNone}, '网络未分组', -7, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdNetNone})
                """.trimIndent()
                db.execSQL(insertBookGroupNetNoneGroupSql)
                @Language("sql")
                val insertBookGroupLocalNoneGroupSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdLocalNone}, '本地未分组', -6, 0
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdLocalNone})
                """.trimIndent()
                db.execSQL(insertBookGroupLocalNoneGroupSql)
                @Language("sql")
                val insertBookGroupVideoSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdVideo}, '视频', -5, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdVideo})
                    """.trimIndent()
                db.execSQL(insertBookGroupVideoSql)
                @Language("sql")
                val insertBookGroupErrorSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdError}, '更新失败', -1, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdError})
                """.trimIndent()
                db.execSQL(insertBookGroupErrorSql)
                @Language("sql")
                val upBookSourceLoginUiSql =
                    "update book_sources set loginUi = null where loginUi = 'null'"
                db.execSQL(upBookSourceLoginUiSql)
                @Language("sql")
                val upRssSourceLoginUiSql =
                    "update rssSources set loginUi = null where loginUi = 'null'"
                db.execSQL(upRssSourceLoginUiSql)
                @Language("sql")
                val upHttpTtsLoginUiSql =
                    "update httpTTS set loginUi = null where loginUi = 'null'"
                db.execSQL(upHttpTtsLoginUiSql)
                @Language("sql")
                val upHttpTtsConcurrentRateSql =
                    "update httpTTS set concurrentRate = '0' where concurrentRate is null"
                db.execSQL(upHttpTtsConcurrentRateSql)
                trimOversizedBookStorageFields(db)
                db.query("select * from keyboardAssists order by serialNo").use {
                    if (it.count == 0) {
                        DefaultData.keyboardAssists.forEach { keyboardAssist ->
                            val contentValues = ContentValues().apply {
                                put("type", keyboardAssist.type)
                                put("key", keyboardAssist.key)
                                put("value", keyboardAssist.value)
                                put("serialNo", keyboardAssist.serialNo)
                            }
                            db.insert(
                                "keyboardAssists",
                                SQLiteDatabase.CONFLICT_REPLACE,
                                contentValues
                            )
                        }
                    }
                }
            }

            private fun trimOversizedBookStorageFields(db: SupportSQLiteDatabase) {
                trimOversizedBookTextField(db, "intro")
                trimOversizedBookTextField(db, "customIntro")
                trimOversizedBookTextField(db, "variable")
                trimOversizedBookUrlField(db, "coverUrl")
                trimOversizedBookUrlField(db, "customCoverUrl")
            }

            private fun trimOversizedBookTextField(db: SupportSQLiteDatabase, column: String) {
                @Language("sql")
                val sql = """
                    update books set $column = case
                        when lower(substr($column, 1, 8)) = '<useweb>'
                            then '<useweb>' || substr($column, 9, ${MaxBookTextFieldLength - 17}) || '</useweb>'
                        when lower(substr($column, 1, 9)) = '<usehtml>'
                            then '<usehtml>' || substr($column, 10, ${MaxBookTextFieldLength - 19}) || '</usehtml>'
                        when lower(substr($column, 1, 4)) = '<md>'
                            then '<md>' || substr($column, 5, ${MaxBookTextFieldLength - 9}) || '</md>'
                        else substr($column, 1, $MaxBookTextFieldLength)
                    end
                    where length($column) > $MaxBookTextFieldLength
                """.trimIndent()
                db.execSQL(sql)
            }

            private fun trimOversizedBookUrlField(db: SupportSQLiteDatabase, column: String) {
                @Language("sql")
                val sql = """
                    update books set $column = null
                    where length($column) > $MaxBookUrlFieldLength
                """.trimIndent()
                db.execSQL(sql)
            }
        }

    }

}
