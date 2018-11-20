package com.heroku.agent.metrics;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heroku.detector.JBossDetector;
import com.heroku.detector.ServerDetector;
import com.heroku.prometheus.client.BufferPoolsExports;
import io.prometheus.client.hotspot.DefaultExports;

public class MetricsAgent {

  private static final List<ServerDetector> SERVER_DETECTORS =
      Collections.singletonList((ServerDetector) new JBossDetector());

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    awaitServerInitialization(instrumentation);

    logDebug("premain", "starting");
    try {
      DefaultExports.initialize();
      new BufferPoolsExports().register();

      logDebug("premain", "polling");
      final Reporter reporter = new Reporter();
      new Poller().poll(new Poller.Callback() {
        @Override
        public void apply(ObjectMapper mapper, ObjectNode metricsJson) {
          try {
            logDebug("premain", "reporting");
            reporter.report(mapper.writer().writeValueAsString(metricsJson));
          } catch (IOException e) {
            logError("report-metrics", e);
          }
        }
      });
    } catch (Exception e) {
      logError("poll-metrics", e);
    }
  }

  static void logDebug(String at, String message) {
    String debug = System.getenv("HEROKU_METRICS_DEBUG");
    if ("1".equals(debug) || "true".equals(debug)) {
      System.out.println("debug at=\"" + at + "\" component=heroku-java-metrics-agent message=\"" + message + "\"");
    }
  }

  private static void logError(String at, Throwable t) {
    System.out.println("error at=\"" + at + "\" component=heroku-java-metrics-agent message=\"" + t.getMessage() + "\"");

    String debug = System.getenv("HEROKU_METRICS_DEBUG");
    if ("1".equals(debug) || "true".equals(debug)) {
      t.printStackTrace();
    }
  }

  private static void awaitServerInitialization(final Instrumentation instrumentation) {
    for (ServerDetector detector : SERVER_DETECTORS) {
      detector.jvmAgentStartup(instrumentation);
    }
  }
}
