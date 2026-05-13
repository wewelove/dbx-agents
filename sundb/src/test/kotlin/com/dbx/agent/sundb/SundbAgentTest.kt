package com.dbx.agent.sundb

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class SundbAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return SundbAgent()
    }

    override fun resultSetSql(): String = "CALL sample_proc()"
}
