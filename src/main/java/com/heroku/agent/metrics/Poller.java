package com.heroku.agent.metrics;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

/**
 * @author Joe Kutner on 11/1/17.
 *         Twitter: @codefinger
 */
public class Poller {

  private static final List<String> GC_METRICS = Arrays.asList(
      "jvm_gc_collection_seconds_count.gc_PS_Scavenge",
      "jvm_gc_collection_seconds_count.gc_PS_MarkSweep",
      "jvm_gc_collection_seconds_count.gc_G1_Young_Generation",
      "jvm_gc_collection_seconds_count.gc_G1_Old_Generation"
  );

  private CollectorRegistry registry;

  private final Timer timer;

  private Map<String, Metric> previousCounters = new ConcurrentHashMap<>();

  public Poller() {
    this.timer = new Timer("heroku-java-metrics-agent",true);
    this.registry = CollectorRegistry.defaultRegistry;
  }

  public void poll(final Callback callback) throws IOException {
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        Map<String, Metric> counters = new ConcurrentHashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode metricsJson = mapper.createObjectNode();
        ObjectNode gaugesJson = metricsJson.putObject("gauges");
        ObjectNode countersJson = metricsJson.putObject("counters");

        Enumeration<Collector.MetricFamilySamples> samples = registry.metricFamilySamples();
        while (samples.hasMoreElements()) {
          Collector.MetricFamilySamples metricFamilySamples = samples.nextElement();
          switch (metricFamilySamples.type) {
            case GAUGE:
              for (Metric m : collectSamples(metricFamilySamples.samples)) {
                gaugesJson.put(m.getKey(), m.getValue());
              }
              break;
            case COUNTER:
              for (Metric m : collectSamples(metricFamilySamples.samples)) {
                countersJson.put(m.getKey(), m.getDerivedValue(previousCounters.get(m.getKey())));
                counters.put(m.getKey(), m);
              }
              break;
            case SUMMARY:
              for (Metric m : collectSamples(metricFamilySamples.samples)) {
                countersJson.put(m.getKey(), m.getDerivedValue(previousCounters.get(m.getKey())));
                counters.put(m.getKey(), m);
              }
              break;
          }
        }
        countersJson.put("jvm_gc_collection_seconds_count.gc_all", sumGc(counters, countersJson));

        previousCounters = counters;
        callback.apply(mapper, metricsJson);
      }
    }, 5, 5);
  }

  public void cancel() {
    this.timer.cancel();
  }

  public static abstract class Callback {
    public abstract void apply(ObjectMapper mapper, ObjectNode metricsJson);
  }

  private Double sumGc(Map<String, Metric> counters, ObjectNode countersJson) {
    Double runningTotal = 0d;
    for (String key : counters.keySet()) {
      if (GC_METRICS.contains(key)) {
        runningTotal += countersJson.get(key).asDouble();
      }
    }
    return runningTotal;
  }

  private List<Metric> collectSamples(List<Collector.MetricFamilySamples.Sample> samples) {
    List<Metric> metrics = new ArrayList<>();
    for (Collector.MetricFamilySamples.Sample sample : samples) {
      StringBuilder key = new StringBuilder(sample.name);
      for (int i = 0; i < sample.labelNames.size(); i++) {
        key.append(".").append(massageLabel(sample.labelNames.get(i) + "_" + sample.labelValues.get(i)));
      }
      metrics.add(new Metric(key.toString(), sample.value));
    }
    return metrics;
  }

  private String massageLabel(String rune) {
    return rune.replaceAll("[^0-9a-zA-Z.,-]", "_");
  }

}
