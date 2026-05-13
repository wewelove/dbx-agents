package com.dbx.agent.informix

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class InformixAgentExecutionTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return InformixAgent()
    }

    override fun resultSetSql(): String = "EXECUTE PROCEDURE sample_proc()"
}
