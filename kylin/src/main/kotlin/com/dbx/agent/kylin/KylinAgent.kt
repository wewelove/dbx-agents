package com.dbx.agent.kylin

import com.dbx.agent.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types

class KylinAgent : DatabaseAgent {
    private var connection: Connection? = null

    override fun getConnection(): Connection? = connection

    companion object {
        private const val MAX_ROWS = 10000
        private val QUERY_PREFIXES = listOf("SELECT", "WITH", "SHOW", "DESCRIBE", "EXPLAIN")
    }

    override fun connect(params: ConnectParams) {
        Class.forName("org.apache.kylin.jdbc.Driver")
        val url = "jdbc:kylin://${params.host}:${params.port}/${params.database}"
        connection = DriverManager.getConnection(url, params.username, params.password)
    }

    override fun testConnection(params: ConnectParams): Boolean {
        Class.forName("org.apache.kylin.jdbc.Driver")
        val url = "jdbc:kylin://${params.host}:${params.port}/${params.database}"
        DriverManager.getConnection(url, params.username, params.password).use { conn ->
            return conn.isValid(5)
        }
    }

    override fun listDatabases(): List<DatabaseInfo> {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val result = mutableListOf<DatabaseInfo>()
        val rs = conn.metaData.catalogs
        rs.use {
            while (it.next()) {
                result.add(DatabaseInfo(it.getString("TABLE_CAT")))
            }
        }
        return result
    }

    override fun listSchemas(): List<String> {
        // Kylin uses project, not schema
        return emptyList()
    }

    override fun listTables(schema: String): List<TableInfo> {
        val conn = connection ?: throw IllegalStateException("Not connected")
        val result = mutableListOf<TableInfo>()
        val rs = conn.metaData.getTables(null, schema, null, null)
        rs.use {
            while (it.next()) {
                val tableType = it.getString("TABLE_TYPE").let { t ->
                    if (t == "BASE TABLE") "TABLE" else t
                }
                result.add(TableInfo(it.getString("TABLE_NAME"), tableType))
            }
        }
        return result
    }

    override fun getColumns(schema: String, table: String): List<ColumnInfo> {
        val conn = connection ?: throw IllegalStateException("Not connected")

        // Get primary key columns
        val primaryKeys = mutableSetOf<String>()
        conn.metaData.getPrimaryKeys(null, schema, table).use { rs ->
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"))
            }
        }

        // Get columns via metadata
        val result = mutableListOf<ColumnInfo>()
        conn.metaData.getColumns(null, schema, table, null).use { rs ->
            while (rs.next()) {
                val colName = rs.getString("COLUMN_NAME")
                result.add(
                    ColumnInfo(
                        name = colName,
                        data_type = rs.getString("TYPE_NAME"),
                        is_nullable = rs.getString("IS_NULLABLE") == "YES",
                        column_default = rs.getString("COLUMN_DEF"),
                        is_primary_key = colName in primaryKeys,
                        extra = null,
                        comment = rs.getString("REMARKS")?.ifEmpty { null },
                        numeric_precision = rs.getObject("COLUMN_SIZE")?.let { (it as Number).toInt() },
                        numeric_scale = rs.getObject("DECIMAL_DIGITS")?.let { (it as Number).toInt() },
                        character_maximum_length = null
                    )
                )
            }
        }
        return result
    }

    override fun listIndexes(schema: String, table: String): List<IndexInfo> {
        // Kylin does not support indexes
        return emptyList()
    }

    override fun listForeignKeys(schema: String, table: String): List<ForeignKeyInfo> {
        // Kylin does not support foreign keys
        return emptyList()
    }

    override fun listTriggers(schema: String, table: String): List<TriggerInfo> {
        // Kylin does not support triggers
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
    JsonRpcServer(KylinAgent()).run()
}
