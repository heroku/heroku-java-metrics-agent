package com.heroku.agent.metrics;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heroku.prometheus.client.BufferPoolsExports;
import io.prometheus.client.hotspot.DefaultExports;

public class MetricsAgent {

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    try {
      DefaultExports.initialize();
      new BufferPoolsExports().register();

      final Reporter reporter = new Reporter();
      new Poller().poll(new Poller.Callback() {
        @Override
        public void apply(ObjectMapper mapper, ObjectNode metricsJson) {
          try {
            reporter.report(mapper.writer().writeValueAsString(metricsJson));
          } catch (IOException e) {
            logError("Failed to report metrics", e);
          }
        }
      });
    } catch (Exception e) {
      logError("Failed to poll metrics", e);
    }
  }

  private static void logError(String message, Throwable t) {
    System.out.println(" ! ERROR: " + message);

    String debug = System.getenv("HEROKU_METRICS_DEBUG");
    if ("1".equals(debug) || "true".equals(debug)) {
      System.out.println(t.getMessage());
      t.printStackTrace();
    }
  }
}
