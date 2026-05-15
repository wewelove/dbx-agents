package com.dbx.agent.highgo

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class HighgoAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return HighgoAgent()
    }

    override fun resultSetSql(): String = "CALL sample_proc()"
}
