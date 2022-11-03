package com.heroku.agent.metrics;

import java.util.Map;

public final class JsonSerializer {
  /**
   * Serializes Heroku JVM metrics into their JSON representation required by the Heroku service for
   * dyno language metrics.
   *
   * @param metrics The metrics to serialize.
   * @param previousMetrics The metrics before the current metrics. Used to calculate deltas, can be
   *     null.
   * @return A JSON string with the serialized metrics.
   */
  public static String serialize(HerokuJvmMetrics metrics, HerokuJvmMetrics previousMetrics) {
    // This method is creating JSON by string concatenation.
    //
    // In general, this would be a bad idea and shouldn't be attempted. In the case of the metrics
    // agent however, we have a strong case for not using a JSON library here. First, we don't have
    // to write our own escaping logic:
    //
    // - General JSON structure is fixed/static
    // - All JSON object keys are static and fully under our control.
    // - All JSON field values are strictly of type double or long, even though they are dynamic, we
    //   know that we never have to escape them:
    //     - https://www.rfc-editor.org/rfc/rfc7159#section-6
    //     - https://docs.oracle.com/javase/7/docs/api/java/lang/Double.html#toString(double)
    //     - https://docs.oracle.com/javase/7/docs/api/java/lang/Long.html#toString(long)
    //
    // This gives us the big benefit of not having to include a JSON library such as jackson or
    // GSON:
    //
    // - These libraries add a non-trivial amount of code to the customer's Java process
    //   (Jackson: ~2MiB), affecting available memory. Considering that the smallest dyno only
    //   provides 512MB of RAM, adding 2MB is significant.
    // - Since these libraries handle much more than dumb writing of JSON, the attack surface is
    //   larger, and we'd have to deal with security issues from time to time in these libraries.
    // - They would pollute the classpath, requiring us to add additional build configuration that
    //   "shades" these libraries into another package to avoid clashes with customer code.
    StringBuilder stringBuilder = new StringBuilder();

    // Start: Root JSON object
    stringBuilder.append("{");

    // Start: Gauges JSON object
    stringBuilder.append("\"gauges\": {");

    stringBuilder.append("\"jvm_memory_bytes_used.area_heap\":");
    stringBuilder.append(metrics.getHeapUsedBytes());

    stringBuilder.append(",");
    stringBuilder.append("\"jvm_memory_bytes_used.area_nonheap\":");
    stringBuilder.append(metrics.getNonHeapUsedBytes());

    stringBuilder.append(",");
    stringBuilder.append("\"jvm_memory_bytes_committed.area_heap\":");
    stringBuilder.append(metrics.getHeapCommittedBytes());

    stringBuilder.append(",");
    stringBuilder.append("\"jvm_memory_bytes_committed.area_nonheap\":");
    stringBuilder.append(metrics.getNonHeapCommittedBytes());

    stringBuilder.append(",");
    stringBuilder.append("\"jvm_buffer_pool_bytes_used.name_direct\":");
    stringBuilder.append(metrics.getDirectBufferPoolUsedBytes());

    stringBuilder.append(",");
    stringBuilder.append("\"jvm_buffer_pool_bytes_used.name_mapped\":");
    stringBuilder.append(metrics.getMappedBufferPoolUsedBytes());

    // End: Gauges JSON object
    stringBuilder.append("}");

    // Start: Counters JSON object
    stringBuilder.append(",");
    stringBuilder.append("\"counters\": {");

    stringBuilder.append("\"jvm_gc_collection_seconds_count.gc_all\":");

    // GC metrics are serialized as a delta between the last metrics and now:
    if (previousMetrics != null) {
      stringBuilder.append(
          Math.max(
              0,
              metrics.getTotalGcCollectionCount() - previousMetrics.getTotalGcCollectionCount()));
    } else {
      stringBuilder.append(metrics.getTotalGcCollectionCount());
    }

    for (Map.Entry<HerokuJvmMetrics.Gc, HerokuJvmMetrics.GcMetrics> gcMetrics :
        metrics.getGcMetrics().entrySet()) {
      String stringGcName = null;
      switch (gcMetrics.getKey()) {
        case PsScavenge:
          stringGcName = "PS_Scavenge";
          break;
        case PsMarkSweep:
          stringGcName = "PS_MarkSweep";
          break;
        case G1OldGeneration:
          stringGcName = "G1_Old_Generation";
          break;
        case G1YoungGeneration:
          stringGcName = "G1_Young_Generation";
          break;
        case ConcurrentMarkSweep:
          stringGcName = "ConcurrentMarkSweep";
          break;
        case ParNew:
          stringGcName = "ParNew";
          break;
        default:
          // LOG
      }

      // GC metrics are serialized as deltas between the last metrics and now:
      long reportedCollectionCount = 0;
      double reportedCollectionTimeInSeconds = 0.0;
      if (previousMetrics == null) {
        reportedCollectionCount = gcMetrics.getValue().getCollectionCount();
        reportedCollectionTimeInSeconds = gcMetrics.getValue().getCollectionTimeInSeconds();
      } else {
        HerokuJvmMetrics.GcMetrics previousGcMetrics =
            previousMetrics.getGcMetrics().get(gcMetrics.getKey());
        if (previousGcMetrics != null) {
          reportedCollectionCount =
              Math.max(
                  0,
                  gcMetrics.getValue().getCollectionCount()
                      - previousGcMetrics.getCollectionCount());
          reportedCollectionTimeInSeconds =
              Math.max(
                  0.0,
                  gcMetrics.getValue().getCollectionTimeInSeconds()
                      - previousGcMetrics.getCollectionTimeInSeconds());
        }
      }

      stringBuilder.append(",");
      stringBuilder
          .append("\"jvm_gc_collection_seconds_count.gc_")
          .append(stringGcName)
          .append("\":");
      stringBuilder.append(reportedCollectionCount);

      stringBuilder.append(",");
      stringBuilder
          .append("\"jvm_gc_collection_seconds_sum.gc_")
          .append(stringGcName)
          .append("\":");
      stringBuilder.append(reportedCollectionTimeInSeconds);
    }

    // End: Counters JSON object
    stringBuilder.append("}");

    // End: Root JSON object
    stringBuilder.append("}");

    return stringBuilder.toString();
  }

  private JsonSerializer() {}
}
