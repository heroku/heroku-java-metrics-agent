package com.heroku.prometheus.client;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exports metrics about JVM buffer pools.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *   new BufferPoolsExports().register();
 * }
 * </pre>
 * Example metrics being exported:
 * <pre>
 *   jvm_buffer_pool_bytes_used{name="direct"} 2000000
 *   jvm_buffer_pool_bytes_capacity{name="direct"} 200000
 *   jvm_buffer_pool_count{name="nonheap"} 20
 * </pre>
 */
public class BufferPoolsExports extends Collector {
  private final List<BufferPoolMXBean> poolBeans;

  public BufferPoolsExports() {
    this(ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class));
  }

  public BufferPoolsExports(List<BufferPoolMXBean> poolBeans) {
    this.poolBeans = poolBeans;
  }

  void addBufferPoolMetrics(List<MetricFamilySamples> sampleFamilies) {
    GaugeMetricFamily used = new GaugeMetricFamily(
        "jvm_buffer_pool_bytes_used",
        "Estimate of the memory (bytes) that the Java virtual machine is using for this buffer pool.",
        Collections.singletonList("name"));

    GaugeMetricFamily capacity = new GaugeMetricFamily(
        "jvm_buffer_pool_bytes_capacity",
        "Estimate of the total capacity (bytes) of the buffers in this pool.",
        Collections.singletonList("name"));

    CounterMetricFamily count = new CounterMetricFamily(
        "jvm_buffer_pool_count",
        "Estimate of the number of buffers in the pool",
        Collections.singletonList("name"));

    for (BufferPoolMXBean poolBean : this.poolBeans) {
      used.addMetric(Collections.singletonList(poolBean.getName()), poolBean.getMemoryUsed());
      capacity.addMetric(Collections.singletonList(poolBean.getName()), poolBean.getTotalCapacity());
      count.addMetric(Collections.singletonList(poolBean.getName()), poolBean.getCount());
    }

    sampleFamilies.add(used);
    sampleFamilies.add(capacity);
    sampleFamilies.add(count);
  }

  public List<MetricFamilySamples> collect() {
    List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
    addBufferPoolMetrics(mfs);
    return mfs;
  }
}