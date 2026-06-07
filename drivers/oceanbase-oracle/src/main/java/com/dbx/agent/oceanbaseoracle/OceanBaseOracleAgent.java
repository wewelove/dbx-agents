package com.dbx.agent.oceanbaseoracle;

import com.dbx.agent.ConfiguredJdbcAgent;
import com.dbx.agent.JdbcAgentProfile;
import com.dbx.agent.JsonRpcServer;
import java.util.Arrays;
import java.util.Collections;

public final class OceanBaseOracleAgent extends ConfiguredJdbcAgent {
    public static final JdbcAgentProfile OCEANBASE_ORACLE_PROFILE = new JdbcAgentProfile(
        "oracle.jdbc.OracleDriver",
        "jdbc:oracle:thin:@//{host}:{port}/{database}",
        2881,
        false,
        Collections.emptySet(),
        Arrays.asList("TABLE", "VIEW")
    ) {
        @Override
        public String schemaSwitchSql(String schema) {
            return "ALTER SESSION SET CURRENT_SCHEMA = " + quoteIdentifier(schema);
        }
    };

    public OceanBaseOracleAgent() {
        super(OCEANBASE_ORACLE_PROFILE);
    }

    public static void main(String[] args) {
        new JsonRpcServer(new OceanBaseOracleAgent()).run();
    }
}
