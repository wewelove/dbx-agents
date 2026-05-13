package com.dbx.agent.bigquery

import com.dbx.agent.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types

class BigQueryAgent : DatabaseAgent {
    private var connection: Connection? = null

    override fun getConnection(): Connection? = connection

    companion object {
        private const val MAX_ROWS = 10000
        private val QUERY_PREFIXES = listOf("SELECT", "WITH", "SHOW", "DESCRIBE", "EXPLAIN")
    }

    override fun connect(params: ConnectParams) {
        Class.forName("com.simba.googlebigquery.jdbc.Driver")
        val url = "jdbc:bigquery://${params.host}:${params.port};ProjectId=${params.database}"
        connection = DriverManager.getConnection(url, params.username, params.password)
    }

    override fun testConnection(params: ConnectParams): Boolean {
        Class.forName("com.simba.googlebigquery.jdbc.Driver")
        val url = "jdbc:bigquery://${params.host}:${params.port};ProjectId=${params.database}"
        DriverManager.getConnection(url, params.username, params.password).use { conn ->
            return conn.isValid(5)
        }
    }

    override fun listDatabases(): List<DatabaseInfo> {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val result = mutableListOf<DatabaseInfo>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT schema_name FROM INFORMATION_SCHEMA.SCHEMATA ORDER BY schema_name").use { rs ->
                while (rs.next()) {
                    result.add(DatabaseInfo(rs.getString(1)))
                }
            }
        }
        return result
    }

    override fun listSchemas(): List<String> {
        return listDatabases().map { it.name }
    }

    override fun listTables(schema: String): List<TableInfo> {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val result = mutableListOf<TableInfo>()
        conn.prepareStatement(
            "SELECT table_name, table_type FROM `$schema`.INFORMATION_SCHEMA.TABLES ORDER BY table_name"
        ).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    val tableType = rs.getString("table_type").let {
                        if (it == "BASE TABLE") "TABLE" else it
                    }
                    result.add(TableInfo(rs.getString("table_name"), tableType))
                }
            }
        }
        return result
    }

    override fun getColumns(schema: String, table: String): List<ColumnInfo> {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val result = mutableListOf<ColumnInfo>()
        conn.prepareStatement(
            """
            SELECT column_name, data_type, is_nullable
            FROM `$schema`.INFORMATION_SCHEMA.COLUMNS
            WHERE table_name = ?
            ORDER BY ordinal_position
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, table)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    result.add(
                        ColumnInfo(
                            name = rs.getString("column_name"),
                            data_type = rs.getString("data_type"),
                            is_nullable = rs.getString("is_nullable") == "YES",
                            column_default = null,
                            is_primary_key = false,
                            extra = null,
                            comment = null,
                            numeric_precision = null,
                            numeric_scale = null,
                            character_maximum_length = null
                        )
                    )
                }
            }
        }
        return result
    }

    override fun listIndexes(schema: String, table: String): List<IndexInfo> {
        // BigQuery does not support indexes
        return emptyList()
    }

    override fun listForeignKeys(schema: String, table: String): List<ForeignKeyInfo> {
        // BigQuery does not support foreign keys
        return emptyList()
    }

    override fun listTriggers(schema: String, table: String): List<TriggerInfo> {
        // BigQuery does not support triggers
        return emptyList()
    }

    override fun executeQuery(sql: String, schema: String?): QueryResult {
        val conn = connection ?: throw IllegalStateException("Not connected")

        val trimmedSql = sql.trim().trimEnd(';')
        val upperSql = trimmedSql.uppercase().trimStart()

        // Translate transaction control to JDBC calls
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

        val startTime = System.currentTimeMillis()
        val isQuery = QUERY_PREFIXES.any { upperSql.startsWith(it) }

        return if (isQuery) {
            conn.createStatement().use { stmt ->
                stmt.maxRows = MAX_ROWS + 1
                stmt.executeQuery(trimmedSql).use { rs ->
                    val meta = rs.metaData
                    val columnCount = meta.columnCount
                    val columns = (1..columnCount).map { meta.getColumnLabel(it) }
                    val rows = mutableListOf<List<Any?>>()

                    while (rs.next() && rows.size < MAX_ROWS) {
                        val row = (1..columnCount).map { i -> getResultValue(rs, i, meta.getColumnType(i)) }
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

    override fun setSchemaSQL(schema: String): String = ""

    override fun disconnect() {
        connection?.close()
        connection = null
    }

    private fun getResultValue(rs: ResultSet, index: Int, sqlType: Int): Any? {
        val value = when (sqlType) {
            Types.BIGINT -> rs.getLong(index)
            Types.INTEGER, Types.SMALLINT, Types.TINYINT -> rs.getInt(index)
            Types.FLOAT, Types.REAL -> rs.getFloat(index)
            Types.DOUBLE -> rs.getDouble(index)
            Types.DECIMAL, Types.NUMERIC -> rs.getBigDecimal(index)
            Types.BOOLEAN, Types.BIT -> rs.getBoolean(index)
            else -> rs.getString(index)
        }
        return if (rs.wasNull()) null else value
    }
}

fun main() {
    JsonRpcServer(BigQueryAgent()).run()
}
