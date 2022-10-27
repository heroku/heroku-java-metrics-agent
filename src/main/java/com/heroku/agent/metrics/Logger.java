package com.heroku.agent.metrics;

public final class Logger {
    public static void logDebug(String at, String message) {
        if (isDebugEnabled()) {
            System.out.println(formatLogLine("debug", at, message));
        }
    }

    public static void logException(String at, Throwable t) {
        System.out.println(formatLogLine("error", at, t.getMessage()));

        if (isDebugEnabled()) {
            t.printStackTrace();
        }
    }

    public static void logResponseError(String at, String message, long status, String response) {
        System.out.println("error at=\"" + at + "\" component=heroku-java-metrics-agent message=\"" + message+ "\" status=" + status + " response=\"" + response + "\"");
    }

    private static String formatLogLine(String level, String at, String message) {
        return level + " at=\"" + at + "\" component=heroku-java-metrics-agent message=\"" + message + "\"";
    }

    private static boolean isDebugEnabled() {
        String herokuMetricsDebugEnvValue = System.getenv("HEROKU_METRICS_DEBUG");
        return "1".equals(herokuMetricsDebugEnvValue) || "true".equals(herokuMetricsDebugEnvValue);
    }

    private Logger() {}
}
