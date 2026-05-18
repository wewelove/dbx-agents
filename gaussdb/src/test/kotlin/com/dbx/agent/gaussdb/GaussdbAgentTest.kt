package com.dbx.agent.gaussdb

import com.dbx.agent.BaseDatabaseAgent
import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GaussdbAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return GaussdbAgent()
    }

    override fun resultSetSql(): String = "CALL sample_proc()"

    @Test
    fun `agent extends base database agent`() {
        assertTrue(BaseDatabaseAgent::class.java.isAssignableFrom(GaussdbAgent::class.java))
    }
}
