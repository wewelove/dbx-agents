package com.dbx.agent.kingbase

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class KingbaseAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return KingbaseAgent()
    }

    override fun resultSetSql(): String = "CALL sample_proc()"
}
