package com.dbx.agent.oracle

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class OracleAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return OracleAgent()
    }

    override fun resultSetSql(): String = "CALL DBMS_XPLAN.DISPLAY_CURSOR()"
}
