package com.dbx.agent.trino

import com.dbx.agent.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types

class TrinoAgent : DatabaseAgent {

    private var connection: Connection? = null

    override fun getConnection(): Connection? = connection

    companion object {
        private const val MAX_ROWS = 10000
        private val QUERY_PREFIXES = listOf("SELECT", "WITH", "SHOW", "DESCRIBE", "EXPLAIN")
    }

    override fun connect(params: ConnectParams) {
        Class.forName("io.trino.jdbc.TrinoDriver")
        val url = "jdbc:trino://${params.host}:${params.port}/${params.database}"
        connection = DriverManager.getConnection(url, params.username, params.password)
    }

    override fun testConnection(params: ConnectParams): Boolean {
        Class.forName("io.trino.jdbc.TrinoDriver")
        val url = "jdbc:trino://${params.host}:${params.port}/${params.database}"
        DriverManager.getConnection(url, params.username, params.password).use { conn ->
            return conn.isValid(5)
        }
    }

    override fun listDatabases(): List<DatabaseInfo> {
        val conn = requireConnection()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SHOW CATALOGS").use { rs ->
                return buildList {
                    while (rs.next()) {
                        add(DatabaseInfo(rs.getString(1)))
                    }
                }
            }
        }
    }

    override fun listSchemas(): List<String> {
        val conn = requireConnection()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SHOW SCHEMAS").use { rs ->
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
            SELECT table_name, table_type
            FROM information_schema.tables
            WHERE table_schema = ?
            ORDER BY table_name
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, schema)
            stmt.executeQuery().use { rs ->
                return buildList {
                    while (rs.next()) {
                        val tableType = rs.getString("table_type").let {
                            if (it == "BASE TABLE") "TABLE" else it
                        }
                        add(TableInfo(rs.getString("table_name"), tableType))
                    }
                }
            }
        }
    }

    override fun getColumns(schema: String, table: String): List<ColumnInfo> {
        val conn = requireConnection()
        conn.prepareStatement(
            """
            SELECT column_name, data_type, is_nullable, column_default,
                   numeric_precision, numeric_scale, character_maximum_length
            FROM information_schema.columns
            WHERE table_schema = ? AND table_name = ?
            ORDER BY ordinal_position
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, schema)
            stmt.setString(2, table)
            stmt.executeQuery().use { rs ->
                return buildList {
                    while (rs.next()) {
                        add(ColumnInfo(
                            name = rs.getString("column_name"),
                            data_type = rs.getString("data_type"),
                            is_nullable = rs.getString("is_nullable") == "YES",
                            column_default = rs.getString("column_default"),
                            is_primary_key = false, // Trino does not expose PK info in information_schema
                            extra = null,
                            comment = null,
                            numeric_precision = rs.getObject("numeric_precision")?.let { (it as Number).toInt() },
                            numeric_scale = rs.getObject("numeric_scale")?.let { (it as Number).toInt() },
                            character_maximum_length = rs.getObject("character_maximum_length")?.let { (it as Number).toInt() }
                        ))
                    }
                }
            }
        }
    }

    override fun listIndexes(schema: String, table: String): List<IndexInfo> {
        // Trino does not support traditional indexes
        return emptyList()
    }

    override fun listForeignKeys(schema: String, table: String): List<ForeignKeyInfo> {
        // Trino does not expose foreign key metadata
        return emptyList()
    }

    override fun listTriggers(schema: String, table: String): List<TriggerInfo> {
        // Trino does not support triggers
        return emptyList()
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

    override fun setSchemaSQL(schema: String): String = "USE \"$schema\""

    override fun disconnect() {
        connection?.close()
        connection = null
    }

    private fun requireConnection(): Connection {
        return connection ?: throw IllegalStateException("Not connected")
    }
}

fun main() {
    JsonRpcServer(TrinoAgent()).run()
}
