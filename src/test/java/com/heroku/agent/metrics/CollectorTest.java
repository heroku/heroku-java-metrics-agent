package com.heroku.agent.metrics;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public final class CollectorTest {
    @Test
    public void test() {
        MemoryMXBean mockedMemoryMxBean = Mockito.mock(MemoryMXBean.class);
        Mockito.when(mockedMemoryMxBean.getHeapMemoryUsage()).thenReturn(new MemoryUsage(0, 1, 2, 3));
        Mockito.when(mockedMemoryMxBean.getNonHeapMemoryUsage()).thenReturn(new MemoryUsage(4, 5, 6, 7));

        List<BufferPoolMXBean> mockedBufferPoolMxBeans = new ArrayList<>();

        BufferPoolMXBean mockedMappedBufferPoolMxBean = Mockito.mock(BufferPoolMXBean.class);
        Mockito.when(mockedMappedBufferPoolMxBean.getName()).thenReturn("mapped");
        Mockito.when(mockedMappedBufferPoolMxBean.getMemoryUsed()).thenReturn(8L);
        mockedBufferPoolMxBeans.add(mockedMappedBufferPoolMxBean);

        BufferPoolMXBean mockedDirectBufferPoolMxBean = Mockito.mock(BufferPoolMXBean.class);
        Mockito.when(mockedDirectBufferPoolMxBean.getName()).thenReturn("direct");
        Mockito.when(mockedDirectBufferPoolMxBean.getMemoryUsed()).thenReturn(9L);
        mockedBufferPoolMxBeans.add(mockedDirectBufferPoolMxBean);

        List<GarbageCollectorMXBean> mockedGarbageCollectorMXBeans = new ArrayList<>();

        {
            GarbageCollectorMXBean mockedGarbageCollectorMxBean = Mockito.mock(GarbageCollectorMXBean.class);
            Mockito.when(mockedGarbageCollectorMxBean.getName()).thenReturn("G1 Old Generation");
            Mockito.when(mockedGarbageCollectorMxBean.getCollectionCount()).thenReturn(10L);
            Mockito.when(mockedGarbageCollectorMxBean.getCollectionTime()).thenReturn(100L);
            mockedGarbageCollectorMXBeans.add(mockedGarbageCollectorMxBean);
        }

        {
            GarbageCollectorMXBean mockedGarbageCollectorMxBean = Mockito.mock(GarbageCollectorMXBean.class);
            Mockito.when(mockedGarbageCollectorMxBean.getName()).thenReturn("G1 Young Generation");
            Mockito.when(mockedGarbageCollectorMxBean.getCollectionCount()).thenReturn(20L);
            Mockito.when(mockedGarbageCollectorMxBean.getCollectionTime()).thenReturn(200L);
            mockedGarbageCollectorMXBeans.add(mockedGarbageCollectorMxBean);
        }

        try (MockedStatic<ManagementFactory> mockedStatic = Mockito.mockStatic(ManagementFactory.class)) {
            mockedStatic
                    .when(ManagementFactory::getMemoryMXBean)
                    .thenReturn(mockedMemoryMxBean);

            mockedStatic
                    .when(() -> ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class))
                    .thenReturn(mockedBufferPoolMxBeans);

            mockedStatic
                    .when(ManagementFactory::getGarbageCollectorMXBeans)
                    .thenReturn(mockedGarbageCollectorMXBeans);

            HerokuJvmMetrics metricsNg = Collector.collect();

            assertThat(metricsNg.getHeapUsedBytes(), is(1L));
            assertThat(metricsNg.getHeapCommittedBytes(), is(2L));
            assertThat(metricsNg.getNonHeapUsedBytes(), is(5L));
            assertThat(metricsNg.getNonHeapCommittedBytes(), is(6L));
            assertThat(metricsNg.getDirectBufferPoolUsedBytes(), is(9L));
            assertThat(metricsNg.getMappedBufferPoolUsedBytes(), is(8L));

            assertThat(metricsNg.getGcMetrics().get(HerokuJvmMetrics.Gc.G1OldGeneration), is(equalTo(new HerokuJvmMetrics.GcMetrics(10, 0.1))));
            assertThat(metricsNg.getGcMetrics().get(HerokuJvmMetrics.Gc.G1YoungGeneration), is(equalTo(new HerokuJvmMetrics.GcMetrics(20, 0.2))));
            assertThat(metricsNg.getTotalGcCollectionCount(), is(30L));
        }
    }
}
