package com.dbx.agent.kylin

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class KylinAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return KylinAgent()
    }

    override fun resultSetSql(): String = "CALL sample_proc()"
}
