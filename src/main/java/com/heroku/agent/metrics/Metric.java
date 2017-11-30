package com.heroku.agent.metrics;

public class Metric {

  private String key;

  private Double value;

  public Metric(String key, Double value) {
    this.key = key;
    this.value = value;
  }

  public Double getValue() {
    return value;
  }

  public Double getDerivedValue(Metric previousMetric) {
    return previousMetric == null || this.value < previousMetric.getValue() ?
      this.value :
      this.value - previousMetric.getValue();
  }

  public String getKey() {
    return key;
  }
}
