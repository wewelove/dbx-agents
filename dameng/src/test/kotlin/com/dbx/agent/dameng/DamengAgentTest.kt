package com.dbx.agent.dameng

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class DamengAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return DamengAgent()
    }

    override fun resultSetSql(): String = "CALL SP_SAMPLE()"
}
