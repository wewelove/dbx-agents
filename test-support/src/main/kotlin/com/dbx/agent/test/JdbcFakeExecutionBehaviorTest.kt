package com.dbx.agent.test

import com.dbx.agent.DatabaseAgent
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

abstract class JdbcFakeExecutionBehaviorTest {
    protected abstract fun createAgent(): DatabaseAgent

    protected abstract fun resultSetSql(): String

    @Test
    fun `executes non select statements that return result sets`() {
        val agent = createAgent()
        setPrivateConnection(agent, JdbcAgentFake.connection())

        val result = agent.executeQuery(resultSetSql(), null)

        assertEquals(listOf("VALUE"), result.columns)
        assertEquals(listOf(listOf("row-value")), result.rows)
        assertEquals(listOf("execute"), JdbcAgentFake.calls)
    }
}
