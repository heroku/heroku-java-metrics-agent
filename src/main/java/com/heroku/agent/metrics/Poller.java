package com.heroku.agent.metrics;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

/**
 * @author Joe Kutner on 11/1/17.
 *         Twitter: @codefinger
 */
public class Poller {

  private CollectorRegistry registry;

  private final ScheduledExecutorService scheduler;

  public Poller() {
    this.scheduler = Executors.newScheduledThreadPool(1);
    this.registry = CollectorRegistry.defaultRegistry;
  }

  public ScheduledFuture<?> poll(final Callback callback) throws IOException {
    return scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode metricsJson = mapper.createObjectNode();
        ObjectNode gauges = metricsJson.putObject("gauges");
        ObjectNode counters = metricsJson.putObject("counters");

        Enumeration<Collector.MetricFamilySamples> samples = registry.metricFamilySamples();
        while (samples.hasMoreElements()) {
          Collector.MetricFamilySamples metricFamilySamples = samples.nextElement();
          switch (metricFamilySamples.type) {
            case GAUGE:
              collectSamples(gauges, metricFamilySamples.samples);
              break;
            case COUNTER:
              collectSamples(counters, metricFamilySamples.samples);
              break;
            case SUMMARY:
              collectSamples(counters, metricFamilySamples.samples);
              break;
          }
        }
        callback.apply(mapper, metricsJson);
      }
    }, 5, 5, TimeUnit.SECONDS);
  }

  public static abstract class Callback {
    public abstract void apply(ObjectMapper mapper, ObjectNode metricsJson);
  }

  private void collectSamples(ObjectNode node, List<Collector.MetricFamilySamples.Sample> samples) {
    for (Collector.MetricFamilySamples.Sample sample : samples) {
      StringBuilder key = new StringBuilder(sample.name);
      for (int i = 0; i < sample.labelNames.size(); i++) {
        key.append(".").append(massageLabel(sample.labelNames.get(i) + "_" + sample.labelValues.get(i)));
      }
      node.put(key.toString(), sample.value);
    }
  }

  private String massageLabel(String rune) {
    return rune.replaceAll("[^0-9a-zA-Z.,-]", "_");
  }

}
