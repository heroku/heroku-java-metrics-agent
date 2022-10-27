package com.heroku.agent.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class HerokuJvmMetrics {
    private final long heapUsedBytes;
    private final long nonHeapUsedBytes;
    private final long heapCommittedBytes;
    private final long nonHeapCommittedBytes;
    private final long directBufferPoolUsedBytes;
    private final long mappedBufferPoolUsedBytes;
    private final Map<Gc, GcMetrics> gcMetrics;

    public HerokuJvmMetrics(long heapUsedBytes, long nonHeapUsedBytes, long heapCommittedBytes, long nonHeapCommittedBytes, long directBufferPoolUsedBytes, long mappedBufferPoolUsedBytes, Map<Gc, GcMetrics> gcMetrics) {
        this.heapUsedBytes = heapUsedBytes;
        this.nonHeapUsedBytes = nonHeapUsedBytes;
        this.heapCommittedBytes = heapCommittedBytes;
        this.nonHeapCommittedBytes = nonHeapCommittedBytes;
        this.directBufferPoolUsedBytes = directBufferPoolUsedBytes;
        this.mappedBufferPoolUsedBytes = mappedBufferPoolUsedBytes;
        this.gcMetrics = new HashMap<>(gcMetrics);
    }

    public long getHeapUsedBytes() {
        return heapUsedBytes;
    }

    public long getNonHeapUsedBytes() {
        return nonHeapUsedBytes;
    }

    public long getHeapCommittedBytes() {
        return heapCommittedBytes;
    }

    public long getNonHeapCommittedBytes() {
        return nonHeapCommittedBytes;
    }

    public long getDirectBufferPoolUsedBytes() {
        return directBufferPoolUsedBytes;
    }

    public long getMappedBufferPoolUsedBytes() {
        return mappedBufferPoolUsedBytes;
    }

    public Map<Gc, GcMetrics> getGcMetrics() {
        return Collections.unmodifiableMap(gcMetrics);
    }

    public long getTotalGcCollectionCount() {
        long totalCollectionCount = 0;

        for (GcMetrics gcMetrics : this.getGcMetrics().values()) {
            totalCollectionCount += gcMetrics.collectionCount;
        }

        return totalCollectionCount;
    }

    public static final class GcMetrics {
        private final long collectionCount;
        private final double collectionTimeInSeconds;

        public GcMetrics(long collectionCount, double collectionTimeInSeconds) {
            this.collectionCount = collectionCount;
            this.collectionTimeInSeconds = collectionTimeInSeconds;
        }

        public long getCollectionCount() {
            return collectionCount;
        }

        public double getCollectionTimeInSeconds() {
            return collectionTimeInSeconds;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GcMetrics gcMetrics = (GcMetrics) o;
            return collectionCount == gcMetrics.collectionCount && Double.compare(gcMetrics.collectionTimeInSeconds, collectionTimeInSeconds) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(collectionCount, collectionTimeInSeconds);
        }

        @Override
        public String toString() {
            return "GcMetrics{" +
                    "collectionCount=" + collectionCount +
                    ", collectionTimeInSeconds=" + collectionTimeInSeconds +
                    '}';
        }
    }

    public enum Gc {
        PsScavenge,
        PsMarkSweep,
        G1OldGeneration,
        G1YoungGeneration,
        ConcurrentMarkSweep,
        ParNew
    }
}
