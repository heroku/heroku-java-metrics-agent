package com.heroku.agent.metrics;

import java.lang.management.*;
import java.util.HashMap;

public final class Collector {

  public static HerokuJvmMetrics collect() {
    HashMap<HerokuJvmMetrics.Gc, HerokuJvmMetrics.GcMetrics> gcMetrics = new HashMap<>();
    for (GarbageCollectorMXBean garbageCollectorMXBean :
        ManagementFactory.getGarbageCollectorMXBeans()) {

      HerokuJvmMetrics.GcMetrics gcm =
          new HerokuJvmMetrics.GcMetrics(
              garbageCollectorMXBean.getCollectionCount(),
              garbageCollectorMXBean.getCollectionTime() / 1000.0);

      switch (garbageCollectorMXBean.getName()) {
        case "PS Scavenge":
          gcMetrics.put(HerokuJvmMetrics.Gc.PsScavenge, gcm);
          break;
        case "PS MarkSweep":
          gcMetrics.put(HerokuJvmMetrics.Gc.PsMarkSweep, gcm);
          break;
        case "G1 Old Generation":
          gcMetrics.put(HerokuJvmMetrics.Gc.G1OldGeneration, gcm);
          break;
        case "G1 Young Generation":
          gcMetrics.put(HerokuJvmMetrics.Gc.G1YoungGeneration, gcm);
          break;
        case "ConcurrentMarkSweep":
          gcMetrics.put(HerokuJvmMetrics.Gc.ConcurrentMarkSweep, gcm);
          break;
        case "ParNew":
          gcMetrics.put(HerokuJvmMetrics.Gc.ParNew, gcm);
          break;
        default:
          Logger.logDebug(
              "collector",
              String.format(
                  "Unexpected GC name '%s'. Metrics of this GC will not be reported!",
                  garbageCollectorMXBean.getName()));
      }
    }

    final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    final MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
    final MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

    return new HerokuJvmMetrics(
        heapMemoryUsage.getUsed(),
        nonHeapMemoryUsage.getUsed(),
        heapMemoryUsage.getCommitted(),
        nonHeapMemoryUsage.getCommitted(),
        getBufferPoolMemoryUsed(Constants.BUFFER_POOL_NAME_DIRECT),
        getBufferPoolMemoryUsed(Constants.BUFFER_POOL_NAME_MAPPED),
        gcMetrics);
  }

  private static long getBufferPoolMemoryUsed(String name) {
    for (BufferPoolMXBean bufferPoolMXBean :
        ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
      if (bufferPoolMXBean.getName().equals(name)) {
        return bufferPoolMXBean.getMemoryUsed();
      }
    }

    Logger.logDebug(
        "collector",
        String.format("Buffer pool '%s' not found. Memory used will be reported as 0!", name));

    return 0;
  }

  private Collector() {}
}
