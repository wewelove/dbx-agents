package com.dbx.agent.goldendb

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class GoldendbAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return GoldendbAgent()
    }

    override fun resultSetSql(): String = "CALL sample_proc()"
}
