package io.legado.app.data

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookSourceType
import io.legado.app.constant.BookType

object DatabaseMigrations {

    val migrations: Array<Migration> by lazy {
        arrayOf(
            migration_10_11, migration_11_12, migration_12_13, migration_13_14,
            migration_14_15, migration_15_17, migration_17_18, migration_18_19,
            migration_19_20, migration_20_21, migration_21_22, migration_22_23,
            migration_23_24, migration_24_25, migration_25_26, migration_26_27,
            migration_27_28, migration_28_29, migration_29_30, migration_30_31,
            migration_31_32, migration_32_33, migration_33_34, migration_34_35,
            migration_35_36, migration_36_37, migration_37_38, migration_38_39,
            migration_39_40, migration_40_41, migration_41_42, migration_42_43,
            migration_90_91, migration_91_92, migration_93_94, migration_94_95,
            migration_95_96, migration_96_97, migration_97_98, migration_98_99,
            migration_99_100, migration_100_101, migration_101_102, migration_102_103,
            migration_103_104, migration_104_105, migration_105_106,
            migration_106_107, migration_107_108, migration_108_109, migration_109_110, migration_110_111,
        )
    }
    private val migration_109_110 = object : Migration(109, 110) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `highlight_rules`")
        }
    }

    private val migration_110_111 = object : Migration(110, 111) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `highlight_rules`")
        }
    }

    private val migration_108_109 = object : Migration(108, 109) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_durChapterTime` ON `books` (`durChapterTime`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_latestChapterTime` ON `books` (`latestChapterTime`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_order` ON `books` (`order`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_name` ON `books` (`name`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_author` ON `books` (`author`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_books_type` ON `books` (`type`)")
        }
    }

    private val migration_107_108 = object : Migration(107, 108) {
        override fun migrate(db: SupportSQLiteDatabase) {
            repairAiAgentAndMemoryTables(db)
        }
    }

    private val migration_106_107 = object : Migration(106, 107) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val characterColumns = columnNames(db, "book_characters")
            if ("gender" !in characterColumns) {
                db.execSQL("ALTER TABLE `book_characters` ADD COLUMN `gender` TEXT NOT NULL DEFAULT ''")
            }
        }
    }

    private fun repairAiAgentAndMemoryTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ai_agent_sessions` (
                `sessionId` TEXT NOT NULL,
                `scope` TEXT NOT NULL DEFAULT '',
                `status` TEXT NOT NULL DEFAULT '',
                `currentGoal` TEXT NOT NULL DEFAULT '',
                `currentTask` TEXT NOT NULL DEFAULT '',
                `currentStep` TEXT NOT NULL DEFAULT '',
                `contextJson` TEXT NOT NULL DEFAULT '',
                `pendingConfirmationsJson` TEXT NOT NULL DEFAULT '',
                `retryStateJson` TEXT NOT NULL DEFAULT '',
                `lastError` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`sessionId`)
            )
            """.trimIndent()
        )
        ensureColumns(db, "ai_agent_sessions", aiAgentSessionColumns)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_agent_sessions_scope_updatedAt` ON `ai_agent_sessions` (`scope`, `updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_agent_sessions_status_updatedAt` ON `ai_agent_sessions` (`status`, `updatedAt`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ai_agent_jobs` (
                `jobId` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL DEFAULT '',
                `type` TEXT NOT NULL DEFAULT '',
                `status` TEXT NOT NULL DEFAULT '',
                `inputJson` TEXT NOT NULL DEFAULT '',
                `checkpointJson` TEXT NOT NULL DEFAULT '',
                `outputJson` TEXT NOT NULL DEFAULT '',
                `error` TEXT NOT NULL DEFAULT '',
                `retryCount` INTEGER NOT NULL DEFAULT 0,
                `maxRetry` INTEGER NOT NULL DEFAULT 2,
                `nextRunAt` INTEGER NOT NULL DEFAULT 0,
                `leaseUntil` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`jobId`)
            )
            """.trimIndent()
        )
        ensureColumns(db, "ai_agent_jobs", aiAgentJobColumns)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_agent_jobs_sessionId_createdAt` ON `ai_agent_jobs` (`sessionId`, `createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_agent_jobs_status_updatedAt` ON `ai_agent_jobs` (`status`, `updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_agent_jobs_type_updatedAt` ON `ai_agent_jobs` (`type`, `updatedAt`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ai_agent_traces` (
                `traceId` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL DEFAULT '',
                `jobId` TEXT NOT NULL DEFAULT '',
                `round` INTEGER NOT NULL DEFAULT 0,
                `eventType` TEXT NOT NULL DEFAULT '',
                `payloadJson` TEXT NOT NULL DEFAULT '',
                `usageJson` TEXT NOT NULL DEFAULT '',
                `success` INTEGER NOT NULL DEFAULT 1,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`traceId`)
            )
            """.trimIndent()
        )
        ensureColumns(db, "ai_agent_traces", aiAgentTraceColumns)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_agent_traces_jobId_round_createdAt` ON `ai_agent_traces` (`jobId`, `round`, `createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_agent_traces_sessionId_createdAt` ON `ai_agent_traces` (`sessionId`, `createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_agent_traces_eventType_createdAt` ON `ai_agent_traces` (`eventType`, `createdAt`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ai_memory_items` (
                `memoryId` TEXT NOT NULL,
                `scope` TEXT NOT NULL DEFAULT '',
                `bookKey` TEXT NOT NULL DEFAULT '',
                `sessionId` TEXT NOT NULL DEFAULT '',
                `type` TEXT NOT NULL DEFAULT '',
                `subject` TEXT NOT NULL DEFAULT '',
                `predicate` TEXT NOT NULL DEFAULT '',
                `objectValue` TEXT NOT NULL DEFAULT '',
                `content` TEXT NOT NULL DEFAULT '',
                `confidence` INTEGER NOT NULL DEFAULT 50,
                `importance` INTEGER NOT NULL DEFAULT 50,
                `sourceIds` TEXT NOT NULL DEFAULT '',
                `sourceChapterIndex` INTEGER NOT NULL DEFAULT -1,
                `fingerprint` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                `lastUsedAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`memoryId`)
            )
            """.trimIndent()
        )
        ensureColumns(db, "ai_memory_items", aiMemoryItemColumns)
        db.execSQL(
            """
            UPDATE `ai_memory_items`
            SET `fingerprint` = 'legacy-memory-' || rowid
            WHERE `fingerprint` = ''
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memory_items_scope_updatedAt` ON `ai_memory_items` (`scope`, `updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memory_items_bookKey_updatedAt` ON `ai_memory_items` (`bookKey`, `updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memory_items_sessionId_updatedAt` ON `ai_memory_items` (`sessionId`, `updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memory_items_type_updatedAt` ON `ai_memory_items` (`type`, `updatedAt`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ai_memory_items_fingerprint` ON `ai_memory_items` (`fingerprint`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ai_memory_fragments` (
                `fragmentId` TEXT NOT NULL,
                `scope` TEXT NOT NULL DEFAULT '',
                `bookKey` TEXT NOT NULL DEFAULT '',
                `sessionId` TEXT NOT NULL DEFAULT '',
                `sourceType` TEXT NOT NULL DEFAULT '',
                `title` TEXT NOT NULL DEFAULT '',
                `content` TEXT NOT NULL DEFAULT '',
                `chapterIndex` INTEGER NOT NULL DEFAULT -1,
                `chapterTitle` TEXT NOT NULL DEFAULT '',
                `paragraphStart` INTEGER NOT NULL DEFAULT -1,
                `paragraphEnd` INTEGER NOT NULL DEFAULT -1,
                `contentHash` TEXT NOT NULL DEFAULT '',
                `importance` INTEGER NOT NULL DEFAULT 50,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                `lastUsedAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`fragmentId`)
            )
            """.trimIndent()
        )
        ensureColumns(db, "ai_memory_fragments", aiMemoryFragmentColumns)
        db.execSQL(
            """
            UPDATE `ai_memory_fragments`
            SET `contentHash` = 'legacy-fragment-' || rowid
            WHERE `contentHash` = ''
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memory_fragments_scope_updatedAt` ON `ai_memory_fragments` (`scope`, `updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memory_fragments_bookKey_chapterIndex` ON `ai_memory_fragments` (`bookKey`, `chapterIndex`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_memory_fragments_sessionId_updatedAt` ON `ai_memory_fragments` (`sessionId`, `updatedAt`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ai_memory_fragments_contentHash` ON `ai_memory_fragments` (`contentHash`)")
        db.execSQL("DROP TABLE IF EXISTS `ai_memory_items_fts`")
        db.execSQL("DROP TABLE IF EXISTS `ai_memory_fragments_fts`")
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS `ai_memory_items_fts`
            USING FTS4(`memoryId` TEXT NOT NULL, `subject` TEXT NOT NULL, `predicate` TEXT NOT NULL, `objectValue` TEXT NOT NULL, `content` TEXT NOT NULL)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS `ai_memory_fragments_fts`
            USING FTS4(`fragmentId` TEXT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `chapterTitle` TEXT NOT NULL)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `ai_memory_items_fts` (`memoryId`, `subject`, `predicate`, `objectValue`, `content`)
            SELECT `memoryId`, `subject`, `predicate`, `objectValue`, `content`
            FROM `ai_memory_items`
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `ai_memory_fragments_fts` (`fragmentId`, `title`, `content`, `chapterTitle`)
            SELECT `fragmentId`, `title`, `content`, `chapterTitle`
            FROM `ai_memory_fragments`
            """.trimIndent()
        )
    }

    private fun ensureColumns(
        db: SupportSQLiteDatabase,
        tableName: String,
        columns: List<Pair<String, String>>
    ) {
        val existingColumns = columnNames(db, tableName)
        columns.forEach { (name, definition) ->
            if (name !in existingColumns) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$name` $definition")
            }
        }
    }

    private val aiAgentSessionColumns = listOf(
        "sessionId" to "TEXT NOT NULL DEFAULT ''",
        "scope" to "TEXT NOT NULL DEFAULT ''",
        "status" to "TEXT NOT NULL DEFAULT ''",
        "currentGoal" to "TEXT NOT NULL DEFAULT ''",
        "currentTask" to "TEXT NOT NULL DEFAULT ''",
        "currentStep" to "TEXT NOT NULL DEFAULT ''",
        "contextJson" to "TEXT NOT NULL DEFAULT ''",
        "pendingConfirmationsJson" to "TEXT NOT NULL DEFAULT ''",
        "retryStateJson" to "TEXT NOT NULL DEFAULT ''",
        "lastError" to "TEXT NOT NULL DEFAULT ''",
        "createdAt" to "INTEGER NOT NULL DEFAULT 0",
        "updatedAt" to "INTEGER NOT NULL DEFAULT 0"
    )

    private val aiAgentJobColumns = listOf(
        "jobId" to "TEXT NOT NULL DEFAULT ''",
        "sessionId" to "TEXT NOT NULL DEFAULT ''",
        "type" to "TEXT NOT NULL DEFAULT ''",
        "status" to "TEXT NOT NULL DEFAULT ''",
        "inputJson" to "TEXT NOT NULL DEFAULT ''",
        "checkpointJson" to "TEXT NOT NULL DEFAULT ''",
        "outputJson" to "TEXT NOT NULL DEFAULT ''",
        "error" to "TEXT NOT NULL DEFAULT ''",
        "retryCount" to "INTEGER NOT NULL DEFAULT 0",
        "maxRetry" to "INTEGER NOT NULL DEFAULT 2",
        "nextRunAt" to "INTEGER NOT NULL DEFAULT 0",
        "leaseUntil" to "INTEGER NOT NULL DEFAULT 0",
        "createdAt" to "INTEGER NOT NULL DEFAULT 0",
        "updatedAt" to "INTEGER NOT NULL DEFAULT 0"
    )

    private val aiAgentTraceColumns = listOf(
        "traceId" to "TEXT NOT NULL DEFAULT ''",
        "sessionId" to "TEXT NOT NULL DEFAULT ''",
        "jobId" to "TEXT NOT NULL DEFAULT ''",
        "round" to "INTEGER NOT NULL DEFAULT 0",
        "eventType" to "TEXT NOT NULL DEFAULT ''",
        "payloadJson" to "TEXT NOT NULL DEFAULT ''",
        "usageJson" to "TEXT NOT NULL DEFAULT ''",
        "success" to "INTEGER NOT NULL DEFAULT 1",
        "createdAt" to "INTEGER NOT NULL DEFAULT 0"
    )

    private val aiMemoryItemColumns = listOf(
        "memoryId" to "TEXT NOT NULL DEFAULT ''",
        "scope" to "TEXT NOT NULL DEFAULT ''",
        "bookKey" to "TEXT NOT NULL DEFAULT ''",
        "sessionId" to "TEXT NOT NULL DEFAULT ''",
        "type" to "TEXT NOT NULL DEFAULT ''",
        "subject" to "TEXT NOT NULL DEFAULT ''",
        "predicate" to "TEXT NOT NULL DEFAULT ''",
        "objectValue" to "TEXT NOT NULL DEFAULT ''",
        "content" to "TEXT NOT NULL DEFAULT ''",
        "confidence" to "INTEGER NOT NULL DEFAULT 50",
        "importance" to "INTEGER NOT NULL DEFAULT 50",
        "sourceIds" to "TEXT NOT NULL DEFAULT ''",
        "sourceChapterIndex" to "INTEGER NOT NULL DEFAULT -1",
        "fingerprint" to "TEXT NOT NULL DEFAULT ''",
        "createdAt" to "INTEGER NOT NULL DEFAULT 0",
        "updatedAt" to "INTEGER NOT NULL DEFAULT 0",
        "lastUsedAt" to "INTEGER NOT NULL DEFAULT 0"
    )

    private val aiMemoryFragmentColumns = listOf(
        "fragmentId" to "TEXT NOT NULL DEFAULT ''",
        "scope" to "TEXT NOT NULL DEFAULT ''",
        "bookKey" to "TEXT NOT NULL DEFAULT ''",
        "sessionId" to "TEXT NOT NULL DEFAULT ''",
        "sourceType" to "TEXT NOT NULL DEFAULT ''",
        "title" to "TEXT NOT NULL DEFAULT ''",
        "content" to "TEXT NOT NULL DEFAULT ''",
        "chapterIndex" to "INTEGER NOT NULL DEFAULT -1",
        "chapterTitle" to "TEXT NOT NULL DEFAULT ''",
        "paragraphStart" to "INTEGER NOT NULL DEFAULT -1",
        "paragraphEnd" to "INTEGER NOT NULL DEFAULT -1",
        "contentHash" to "TEXT NOT NULL DEFAULT ''",
        "importance" to "INTEGER NOT NULL DEFAULT 50",
        "createdAt" to "INTEGER NOT NULL DEFAULT 0",
        "updatedAt" to "INTEGER NOT NULL DEFAULT 0",
        "lastUsedAt" to "INTEGER NOT NULL DEFAULT 0"
    )

    private val migration_105_106 = object : Migration(105, 106) {
        override fun migrate(db: SupportSQLiteDatabase) {
            repairReadAloudAudioTables(db)
            splitReadAloudAudioGroupsByType(db)
            repairReadAloudSpeakerGroupTables(db)
        }
    }

    private val migration_104_105 = object : Migration(104, 105) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `httpTTS` ADD COLUMN `synthesisThreadCount` INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val migration_103_104 = object : Migration(103, 104) {
        override fun migrate(db: SupportSQLiteDatabase) {
            repairReadAloudAudioTables(db)
            repairAiReadAloudUsageTable(db)
        }
    }

    private val migration_102_103 = object : Migration(102, 103) {
        override fun migrate(db: SupportSQLiteDatabase) {
            repairReadAloudAudioTables(db)
        }
    }

    private fun repairReadAloudAudioTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `read_aloud_bgm_groups` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL DEFAULT '',
                `assetType` TEXT NOT NULL DEFAULT 'bgm',
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        val groupColumns = columnNames(db, "read_aloud_bgm_groups")
        if ("assetType" !in groupColumns) {
            db.execSQL("ALTER TABLE `read_aloud_bgm_groups` ADD COLUMN `assetType` TEXT NOT NULL DEFAULT 'bgm'")
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_groups_sortOrder_id` ON `read_aloud_bgm_groups` (`sortOrder`, `id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_groups_assetType_sortOrder_id` ON `read_aloud_bgm_groups` (`assetType`, `sortOrder`, `id`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `read_aloud_bgm_tracks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `groupId` INTEGER NOT NULL DEFAULT 0,
                `assetType` TEXT NOT NULL DEFAULT 'bgm',
                `name` TEXT NOT NULL DEFAULT '',
                `fileName` TEXT NOT NULL DEFAULT '',
                `filePath` TEXT NOT NULL DEFAULT '',
                `tags` TEXT NOT NULL DEFAULT '',
                `checksum` TEXT NOT NULL DEFAULT '',
                `durationMs` INTEGER NOT NULL DEFAULT 0,
                `defaultVolume` REAL NOT NULL DEFAULT 1.0,
                `enabled` INTEGER NOT NULL DEFAULT 1,
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        val trackColumns = columnNames(db, "read_aloud_bgm_tracks")
        if ("assetType" !in trackColumns) {
            db.execSQL("ALTER TABLE `read_aloud_bgm_tracks` ADD COLUMN `assetType` TEXT NOT NULL DEFAULT 'bgm'")
        }
        if ("defaultVolume" !in trackColumns) {
            db.execSQL("ALTER TABLE `read_aloud_bgm_tracks` ADD COLUMN `defaultVolume` REAL NOT NULL DEFAULT 1.0")
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_tracks_groupId_sortOrder_id` ON `read_aloud_bgm_tracks` (`groupId`, `sortOrder`, `id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_tracks_assetType_enabled_groupId_sortOrder_id` ON `read_aloud_bgm_tracks` (`assetType`, `enabled`, `groupId`, `sortOrder`, `id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_tracks_checksum` ON `read_aloud_bgm_tracks` (`checksum`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_tracks_enabled` ON `read_aloud_bgm_tracks` (`enabled`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `read_aloud_bgm_assignment_caches` (
                `cacheKey` TEXT NOT NULL,
                `bookUrl` TEXT NOT NULL DEFAULT '',
                `chapterKey` TEXT NOT NULL DEFAULT '',
                `chapterIndex` INTEGER NOT NULL DEFAULT 0,
                `chapterTitle` TEXT NOT NULL DEFAULT '',
                `contentHash` TEXT NOT NULL DEFAULT '',
                `modelId` TEXT NOT NULL DEFAULT '',
                `catalogHash` TEXT NOT NULL DEFAULT '',
                `assignmentsJson` TEXT NOT NULL DEFAULT '',
                `status` TEXT NOT NULL DEFAULT 'success',
                `lastError` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`cacheKey`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_assignment_caches_bookUrl_chapterIndex` ON `read_aloud_bgm_assignment_caches` (`bookUrl`, `chapterIndex`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_assignment_caches_bookUrl_contentHash` ON `read_aloud_bgm_assignment_caches` (`bookUrl`, `contentHash`)")
    }

    private fun splitReadAloudAudioGroupsByType(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "read_aloud_bgm_groups") || !tableExists(db, "read_aloud_bgm_tracks")) return
        db.query(
            """
            SELECT `id`, `name`, `sortOrder`, `createdAt`, `updatedAt`
            FROM `read_aloud_bgm_groups`
            ORDER BY `id` ASC
            """.trimIndent()
        ).use { cursor ->
            val groups = mutableListOf<Array<Any>>()
            while (cursor.moveToNext()) {
                groups += arrayOf(
                    cursor.getLong(0),
                    cursor.getString(1).orEmpty(),
                    cursor.getInt(2),
                    cursor.getLong(3),
                    cursor.getLong(4)
                )
            }
            groups.forEach { row ->
                val id = row[0] as Long
                val name = row[1] as String
                val sortOrder = row[2] as Int
                val createdAt = row[3] as Long
                val updatedAt = row[4] as Long
                val hasBgm = hasAudioTracks(db, id, "bgm")
                val hasSfx = hasAudioTracks(db, id, "sfx")
                when {
                    hasBgm && hasSfx -> {
                        db.execSQL(
                            """
                            INSERT INTO `read_aloud_bgm_groups`
                            (`name`, `assetType`, `sortOrder`, `createdAt`, `updatedAt`)
                            VALUES (?, 'sfx', ?, ?, ?)
                            """.trimIndent(),
                            arrayOf<Any>(name, sortOrder, createdAt, updatedAt)
                        )
                        val newId = lastInsertRowId(db)
                        if (newId > 0L) {
                            db.execSQL(
                                "UPDATE `read_aloud_bgm_tracks` SET `groupId` = ? WHERE `groupId` = ? AND `assetType` = 'sfx'",
                                arrayOf<Any>(newId, id)
                            )
                        }
                        db.execSQL("UPDATE `read_aloud_bgm_groups` SET `assetType` = 'bgm' WHERE `id` = ?", arrayOf<Any>(id))
                    }
                    hasSfx -> db.execSQL("UPDATE `read_aloud_bgm_groups` SET `assetType` = 'sfx' WHERE `id` = ?", arrayOf<Any>(id))
                    else -> db.execSQL("UPDATE `read_aloud_bgm_groups` SET `assetType` = 'bgm' WHERE `id` = ?", arrayOf<Any>(id))
                }
            }
        }
    }

    private fun hasAudioTracks(db: SupportSQLiteDatabase, groupId: Long, assetType: String): Boolean {
        db.query(
            "SELECT `id` FROM `read_aloud_bgm_tracks` WHERE `groupId` = ? AND `assetType` = ? LIMIT 1",
            arrayOf<Any>(groupId, assetType)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun lastInsertRowId(db: SupportSQLiteDatabase): Long {
        db.query("SELECT last_insert_rowid()").use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    private fun repairReadAloudSpeakerGroupTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `read_aloud_speaker_groups` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL DEFAULT '',
                `enabled` INTEGER NOT NULL DEFAULT 1,
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_speaker_groups_enabled_sortOrder_id` ON `read_aloud_speaker_groups` (`enabled`, `sortOrder`, `id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_speaker_groups_sortOrder_id` ON `read_aloud_speaker_groups` (`sortOrder`, `id`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `read_aloud_speaker_group_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `groupId` INTEGER NOT NULL DEFAULT 0,
                `engineType` TEXT NOT NULL DEFAULT '',
                `engineValue` TEXT NOT NULL DEFAULT '',
                `engineName` TEXT NOT NULL DEFAULT '',
                `speakerName` TEXT NOT NULL DEFAULT '',
                `toneID` TEXT NOT NULL DEFAULT '',
                `sourceGroupId` TEXT NOT NULL DEFAULT '',
                `sourceGroupName` TEXT NOT NULL DEFAULT '',
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL DEFAULT 0,
                `updatedAt` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_speaker_group_items_groupId_sortOrder_id` ON `read_aloud_speaker_group_items` (`groupId`, `sortOrder`, `id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_speaker_group_items_engineType_engineValue_toneID` ON `read_aloud_speaker_group_items` (`engineType`, `engineValue`, `toneID`)")
    }

    private fun repairAiReadAloudUsageTable(db: SupportSQLiteDatabase) {
        val tempTable = "ai_read_aloud_usage_records_migration_tmp"
        val hadOldTable = tableExists(db, "ai_read_aloud_usage_records")
        if (hadOldTable) {
            db.execSQL("DROP TABLE IF EXISTS `$tempTable`")
            db.execSQL("ALTER TABLE `ai_read_aloud_usage_records` RENAME TO `$tempTable`")
        }
        createAiReadAloudUsageTable(db, "ai_read_aloud_usage_records")
        if (hadOldTable) {
            copyAiReadAloudUsageRows(db, tempTable)
            db.execSQL("DROP TABLE IF EXISTS `$tempTable`")
        }
        createAiReadAloudUsageIndexes(db)
    }

    private fun createAiReadAloudUsageTable(db: SupportSQLiteDatabase, tableName: String) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `$tableName` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL DEFAULT '',
                `status` TEXT NOT NULL DEFAULT '',
                `bookUrl` TEXT NOT NULL DEFAULT '',
                `bookName` TEXT NOT NULL DEFAULT '',
                `chapterTitle` TEXT NOT NULL DEFAULT '',
                `chapterIndex` INTEGER NOT NULL DEFAULT 0,
                `cacheKey` TEXT NOT NULL DEFAULT '',
                `batchName` TEXT NOT NULL DEFAULT '',
                `providerName` TEXT NOT NULL DEFAULT '',
                `modelId` TEXT NOT NULL DEFAULT '',
                `elapsedMillis` INTEGER NOT NULL DEFAULT 0,
                `requestCount` INTEGER NOT NULL DEFAULT 0,
                `inputTokens` INTEGER NOT NULL DEFAULT 0,
                `cachedInputTokens` INTEGER NOT NULL DEFAULT 0,
                `outputTokens` INTEGER NOT NULL DEFAULT 0,
                `totalTokens` INTEGER NOT NULL DEFAULT 0,
                `summary` TEXT NOT NULL DEFAULT '',
                `error` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    private fun copyAiReadAloudUsageRows(db: SupportSQLiteDatabase, oldTableName: String) {
        val oldColumns = columnNames(db, oldTableName)
        val copyColumns = aiReadAloudUsageColumns.filter { it in oldColumns }
        if (copyColumns.isEmpty()) return
        val columnsSql = copyColumns.joinToString(", ") { "`$it`" }
        db.execSQL(
            """
            INSERT INTO `ai_read_aloud_usage_records` ($columnsSql)
            SELECT $columnsSql FROM `$oldTableName`
            """.trimIndent()
        )
    }

    private fun createAiReadAloudUsageIndexes(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_read_aloud_usage_records_type_createdAt` ON `ai_read_aloud_usage_records` (`type`, `createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_read_aloud_usage_records_bookUrl_chapterIndex` ON `ai_read_aloud_usage_records` (`bookUrl`, `chapterIndex`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_read_aloud_usage_records_cacheKey` ON `ai_read_aloud_usage_records` (`cacheKey`)")
    }

    private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName' LIMIT 1").use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun columnNames(db: SupportSQLiteDatabase, tableName: String): Set<String> {
        val columns = linkedSetOf<String>()
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex >= 0) {
                    columns += cursor.getString(nameIndex)
                }
            }
        }
        return columns
    }

    private val aiReadAloudUsageColumns = listOf(
        "id",
        "type",
        "status",
        "bookUrl",
        "bookName",
        "chapterTitle",
        "chapterIndex",
        "cacheKey",
        "batchName",
        "providerName",
        "modelId",
        "elapsedMillis",
        "requestCount",
        "inputTokens",
        "cachedInputTokens",
        "outputTokens",
        "totalTokens",
        "summary",
        "error",
        "createdAt"
    )

    private val migration_101_102 = object : Migration(101, 102) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_read_aloud_usage_records` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `type` TEXT NOT NULL DEFAULT '',
                    `status` TEXT NOT NULL DEFAULT '',
                    `bookUrl` TEXT NOT NULL DEFAULT '',
                    `bookName` TEXT NOT NULL DEFAULT '',
                    `chapterTitle` TEXT NOT NULL DEFAULT '',
                    `chapterIndex` INTEGER NOT NULL DEFAULT 0,
                    `cacheKey` TEXT NOT NULL DEFAULT '',
                    `batchName` TEXT NOT NULL DEFAULT '',
                    `providerName` TEXT NOT NULL DEFAULT '',
                    `modelId` TEXT NOT NULL DEFAULT '',
                    `elapsedMillis` INTEGER NOT NULL DEFAULT 0,
                    `requestCount` INTEGER NOT NULL DEFAULT 0,
                    `inputTokens` INTEGER NOT NULL DEFAULT 0,
                    `cachedInputTokens` INTEGER NOT NULL DEFAULT 0,
                    `outputTokens` INTEGER NOT NULL DEFAULT 0,
                    `totalTokens` INTEGER NOT NULL DEFAULT 0,
                    `summary` TEXT NOT NULL DEFAULT '',
                    `error` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_read_aloud_usage_records_type_createdAt` ON `ai_read_aloud_usage_records` (`type`, `createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_read_aloud_usage_records_bookUrl_chapterIndex` ON `ai_read_aloud_usage_records` (`bookUrl`, `chapterIndex`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_read_aloud_usage_records_cacheKey` ON `ai_read_aloud_usage_records` (`cacheKey`)")
        }
    }

    private val migration_100_101 = object : Migration(100, 101) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `read_aloud_bgm_groups` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL DEFAULT '',
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_groups_sortOrder_id` ON `read_aloud_bgm_groups` (`sortOrder`, `id`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `read_aloud_bgm_tracks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `groupId` INTEGER NOT NULL DEFAULT 0,
                    `name` TEXT NOT NULL DEFAULT '',
                    `fileName` TEXT NOT NULL DEFAULT '',
                    `filePath` TEXT NOT NULL DEFAULT '',
                    `tags` TEXT NOT NULL DEFAULT '',
                    `checksum` TEXT NOT NULL DEFAULT '',
                    `durationMs` INTEGER NOT NULL DEFAULT 0,
                    `enabled` INTEGER NOT NULL DEFAULT 1,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_tracks_groupId_sortOrder_id` ON `read_aloud_bgm_tracks` (`groupId`, `sortOrder`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_tracks_checksum` ON `read_aloud_bgm_tracks` (`checksum`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_tracks_enabled` ON `read_aloud_bgm_tracks` (`enabled`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `read_aloud_bgm_assignment_caches` (
                    `cacheKey` TEXT NOT NULL,
                    `bookUrl` TEXT NOT NULL DEFAULT '',
                    `chapterKey` TEXT NOT NULL DEFAULT '',
                    `chapterIndex` INTEGER NOT NULL DEFAULT 0,
                    `chapterTitle` TEXT NOT NULL DEFAULT '',
                    `contentHash` TEXT NOT NULL DEFAULT '',
                    `modelId` TEXT NOT NULL DEFAULT '',
                    `catalogHash` TEXT NOT NULL DEFAULT '',
                    `assignmentsJson` TEXT NOT NULL DEFAULT '',
                    `status` TEXT NOT NULL DEFAULT 'success',
                    `lastError` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`cacheKey`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_assignment_caches_bookUrl_chapterIndex` ON `read_aloud_bgm_assignment_caches` (`bookUrl`, `chapterIndex`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_aloud_bgm_assignment_caches_bookUrl_contentHash` ON `read_aloud_bgm_assignment_caches` (`bookUrl`, `contentHash`)")
        }
    }

    private val migration_99_100 = object : Migration(99, 100) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `httpTTS` ADD COLUMN `speakersJson` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `httpTTS` ADD COLUMN `emotionsJson` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `book_characters` ADD COLUMN `speechRouteJson` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `book_characters` ADD COLUMN `autoCreated` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `book_characters` ADD COLUMN `source` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `book_characters` ADD COLUMN `lastDetectedAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `ai_read_aloud_role_caches` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'success'")
            db.execSQL("ALTER TABLE `ai_read_aloud_role_caches` ADD COLUMN `retryCount` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `ai_read_aloud_role_caches` ADD COLUMN `lastError` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_read_aloud_role_caches` ADD COLUMN `createdCharacterIdsJson` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_read_aloud_role_caches` ADD COLUMN `characterHash` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_read_aloud_role_caches` ADD COLUMN `voiceHash` TEXT NOT NULL DEFAULT ''")
        }
    }

    private val migration_98_99 = object : Migration(98, 99) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `book_ai_chapter_summaries` (
                    `cacheKey` TEXT NOT NULL,
                    `bookUrl` TEXT NOT NULL DEFAULT '',
                    `bookName` TEXT NOT NULL DEFAULT '',
                    `chapterIndex` INTEGER NOT NULL DEFAULT 0,
                    `chapterKey` TEXT NOT NULL DEFAULT '',
                    `chapterTitle` TEXT NOT NULL DEFAULT '',
                    `contentHash` TEXT NOT NULL DEFAULT '',
                    `modelId` TEXT NOT NULL DEFAULT '',
                    `modelName` TEXT NOT NULL DEFAULT '',
                    `summary` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`cacheKey`)
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_ai_chapter_summaries_bookUrl_chapterIndex` ON `book_ai_chapter_summaries` (`bookUrl`, `chapterIndex`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_ai_chapter_summaries_bookUrl_contentHash` ON `book_ai_chapter_summaries` (`bookUrl`, `contentHash`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_read_aloud_role_caches` (
                    `cacheKey` TEXT NOT NULL,
                    `bookUrl` TEXT NOT NULL DEFAULT '',
                    `chapterKey` TEXT NOT NULL DEFAULT '',
                    `chapterIndex` INTEGER NOT NULL DEFAULT 0,
                    `chapterTitle` TEXT NOT NULL DEFAULT '',
                    `contentHash` TEXT NOT NULL DEFAULT '',
                    `mode` TEXT NOT NULL DEFAULT '',
                    `paragraphCount` INTEGER NOT NULL DEFAULT 0,
                    `segmentsJson` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`cacheKey`)
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_read_aloud_role_caches_bookUrl_chapterIndex` ON `ai_read_aloud_role_caches` (`bookUrl`, `chapterIndex`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_read_aloud_role_caches_bookUrl_contentHash` ON `ai_read_aloud_role_caches` (`bookUrl`, `contentHash`)")
        }
    }

    private val migration_97_98 = object : Migration(97, 98) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `bookKey` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `bookName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `bookAuthor` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `chapterKey` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `chapterIndex` INTEGER NOT NULL DEFAULT -1")
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `chapterTitle` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `characterId` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `characterName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `sourceType` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `ai_generated_images` ADD COLUMN `sourceText` TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_generated_images_bookKey` ON `ai_generated_images` (`bookKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_generated_images_chapterKey` ON `ai_generated_images` (`chapterKey`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_generated_images_characterId` ON `ai_generated_images` (`characterId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_generated_images_sourceType` ON `ai_generated_images` (`sourceType`)")
        }
    }

    private val migration_96_97 = object : Migration(96, 97) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `book_characters` ADD COLUMN `roleLevel` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val migration_95_96 = object : Migration(95, 96) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `book_characters` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `bookUrl` TEXT NOT NULL DEFAULT '',
                    `name` TEXT NOT NULL DEFAULT '',
                    `avatar` TEXT NOT NULL DEFAULT '',
                    `identity` TEXT NOT NULL DEFAULT '',
                    `skills` TEXT NOT NULL DEFAULT '',
                    `attributes` TEXT NOT NULL DEFAULT '',
                    `appearance` TEXT NOT NULL DEFAULT '',
                    `personality` TEXT NOT NULL DEFAULT '',
                    `biography` TEXT NOT NULL DEFAULT '',
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_characters_bookUrl` ON `book_characters` (`bookUrl`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_book_characters_bookUrl_name` ON `book_characters` (`bookUrl`, `name`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `book_character_relations` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `bookUrl` TEXT NOT NULL DEFAULT '',
                    `fromCharacterId` INTEGER NOT NULL DEFAULT 0,
                    `toCharacterId` INTEGER NOT NULL DEFAULT 0,
                    `relationName` TEXT NOT NULL DEFAULT '',
                    `relationType` TEXT NOT NULL DEFAULT '',
                    `description` TEXT NOT NULL DEFAULT '',
                    `strength` INTEGER NOT NULL DEFAULT 50,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`fromCharacterId`) REFERENCES `book_characters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`toCharacterId`) REFERENCES `book_characters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_character_relations_bookUrl` ON `book_character_relations` (`bookUrl`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_character_relations_fromCharacterId` ON `book_character_relations` (`fromCharacterId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_character_relations_toCharacterId` ON `book_character_relations` (`toCharacterId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_book_character_relations_bookUrl_fromCharacterId_toCharacterId_relationName` ON `book_character_relations` (`bookUrl`, `fromCharacterId`, `toCharacterId`, `relationName`)")
        }
    }

    private val migration_94_95 = object : Migration(94, 95) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_image_groups` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_generated_images` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `prompt` TEXT NOT NULL,
                    `providerId` TEXT NOT NULL,
                    `providerName` TEXT NOT NULL,
                    `model` TEXT NOT NULL,
                    `localPath` TEXT NOT NULL,
                    `originalSource` TEXT NOT NULL,
                    `favorite` INTEGER NOT NULL,
                    `groupId` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_generated_images_groupId` ON `ai_generated_images` (`groupId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_generated_images_favorite` ON `ai_generated_images` (`favorite`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_generated_images_createdAt` ON `ai_generated_images` (`createdAt`)")
        }
    }

    private val migration_93_94 = object : Migration(93, 94) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `read_menu_custom_buttons` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL DEFAULT '',
                    `iconPath` TEXT NOT NULL DEFAULT '',
                    `jsLib` TEXT NOT NULL DEFAULT '',
                    `loginUrl` TEXT NOT NULL DEFAULT '',
                    `loginUi` TEXT NOT NULL DEFAULT '',
                    `enabledCookieJar` INTEGER NOT NULL DEFAULT 0,
                    `script` TEXT NOT NULL DEFAULT '',
                    `timeoutMillisecond` INTEGER NOT NULL DEFAULT 3000,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `updateTime` INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_read_menu_custom_buttons_id` ON `read_menu_custom_buttons` (`id`)")
        }
    }

    private val migration_91_92 = object : Migration(91, 92) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `paragraph_rules` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL DEFAULT '',
                    `jsLib` TEXT NOT NULL DEFAULT '',
                    `loginUrl` TEXT NOT NULL DEFAULT '',
                    `loginUi` TEXT NOT NULL DEFAULT '',
                    `script` TEXT NOT NULL DEFAULT '',
                    `timeoutMillisecond` INTEGER NOT NULL DEFAULT 3000,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    `updateTime` INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_paragraph_rules_id` ON `paragraph_rules` (`id`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `book_paragraph_rules` (
                    `bookUrl` TEXT NOT NULL,
                    `ruleId` INTEGER NOT NULL,
                    `enabled` INTEGER NOT NULL DEFAULT 1,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`bookUrl`, `ruleId`),
                    FOREIGN KEY(`ruleId`) REFERENCES `paragraph_rules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_paragraph_rules_bookUrl` ON `book_paragraph_rules` (`bookUrl`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_paragraph_rules_ruleId` ON `book_paragraph_rules` (`ruleId`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `paragraph_rule_vars` (
                    `ruleId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    PRIMARY KEY(`ruleId`, `name`),
                    FOREIGN KEY(`ruleId`) REFERENCES `paragraph_rules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_paragraph_rule_vars_ruleId` ON `paragraph_rule_vars` (`ruleId`)")
        }
    }
    private val migration_90_91 = object : Migration(90, 91) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `readRecentBooks` (
                    `bookUrl` TEXT NOT NULL,
                    `lastRead` INTEGER NOT NULL,
                    PRIMARY KEY(`bookUrl`)
                )
                """
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO `readRecentBooks` (`bookUrl`, `lastRead`)
                SELECT `bookUrl`, `durChapterTime` FROM `books`
                WHERE `durChapterTime` > 0
                  AND (
                    `durChapterIndex` > 0
                    OR `durChapterPos` > 0
                    OR (`durChapterTitle` IS NOT NULL AND `durChapterTitle` != '')
                  )
                """
            )
        }
    }

    private val migration_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE txtTocRules")
            db.execSQL(
                """CREATE TABLE txtTocRules(id INTEGER NOT NULL, 
                    name TEXT NOT NULL, rule TEXT NOT NULL, serialNumber INTEGER NOT NULL, 
                    enable INTEGER NOT NULL, PRIMARY KEY (id))"""
            )
        }
    }

    private val migration_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE rssSources ADD style TEXT ")
        }
    }

    private val migration_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE rssSources ADD articleStyle INTEGER NOT NULL DEFAULT 0 ")
        }
    }

    private val migration_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `books_new` (`bookUrl` TEXT NOT NULL, `tocUrl` TEXT NOT NULL, `origin` TEXT NOT NULL,
                    `originName` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT NOT NULL, `kind` TEXT, `customTag` TEXT, `coverUrl` TEXT, 
                    `customCoverUrl` TEXT, `intro` TEXT, `customIntro` TEXT, `charset` TEXT, `type` INTEGER NOT NULL, `group` INTEGER NOT NULL, 
                    `latestChapterTitle` TEXT, `latestChapterTime` INTEGER NOT NULL, `lastCheckTime` INTEGER NOT NULL, `lastCheckCount` INTEGER NOT NULL, 
                    `totalChapterNum` INTEGER NOT NULL, `durChapterTitle` TEXT, `durChapterIndex` INTEGER NOT NULL, `durChapterPos` INTEGER NOT NULL, 
                    `durChapterTime` INTEGER NOT NULL, `wordCount` TEXT, `canUpdate` INTEGER NOT NULL, `order` INTEGER NOT NULL, 
                    `originOrder` INTEGER NOT NULL, `useReplaceRule` INTEGER NOT NULL, `variable` TEXT, PRIMARY KEY(`bookUrl`))"""
            )
            db.execSQL("INSERT INTO books_new select * from books ")
            db.execSQL("DROP TABLE books")
            db.execSQL("ALTER TABLE books_new RENAME TO books")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_books_name_author` ON `books` (`name`, `author`) ")
        }
    }

    private val migration_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE bookmarks ADD bookAuthor TEXT NOT NULL DEFAULT ''")
        }
    }

    private val migration_15_17 = object : Migration(15, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `readRecord` (`bookName` TEXT NOT NULL, `readTime` INTEGER NOT NULL, PRIMARY KEY(`bookName`))")
        }
    }

    private val migration_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `httpTTS` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    private val migration_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `readRecordNew` (`androidId` TEXT NOT NULL, `bookName` TEXT NOT NULL, `readTime` INTEGER NOT NULL, 
                    PRIMARY KEY(`androidId`, `bookName`))"""
            )
            db.execSQL("INSERT INTO readRecordNew(androidId, bookName, readTime) select '${AppConst.androidId}' as androidId, bookName, readTime from readRecord")
            db.execSQL("DROP TABLE readRecord")
            db.execSQL("ALTER TABLE readRecordNew RENAME TO readRecord")
        }
    }
    private val migration_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE book_sources ADD bookSourceComment TEXT")
        }
    }

    private val migration_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE book_groups ADD show INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val migration_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `books_new` (`bookUrl` TEXT NOT NULL, `tocUrl` TEXT NOT NULL, `origin` TEXT NOT NULL, 
                    `originName` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT NOT NULL, `kind` TEXT, `customTag` TEXT, 
                    `coverUrl` TEXT, `customCoverUrl` TEXT, `intro` TEXT, `customIntro` TEXT, `charset` TEXT, `type` INTEGER NOT NULL, 
                    `group` INTEGER NOT NULL, `latestChapterTitle` TEXT, `latestChapterTime` INTEGER NOT NULL, `lastCheckTime` INTEGER NOT NULL, 
                    `lastCheckCount` INTEGER NOT NULL, `totalChapterNum` INTEGER NOT NULL, `durChapterTitle` TEXT, `durChapterIndex` INTEGER NOT NULL, 
                    `durChapterPos` INTEGER NOT NULL, `durChapterTime` INTEGER NOT NULL, `wordCount` TEXT, `canUpdate` INTEGER NOT NULL, 
                    `order` INTEGER NOT NULL, `originOrder` INTEGER NOT NULL, `variable` TEXT, `readConfig` TEXT, PRIMARY KEY(`bookUrl`))"""
            )
            db.execSQL(
                """INSERT INTO books_new select `bookUrl`, `tocUrl`, `origin`, `originName`, `name`, `author`, `kind`, `customTag`, `coverUrl`, 
                    `customCoverUrl`, `intro`, `customIntro`, `charset`, `type`, `group`, `latestChapterTitle`, `latestChapterTime`, `lastCheckTime`, 
                    `lastCheckCount`, `totalChapterNum`, `durChapterTitle`, `durChapterIndex`, `durChapterPos`, `durChapterTime`, `wordCount`, `canUpdate`, 
                    `order`, `originOrder`, `variable`, null
                    from books"""
            )
            db.execSQL("DROP TABLE books")
            db.execSQL("ALTER TABLE books_new RENAME TO books")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_books_name_author` ON `books` (`name`, `author`) ")
        }
    }

    private val migration_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE chapters ADD baseUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    private val migration_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `caches` (`key` TEXT NOT NULL, `value` TEXT, `deadline` INTEGER NOT NULL, PRIMARY KEY(`key`))")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_caches_key` ON `caches` (`key`)")
        }
    }

    private val migration_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `sourceSubs` 
                    (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `type` INTEGER NOT NULL, `customOrder` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`))"""
            )
        }
    }

    private val migration_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `ruleSubs` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `type` INTEGER NOT NULL, 
                    `customOrder` INTEGER NOT NULL, `autoUpdate` INTEGER NOT NULL, `update` INTEGER NOT NULL, PRIMARY KEY(`id`))"""
            )
            db.execSQL(" insert into `ruleSubs` select *, 0, 0 from `sourceSubs` ")
            db.execSQL("DROP TABLE `sourceSubs`")
        }
    }

    private val migration_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(" ALTER TABLE rssSources ADD singleUrl INTEGER NOT NULL DEFAULT 0 ")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `bookmarks1` (`time` INTEGER NOT NULL, `bookUrl` TEXT NOT NULL, `bookName` TEXT NOT NULL, 
                        `bookAuthor` TEXT NOT NULL, `chapterIndex` INTEGER NOT NULL, `chapterPos` INTEGER NOT NULL, `chapterName` TEXT NOT NULL, 
                        `bookText` TEXT NOT NULL, `content` TEXT NOT NULL, PRIMARY KEY(`time`))"""
            )
            db.execSQL(
                """insert into `bookmarks1` 
                        select `time`, `bookUrl`, `bookName`, `bookAuthor`, `chapterIndex`, `pageIndex`, `chapterName`, '', `content` 
                        from bookmarks"""
            )
            db.execSQL(" DROP TABLE `bookmarks` ")
            db.execSQL(" ALTER TABLE bookmarks1 RENAME TO bookmarks ")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bookmarks_time` ON `bookmarks` (`time`)")
        }
    }

    private val migration_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE rssArticles ADD variable TEXT")
            db.execSQL("ALTER TABLE rssStars ADD variable TEXT")
        }
    }

    private val migration_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE rssSources ADD sourceComment TEXT")
        }
    }

    private val migration_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE chapters ADD `startFragmentId` TEXT")
            db.execSQL("ALTER TABLE chapters ADD `endFragmentId` TEXT")
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `epubChapters` 
                    (`bookUrl` TEXT NOT NULL, `href` TEXT NOT NULL, `parentHref` TEXT, 
                    PRIMARY KEY(`bookUrl`, `href`), FOREIGN KEY(`bookUrl`) REFERENCES `books`(`bookUrl`) ON UPDATE NO ACTION ON DELETE CASCADE )
                """
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_epubChapters_bookUrl` ON `epubChapters` (`bookUrl`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_epubChapters_bookUrl_href` ON `epubChapters` (`bookUrl`, `href`)")
        }
    }

    private val migration_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE readRecord RENAME TO readRecord1")
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `readRecord` (`deviceId` TEXT NOT NULL, `bookName` TEXT NOT NULL, `readTime` INTEGER NOT NULL, PRIMARY KEY(`deviceId`, `bookName`))
                """
            )
            db.execSQL("insert into readRecord (deviceId, bookName, readTime) select androidId, bookName, readTime from readRecord1")
        }
    }

    private val migration_31_32 = object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE `epubChapters`")
        }
    }

    private val migration_32_33 = object : Migration(32, 33) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE bookmarks RENAME TO bookmarks_old")
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `bookmarks` (`time` INTEGER NOT NULL,
                    `bookName` TEXT NOT NULL, `bookAuthor` TEXT NOT NULL, `chapterIndex` INTEGER NOT NULL, 
                    `chapterPos` INTEGER NOT NULL, `chapterName` TEXT NOT NULL, `bookText` TEXT NOT NULL, 
                    `content` TEXT NOT NULL, PRIMARY KEY(`time`))
                """
            )
            db.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `index_bookmarks_bookName_bookAuthor` ON `bookmarks` (`bookName`, `bookAuthor`)
                """
            )
            db.execSQL(
                """
                    insert into bookmarks (time, bookName, bookAuthor, chapterIndex, chapterPos, chapterName, bookText, content)
                    select time, ifNull(b.name, bookName) bookName, ifNull(b.author, bookAuthor) bookAuthor, 
                    chapterIndex, chapterPos, chapterName, bookText, content from bookmarks_old o
                    left join books b on o.bookUrl = b.bookUrl
                """
            )
        }
    }

    private val migration_33_34 = object : Migration(33, 34) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `book_groups` ADD `cover` TEXT")
        }
    }

    private val migration_34_35 = object : Migration(34, 35) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `book_sources` ADD `concurrentRate` TEXT")
        }
    }

    private val migration_35_36 = object : Migration(35, 36) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `book_sources` ADD `loginUi` TEXT")
            db.execSQL("ALTER TABLE `book_sources` ADD`loginCheckJs` TEXT")
        }
    }

    private val migration_36_37 = object : Migration(36, 37) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `rssSources` ADD `loginUrl` TEXT")
            db.execSQL("ALTER TABLE `rssSources` ADD `loginUi` TEXT")
            db.execSQL("ALTER TABLE `rssSources` ADD `loginCheckJs` TEXT")
        }
    }

    private val migration_37_38 = object : Migration(37, 38) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `book_sources` ADD `respondTime` INTEGER NOT NULL DEFAULT 180000")
        }
    }

    private val migration_38_39 = object : Migration(38, 39) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `rssSources` ADD `concurrentRate` TEXT")
        }
    }

    private val migration_39_40 = object : Migration(39, 40) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `chapters` ADD `isVip` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `chapters` ADD `isPay` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val migration_40_41 = object : Migration(40, 41) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `httpTTS` ADD `loginUrl` TEXT")
            db.execSQL("ALTER TABLE `httpTTS` ADD `loginUi` TEXT")
            db.execSQL("ALTER TABLE `httpTTS` ADD `loginCheckJs` TEXT")
            db.execSQL("ALTER TABLE `httpTTS` ADD `header` TEXT")
            db.execSQL("ALTER TABLE `httpTTS` ADD `concurrentRate` TEXT")
        }
    }

    private val migration_41_42 = object : Migration(41, 42) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE 'httpTTS' ADD `contentType` TEXT")
        }
    }

    private val migration_42_43 = object : Migration(42, 43) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `chapters` ADD `isVolume` INTEGER NOT NULL DEFAULT 0")
        }
    }


    @Suppress("ClassName")
    class Migration_54_55 : AutoMigrationSpec {

        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                update books set type = ${BookType.audio}
                where type = ${BookSourceType.audio}
            """.trimIndent()
            )
            db.execSQL(
                """
                update books set type = ${BookType.image}
                where type = ${BookSourceType.image}
            """.trimIndent()
            )
            db.execSQL(
                """
                update books set type = ${BookType.webFile}
                where type = ${BookSourceType.file}
            """.trimIndent()
            )
            db.execSQL(
                """
                update books set type = ${BookType.text}
                where type = ${BookSourceType.default}
            """.trimIndent()
            )
            db.execSQL(
                """
                update books set type = type | ${BookType.local}
                where origin like '${BookType.localTag}%' or origin like '${BookType.webDavTag}%'
            """.trimIndent()
            )
        }

    }


    @Suppress("ClassName")
    @DeleteColumn(
        tableName = "book_sources",
        columnName = "enabledReview"
    )
    class Migration_64_65 : AutoMigrationSpec

    @Suppress("ClassName")
    class Migration_80_81 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
            CREATE TABLE rssArticles_new (
                origin TEXT NOT NULL DEFAULT '',
                sort TEXT NOT NULL DEFAULT '',
                title TEXT NOT NULL DEFAULT '',
                `order` INTEGER NOT NULL DEFAULT 0,
                link TEXT NOT NULL DEFAULT '',
                pubDate TEXT,
                description TEXT,
                content TEXT,
                image TEXT,
                `group` TEXT NOT NULL DEFAULT '默认分组',
                read INTEGER NOT NULL DEFAULT 0,
                variable TEXT,
                PRIMARY KEY (origin, link, sort)
            )
        """.trimIndent())
            db.execSQL("""
            INSERT INTO rssArticles_new (origin, sort, title, `order`, link, pubDate, description, content, image, `group`, read, variable)
            SELECT origin, sort, title, `order`, link, pubDate, description, content, image, `group`, read, variable FROM rssArticles
        """.trimIndent())
            db.execSQL("DROP TABLE rssArticles")
            db.execSQL("ALTER TABLE rssArticles_new RENAME TO rssArticles")
        }
    }

    @Suppress("ClassName")
    @DeleteColumn(
        tableName = "rssArticles",
        columnName = "ratio"
    )
    class Migration_83_84 : AutoMigrationSpec

    @Suppress("ClassName")
    @DeleteColumn(
        tableName = "chapters",
        columnName = "lyric"
    )
    @DeleteColumn(
        tableName = "chapters",
        columnName = "reviewImg"
    )
    class Migration_84_85 : AutoMigrationSpec

}
