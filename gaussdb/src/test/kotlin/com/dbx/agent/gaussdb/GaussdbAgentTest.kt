package com.dbx.agent.gaussdb

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class GaussdbAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return GaussdbAgent()
    }

    override fun resultSetSql(): String = "CALL sample_proc()"
}
