package com.dbx.agent.vastbase;

import com.dbx.agent.JsonRpcServer;
import com.dbx.agent.PostgresLikeAgent;
import com.dbx.agent.PostgresLikeAgentProfile;

public final class VastbaseAgent extends PostgresLikeAgent {
    public static final PostgresLikeAgentProfile VASTBASE_PROFILE = new PostgresLikeAgentProfile(
        "cn.com.vastbase.Driver",
        "jdbc:vastbase://{host}:{port}/{database}"
    );

    public VastbaseAgent() {
        super(VASTBASE_PROFILE);
    }

    public static void main(String[] args) {
        new JsonRpcServer(new VastbaseAgent()).run();
    }
}
