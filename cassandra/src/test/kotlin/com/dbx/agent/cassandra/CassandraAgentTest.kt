package com.dbx.agent.cassandra

import com.dbx.agent.DatabaseAgent
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest

class CassandraAgentTest : JdbcFakeExecutionBehaviorTest() {
    override fun createAgent(): DatabaseAgent {
        return CassandraAgent()
    }

    override fun resultSetSql(): String = "LIST ROLES"
}
