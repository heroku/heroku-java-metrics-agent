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

  public static void logReporterResponseError(
      String at, String message, long status, String response) {
    // The metrics endpoint returns 401 and 429 from time to time. These errors aren't harmful as
    // the request will be automatically retried. Customers previously reported log clutter due to these messages. Since
    // these errors are not actionable for the customer, we now only log at the debug level.
    if (isDebugEnabled()) {
      System.out.println(
          "debug at=\""
              + at
              + "\" component=heroku-java-metrics-agent message=\""
              + message
              + "\" status="
              + status
              + " response=\""
              + response
              + "\"");
    }
  }

  private static String formatLogLine(String level, String at, String message) {
    return level
        + " at=\""
        + at
        + "\" component=heroku-java-metrics-agent message=\""
        + message
        + "\"";
  }

  private static boolean isDebugEnabled() {
    String herokuMetricsDebugEnvValue = System.getenv("HEROKU_METRICS_DEBUG");
    return "1".equals(herokuMetricsDebugEnvValue) || "true".equals(herokuMetricsDebugEnvValue);
  }

  private Logger() {}
}
