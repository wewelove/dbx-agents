package com.dbx.agent.oracle10g

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class Oracle10gAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return Oracle10gAgent()
    }

    override fun resultSetSql(): String = "CALL DBMS_XPLAN.DISPLAY_CURSOR()"
}
