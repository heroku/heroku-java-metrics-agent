package com.heroku.agent.metrics;

import com.heroku.agent.metrics.detector.JBossDetector;
import com.heroku.agent.metrics.detector.ServerDetector;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Timer;

public final class MetricsAgent {

  /**
   * Entry point of this JVM agent.
   *
   * @param agentArgs The arguments to this agent.
   * @param instrumentation JVM instrumentation API.
   */
  public static void premain(String agentArgs, final Instrumentation instrumentation) {
    final URL metricsEndpointUrl = getMetricsEndpointUrl();

    // This agent will not attempt to do anything when no metrics endpoint is available.
    if (metricsEndpointUrl == null) {
      Logger.logDebug("premain", "no metrics endpoint URL available");
      return;
    }

    // We hand off any further logic to a separate thread to allow the customers' application to
    // continue to start.
    // In some cases, we wait until certain bits of the customer application have been loaded to not
    // interfere.
    Thread thread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                for (ServerDetector detector : SERVER_DETECTORS) {
                  Logger.logDebug("await-server", detector.getClass().toString());
                  detector.jvmAgentStartup(instrumentation);
                }

                Timer timer = new Timer("heroku-java-metrics-agent", true);
                timer.scheduleAtFixedRate(
                    new TimerTask(new Reporter(metricsEndpointUrl)),
                    Constants.METRICS_REPORTING_INTERVAL_MS,
                    Constants.METRICS_REPORTING_INTERVAL_MS);
              }
            });

    thread.setDaemon(true);
    thread.start();
  }

  private static URL getMetricsEndpointUrl() {
    String metricsUrlString = System.getenv("HEROKU_METRICS_URL");
    if (metricsUrlString != null) {
      try {
        return new URL(metricsUrlString);
      } catch (MalformedURLException e) {
        Logger.logException("get-metrics-endpoint-url", e);
      }
    }

    return null;
  }

  private MetricsAgent() {}

  private static final class TimerTask extends java.util.TimerTask {
    private final Reporter reporter;

    private HerokuJvmMetrics previousMetrics = null;

    public TimerTask(Reporter reporter) {
      this.reporter = reporter;
    }

    @Override
    public void run() {
      HerokuJvmMetrics metrics = Collector.collect();

      reporter.report(JsonSerializer.serialize(metrics, previousMetrics));

      previousMetrics = metrics;
    }
  }

  private static final List<ServerDetector> SERVER_DETECTORS =
      Collections.singletonList((ServerDetector) new JBossDetector());
}
