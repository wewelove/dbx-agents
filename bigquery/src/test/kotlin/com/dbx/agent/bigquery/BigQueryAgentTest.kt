package com.dbx.agent.bigquery

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class BigQueryAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return BigQueryAgent()
    }

    override fun resultSetSql(): String = "CALL dataset.proc()"
}
