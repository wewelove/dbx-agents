package com.dbx.agent.trino

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class TrinoAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return TrinoAgent()
    }

    override fun resultSetSql(): String = "CALL system.runtime.nodes()"
}
