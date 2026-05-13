package com.dbx.agent.vastbase

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class VastbaseAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return VastbaseAgent()
    }

    override fun resultSetSql(): String = "CALL sample_proc()"
}
