package com.dbx.agent.oceanbaseoracle;

import com.dbx.agent.ConfiguredJdbcAgent;
import com.dbx.agent.JdbcAgentProfile;
import com.dbx.agent.JsonRpcServer;

public final class OceanBaseOracleAgent extends ConfiguredJdbcAgent {
    public static final JdbcAgentProfile OCEANBASE_ORACLE_PROFILE = new JdbcAgentProfile(
        "com.oceanbase.jdbc.Driver",
        "jdbc:oceanbase://{host}:{port}/{database}",
        2881,
        false
    ) {
        @Override
        public String schemaSwitchSql(String schema, String quote) {
            return "ALTER SESSION SET CURRENT_SCHEMA = " + quote + schema.replace(quote, quote + quote) + quote;
        }
    };

    public OceanBaseOracleAgent() {
        super(OCEANBASE_ORACLE_PROFILE);
    }

    public static void main(String[] args) {
        new JsonRpcServer(new OceanBaseOracleAgent()).run();
    }
}
