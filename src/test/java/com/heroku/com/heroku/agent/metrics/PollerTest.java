package com.heroku.com.heroku.agent.metrics;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heroku.agent.metrics.Poller;
import com.heroku.prometheus.client.BufferPoolsExports;
import io.prometheus.client.hotspot.DefaultExports;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joe Kutner on 11/6/17.
 *         Twitter: @codefinger
 */
public class PollerTest {

  @Before
  public void setup() {
    DefaultExports.initialize();
    new BufferPoolsExports().register();
  }

  @Test
  public void testMemoryPools() throws Throwable {
    Poller p = new Poller();

    final CountDownLatch latch = new CountDownLatch(1);
    TestCallback callback = new TestCallback() {
      @Override
      public void apply(ObjectMapper mapper, ObjectNode metricsJson) {
        try {
          JsonNode gaugesNode = metricsJson.get("gauges");
          assertNotNull(gaugesNode);
          assertTrue(gaugesNode.has("jvm_memory_bytes_used.area_heap"));
          assertTrue(gaugesNode.has("jvm_buffer_pool_bytes_capacity.name_direct"));

          JsonNode countersNode = metricsJson.get("counters");
          assertNotNull(countersNode);
          assertTrue(countersNode.has("jvm_gc_collection_seconds_count.gc_PS_Scavenge"));
          assertTrue(countersNode.has("jvm_buffer_pool_count.name_direct"));
        } catch (Throwable t) {
          this.throwable = t;
        } finally {
          latch.countDown();
        }
      }
    };
    p.poll(callback);
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    p.cancel();

    if (callback.hasThrowable()) throw callback.throwable;
  }

  static abstract class TestCallback extends Poller.Callback {

    Throwable throwable = null;

    Boolean hasThrowable() {
      return this.throwable != null;
    }
  }
}
