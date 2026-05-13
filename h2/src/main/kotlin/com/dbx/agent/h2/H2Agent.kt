package com.dbx.agent.h2

import com.dbx.agent.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types

class H2Agent : DatabaseAgent {

    private var connection: Connection? = null

    override fun getConnection(): Connection? = connection

    companion object {
        private const val MAX_ROWS = 10000
        private val QUERY_PREFIXES = listOf("SELECT", "WITH", "SHOW", "DESCRIBE", "EXPLAIN")
    }

    override fun connect(params: ConnectParams) {
        Class.forName("org.h2.Driver")
        val url = if (params.host.isBlank()) {
            "jdbc:h2:${params.database}"
        } else {
            "jdbc:h2:tcp://${params.host}:${params.port}/${params.database}"
        }
        connection = DriverManager.getConnection(url, params.username, params.password)
    }

    override fun testConnection(params: ConnectParams): Boolean {
        Class.forName("org.h2.Driver")
        val url = if (params.host.isBlank()) {
            "jdbc:h2:${params.database}"
        } else {
            "jdbc:h2:tcp://${params.host}:${params.port}/${params.database}"
        }
        DriverManager.getConnection(url, params.username, params.password).use { conn ->
            return conn.isValid(5)
        }
    }

    override fun listDatabases(): List<DatabaseInfo> {
        val conn = requireConnection()
        return try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SHOW DATABASES").use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(DatabaseInfo(rs.getString(1)))
                        }
                    }
                }
            }
        } catch (_: Exception) {
            listOf(DatabaseInfo("default"))
        }
    }

    override fun listSchemas(): List<String> {
        val conn = requireConnection()
        conn.prepareStatement(
            "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA ORDER BY SCHEMA_NAME"
        ).use { stmt ->
            stmt.executeQuery().use { rs ->
                return buildList {
                    while (rs.next()) {
                        add(rs.getString(1))
                    }
                }
            }
        }
    }

    override fun listTables(schema: String): List<TableInfo> {
        val conn = requireConnection()
        conn.prepareStatement(
            """
            SELECT TABLE_NAME, TABLE_TYPE
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = ?
            ORDER BY TABLE_NAME
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, schema)
            stmt.executeQuery().use { rs ->
                return buildList {
                    while (rs.next()) {
                        val tableType = rs.getString("TABLE_TYPE").let {
                            if (it == "BASE TABLE") "TABLE" else it
                        }
                        add(TableInfo(rs.getString("TABLE_NAME"), tableType))
                    }
                }
            }
        }
    }

    override fun getColumns(schema: String, table: String): List<ColumnInfo> {
        val conn = requireConnection()

        // Get primary key columns
        val primaryKeys = mutableSetOf<String>()
        conn.prepareStatement(
            """
            SELECT COLUMN_NAME
            FROM INFORMATION_SCHEMA.INDEXES
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND PRIMARY_KEY = TRUE
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"))
                }
            }
        }

        // Get columns
        conn.prepareStatement(
            """
            SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT,
                   NUMERIC_PRECISION, NUMERIC_SCALE, CHARACTER_MAXIMUM_LENGTH
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                return buildList {
                    while (rs.next()) {
                        val colName = rs.getString("COLUMN_NAME")
                        add(ColumnInfo(
                            name = colName,
                            data_type = rs.getString("DATA_TYPE"),
                            is_nullable = rs.getString("IS_NULLABLE") == "YES",
                            column_default = rs.getString("COLUMN_DEFAULT"),
                            is_primary_key = colName in primaryKeys,
                            extra = null,
                            comment = null,
                            numeric_precision = rs.getObject("NUMERIC_PRECISION")?.let { (it as Number).toInt() },
                            numeric_scale = rs.getObject("NUMERIC_SCALE")?.let { (it as Number).toInt() },
                            character_maximum_length = rs.getObject("CHARACTER_MAXIMUM_LENGTH")?.let { (it as Number).toInt() }
                        ))
                    }
                }
            }
        }
    }

    override fun listIndexes(schema: String, table: String): List<IndexInfo> {
        val conn = requireConnection()
        conn.prepareStatement(
            """
            SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE, PRIMARY_KEY, INDEX_TYPE_NAME
            FROM INFORMATION_SCHEMA.INDEXES
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            ORDER BY INDEX_NAME, ORDINAL_POSITION
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                val indexMap = linkedMapOf<String, MutableList<String>>()
                val uniqueMap = mutableMapOf<String, Boolean>()
                val primaryMap = mutableMapOf<String, Boolean>()
                val typeMap = mutableMapOf<String, String>()

                while (rs.next()) {
                    val indexName = rs.getString("INDEX_NAME")
                    val colName = rs.getString("COLUMN_NAME")
                    val nonUnique = rs.getBoolean("NON_UNIQUE")
                    val isPrimary = rs.getBoolean("PRIMARY_KEY")
                    val indexType = rs.getString("INDEX_TYPE_NAME")

                    indexMap.getOrPut(indexName) { mutableListOf() }.add(colName)
                    uniqueMap[indexName] = !nonUnique
                    primaryMap[indexName] = isPrimary
                    typeMap[indexName] = indexType ?: ""
                }

                return indexMap.map { (name, cols) ->
                    IndexInfo(
                        name = name,
                        columns = cols,
                        is_unique = uniqueMap[name] ?: false,
                        is_primary = primaryMap[name] ?: false,
                        index_type = typeMap[name]
                    )
                }
            }
        }
    }

    override fun listForeignKeys(schema: String, table: String): List<ForeignKeyInfo> {
        val conn = requireConnection()
        conn.prepareStatement(
            """
            SELECT FK_NAME, FKCOLUMN_NAME, PKTABLE_NAME, PKCOLUMN_NAME
            FROM INFORMATION_SCHEMA.CROSS_REFERENCES
            WHERE FKTABLE_SCHEMA = ? AND FKTABLE_NAME = ?
            ORDER BY FK_NAME
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                return buildList {
                    while (rs.next()) {
                        add(ForeignKeyInfo(
                            name = rs.getString("FK_NAME"),
                            column = rs.getString("FKCOLUMN_NAME"),
                            ref_table = rs.getString("PKTABLE_NAME"),
                            ref_column = rs.getString("PKCOLUMN_NAME")
                        ))
                    }
                }
            }
        }
    }

    override fun listTriggers(schema: String, table: String): List<TriggerInfo> {
        val conn = requireConnection()
        conn.prepareStatement(
            """
            SELECT TRIGGER_NAME, EVENT_MANIPULATION, ACTION_TIMING
            FROM INFORMATION_SCHEMA.TRIGGERS
            WHERE TRIGGER_SCHEMA = ? AND EVENT_OBJECT_TABLE = ?
            ORDER BY TRIGGER_NAME
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                return buildList {
                    while (rs.next()) {
                        add(TriggerInfo(
                            name = rs.getString("TRIGGER_NAME"),
                            event = rs.getString("EVENT_MANIPULATION"),
                            timing = rs.getString("ACTION_TIMING")
                        ))
                    }
                }
            }
        }
    }

    override fun executeQuery(sql: String, schema: String?): QueryResult {
        val conn = requireConnection()
        val trimmedSql = sql.trim().trimEnd(';')
        val upperSql = trimmedSql.uppercase().trimStart()

        // Transaction control
        if (upperSql == "BEGIN" || upperSql == "BEGIN TRANSACTION") {
            val start = System.currentTimeMillis()
            conn.autoCommit = false
            return QueryResult(emptyList(), emptyList(), 0, System.currentTimeMillis() - start)
        }
        if (upperSql == "COMMIT") {
            val start = System.currentTimeMillis()
            conn.commit()
            conn.autoCommit = true
            return QueryResult(emptyList(), emptyList(), 0, System.currentTimeMillis() - start)
        }
        if (upperSql == "ROLLBACK") {
            val start = System.currentTimeMillis()
            conn.rollback()
            conn.autoCommit = true
            return QueryResult(emptyList(), emptyList(), 0, System.currentTimeMillis() - start)
        }

        // Set schema if provided
        if (!schema.isNullOrBlank()) {
            conn.createStatement().use { stmt ->
                stmt.execute(setSchemaSQL(schema))
            }
        }

        val startTime = System.currentTimeMillis()
        val isQuery = QUERY_PREFIXES.any { upperSql.startsWith(it) }

        return if (isQuery) {
            conn.createStatement().use { stmt ->
                stmt.maxRows = MAX_ROWS + 1
                stmt.executeQuery(trimmedSql).use { rs ->
                    val meta = rs.metaData
                    val colCount = meta.columnCount
                    val columns = (1..colCount).map { meta.getColumnLabel(it) }
                    val rows = mutableListOf<List<Any?>>()

                    while (rs.next() && rows.size < MAX_ROWS) {
                        val row = (1..colCount).map { i ->
                            val value = rs.getObject(i)
                            if (rs.wasNull()) null else value?.toString()
                        }
                        rows.add(row)
                    }

                    val truncated = rs.next()
                    val elapsed = System.currentTimeMillis() - startTime

                    QueryResult(
                        columns = columns,
                        rows = rows,
                        affected_rows = 0,
                        execution_time_ms = elapsed,
                        truncated = truncated
                    )
                }
            }
        } else {
            conn.createStatement().use { stmt ->
                val affected = stmt.executeUpdate(trimmedSql)
                val elapsed = System.currentTimeMillis() - startTime
                QueryResult(
                    columns = emptyList(),
                    rows = emptyList(),
                    affected_rows = affected.toLong(),
                    execution_time_ms = elapsed
                )
            }
        }
    }

    override fun setSchemaSQL(schema: String): String = "SET SCHEMA \"$schema\""

    override fun disconnect() {
        connection?.close()
        connection = null
    }

    private fun requireConnection(): Connection {
        return connection ?: throw IllegalStateException("Not connected")
    }
}

fun main() {
    JsonRpcServer(H2Agent()).run()
}
