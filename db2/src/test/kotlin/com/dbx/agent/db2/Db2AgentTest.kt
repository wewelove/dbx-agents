package com.dbx.agent.db2

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class Db2AgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return Db2Agent()
    }

    override fun resultSetSql(): String = "CALL ADMIN_CMD('list applications')"
}
