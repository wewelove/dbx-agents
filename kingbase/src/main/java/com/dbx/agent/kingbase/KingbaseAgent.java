package com.dbx.agent.kingbase;

import com.dbx.agent.JsonRpcServer;
import com.dbx.agent.PostgresLikeAgent;
import com.dbx.agent.PostgresLikeAgentProfile;

public final class KingbaseAgent extends PostgresLikeAgent {
    public static final PostgresLikeAgentProfile KINGBASE_PROFILE = new PostgresLikeAgentProfile(
        "com.kingbase8.Driver",
        "jdbc:kingbase8://{host}:{port}/{database}"
    );

    public KingbaseAgent() {
        super(KINGBASE_PROFILE);
    }

    public static void main(String[] args) {
        new JsonRpcServer(new KingbaseAgent()).run();
    }
}
