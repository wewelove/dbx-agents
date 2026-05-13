package com.dbx.agent.snowflake

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class SnowflakeAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return SnowflakeAgent()
    }

    override fun resultSetSql(): String = "CALL sample_proc()"
}
