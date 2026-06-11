package com.dbx.agent.oracle;

import com.dbx.agent.DatabaseAgent;
import com.dbx.agent.ConnectParams;
import com.dbx.agent.test.JdbcFakeExecutionBehaviorTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OracleAgentTest extends JdbcFakeExecutionBehaviorTest {
    @Override
    protected DatabaseAgent createAgent() {
        return new OracleAgent();
    }

    @Override
    protected String resultSetSql() {
        return "CALL DBMS_XPLAN.DISPLAY_CURSOR()";
    }

    @Test
    void buildUrlUsesExplicitConnectionString() {
        ConnectParams params = new ConnectParams(
            "oracle.example.com",
            1521,
            "ORCL",
            "scott",
            "tiger",
            "",
            "jdbc:oracle:thin:@oracle.example.com:1521:ORCL",
            false
        );

        Assertions.assertEquals("jdbc:oracle:thin:@oracle.example.com:1521:ORCL", OracleAgent.buildUrl(params));
    }

    @Test
    void prepareExecutableSqlKeepsPlsqlObjectTerminator() {
        String sql = "CREATE OR REPLACE PROCEDURE APP_PROC AS BEGIN NULL; END;";

        Assertions.assertEquals(sql, OracleAgent.prepareExecutableSql(sql));
    }

    @Test
    void prepareExecutableSqlTrimsPlainStatementTerminator() {
        Assertions.assertEquals("SELECT 1 FROM DUAL", OracleAgent.prepareExecutableSql("SELECT 1 FROM DUAL;"));
    }

    @Test
    void prepareExecutableSqlStillRewritesFetchFirst() {
        Assertions.assertEquals(
            "SELECT * FROM (SELECT * FROM EMP) WHERE ROWNUM <= 10",
            OracleAgent.prepareExecutableSql("SELECT * FROM EMP FETCH FIRST 10 ROWS ONLY;")
        );
    }
}
