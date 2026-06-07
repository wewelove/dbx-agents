package com.dbx.agent.gbase8s;

import com.dbx.agent.ConfiguredJdbcAgent;
import com.dbx.agent.ConnectParams;
import com.dbx.agent.JdbcAgentProfile;
import com.dbx.agent.JsonRpcServer;
import java.util.ArrayList;
import java.util.List;

public final class Gbase8sAgent extends ConfiguredJdbcAgent {
    public static final JdbcAgentProfile GBASE8S_PROFILE = new JdbcAgentProfile(
        "com.gbasedbt.jdbc.Driver",
        "jdbc:gbasedbt-sqli://{host}:{port}/{database}:GBASEDBTSERVER=gbase8s",
        9088
    );

    public Gbase8sAgent() {
        super(GBASE8S_PROFILE);
    }

    public static String buildUrl(ConnectParams params) {
        if (!params.getConnection_string().trim().isEmpty()) {
            return params.getConnection_string();
        }
        String extraParams = trimEnd(trimStart(params.getUrl_params().trim(), ':', ';'), ';');
        String database = params.getDatabase().trim().isEmpty() ? "sysmaster" : params.getDatabase().trim();
        String serverParam = containsIgnoreCase(extraParams, "GBASEDBTSERVER=")
            ? ""
            : "GBASEDBTSERVER=" + defaultGbaseServer(params.getHost());
        List<String> jdbcParams = new ArrayList<>();
        if (!serverParam.isBlank()) {
            jdbcParams.add(serverParam);
        }
        if (!extraParams.isBlank()) {
            jdbcParams.add(extraParams);
        }
        return "jdbc:gbasedbt-sqli://" + params.getHost() + ":" + port(params) + "/" + database + ":"
            + String.join(";", jdbcParams);
    }

    @Override
    protected String buildJdbcUrl(ConnectParams params) {
        return buildUrl(params);
    }

    public static void main(String[] args) {
        new JsonRpcServer(new Gbase8sAgent()).run();
    }

    private static int port(ConnectParams params) {
        return params.getPort() > 0 ? params.getPort() : GBASE8S_PROFILE.getDefaultPort();
    }

    private static String defaultGbaseServer(String host) {
        return isIpAddress(host) ? "gbase8s" : host;
    }

    private static boolean isIpAddress(String host) {
        return host.matches("\\d{1,3}(\\.\\d{1,3}){3}") || host.contains(":");
    }

    private static String trimStart(String value, char... chars) {
        int start = 0;
        while (start < value.length() && contains(chars, value.charAt(start))) {
            start++;
        }
        return value.substring(start);
    }

    private static String trimEnd(String value, char... chars) {
        int end = value.length();
        while (end > 0 && contains(chars, value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean contains(char[] chars, char value) {
        for (char ch : chars) {
            if (ch == value) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value.toLowerCase(java.util.Locale.ROOT).contains(needle.toLowerCase(java.util.Locale.ROOT));
    }
}
