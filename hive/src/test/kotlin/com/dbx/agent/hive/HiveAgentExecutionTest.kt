package com.dbx.agent.hive

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class HiveAgentExecutionTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return HiveAgent()
    }

    override fun resultSetSql(): String = "MSCK REPAIR TABLE sample"
}
