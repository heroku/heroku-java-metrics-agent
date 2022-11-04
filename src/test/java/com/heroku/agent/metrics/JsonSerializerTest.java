package com.heroku.agent.metrics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public final class JsonSerializerTest {

  @Test
  public void serializeTest() {
    Map<HerokuJvmMetrics.Gc, HerokuJvmMetrics.GcMetrics> gcMetrics = new HashMap<>();
    gcMetrics.put(HerokuJvmMetrics.Gc.G1OldGeneration, new HerokuJvmMetrics.GcMetrics(2, 3));
    gcMetrics.put(HerokuJvmMetrics.Gc.G1YoungGeneration, new HerokuJvmMetrics.GcMetrics(4, 5));

    String jsonString =
        JsonSerializer.serialize(new HerokuJvmMetrics(1, 2, 3, 4, 5, 6, gcMetrics), null);

    JsonObject json = new Gson().fromJson(jsonString, JsonObject.class);
    JsonObject gaugesJson = json.getAsJsonObject("gauges");
    assertThat(gaugesJson.get("jvm_memory_bytes_used.area_heap").getAsDouble(), is(1.0));
    assertThat(gaugesJson.get("jvm_memory_bytes_used.area_nonheap").getAsDouble(), is(2.0));
    assertThat(gaugesJson.get("jvm_memory_bytes_committed.area_heap").getAsDouble(), is(3.0));
    assertThat(gaugesJson.get("jvm_memory_bytes_committed.area_nonheap").getAsDouble(), is(4.0));
    assertThat(gaugesJson.get("jvm_buffer_pool_bytes_used.name_direct").getAsDouble(), is(5.0));
    assertThat(gaugesJson.get("jvm_buffer_pool_bytes_used.name_mapped").getAsDouble(), is(6.0));

    JsonObject countersJson = json.getAsJsonObject("counters");
    assertThat(countersJson.get("jvm_gc_collection_seconds_count.gc_all").getAsDouble(), is(6.0));
    assertThat(
        countersJson.get("jvm_gc_collection_seconds_count.gc_G1_Old_Generation").getAsDouble(),
        is(2.0));
    assertThat(
        countersJson.get("jvm_gc_collection_seconds_sum.gc_G1_Old_Generation").getAsDouble(),
        is(3.0));
    assertThat(
        countersJson.get("jvm_gc_collection_seconds_count.gc_G1_Young_Generation").getAsDouble(),
        is(4.0));
    assertThat(
        countersJson.get("jvm_gc_collection_seconds_sum.gc_G1_Young_Generation").getAsDouble(),
        is(5.0));
  }

  @Test
  public void serializeTestWithPreviousMetrics() {
    Map<HerokuJvmMetrics.Gc, HerokuJvmMetrics.GcMetrics> previousGcMetrics = new HashMap<>();
    previousGcMetrics.put(
        HerokuJvmMetrics.Gc.G1OldGeneration, new HerokuJvmMetrics.GcMetrics(1, 2));
    previousGcMetrics.put(
        HerokuJvmMetrics.Gc.G1YoungGeneration, new HerokuJvmMetrics.GcMetrics(3, 4));
    HerokuJvmMetrics previousMetrics =
        new HerokuJvmMetrics(10, 20, 30, 40, 50, 60, previousGcMetrics);

    Map<HerokuJvmMetrics.Gc, HerokuJvmMetrics.GcMetrics> gcMetrics = new HashMap<>();
    gcMetrics.put(HerokuJvmMetrics.Gc.G1OldGeneration, new HerokuJvmMetrics.GcMetrics(2, 3));
    gcMetrics.put(HerokuJvmMetrics.Gc.G1YoungGeneration, new HerokuJvmMetrics.GcMetrics(4, 5));
    HerokuJvmMetrics metrics = new HerokuJvmMetrics(1, 2, 3, 4, 5, 6, gcMetrics);

    String jsonString = JsonSerializer.serialize(metrics, previousMetrics);

    JsonObject json = new Gson().fromJson(jsonString, JsonObject.class);
    JsonObject gaugesJson = json.getAsJsonObject("gauges");
    assertThat(gaugesJson.get("jvm_memory_bytes_used.area_heap").getAsDouble(), is(1.0));
    assertThat(gaugesJson.get("jvm_memory_bytes_used.area_nonheap").getAsDouble(), is(2.0));
    assertThat(gaugesJson.get("jvm_memory_bytes_committed.area_heap").getAsDouble(), is(3.0));
    assertThat(gaugesJson.get("jvm_memory_bytes_committed.area_nonheap").getAsDouble(), is(4.0));
    assertThat(gaugesJson.get("jvm_buffer_pool_bytes_used.name_direct").getAsDouble(), is(5.0));
    assertThat(gaugesJson.get("jvm_buffer_pool_bytes_used.name_mapped").getAsDouble(), is(6.0));

    // Counters must be serialized as deltas:
    JsonObject countersJson = json.getAsJsonObject("counters");
    assertThat(countersJson.get("jvm_gc_collection_seconds_count.gc_all").getAsDouble(), is(2.0));
    assertThat(
        countersJson.get("jvm_gc_collection_seconds_count.gc_G1_Old_Generation").getAsDouble(),
        is(1.0));
    assertThat(
        countersJson.get("jvm_gc_collection_seconds_sum.gc_G1_Old_Generation").getAsDouble(),
        is(1.0));
    assertThat(
        countersJson.get("jvm_gc_collection_seconds_count.gc_G1_Young_Generation").getAsDouble(),
        is(1.0));
    assertThat(
        countersJson.get("jvm_gc_collection_seconds_sum.gc_G1_Young_Generation").getAsDouble(),
        is(1.0));
  }
}
