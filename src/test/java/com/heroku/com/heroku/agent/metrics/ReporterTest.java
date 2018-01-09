package com.heroku.com.heroku.agent.metrics;

import com.heroku.agent.metrics.Reporter;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReporterTest {

  @Test
  public void testSendReport() throws IOException {
    URL herokuMetricsUrl = Mockito.mock(URL.class);
    HttpURLConnection conn = Mockito.mock(HttpURLConnection.class);
    Reporter reporter = new Reporter(herokuMetricsUrl);

    Mockito.when(herokuMetricsUrl.openConnection()).thenReturn(conn);
    Mockito.when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    Mockito.when(conn.getResponseCode()).thenReturn(200);

    assertTrue(reporter.report("{}"));
  }

  @Test
  public void testSendReportFail() throws IOException {
    URL herokuMetricsUrl = Mockito.mock(URL.class);
    HttpURLConnection conn = Mockito.mock(HttpURLConnection.class);
    Reporter reporter = new Reporter(herokuMetricsUrl);

    Mockito.when(herokuMetricsUrl.openConnection()).thenReturn(conn);
    Mockito.when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    Mockito.when(conn.getResponseCode()).thenReturn(500);

    assertFalse(reporter.report("{}"));
  }

  @Test
  public void testSendReportCurl() throws IOException {
    URL herokuMetricsUrl = Mockito.mock(URL.class);
    HttpURLConnection conn = Mockito.mock(HttpURLConnection.class);
    Reporter reporter = new Reporter(herokuMetricsUrl);

    Mockito.when(herokuMetricsUrl.openConnection()).thenReturn(conn);
    Mockito.when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    Mockito.when(conn.getResponseCode()).thenThrow(new RuntimeException("mock exception"));
    Mockito.when(herokuMetricsUrl.toString()).thenReturn("https://httpbin.org/post");

    assertTrue(reporter.report("{}"));
  }

  @Test(expected = IOException.class)
  public void testSendReportCurlErrors() throws IOException {
    URL herokuMetricsUrl = Mockito.mock(URL.class);
    HttpURLConnection conn = Mockito.mock(HttpURLConnection.class);
    Reporter reporter = new Reporter(herokuMetricsUrl);

    Mockito.when(herokuMetricsUrl.openConnection()).thenReturn(conn);
    Mockito.when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    Mockito.when(conn.getResponseCode()).thenThrow(new RuntimeException("mock exception"));
    Mockito.when(herokuMetricsUrl.toString()).thenReturn("8as7fy8asdfiahusdfiu");

    reporter.report("{}");
  }
}
