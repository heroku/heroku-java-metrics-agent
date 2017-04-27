package com.heroku.prometheus.client;


import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.management.BufferPoolMXBean;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class BufferPoolsExportsTest {

  private BufferPoolMXBean mockPoolsBean1 = Mockito.mock(BufferPoolMXBean.class);
  private BufferPoolMXBean mockPoolsBean2 = Mockito.mock(BufferPoolMXBean.class);
  private List<BufferPoolMXBean> mockList = Arrays.asList(mockPoolsBean1, mockPoolsBean2);
  private CollectorRegistry registry = new CollectorRegistry();
  private BufferPoolsExports collectorUnderTest;

  @Before
  public void setUp() {
    when(mockPoolsBean1.getName()).thenReturn("direct");
    when(mockPoolsBean1.getTotalCapacity()).thenReturn(200000L);
    when(mockPoolsBean1.getMemoryUsed()).thenReturn(100000L);
    when(mockPoolsBean1.getCount()).thenReturn(3L);
    when(mockPoolsBean2.getName()).thenReturn("mapped");
    when(mockPoolsBean2.getTotalCapacity()).thenReturn(0L);
    when(mockPoolsBean2.getMemoryUsed()).thenReturn(0L);
    when(mockPoolsBean2.getCount()).thenReturn(0L);
    collectorUnderTest = new BufferPoolsExports(mockList).register(registry);
  }

  @Test
  public void testMemoryPools() {
    assertEquals(
        100000L,
        registry.getSampleValue(
            "jvm_buffer_pool_bytes_used",
            new String[]{"name"},
            new String[]{"direct"}),
        .0000001);
    assertEquals(
        200000L,
        registry.getSampleValue(
            "jvm_buffer_pool_bytes_capacity",
            new String[]{"name"},
            new String[]{"direct"}),
        .0000001);
    assertEquals(
        3L,
        registry.getSampleValue(
            "jvm_buffer_pool_count",
            new String[]{"name"},
            new String[]{"direct"}),
        .0000001);
    assertEquals(
        0L,
        registry.getSampleValue(
            "jvm_buffer_pool_bytes_used",
            new String[]{"name"},
            new String[]{"mapped"}),
        .0000001);
    assertEquals(
        0L,
        registry.getSampleValue(
            "jvm_buffer_pool_bytes_capacity",
            new String[]{"name"},
            new String[]{"mapped"}),
        .0000001);
    assertEquals(
        0L,
        registry.getSampleValue(
            "jvm_buffer_pool_count",
            new String[]{"name"},
            new String[]{"mapped"}),
        .0000001);
  }
}